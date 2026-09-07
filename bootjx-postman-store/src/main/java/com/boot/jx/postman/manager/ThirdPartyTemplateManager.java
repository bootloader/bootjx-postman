package com.boot.jx.postman.manager;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.boot.jx.api.ApiFieldError;
import com.boot.jx.api.ApiResponseUtil;
import com.boot.jx.logger.AuditDetailProvider;
import com.boot.jx.mongo.CommonMongoQB.MongoQueryBuilder;
import com.boot.jx.mongo.CommonMongoTemplate;
import com.boot.jx.postman.PMConstants.CHANNEL_TYPE;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.PMEnvironment.PMClientConfig;
import com.boot.jx.postman.PMEnvironment.PMConfigurationObject;
import com.boot.jx.postman.channel.ChannelClientFactory;
import com.boot.jx.postman.channel.ChannelClientFactory.ChannelClient;
import com.boot.jx.postman.doc.HSMTemplate3rdParty;
import com.boot.jx.postman.doc.HSMTemplateDoc;
import com.boot.jx.postman.doc.tpo.WABAFlows;
import com.boot.jx.postman.model.MessageDefinitions.Contactable;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.store.TemplateStore;
import com.boot.jx.postman.wa360.WA360Client;
import com.boot.jx.postman.wa360.WA360Template;
import com.boot.jx.postman.wacfb.WacfbClient;
import com.boot.jx.utils.PostManUtil;
import com.boot.model.MapModel;
import com.boot.utils.ArgUtil;
import com.boot.utils.Constants;
import com.boot.utils.CryptoUtil;
import com.boot.utils.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ThirdPartyTemplateManager {

	@Autowired
	private WA360Client wa360Client;

	@Autowired
	private WacfbClient wacfbClient;

	@Autowired
	private ChannelClientFactory clientFactory;

	@Autowired
	private CommonMongoTemplate commonMongoTemplate;

	@Autowired
	private PMEnvironment pmEnvironment;

	@Autowired
	private PMClientConfig pmClientConfig;

	@Lazy
	@Autowired
	private AuditDetailProvider auditDetailProvider;

	@Autowired
	private TemplateStore templateStore;

	private static final Logger LOGGER = LoggerFactory.getLogger(ThirdPartyTemplateManager.class);
	private static final Pattern NUMERIC_VAR_PATTERN = Pattern.compile("\\{\\{(\\d+)\\}\\}");
	private static final String RCS_DEFAULT_LANG = "default";

	public void refreshThirdPartyTemplates(ChannelConfig channelConfig) {
		if (CHANNEL_TYPE.RCS_TELINFY.equalsIgnoreCase(channelConfig.getChannelType())) {
			refreshRCSTemplates(channelConfig);
			return;
		}
		refreshWA360Templates(channelConfig);
	}

	public void refreshRCSTemplates(ChannelConfig channelConfig) {
		ChannelClient channelClient = clientFactory.get(channelConfig);
		if (!ArgUtil.is(channelClient)) {
			LOGGER.warn("No channel client registered for RCS channel {}", channelConfig.getChannelId());
			return;
		}

		MapModel resp = channelClient.fetchTemplates(channelConfig);
		List<Map<String, Object>> rcsTemplates = parseRCSTemplateEntries(resp);

		for (Map<String, Object> rcsTemplate : rcsTemplates) {
			String templateName = ArgUtil.parseAsString(rcsTemplate.get("name"), Constants.BLANK);
			if (!ArgUtil.is(templateName)) {
				continue;
			}
			String status = ArgUtil.nonEmpty(ArgUtil.parseAsString(rcsTemplate.get("status"), Constants.BLANK),
					"approved");

			HSMTemplate3rdParty thirdPartyTemplate = toHSM3rdParty(channelConfig, templateName, status, rcsTemplate);
			commonMongoTemplate.save(thirdPartyTemplate);
			linkRefresh(thirdPartyTemplate, thirdPartyTemplate.getHsmTemplateId(), status);
		}
		templateStore.publishUpdate();
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> parseRCSTemplateEntries(MapModel resp) {
		List<Map<String, Object>> parsed = new ArrayList<Map<String, Object>>();
		if (!ArgUtil.is(resp)) {
			return parsed;
		}

		Object[] candidateKeys = new Object[] { "templates", "template", "data", "rcs_templates", "pendingTemplates" };
		for (Object keyObj : candidateKeys) {
			String key = ArgUtil.parseAsString(keyObj);
			Object value = resp.get(key);
			collectRCSTemplateEntries(value, parsed);
			if (!parsed.isEmpty()) {
				return parsed;
			}
		}

		Object rootList = resp.get("items");
		collectRCSTemplateEntries(rootList, parsed);
		return parsed;
	}

	@SuppressWarnings("unchecked")
	private void collectRCSTemplateEntries(Object value, List<Map<String, Object>> parsed) {
		if (!ArgUtil.is(value)) {
			return;
		}

		if (value instanceof List<?>) {
			for (Object item : (List<?>) value) {
				addRCSTemplateEntry(item, parsed);
			}
			return;
		}

		if (value instanceof Map<?, ?>) {
			Map<String, Object> map = (Map<String, Object>) value;
			if (looksLikeRCSTemplateMap(map)) {
				addRCSTemplateEntry(map, parsed);
				return;
			}
			for (Object nested : map.values()) {
				collectRCSTemplateEntries(nested, parsed);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void addRCSTemplateEntry(Object item, List<Map<String, Object>> parsed) {
		if (item instanceof String) {
			String name = ArgUtil.parseAsString(item, Constants.BLANK);
			if (ArgUtil.is(name)) {
				Map<String, Object> template = new HashMap<String, Object>();
				template.put("name", name);
				template.put("status", "approved");
				parsed.add(template);
			}
			return;
		}

		if (!(item instanceof Map<?, ?>)) {
			return;
		}

		Map<String, Object> source = (Map<String, Object>) item;
		String name = ArgUtil.nonEmpty(ArgUtil.parseAsString(source.get("name"), Constants.BLANK),
				ArgUtil.parseAsString(source.get("templateName"), Constants.BLANK),
				ArgUtil.parseAsString(source.get("code"), Constants.BLANK),
				ArgUtil.parseAsString(source.get("template"), Constants.BLANK),
				ArgUtil.parseAsString(source.get("data"), Constants.BLANK));
		if (!ArgUtil.is(name)) {
			return;
		}

		Map<String, Object> template = new HashMap<String, Object>(source);
		template.put("name", name);
		if (!ArgUtil.is(template.get("status"))) {
			template.put("status", "approved");
		}
		parsed.add(template);
	}

	private boolean looksLikeRCSTemplateMap(Map<String, Object> map) {
		return ArgUtil.is(map.get("name")) || ArgUtil.is(map.get("templateName")) || ArgUtil.is(map.get("code"))
				|| ArgUtil.is(map.get("template")) || ArgUtil.is(map.get("data"));
	}

	private HSMTemplate3rdParty toHSM3rdParty(ChannelConfig channelConfig, String templateName, String status,
			Map<String, Object> rawTemplate) {
		String id = String.format("%s/%s/%s", channelConfig.getChannelId(), templateName, RCS_DEFAULT_LANG);

		HSMTemplate3rdParty thirdPartyTemplate = commonMongoTemplate.findById(id, HSMTemplate3rdParty.class);
		if (!ArgUtil.is(thirdPartyTemplate)) {
			thirdPartyTemplate = new HSMTemplate3rdParty();
			thirdPartyTemplate.setId(id);
		}

		Map<String, Object> template = ArgUtil.is(rawTemplate) ? new HashMap<String, Object>(rawTemplate)
				: new HashMap<String, Object>();
		template.put("name", templateName);
		template.put("status", status);

		thirdPartyTemplate.setChannelId(channelConfig.getChannelId());
		thirdPartyTemplate.setCode(templateName);
		thirdPartyTemplate.setLang(RCS_DEFAULT_LANG);
		thirdPartyTemplate.setContactType(ArgUtil.parseAsString(channelConfig.getContactType()));
		thirdPartyTemplate.setChannelType(channelConfig.getChannelType());
		thirdPartyTemplate.setTemplate(template);
		return thirdPartyTemplate;
	}

	public void refreshWA360Templates(ChannelConfig channelConfig) {
		MapModel resp = null;

		ChannelClient channelClient = clientFactory.get(channelConfig);

		resp = channelClient.fetchTemplates(channelConfig);

		if (resp.containsKey("data")) {
			Object value = resp.get("data");
			resp.remove("data");
			resp.put("waba_templates", value);
		}

		List<WA360Template> wabaTemplates = resp.keyEntry("waba_templates").asList(WA360Template.class);

//		MongoQueryBuilder<HSMTemplate3rdParty> cmqb = MongoQueryBuilder.collection(HSMTemplate3rdParty.class)
//				.where(Criteria.where("channelId").is(channelConfig.getChannelId())).set("template.status", "deleted");
//
//		commonMongoTemplate.update(cmqb);
//		templateStore.publishUpdate();

		for (WA360Template wa360Template : wabaTemplates) {

			HSMTemplate3rdParty thirdPartyTemplate = toHSM3rdParty(channelConfig, wa360Template);

			commonMongoTemplate.save(thirdPartyTemplate);
			linkRefresh(thirdPartyTemplate, thirdPartyTemplate.getHsmTemplateId(),
					ArgUtil.parseAsString(thirdPartyTemplate.getTemplate().get("status"), Constants.BLANK));
		}
	}

	/**
	 * Public method to convert WA360Template to HSMTemplate3rdParty Reuses the
	 * private toHSM3rdParty() method
	 * 
	 * @param channelConfig Channel configuration
	 * @param wa360Template WA360 template from Meta API
	 * @return HSMTemplate3rdParty instance
	 */
	public HSMTemplate3rdParty convertToHSM3rdParty(ChannelConfig channelConfig, WA360Template wa360Template) {
		return toHSM3rdParty(channelConfig, wa360Template);
	}

	private HSMTemplate3rdParty toHSM3rdParty(ChannelConfig channelConfig, WA360Template wa360Template) {

		String id = String.format("%s/%s/%s", channelConfig.getChannelId(), wa360Template.getName(),
				wa360Template.getLanguage());

		HSMTemplate3rdParty thirdPartyTemplate = commonMongoTemplate.findById(id, HSMTemplate3rdParty.class);

		if (!ArgUtil.is(thirdPartyTemplate)) {
			thirdPartyTemplate = new HSMTemplate3rdParty();
			thirdPartyTemplate.setId(id);
		}

		thirdPartyTemplate.setChannelId(channelConfig.getChannelId());
		thirdPartyTemplate.setCode(wa360Template.getName());
		thirdPartyTemplate.setLang(wa360Template.getLanguage());

		// thirdPartyTemplate.setCategory(wa360Template.getCategory());
		thirdPartyTemplate.setContactType(ArgUtil.parseAsString(channelConfig.getContactType()));
		thirdPartyTemplate.setChannelType(channelConfig.getChannelType());

		thirdPartyTemplate.setTemplate(JsonUtil.toMap(wa360Template));
		return thirdPartyTemplate;
	}

	public HSMTemplate3rdParty createhWA360Templates(ChannelConfig channelConfig,
			Map<String, Object> templateStructure) {

		// Process URLs for Meta approval and store original URLs
		Map<String, Object> originalUrls = processUrlsForApproval(templateStructure, channelConfig);

		// Send to Meta API
		MapModel resp = sendToMetaAPI(channelConfig, templateStructure);

		// --- SAFETY CHECK ADDED HERE ---
		if (resp == null || "FAILED".equals(resp.get("status"))) {
			// If Meta rejected the upload or threw an error, stop processing to prevent a
			// NullPointerException
			return null;
		}

		// Convert response to our format
		WA360Template wa360Template = resp.as(WA360Template.class);
		HSMTemplate3rdParty result = toHSM3rdParty(channelConfig, wa360Template);

		// Store original URLs directly in localDetails
		if (originalUrls != null && !originalUrls.isEmpty()) {
			result.setLocalDetails(originalUrls);
		}

		auditDetailProvider.auditCreate(result);
		commonMongoTemplate.save(result);
		templateStore.publishUpdate();
		return result;
	}

	public HSMTemplate3rdParty deleteWA360Templates(ChannelConfig channelConfig, HSMTemplate3rdParty temp) {
		WA360Template x = JsonUtil.toObject(temp.getTemplate(), WA360Template.class);
		if (ArgUtil.is(x)) {
			if (!"deleted".equalsIgnoreCase(x.getStatus())) {
				wacfbClient.deleteTemplates(channelConfig, x.getName());

				String deletedBy = auditDetailProvider.getAuditUser();
				Long deletedStamp = System.currentTimeMillis();
				MongoQueryBuilder<HSMTemplate3rdParty> updateDeleteAudit = MongoQueryBuilder
						.collection(HSMTemplate3rdParty.class).where(Criteria.where("_id").is(temp.getId()))
						.set("deletedBy", deletedBy).set("deletedStamp", deletedStamp)
						.set("deletedSource", "ADMIN_API");
				commonMongoTemplate.update(updateDeleteAudit);

				HSMTemplateDoc hsmDoc = commonMongoTemplate.findById(temp.getHsmTemplateId(), HSMTemplateDoc.class);
				if (hsmDoc != null) {
					hsmDoc.approved(channelConfig.getChannelId(), x.getName(), "deleted");
					templateStore.save(hsmDoc);
				} else {
					templateStore.publishUpdate();
				}

			}
			// commonMongoTemplate.remove(temp);
		}
		return temp;
	}

	public HSMTemplate3rdParty unarchiveWABATemplate(ChannelConfig channelConfig, HSMTemplate3rdParty temp) {
		if (!ArgUtil.is(temp) || !ArgUtil.is(temp.getTemplate())) {
			return temp;
		}
		if (!"wacfb".equalsIgnoreCase(channelConfig.getChannelType())) {
			ApiResponseUtil.throwInputException(new ApiFieldError().field("channelId").codeKey("UNSUPPORTED")
					.description("Unarchive is supported for WACFB channels only"));
		}

		String status = ArgUtil.parseAsString(temp.getTemplate().get("status"), Constants.BLANK);
		if (!"ARCHIVED".equalsIgnoreCase(status)) {
			ApiResponseUtil.throwInputException(new ApiFieldError().field("status").codeKey("INVALID_STATUS")
					.description("Only ARCHIVED templates can be unarchived. Current status: " + status));
		}

		// Meta numeric id (template.id) — required for unarchive; not hsmTemplateId or
		// Mongo _id
		String metaIdFromDoc = ArgUtil.parseAsString(temp.getTemplate().get("id"), Constants.BLANK);
		String metaIdFromLookup = Constants.BLANK;
		if (!ArgUtil.is(metaIdFromDoc)) {
			// template.id missing — fetch Meta id for this archived template (name + lang)
			String name = ArgUtil.nonEmpty(temp.getCode(),
					ArgUtil.parseAsString(temp.getTemplate().get("name"), Constants.BLANK));
			String lang = ArgUtil.nonEmpty(temp.getLang(),
					ArgUtil.parseAsString(temp.getTemplate().get("language"), Constants.BLANK));

			metaIdFromLookup = wacfbClient.resolveTemplateId(channelConfig, name, lang, "ARCHIVED");
			if (ArgUtil.is(metaIdFromLookup)) {
				temp.getTemplate().put("id", metaIdFromLookup);
				commonMongoTemplate.save(temp);
			}
		}
		final String metaTemplateId = ArgUtil.nonEmpty(metaIdFromDoc, metaIdFromLookup);
		if (!ArgUtil.is(metaTemplateId)) {
			ApiResponseUtil.throwInputException(new ApiFieldError().field("id").codeKey("META_ID_MISSING").description(
					"Meta template ID not found for archived template: " + temp.getCode() + " / " + temp.getLang()));
		}

		MapModel resp = wacfbClient.unarchiveTemplates(channelConfig, Arrays.asList(metaTemplateId));

		List<String> unarchived = resp.keyEntry("unarchived_templates").asList(String.class);
		boolean success = ArgUtil.is(unarchived)
				&& unarchived.stream().anyMatch(id -> ArgUtil.areEqual(id, metaTemplateId));
		if (!success) {
			Map<String, Object> failedMap = resp.keyEntry("failed_templates").asMap();
			String reason = failedMap != null ? ArgUtil.parseAsString(failedMap.get(metaTemplateId)) : Constants.BLANK;
			ApiResponseUtil.throwInputException(new ApiFieldError().field("id").codeKey("UNARCHIVE_FAILED")
					.description(ArgUtil.is(reason) ? reason : "Meta unarchive failed"));
		}

		String restoredStatus = "APPROVED";
		temp.getTemplate().put("status", restoredStatus);
		commonMongoTemplate.save(temp);

		if (ArgUtil.is(temp.getHsmTemplateId())) {
			HSMTemplateDoc hsmDoc = commonMongoTemplate.findById(temp.getHsmTemplateId(), HSMTemplateDoc.class);
			if (hsmDoc != null) {
				hsmDoc.approved(channelConfig.getChannelId(), temp.getHsmTemplateId(), restoredStatus);
				templateStore.save(hsmDoc);
			} else {
				templateStore.publishUpdate();
			}
		} else {
			templateStore.publishUpdate();
		}

		return temp;
	}

	public HSMTemplate3rdParty migrateWABATemplate(HSMTemplate3rdParty fromTemplate, String toChannelId) {
		Contactable channelInfo = PostManUtil.parseChannelId(toChannelId);
		HSMTemplate3rdParty newTemp = JsonUtil.deepCopy(fromTemplate, HSMTemplate3rdParty.class);
		newTemp.setChannelId(toChannelId);
		newTemp.setChannelType(channelInfo.getChannelType());
		newTemp.setContactType(channelInfo.getContactType());
		return newTemp;
	}

	public List<HSMTemplate3rdParty> migrateWABATemplate(String fromChannelId, String toChannelId) {
		MongoQueryBuilder<HSMTemplate3rdParty> q = MongoQueryBuilder.collection(HSMTemplate3rdParty.class)
				.where(Criteria.where("channelId").is(fromChannelId));
		List<HSMTemplate3rdParty> tmps = commonMongoTemplate.find(q);
		for (HSMTemplate3rdParty hsmTemplate3rdParty : tmps) {
			HSMTemplate3rdParty newTemp = migrateWABATemplate(hsmTemplate3rdParty, toChannelId);
			commonMongoTemplate.save(newTemp);
		}
		templateStore.publishUpdate();
		MongoQueryBuilder<HSMTemplate3rdParty> q2 = MongoQueryBuilder.collection(HSMTemplate3rdParty.class)
				.where(Criteria.where("channelId").is(toChannelId));

		return commonMongoTemplate.find(q2);
	}

	/*
	 * public HSMTemplate3rdParty migrateWABATemplate(HSMTemplate3rdParty
	 * fromTemplate, String toChannelId) { Contactable channelInfo =
	 * PostManUtil.parseChannelId(toChannelId); HSMTemplate3rdParty newTemp =
	 * JsonUtil.deepCopy(fromTemplate, HSMTemplate3rdParty.class);
	 * newTemp.setChannelId(toChannelId);
	 * newTemp.setChannelType(channelInfo.getChannelType());
	 * newTemp.setContactType(channelInfo.getContactType());
	 * 
	 * if (fromTemplate.getVarMap() != null) { newTemp.setVarMap(new
	 * HashMap<>(fromTemplate.getVarMap())); } else { newTemp.setVarMap(null); }
	 * 
	 * return newTemp; }
	 * 
	 * public List<HSMTemplate3rdParty> migrateWABATemplate(String fromChannelId,
	 * String toChannelId) { MongoQueryBuilder<HSMTemplate3rdParty> fromQuery =
	 * MongoQueryBuilder.collection(HSMTemplate3rdParty.class)
	 * .where(Criteria.where("channelId").is(fromChannelId));
	 * List<HSMTemplate3rdParty> fromTemplates =
	 * commonMongoTemplate.find(fromQuery);
	 * 
	 * MongoQueryBuilder<HSMTemplate3rdParty> toQuery =
	 * MongoQueryBuilder.collection(HSMTemplate3rdParty.class)
	 * .where(Criteria.where("channelId").is(toChannelId));
	 * List<HSMTemplate3rdParty> toTemplates = commonMongoTemplate.find(toQuery);
	 * 
	 * List<String> toTemplateIds = new ArrayList<>(); for (HSMTemplate3rdParty
	 * template : toTemplates) { toTemplateIds.add(template.getTemplate() + "_" +
	 * template.getLang()); }
	 * 
	 * for (HSMTemplate3rdParty fromTemplate : fromTemplates) { String
	 * fromTemplateIds = fromTemplate.getTemplate() + "_" + fromTemplate.getLang();
	 * 
	 * if (!toTemplateIds.contains(fromTemplateIds)) { HSMTemplate3rdParty newTemp =
	 * migrateWABATemplate(fromTemplate, toChannelId);
	 * commonMongoTemplate.save(newTemp); } } //if template is there not varmap-
	 * copy varmap
	 * 
	 * return commonMongoTemplate.find(toQuery); }
	 */
	public List<HSMTemplate3rdParty> getTemplates(ChannelConfig channelConfig, String code) {
		MongoQueryBuilder<HSMTemplate3rdParty> q = MongoQueryBuilder.collection(HSMTemplate3rdParty.class)
				.where(Criteria.where("channelId").is(channelConfig.getChannelId()));

		if (ArgUtil.is(code)) {
			q.where("code", code);
		}

		return commonMongoTemplate.find(q);
	}

	public List<HSMTemplate3rdParty> getTemplates(ChannelConfig channelConfig) {
		return this.getTemplates(channelConfig, null);
	}

	public List<WABAFlows> getFlows(ChannelConfig channelConfig, String code) {

		MongoQueryBuilder<WABAFlows> q = MongoQueryBuilder.collection(WABAFlows.class)
				.where(Criteria.where("wabaId").is(channelConfig.getWacfb().getWabaId()));

		if (ArgUtil.is(code)) {
			q.where("code", code);
		}

		return commonMongoTemplate.find(q);
	}

	public List<WABAFlows> getFlows(ChannelConfig channelConfig) {
		return this.getFlows(channelConfig, null);
	}

	public HSMTemplate3rdParty link(String thirdPartyTemplateId, String hsmTemplateId) {
		HSMTemplate3rdParty thirdPartyTemplate = commonMongoTemplate.findById(thirdPartyTemplateId,
				HSMTemplate3rdParty.class);
		String hsmTemplateIdOld = thirdPartyTemplate.getHsmTemplateId();
		thirdPartyTemplate.setHsmTemplateId(hsmTemplateId);
		auditDetailProvider.auditCreate(thirdPartyTemplate);

		commonMongoTemplate.save(thirdPartyTemplate);

		this.linkRefresh(thirdPartyTemplate, hsmTemplateIdOld, null);
		this.linkRefresh(thirdPartyTemplate, hsmTemplateId,
				ArgUtil.parseAsString(thirdPartyTemplate.getTemplate().get("status"), Constants.BLANK));
		templateStore.publishUpdate();
		return thirdPartyTemplate;
	}

	@SuppressWarnings("unchecked")
	private String getBodyText(Map<String, Object> wabaTemplate) {

		if (!ArgUtil.is(wabaTemplate)) {
			return null;
		}

		Object componentsObj = wabaTemplate.get("components");
		if (!(componentsObj instanceof List<?>)) {
			return null;
		}

		List<Map<String, Object>> components = (List<Map<String, Object>>) componentsObj;
		for (Map<String, Object> component : components) {

			if ("BODY".equalsIgnoreCase(ArgUtil.parseAsString(component.get("type")))) {
				return ArgUtil.parseAsString(component.get("text"));
			}
		}
		return null;
	}

	public HSMTemplate3rdParty linkRefresh(HSMTemplate3rdParty thirdPartyTemplate, String hsmTemplateId,
			String status) {

		if (ArgUtil.is(hsmTemplateId)) {
			HSMTemplateDoc hsmTemplateDoc = commonMongoTemplate.findById(hsmTemplateId, HSMTemplateDoc.class);

			if (ArgUtil.is(hsmTemplateDoc)) {
				
				Map<String, Object> wabaTemplate = thirdPartyTemplate.getTemplate();
				// Update WABA options
				hsmTemplateDoc.options().put("waba", wabaTemplate);
				
				// Meta does not provide categorySubType; derive CALL_PERMISSION from WABA
				// components when linking (Business Manager → sync → clone → link).
				if (ArgUtil.is(status) && hasCallPermissionRequest(wabaTemplate)) {
					hsmTemplateDoc.setCategorySubType("CALL_PERMISSION");
				}

				String metaBody = getBodyText(thirdPartyTemplate.getTemplate());

				String convertedBody = convertMetaPlaceholdersToLocal(metaBody, thirdPartyTemplate.getVarMap(),
						hsmTemplateDoc.getCode());

				if (convertedBody != null && !ArgUtil.areEqual(convertedBody, hsmTemplateDoc.getTemplate())) {
					hsmTemplateDoc.setTemplate(convertedBody);
				}

				// Always update approval status
				hsmTemplateDoc.approved(thirdPartyTemplate.getChannelId(), thirdPartyTemplate.getHsmTemplateId(),
						status);

				templateStore.save(hsmTemplateDoc);
			}
		}

		return thirdPartyTemplate;
	}
	
	/** True if Meta WABA template includes a CALL_PERMISSION_REQUEST component. */
	private boolean hasCallPermissionRequest(Map<String, Object> wabaTemplate) {
		if (!ArgUtil.is(wabaTemplate)) {
			return false;
		}
		for (Map<String, Object> component : MapModel.from(wabaTemplate).entry("components").asListOfMap()) {
			if ("CALL_PERMISSION_REQUEST".equalsIgnoreCase(ArgUtil.parseAsString(component.get("type")))) {
				return true;
			}
		}
		return false;
	}

	private String convertMetaPlaceholdersToLocal(String metaBody, Map<String, Object> varMap, String templateCode) {
		if (!ArgUtil.is(metaBody)) {
			return metaBody;
		}

		if (!NUMERIC_VAR_PATTERN.matcher(metaBody).find()) {
			return metaBody;
		}

		if (!ArgUtil.is(varMap)) {
			LOGGER.info("Skipping placeholder conversion for template [{}] as no variable mapping is available.",
					templateCode);
			return null;
		}

		Object bodyObj = varMap.get("body");
		if (!(bodyObj instanceof List<?>)) {
			LOGGER.info("Skipping placeholder conversion for template [{}] as body variable mapping is unavailable.",
					templateCode);
			return null;
		}

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> bodyMappings = (List<Map<String, Object>>) bodyObj;

		if (bodyMappings.isEmpty()) {
			LOGGER.info("Skipping placeholder conversion for template [{}] as body variable mapping is empty.",
					templateCode);
			return null;
		}

		final Map<String, String> variableLookup = new HashMap<>(bodyMappings.size());

		for (Map<String, Object> mapping : bodyMappings) {
			if (mapping == null) {
				continue;
			}

			String numVar = ArgUtil.parseAsString(mapping.get("numVar"));
			String path = ArgUtil.parseAsString(mapping.get("path"));

			if (ArgUtil.is(numVar) && ArgUtil.is(path)) {
				variableLookup.put(numVar, "{{" + path + "}}");
			}
		}

		if (variableLookup.isEmpty()) {
			LOGGER.info(
					"Skipping placeholder conversion for template [{}] as no valid body variable mappings were found.",
					templateCode);
			return null;
		}

		Matcher matcher = NUMERIC_VAR_PATTERN.matcher(metaBody);
		StringBuffer sb = new StringBuffer();

		while (matcher.find()) {
			String placeholder = matcher.group();
			String replacement = variableLookup.get(placeholder);

			if (replacement == null) {
				LOGGER.warn("Skipping HSM body sync for template [{}]. Missing mapping for placeholder [{}].",
						templateCode, placeholder);
				return null;
			}

			matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
		}

		matcher.appendTail(sb);
		return sb.toString();
	}

	public HSMTemplate3rdParty varMap(String thirdPartyTemplateId, Map<String, Object> varMap) {
		HSMTemplate3rdParty thirdPartyTemplate = commonMongoTemplate.findById(thirdPartyTemplateId,
				HSMTemplate3rdParty.class);
		thirdPartyTemplate.setVarMap(varMap);
		commonMongoTemplate.save(thirdPartyTemplate);
		templateStore.publishUpdate();
		return thirdPartyTemplate;
	}

	public void refreshWabaFlows(ChannelConfig channelConfig) {
		ChannelClient channelClient = clientFactory.get(channelConfig);
		MapModel resp = channelClient.listOfFlows(channelConfig);
		List<Map<String, Object>> flows = resp.entry("data").asListOfMap();

		for (Map<String, Object> flowData : flows) {
			String flowId = (String) flowData.get("id");

			String id = String.format("%s/%s", channelConfig.getWacfb().getWabaId(), flowId);
			WABAFlows flowDoc = commonMongoTemplate.findById(id, WABAFlows.class);
			if (!ArgUtil.is(flowDoc)) {
				flowDoc = new WABAFlows();
			}
			flowDoc.setWabaId(channelConfig.getWacfb().getWabaId());
			flowDoc.setId(id);
			flowDoc.setFlowId(id);
			flowDoc.setMeta(flowData);
			commonMongoTemplate.save(flowDoc);
			fetchAndSetScreenId(flowId, channelConfig);

		}

	}

	@Async
	public void fetchAndSetScreenId(String flowId, ChannelConfig channelConfig) {
		try {
			// Fetching flow assets
			ChannelClient channelClient = clientFactory.get(channelConfig);
			MapModel resp = channelClient.flowsAssets(flowId, channelConfig);

			List<Map<String, Object>> data = (List<Map<String, Object>>) resp.toMap().get("data");

			if (data != null && !data.isEmpty()) {
				String downloadUrl = (String) data.get(0).get("download_url");

				RestTemplate restTemplate = new RestTemplate();
				String jsonResponseString = restTemplate.getForObject(downloadUrl, String.class);

				ObjectMapper objectMapper = new ObjectMapper();
				Map<String, Object> jsonResponse = objectMapper.readValue(jsonResponseString,
						new TypeReference<Map<String, Object>>() {
						});

				List<Map<String, String>> fieldMe = fetchFieldMeta(jsonResponse);

				String id = String.format("%s/%s", channelConfig.getWacfb().getWabaId(), flowId);
				WABAFlows flow = commonMongoTemplate.findById(id, WABAFlows.class);

				if (flow != null) {
					flow.setJson(jsonResponse);
					flow.setFieldMeta(fieldMe);
					commonMongoTemplate.save(flow);
				}
			}
		} catch (Exception e) {
			System.out.print(e);
		}
	}

	public static List<Map<String, String>> fetchFieldMeta(Map<String, Object> jsonResponse) throws IOException {
		List<Map<String, String>> fieldMeta = new ArrayList<>();
		List<Map<String, Object>> screens = (List<Map<String, Object>>) jsonResponse.get("screens");

		if (screens != null) {
			for (int i = 0; i < screens.size(); i++) {
				Map<String, Object> screen = screens.get(i);

				boolean lastScreen = i == screens.size() - 1;

				Map<String, Object> layout = (Map<String, Object>) screen.get("layout");
				if (layout != null) {
					fetchChildren((List<Map<String, Object>>) layout.get("children"), fieldMeta, screens);
				}
			}
		}
		return fieldMeta;
	}

	private static void fetchChildren(List<Map<String, Object>> children, List<Map<String, String>> fieldMeta,
			List<Map<String, Object>> screens) {
		if (children != null) {
			for (Map<String, Object> child : children) {
				String type = (String) child.get("type");

				if ("Form".equals(type)) {
					Object childChildren = child.get("children");
					if (childChildren instanceof List) {
						fetchChildren((List<Map<String, Object>>) childChildren, fieldMeta, screens);
					}
				} else if ("Footer".equals(type)) {
					Map<String, Object> onClickAction = (Map<String, Object>) child.get("on-click-action");
					if (onClickAction != null) {
						Map<String, Object> payload = (Map<String, Object>) onClickAction.get("payload");
						if (payload != null) {
							for (Map.Entry<String, Object> entry : payload.entrySet()) {
								if (!String.valueOf(entry.getValue()).contains("data.")) {
									String payloadKey = entry.getKey();

									findPayloadType(String.valueOf(entry.getValue()), payloadKey, screens, fieldMeta);

								}
							}
						}
					}
				}

			}
		}
	}

	private static void findPayloadType(String payloadValue, String payloadKey, List<Map<String, Object>> screens,
			List<Map<String, String>> fieldMeta) {
		String fieldName = null;
		if (payloadValue.contains("${form.")) {
			fieldName = payloadValue.replace("${form.", "").replace("}", "");
		}

		for (Map<String, Object> screen : screens) {
			Map<String, Object> layout = (Map<String, Object>) screen.get("layout");
			if (layout != null) {
				List<Map<String, Object>> children = (List<Map<String, Object>>) layout.get("children");
				if (children != null) {
					searchForFieldType(fieldName, payloadKey, children, fieldMeta);
				}
			}
		}

	}

	private static void searchForFieldType(String fieldName, String payloadKey, List<Map<String, Object>> children,
			List<Map<String, String>> fieldMeta) {
		for (Map<String, Object> child : children) {
			String name = (String) child.get("name");
			String type = (String) child.get("type");
			String label = (String) child.get("label");
			Map<String, String> meta = new HashMap<>();

			if (fieldName.equals(name)) {
				meta.put("key", payloadKey);
				meta.put("label", label);
				meta.put("type", type);

				if (child.containsKey("data-source")) {
					List<Map<String, String>> dataSource = (List<Map<String, String>>) child.get("data-source");
					meta.put("data-source", dataSource != null ? dataSource.toString() : "[]");
				}
				fieldMeta.add(meta);

			}

			List<Map<String, Object>> childChildren = (List<Map<String, Object>>) child.get("children");
			if (childChildren != null) {
				searchForFieldType(fieldName, payloadKey, childChildren, fieldMeta);

			}
		}

	}

	/**
	 * Process URLs for Meta approval - stores original URLs and replaces with
	 * tracking URLs Uses complete tracking URLs with variable placeholders ({{1}})
	 * for Meta compatibility
	 * 
	 * @return Map of original URLs for later restoration
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> processUrlsForApproval(Map<String, Object> templateStructure,
			ChannelConfig channelConfig) {
		Map<String, Object> originalUrls = new HashMap<>();

		// Check if URL tracking is enabled
		PMConfigurationObject trackMessage = pmEnvironment.local().prefsEntry("postman.track.message");
		if (!trackMessage.exists() || !trackMessage.asBoolean()) {
			return originalUrls; // No tracking enabled
		}

		String trackUrl = pmEnvironment.local().prefsEntry("postman.track.message.url").asString();
		// Fallback mechanism - same as HSM
		if (!ArgUtil.is(trackUrl)) {
			trackUrl = pmClientConfig.getWebhookBase(channelConfig, "nexus");
		}

		String channelCode = channelConfig.getContactType().getShortCode();

		// Process template components
		if (templateStructure.containsKey("components")) {
			List<Map<String, Object>> components = (List<Map<String, Object>>) templateStructure.get("components");
			for (int i = 0; i < components.size(); i++) {
				Map<String, Object> component = components.get(i);
				if ("BUTTONS".equals(component.get("type")) && component.containsKey("buttons")) {
					List<Map<String, Object>> buttons = (List<Map<String, Object>>) component.get("buttons");
					processButtonUrls(buttons, trackUrl, channelCode, originalUrls, i, templateStructure);
				}
			}
		}

		return originalUrls;
	}

	/**
	 * Process button URLs - stores originals and replaces with tracking URLs for
	 * Meta approval Uses complete tracking URL with variable placeholder and
	 * provides sample data for Meta compatibility
	 */
	private void processButtonUrls(List<Map<String, Object>> buttons, String trackUrl, String channelCode,
			Map<String, Object> originalUrls, int componentIndex, Map<String, Object> templateStructure) {
		for (int j = 0; j < buttons.size(); j++) {
			Map<String, Object> button = buttons.get(j);
			if ("URL".equals(button.get("type")) && button.containsKey("url")) {
				String originalUrl = (String) button.get("url");
				String key = "component_" + componentIndex + "_button_" + j;
				originalUrls.put(key, originalUrl);

				try {
					String urlHash = CryptoUtil.getMD5Hash(originalUrl);
					String templateName = (String) templateStructure.get("name");
					String sampleMsgHash = templateName + "_" + urlHash; // templateName_urlHash format

					// Create complete tracking URL with variable placeholder for Meta approval
					// Meta will receive:
					// https://demo.mehery.xyz/nexus/link/short/template/wa?msgHash={{1}}
					String trackingUrl = String.format("%s/link/short/template/%s?msgHash={{1}}", trackUrl,
							channelCode);
					button.put("url", trackingUrl);

					// This is required for URL buttons with query parameter variables
					button.put("example", Arrays.asList(sampleMsgHash));

				} catch (NoSuchAlgorithmException e) {
					// Continue without tracking URL
				}
			}
		}
	}

	/**
	 * Send template to Meta API
	 */
	private MapModel sendToMetaAPI(ChannelConfig channelConfig, Map<String, Object> templateStructure) {
		String status = ArgUtil.parseAsString(templateStructure.get("status"), Constants.BLANK);
		ChannelClient channelClient = clientFactory.get(channelConfig);

		if ("approved".equalsIgnoreCase(status) || "rejected".equalsIgnoreCase(status)
				|| "paused".equalsIgnoreCase(status)) {
			return channelClient.updateTemplates(channelConfig, MapModel.from(templateStructure));
		} else {
			MapModel resp = channelClient.createTemplates(channelConfig, MapModel.from(templateStructure));

			// --- SAFETY CHECK ADDED HERE ---
			if (resp != null) {
				resp.put("language", templateStructure.get("language"));
				resp.put("name", templateStructure.get("name"));
				resp.put("status", templateStructure.get("status"));
				resp.put("rejected_reason", templateStructure.get("rejected_reason"));
			}

			return resp;
		}
	}
}
