package com.boot.jx.inbound;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.boot.jx.AppContextUtil;
import com.boot.jx.chat.ChatStatusProcessor;
import com.boot.jx.chat.ConnectorHandlerFactory;
import com.boot.jx.chat.ConnectorHandlerFactory.ConnectorHandler;
import com.boot.jx.dict.ContactType;
import com.boot.jx.logger.AuditService;
import com.boot.jx.model.CommonFile;
import com.boot.jx.mongo.CommonMongoTemplate;
import com.boot.jx.postman.PMAuditEvent;
import com.boot.jx.postman.PMConfiguration;
import com.boot.jx.postman.PMConstants.CHANNEL_TYPE;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.doc.HSMTemplate3rdParty;
import com.boot.jx.postman.doc.HSMTemplateDoc;
import com.boot.jx.postman.doc.MessageDoc;
import com.boot.jx.postman.doc.tpo.PayloadDumpCollection;
import com.boot.jx.postman.manager.ConfigManager;
import com.boot.jx.postman.manager.ThirdPartyTemplateManager;
import com.boot.jx.postman.model.MessageBoxEvent;
import com.boot.jx.postman.model.MessageDefinitions.Contactable;
import com.boot.jx.postman.model.TemplateStatusEvent;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.store.MessageStore;
import com.boot.jx.postman.store.SessionStore;
import com.boot.jx.postman.store.TemplateStore;
import com.boot.jx.postman.wa360.WA360Template;
import com.boot.jx.postman.wacfb.WacfbClient;
import com.boot.jx.rest.RestService;
import com.boot.jx.utils.PostManUtil;
import com.boot.model.MapModel;
import com.boot.utils.ArgUtil;

/**
 * 
 * InBoundRouter is responsible for calling connector handler for domain ALREADY
 * decided
 * 
 * @author lalittanwar
 *
 */
@Component
public class InBoundRouter {

	private static final Logger LOGGER = LoggerFactory.getLogger(InBoundRouter.class);

	@Autowired
	private PMEnvironment pmEnvironment;

	@Autowired
	private ConnectorHandlerFactory connectorHandlerFactory;

	@Autowired
	private AuditService auditService;

	@Autowired
	private InBoundService inBoundService;

	@Autowired
	private InBoundMessageProcessor inBoundMessageService;

	@Autowired
	private ChatStatusProcessor inBoundStatusService;

	@Autowired
	private SessionStore sessionStore;

	@Autowired
	private MessageStore messageStore;

	@Autowired
	private RestService restService;

	@Autowired
	private ConfigManager configManager;

	@Autowired
	private CommonMongoTemplate commonMongoTemplate;

	@Autowired
	private ThirdPartyTemplateManager thirdPartyTemplateManager;

	@Autowired
	private WacfbClient wacfbClient;

	@Autowired
	private TemplateStore templateStore;

	public void inboundMessageEvent(String channelType, String channelId, Map<String, Object> data) {
		MapModel map = MapModel.from(data);
		PMConfiguration config = pmEnvironment.config();
		ChannelConfig channelConfig = config.channel(channelId);

		if (!ArgUtil.is(channelConfig)) {
			Contactable c = PostManUtil.parseChannelId(channelId);
			channelConfig = configManager.saveChannelConfig(c.getChannelType(), MapModel.createInstance()
					.put("channelId", channelId).put("lane", c.getLane()).put("isAutoCreated", true).toMap());

		}
		ConnectorHandler connector = connectorHandlerFactory.get(channelConfig);

		if (!ArgUtil.is(connector)) {
			dumpUnhandledEvent(channelConfig.getContactType(), channelConfig.getChannelType(), channelId, channelConfig,
					data, "Connector Not Found for " + channelId);
			return;
		}

		try {
			MessageBoxEvent messageBoxEvent = connector.inboundMessageBoxEvent(channelConfig, map,
					new MessageBoxEvent());
			if (ArgUtil.is(messageBoxEvent) && ArgUtil.is(messageBoxEvent.getInboxMessages())) {
				messageBoxEvent.getInboxMessages().forEach(inboxMessage -> {
					connector.prompt(inboxMessage);
					inBoundMessageService.publishAsync(inboxMessage);
				});
				connector.onReceiveInboxMessage(messageBoxEvent.getInboxMessages());
			} else if (ArgUtil.is(messageBoxEvent) && ArgUtil.is(messageBoxEvent.getMessageReports())) {
				connector.onMessageReports(messageBoxEvent.getMessageReports());
				inBoundStatusService.publishAsync(messageBoxEvent.getMessageReports());
			} else if (ArgUtil.is(messageBoxEvent) && ArgUtil.is(messageBoxEvent.getTemplateStatusEvents())) {
				// Process template status updates
				LOGGER.debug("Processing {} template status event(s)",
						messageBoxEvent.getTemplateStatusEvents().size());
				processTemplateStatusEvents(messageBoxEvent.getTemplateStatusEvents());
			} else if (ArgUtil.is(channelConfig) && ArgUtil.is(channelConfig.getUnhandledInboundForward())) {
				restService.ajax(channelConfig.getUnhandledInboundForward()).post(data).asNone();
			} else {
				dumpUnhandledEvent(channelConfig.getContactType(), channelConfig.getChannelType(),
						channelConfig.getChannelId(), channelConfig, data, null);
			}

		} catch (Exception e) {
			auditService.excep(new PMAuditEvent(PMAuditEvent.Type.INBOUND_ERROR).data(data), LOGGER, e);
			dumpUnhandledEvent(channelConfig.getContactType(), channelConfig.getChannelType(), channelId, channelConfig,
					data, e.getMessage());
		}
	}

	private void dumpUnhandledEvent(ContactType contactType, String channelType, String channelId,
			ChannelConfig channelConfig, Map<String, Object> data, String log) {
		PayloadDumpCollection d = new PayloadDumpCollection();
		d.setType("UNHANDLED_INBOX_EVENT");
		if (ArgUtil.is(channelConfig)) {
			d.setContactType(channelConfig.getContactType());
			d.setChannelType(channelConfig.getChannelType());
			d.setChannelId(channelConfig.getChannelId());
			d.setAutoCreatedChannel(channelConfig.isAutoCreated());
		} else {
			d.setContactType(contactType);
			d.setChannelType(channelType);
			d.setChannelId(channelId);
		}
		if (ArgUtil.is(log)) {
			LOGGER.error(log);
			d.logs().add(log);
		}
		d.setDump(data);
		commonMongoTemplate.save(d);
	}

	@Async
	public void inboundMessageEventAsync(String channelType, String channelId, Map<String, Object> data) {
		this.inboundMessageEvent(channelType, channelId, data);
	}

	@Async
	public void inboundMessageEventAsync(String domain, String channelType, String channelId,
			Map<String, Object> data) {
		AppContextUtil.clear();
		AppContextUtil.setTenant(domain);
		AppContextUtil.init();
		this.inboundMessageEvent(channelType, channelId, data);
	}

	public CommonFile reloadMedia(String sessionId, String messageId, Integer index)
			throws FileNotFoundException, IOException {
		ChatSessionDoc session = sessionStore.getSession(sessionId);
		PMConfiguration config = pmEnvironment.config();
		String channelId = PostManUtil.CHANNEL_ID(session.contact());
		ChannelConfig channelConfig = config.channel(channelId);
		ConnectorHandler connector = connectorHandlerFactory.get(channelConfig);
		if (!ArgUtil.is(connector)) {
			LOGGER.error("Channel Not Found for " + channelId);
		}
		MessageDoc msg = messageStore.findByMessageId(messageId, session.contact().getContactType());
		return connector.reloadMedia(channelConfig, msg, index);
	}

	public void reloadMedia(String sessionId, String messageId) throws FileNotFoundException, IOException {
		ChatSessionDoc session = sessionStore.getSession(sessionId);
		PMConfiguration config = pmEnvironment.config();
		String channelId = PostManUtil.CHANNEL_ID(session.contact());
		ChannelConfig channelConfig = config.channel(channelId);
		ConnectorHandler connector = connectorHandlerFactory.get(channelConfig);
		if (!ArgUtil.is(connector)) {
			LOGGER.error("Channel Not Found for " + channelId);
		}
		MessageDoc msg = messageStore.findByMessageId(messageId, session.contact().getContactType());
		connector.reloadMedia(channelConfig, msg);
	}

	/**
	 * Process template status events from Meta webhooks Efficient: Processes all
	 * events in batch with minimal DB calls
	 * 
	 * @param events List of template status events to process
	 */
	private void processTemplateStatusEvents(List<TemplateStatusEvent> events) {
		for (TemplateStatusEvent event : events) {
			try {
				processTemplateStatusEvent(event);
			} catch (Exception e) {
				LOGGER.error("Failed to process template status event: {} - {}", event.getTemplateName(),
						e.getMessage(), e);
			}
		}
	}

	/**
	 * Process a single template status event
	 * 
	 * Strategy: Find templates by name + language (not channelId) Reason: Meta
	 * approves templates at WABA level, not per phone number
	 * 
	 * This approach: - Finds ALL templates with matching name + language across all
	 * channels - Updates all instances (they share same WABA approval) - Works
	 * regardless of which channel webhook was routed through
	 * 
	 * @param event Template status event from Meta
	 */
	private void processTemplateStatusEvent(TemplateStatusEvent event) {

		// Build query to find ALL templates with this name + language
		// Note: We don't use channelId because Meta's approval is WABA-level
		Query query = new Query(Criteria.where("code").is(event.getTemplateName()).and("lang").is(event.getLanguage()));

		// Find all matching templates across all channels
		List<HSMTemplate3rdParty> templates = commonMongoTemplate.find(query, HSMTemplate3rdParty.class);

		// If no templates found, try to create from webhook data
		if (templates == null || templates.isEmpty()) {
			HSMTemplate3rdParty created = createTemplateFromWebhook(event);
			if (created != null) {
				// Update the newly created template
				updateTemplateStatus(created, event);
			} else {
				LOGGER.warn("No templates found with name: {} lang: {}", event.getTemplateName(), event.getLanguage());
			}
			return;
		}

		// Update each template instance
		String newStatus = event.getEvent();
		int successCount = 0;
		for (HSMTemplate3rdParty template3rd : templates) {
			try {
				updateTemplateStatus(template3rd, event);
				successCount++;
			} catch (Exception e) {
				LOGGER.error("Failed to update template instance: {} - {}", template3rd.getId(), e.getMessage(), e);
			}
		}

		// Log completion summary
		LOGGER.debug("Template status update completed: {} -> {} ({}/{} instances updated)", event.getTemplateName(),
				newStatus, successCount, templates.size());
	}

	/**
	 * Update template status and linked HSM template , also add id to template.
	 * 
	 * @param template Template to update
	 * @param event    Template status event containing new status
	 */
	private void updateTemplateStatus(HSMTemplate3rdParty template, TemplateStatusEvent event) {
		String newStatus = event.getEvent();

		if (!ArgUtil.is(newStatus)) {
			LOGGER.warn("Template status event has no status value, skipping update");
			return;
		}
		
		// UNARCHIVED is a webhook event; stored status should be APPROVED (sendable on Meta)
		if ("UNARCHIVED".equalsIgnoreCase(newStatus)) {
			newStatus = "APPROVED";
		}

		// Ensure template map exists
		if (template.getTemplate() == null) {
			template.setTemplate(new HashMap<>());
		}

		// Update template status
		template.getTemplate().put("status", newStatus);
		
		//Storing templateId so that we can use it to unarchive templates
		if (ArgUtil.is(event.getTemplateId()) && !ArgUtil.is(template.getTemplate().get("id"))) {
			template.getTemplate().put("id", event.getTemplateId());
		}

		// Add rejection/pause reason if present
		if (ArgUtil.is(event.getReason()) && !"NONE".equalsIgnoreCase(event.getReason())) {
			template.getTemplate().put("rejected_reason", event.getReason());
		}
		
		// Record source for Meta-driven deletion lifecycle states.
		if ("PENDING_DELETION".equalsIgnoreCase(newStatus) || "DELETED".equalsIgnoreCase(newStatus)) {
			template.setDeletedSource("META_WEBHOOK");
			template.setDeletedStamp(ArgUtil.parseAsLong(event.getTimestamp(), System.currentTimeMillis()));
		}
		
		// Save updated WABA template
		commonMongoTemplate.save(template);
		boolean clearedViaHsmSave = false;

		// Update linked HSM template if exists
		if (ArgUtil.is(template.getHsmTemplateId())) {
			HSMTemplateDoc hsmDoc = commonMongoTemplate.findById(template.getHsmTemplateId(), HSMTemplateDoc.class);

			if (hsmDoc != null) {
				// Update approval status for this channel
				hsmDoc.approved(template.getChannelId(), template.getHsmTemplateId(), newStatus);
				templateStore.save(hsmDoc);
				clearedViaHsmSave = true;
			}
		}

		if (!clearedViaHsmSave) {
			templateStore.publishUpdate();
		}
	}

	/**
	 * TODO:- @Tejal - need to see if this method can be moved to connectors
	 * 
	 * Create minimal template from webhook event data (no API call)
	 * 
	 * @param event Template status event from Meta
	 * @return Created template or null if channelConfig not found
	 */
	private HSMTemplate3rdParty createTemplateFromWebhook(TemplateStatusEvent event) {
		// Get channelConfig from event
		ChannelConfig channelConfig = pmEnvironment.config().channel(event.getChannelId());
		if (channelConfig == null) {
			LOGGER.warn("ChannelConfig not found for channelId: {}", event.getChannelId());
			return null;
		}

		// Create ID (same format as toHSM3rdParty)
		String id = String.format("%s/%s/%s", event.getChannelId(), event.getTemplateName(), event.getLanguage());

		// Check if template already exists (race condition protection)
		HSMTemplate3rdParty template = commonMongoTemplate.findById(id, HSMTemplate3rdParty.class);
		if (template != null) {
			LOGGER.debug("Template already exists, will update: {}", id);
			return template;
		}

		// Try to fetch full template from Meta API if templateId is available
		if (ArgUtil.is(event.getTemplateId()) && CHANNEL_TYPE.WACFB.equalsIgnoreCase(channelConfig.getChannelType())) {
			try {
				MapModel templateResp = wacfbClient.fetchTemplateById(channelConfig, event.getTemplateId());

				if (templateResp != null && !templateResp.isEmpty()) {
					// Convert MapModel → WA360Template (same as refresh method)
					WA360Template wa360Template = templateResp.as(WA360Template.class);

					if (wa360Template != null && ArgUtil.is(wa360Template.getName())
							&& ArgUtil.is(wa360Template.getLanguage())) {
						// REUSE toHSM3rdParty() via public wrapper (same as refresh method!)
						template = thirdPartyTemplateManager.convertToHSM3rdParty(channelConfig, wa360Template);

						// Save template
						commonMongoTemplate.save(template);
						templateStore.publishUpdate();
						return template;
					}
				}
			} catch (Exception e) {
				LOGGER.warn("Failed to fetch template from Meta API, using minimal creation: {}", e.getMessage());
			}
		}
		return null;
	}
}
