package com.boot.jx.postman.store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import com.boot.jx.AppConfigPackage.AppSharedConfig;
import com.boot.jx.AppConfigPackage.AppSharedConfigChange;
import com.boot.jx.AppContextUtil;
import com.boot.jx.api.ApiFieldError;
import com.boot.jx.api.ApiResponseUtil;
import com.boot.jx.cache.TenantAwareCacheLoader;
import com.boot.jx.def.TenantAwareKey;
import com.boot.jx.dict.ContactType;
import com.boot.jx.model.CommonTemplateMeta;
import com.boot.jx.mongo.CommonMongoQueryBuilder;
import com.boot.jx.mongo.CommonMongoStore.PaginatedQuery;
import com.boot.jx.mongo.CommonMongoTemplate.ReadOnlyMongoTemplate;
import com.boot.jx.postman.PMConstants;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.PostmanPackages.TemplateResolver;
import com.boot.jx.postman.doc.CustomerProfileDoc;
import com.boot.jx.postman.doc.HSMTemplate3rdParty;
import com.boot.jx.postman.doc.HSMTemplateDoc;
import com.boot.jx.postman.doc.QuickReply;
import com.boot.jx.postman.manager.ChatLogger;
import com.boot.jx.postman.model.ITemplates.BasicTemplate;
import com.boot.jx.postman.model.MessageDefinitions.Contactable;
import com.boot.model.MapModel;
import com.boot.model.MapModel.MapPathEntry;
import com.boot.utils.ArgUtil;
import com.boot.utils.Constants;
import com.boot.utils.EntityDtoUtil;
import com.boot.utils.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@Component
public class TemplateStore implements TemplateResolver, AppSharedConfig {

	private static final Logger LOGGER = LoggerFactory.getLogger(TemplateStore.class);

	@Autowired
	protected QuickStore quickStore;

	@Autowired
	protected ReadOnlyMongoTemplate readOnlyMongoTemplate;

	@Autowired
	protected PMEnvironment environment;

	@Autowired
	protected ContactStore contactStore;

	private LoadingCache<String, HSMTemplateCache> localHSMap = CacheBuilder.newBuilder().maximumSize(200)
			.expireAfterWrite(30, TimeUnit.MINUTES).build(CacheLoader.from(this::loadTenantCache));

	private final LoadingCache<String, HSMTemplateDoc> templateByIdCache = CacheBuilder.newBuilder().maximumSize(5_000)
			.expireAfterWrite(30, TimeUnit.MINUTES).build(CacheLoader.from(this::loadTemplateById));
	private final LoadingCache<String, List<HSMTemplateDoc>> templateByCodeCache = CacheBuilder.newBuilder()
			.maximumSize(2_000).expireAfterWrite(30, TimeUnit.MINUTES)
			.build(CacheLoader.from(this::loadTemplateByCode));

	private final LoadingCache<TenantAwareKey, Optional<HSMTemplate3rdParty>> templateExtByCodeLangCache = CacheBuilder
			.newBuilder().maximumSize(2_000).expireAfterAccess(30, TimeUnit.MINUTES)
			.build(TenantAwareCacheLoader.from(this::loadTemplateExtByCodeLang));

	private static String k(String tenant, String key) {
		return tenant + "::" + key;
	}

	private HSMTemplateCache loadTenantCache(String tenant) {
		try {
			List<HSMTemplateDoc> docs = readOnlyMongoTemplate.findAll(HSMTemplateDoc.class);

			Map<String, HSMTemplateDoc> byId = new HashMap<>();
			Map<String, List<HSMTemplateDoc>> byCode = new HashMap<>();

			for (HSMTemplateDoc d : docs) {
				byId.put(d.getId(), d);
				byCode.computeIfAbsent(d.getCode(), k -> new ArrayList<>()).add(d);
			}

			byCode.replaceAll((k, v) -> List.copyOf(v));

			return new HSMTemplateCache(Map.copyOf(byId), Map.copyOf(byCode));

		} catch (Exception e) {
			LOGGER.error("Failed to load templates for tenant {}", tenant, e);
			throw e;
		}
	}

	private HSMTemplateDoc loadTemplateById(String cacheKey) {
		String[] parts = cacheKey.split("::", 2);
		String tenant = parts[0];
		String templateId = parts[1];
		try {
			AppContextUtil.setTenant(tenant);
			// AppContextUtil.init();
			HSMTemplateDoc template = quickStore.findById(templateId, HSMTemplateDoc.class);
			if (template == null) {
				LOGGER.warn("Cannot load templates for tenant {} with id {}", tenant, templateId);
			}
			return template;
		} catch (Exception e) {
			LOGGER.error("Failed to load templates for tenant {} with id", tenant, templateId, e);
			throw e;
		}
	}

	private List<HSMTemplateDoc> loadTemplateByCode(String cacheKey) {
		String[] parts = cacheKey.split("::", 2);
		String tenant = parts[0];
		String code = parts[1];

		try {
			AppContextUtil.setTenant(tenant);
			// AppContextUtil.init();
			List<HSMTemplateDoc> templates = quickStore
					.find(CommonMongoQueryBuilder.collection(HSMTemplateDoc.class).where("code", code));
			if (ArgUtil.isEmpty(templates)) {
				LOGGER.warn("Cannot load templates for tenant {} with code {}", tenant, code);
			}
			return List.copyOf(templates);
		} catch (Exception e) {
			LOGGER.error("Failed to load templates for tenant {} with code {}", tenant, code, e);
			throw e;
		}

	}

	public static class HSMTemplateCache {
		final Map<String, HSMTemplateDoc> byId;
		final Map<String, List<HSMTemplateDoc>> byCode;

		HSMTemplateCache(Map<String, HSMTemplateDoc> byId, Map<String, List<HSMTemplateDoc>> byCode) {
			this.byId = byId;
			this.byCode = byCode;
		}

		public HSMTemplateDoc byId(String id) {
			return this.byId.get(id);
		}

		public List<HSMTemplateDoc> byCode(String code) {
			return this.byCode.getOrDefault(code, List.of());
		}
	}

	private HSMTemplateCache cache() {
		return localHSMap.getUnchecked(AppContextUtil.getTenant());
	}

	public HSMTemplateDoc findById(String templateId) {
		if (templateId == null) {
			ApiResponseUtil.throwMissinInputException(new ApiFieldError().field("templateId"));
			return null;
		}

		String key = k(AppContextUtil.getTenant(), templateId);
		HSMTemplateDoc x = templateByIdCache.getUnchecked(key);
		if (x != null) {
			return x;
		}
		// x = cache().byId(templateId);
		if (!ArgUtil.is(x)) {
			x = quickStore.findById(templateId, HSMTemplateDoc.class);
		}
		return x;
	}


	public List<HSMTemplateDoc> findByCode(String templateCode) {
		if (templateCode == null) {
			ApiResponseUtil.throwMissinInputException(new ApiFieldError().field("templateCode"));
			return null;
		}
		String key = k(AppContextUtil.getTenant(), templateCode);
		List<HSMTemplateDoc> temps = templateByCodeCache.getUnchecked(key);
		if (!ArgUtil.is(temps)) {
			return temps;
		}
		// temps = cache().byCode(templateCode);
		if (!ArgUtil.is(temps)) {
			temps = quickStore
					.find(CommonMongoQueryBuilder.collection(HSMTemplateDoc.class).where("code", templateCode));
		}
		return temps;
	}

	public List<HSMTemplateDoc> findAll() {
		return List.copyOf(cache().byId.values());
	}

	public HSMTemplateDoc copied(HSMTemplateDoc template) {
		if (!ArgUtil.is(template)) {
			LOGGER.warn("@copy - Template is Null");
			return template;
		}

		if (ArgUtil.is(template.getHeader())) { // If needs cloning global condittion
			HSMTemplateDoc copied = EntityDtoUtil.copyProperties(new HSMTemplateDoc(), template);
			MapModel options = MapModel.from(copied.options());

			MapPathEntry subject = options.entry("subject");
			if (ArgUtil.is(template.getHeader()) && subject.isEmpty()) {
				subject.save(template.getHeader());
			}

			return copied;
		}
		return template;
	}

	public HSMTemplateDoc resolve(CommonTemplateMeta template, ContactType contactType) {

		if (ArgUtil.is(template.getId())) {
			if (template.getId().startsWith("QR=")) {
				QuickReply qr = quickStore.findById(template.getId().split("QR=")[1], QuickReply.class);
				return createTemplateDoc(template, qr);
			} else {
				return findById(template.getId());
			}
		} else if (ArgUtil.is(template.getCode())) {

			if (template.getCode().startsWith("QR=")) {
				QuickReply qr = quickStore.findByCode(template.getCode().split("QR=")[1], QuickReply.class);
				return createTemplateDoc(template, qr);
			} else {
				List<HSMTemplateDoc> temps = findByCode(template.getCode());
				if (ArgUtil.is(temps)) {
					HSMTemplateDoc resolvedTemplate = null;
					if (temps.size() > 1) {
						HSMTemplateDoc wildCardTemp = null;
						HSMTemplateDoc exactTemp = null;
						HSMTemplateDoc noLangTemp = null;
						HSMTemplateDoc noContactTemp = null;
						HSMTemplateDoc engLangTemp = null;

						for (HSMTemplateDoc hsmTemplate3rdParty : temps) {
							if (ArgUtil.not(hsmTemplate3rdParty.getContactType())
									&& ArgUtil.not(hsmTemplate3rdParty.getLang())) {
								wildCardTemp = hsmTemplate3rdParty;
							} else if (ArgUtil.is(hsmTemplate3rdParty.getContactType(), contactType)
									&& ArgUtil.is(hsmTemplate3rdParty.getLang(), template.getLang())) {
								exactTemp = hsmTemplate3rdParty;
								break;
							} else if (ArgUtil.is(hsmTemplate3rdParty.getContactType(), contactType)
									&& (ArgUtil.not(hsmTemplate3rdParty.getLang()) || (ArgUtil.not(noLangTemp)
											&& ArgUtil.is(hsmTemplate3rdParty.getLang(), "en", "en_US", "en_GB")))) {
								noLangTemp = hsmTemplate3rdParty;
							} else if (ArgUtil.not(hsmTemplate3rdParty.getContactType())
									&& ArgUtil.is(hsmTemplate3rdParty.getLang(), template.getLang())) {
								noContactTemp = hsmTemplate3rdParty;
							} else if (ArgUtil.not(hsmTemplate3rdParty.getContactType())
									&& ArgUtil.is(hsmTemplate3rdParty.getLang(), "en", "en_US", "en_GB")) {
								engLangTemp = hsmTemplate3rdParty;
							}
						}

						resolvedTemplate = ArgUtil.anyOf(exactTemp, noLangTemp, noContactTemp, engLangTemp,
								wildCardTemp);

					} else {
						resolvedTemplate = temps.get(0);
					}
					return resolvedTemplate;
				}
			}

		}
		return null;
	}

	private HSMTemplateDoc createTemplateDoc(CommonTemplateMeta template, QuickReply qr) {
		if (ArgUtil.is(qr)) {
			HSMTemplateDoc tmpl = new HSMTemplateDoc();
			tmpl.setId(template.getId());
			tmpl.setCode(qr.getCode());
			tmpl.setCategory(qr.getCategory());
			tmpl.setTemplate(qr.getTemplate());
			return tmpl;
		}
		return null;
	}

	@Override
	public BasicTemplate get(String templateId) {
		HSMTemplateDoc x = copied(findById(templateId));
		return x;
	}

	@Override
	public BasicTemplate get(CommonTemplateMeta template, Contactable contactable, MapModel model) {
		HSMTemplateDoc basicTemplate = copied(resolve(template, contactable.type()));
		if (ArgUtil.is(basicTemplate)) {
			template.setCode(basicTemplate.getCode());
			template.setId(basicTemplate.getId());
			fill(basicTemplate, contactable, model);
		}
		return basicTemplate;
	}

	public BasicTemplate get(CommonTemplateMeta template, ContactType conctType) {
		HSMTemplateDoc basicTemplate = copied(resolve(template, conctType));
		if (ArgUtil.is(basicTemplate)) {
			template.setCode(basicTemplate.getCode());
			template.setId(basicTemplate.getId());
		}
		return basicTemplate;
	}

	@Override
	public BasicTemplate get(CommonTemplateMeta template) {
		return get(template, null);
	}

	public void fill(HSMTemplateDoc basicTemplate, Contactable contactable, MapModel model) {
		model.put("contact", contactable);
		model.put("global", environment.local().globalVars().toObject());
		Map<String, Object> cp = getProfile(contactable, basicTemplate);
		if (ArgUtil.is(cp)) {
			model.put("profile", JsonUtil.toMap(cp));
		}
	}

	@Autowired
	protected ChatLogger logManager;

	public Map<String, Object> getProfile(Contactable contactable, HSMTemplateDoc template) {

		Map<String, Object> profileMap = new HashMap<>();

		try {
			Map<String, Object> model = new HashMap<>();
			CustomerProfileDoc profile = null;

			if (ArgUtil.is(contactable) && ArgUtil.is(template) && template.getTemplate().contains("profile.")) {
				if (ArgUtil.is(contactable.getPhone())) {
					profile = contactStore.findProfileByPhone(contactable.getPhone());
				} else if (ArgUtil.is(contactable.getEmail())) {
					profile = contactStore.findProfileByEmail(contactable.getEmail());
				}
			}

			if (profile != null && ArgUtil.is(template) && ArgUtil.is(template.getProfileVarMap())) {
				Map<String, Object> profileVarMap = template.getProfileVarMap();

				model.put("profile", JsonUtil.toMap(profile));

				ObjectMapper mapper = new ObjectMapper();
				JsonNode root = mapper.readTree(JsonUtil.toJson(model));

				profileVarMap.forEach((key, value) -> {
					String keyValue = ArgUtil.parseAsString(getValueByPath(root, value), PMConstants.NOT_AVALIABE);
					profileMap.put(key, keyValue);
				});
			}
		} catch (Exception e) {
			logManager.error(e);
		}
		return profileMap;
	}

	private static String getValueByPath(JsonNode root, Object path) {
		String[] parts = path.toString().split("\\.");
		JsonNode current = root;
		for (String part : parts) {
			if (current == null)
				return null;
			current = current.get(part);

			if (current != null && current.isArray() && current.size() > 0) {
				current = current.get(0);
			}
		}
		return current != null ? current.asText() : null;
	}

	@Override
	public void clear(AppSharedConfigChange change) {
		if (ArgUtil.is(change.getName(), name())) {
			String tnt = AppContextUtil.getTenant();
			localHSMap.invalidate(tnt);

			// hsm
			String prefix = tnt + "::";
			templateByIdCache.asMap().keySet().removeIf(k -> k.startsWith(prefix));
			templateByCodeCache.asMap().keySet().removeIf(k -> k.startsWith(prefix));
			// 3rdParty
			templateExtByCodeLangCache.invalidateAll(templateExtByCodeLangCache.asMap().keySet().stream()
					.filter(k -> k.tenant().equals(tnt)).collect(Collectors.toList()));

		}
	}

	public List<HSMTemplateDoc> search(
			// int pageNo, int pageSize, String sortBy, String sortDir,
			String category, String desc, String categoryType, String name, String code, String status) {
		MapModel extparams = MapModel.createInstance();

		if (ArgUtil.is(category)) {
			extparams.put("category", "*" + category + "*");
		}

		if (ArgUtil.is(desc)) {
			extparams.put("desc", "*" + desc + "*");
		}

		if (ArgUtil.is(categoryType)) {
			extparams.put("categoryType", "*" + categoryType + "*");
		}

		if (ArgUtil.is(name)) {
			extparams.put("name", "*" + name + "*");
		}
		if (ArgUtil.is(code)) {
			extparams.put("code", "*" + code + "*");
		}
		if (ArgUtil.is(status)) {
			extparams.put("approved.status", "*" + status + "*");
		}

		return quickStore.getPages(PaginatedQuery.select(HSMTemplateDoc.class, "DICT_HSM_TEMPLATES") //
				// .pageNo(pageNo).pageSize(pageSize).sortBy(sortBy).sortDir(sortDir) //
				.extraParams(extparams).count()).getResults();
	}

	public void save(HSMTemplateDoc template) {
		quickStore.save(template);
		publishUpdate();
	}

	////// TMEPLATE EXT

	public HSMTemplate3rdParty templateExt(String hsmTemplateId, String channelId, String code, String lang) {
		List<HSMTemplate3rdParty> temps = quickStore
				.find(CommonMongoQueryBuilder.collection(HSMTemplate3rdParty.class).where(Criteria
						.where("hsmTemplateId").is(hsmTemplateId).and("channelId").is(channelId).and("code").is(code)));
		return templateExtFilter(temps, lang);
	}

	public HSMTemplate3rdParty templateExt(String hsmTemplateId, String channelId, String lang) {
		List<HSMTemplate3rdParty> temps = quickStore.find(CommonMongoQueryBuilder.collection(HSMTemplate3rdParty.class)
				.where(Criteria.where("hsmTemplateId").is(hsmTemplateId).and("channelId").is(channelId)));
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(JsonUtil.toJson(temps));
		}
		return templateExtFilter(temps, lang);
	}

	private HSMTemplate3rdParty templateExtFilter(List<HSMTemplate3rdParty> temps, String lang) {
		if (ArgUtil.is(temps)) {
			HSMTemplate3rdParty resolvedTemplate = null;
			if (temps.size() > 1) {
				for (HSMTemplate3rdParty hsmTemplate3rdParty : temps) {
					if (ArgUtil.areEqual(hsmTemplate3rdParty.getLang(), lang)) {
						resolvedTemplate = hsmTemplate3rdParty;
						break;
					} else if (ArgUtil.is(hsmTemplate3rdParty.getLang())) {
						resolvedTemplate = hsmTemplate3rdParty;
					}
				}
			} else {
				resolvedTemplate = temps.get(0);
			}
			return resolvedTemplate;
		}
		return null;
	}

	private Optional<HSMTemplate3rdParty> loadTemplateExtByCodeLang(TenantAwareKey cacheKey) {
		String code = cacheKey.code();
		String[] args = cacheKey.args();
		if (args.length == 3) {
			return Optional.ofNullable(templateExt(code, args[0], args[1], args[2]));
		} else if (args.length == 2) {
			return Optional.ofNullable(templateExt(code, args[0], args[1]));
		}
		return Optional.empty();
	}

	public HSMTemplate3rdParty templateExtCached(String hsmTemplateId, String channelId, String code, String lang) {
		TenantAwareKey key = TenantAwareKey.fromCodeArgs(hsmTemplateId, channelId, code,
				ArgUtil.parseAsString(lang, Constants.BLANK));
		return templateExtByCodeLangCache.getUnchecked(key).orElse(null);
	}

	public HSMTemplate3rdParty templateExtCached(String hsmTemplateId, String channelId, String lang) {
		TenantAwareKey key = TenantAwareKey.fromCodeArgs(hsmTemplateId, channelId,
				ArgUtil.parseAsString(lang, Constants.BLANK));
		return templateExtByCodeLangCache.getUnchecked(key).orElse(null);
	}
}
