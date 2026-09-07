package com.boot.jx.connectors;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.boot.jx.chat.ConnectorHandlerFactory;
import com.boot.jx.chat.ConnectorHandlerFactory.ConnectorHandler;
import com.boot.jx.dict.ContactType;
import com.boot.jx.dict.FileFormat;
import com.boot.jx.dict.FileType;
import com.boot.jx.exception.AmxApiException;
import com.boot.jx.exception.ApiHttpExceptions.ApiStatusCodes;
import com.boot.jx.logger.LoggerService;
import com.boot.jx.model.CommonFile;
import com.boot.jx.model.CommonFileStream;
import com.boot.jx.mongo.CommonMongoTemplate;
import com.boot.jx.postman.PMConstants.MESSAGE_SEND_TYPE;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.PMEnvironment.AChannelDetails;
import com.boot.jx.postman.PMEnvironment.PMClientConfig;
import com.boot.jx.postman.channel.ChannelClientFactory;
import com.boot.jx.postman.channel.ChannelClientFactory.ChannelClient;
import com.boot.jx.postman.client.PMFileStoreClient;
import com.boot.jx.postman.client.TmplClient;
import com.boot.jx.postman.doc.ChatContactDoc;
import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.doc.CustomerProfileDoc;
import com.boot.jx.postman.doc.HSMTemplate3rdParty;
import com.boot.jx.postman.doc.HSMTemplateDoc;
import com.boot.jx.postman.doc.MessageDoc;
import com.boot.jx.postman.doc.config.ChannelConfigLogger;
import com.boot.jx.postman.guard.MessageFailureGuard;
import com.boot.jx.postman.manager.ChatLogger;
import com.boot.jx.postman.model.Attachment;
import com.boot.jx.postman.model.ContactMeta;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.Message;
import com.boot.jx.postman.model.Message.Status;
import com.boot.jx.postman.model.MessageDefinitions.IMessage;
import com.boot.jx.postman.model.MessagePrompt;
import com.boot.jx.postman.model.MessageReport;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.model.ext.InBoundMsgStatus;
import com.boot.jx.postman.model.outbound.OutBoundMsgBasic.ApiOutBoundMsg;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.plugin.ChannelPluginProvider.ChannelPlugin;
import com.boot.jx.postman.query.ChatContactQuery;
import com.boot.jx.postman.service.ChatDTOUtil;
import com.boot.jx.postman.store.ContactStore;
import com.boot.jx.postman.store.MessageContext;
import com.boot.jx.postman.store.TemplateStore;
import com.boot.jx.utils.PostManUtil;
import com.boot.model.MapModel;
import com.boot.utils.ArgUtil;
import com.boot.utils.Urly;

public abstract class AbstractConnector<CD extends AChannelDetails, P extends ChannelPlugin<CD>>
		implements ConnectorHandler {

	public static Logger LOGGER = LoggerService.getLogger(AbstractConnector.class);

	public static abstract class DefaultConnector<CD extends AChannelDetails, P extends ChannelPlugin<CD>>
			extends AbstractConnector<CD, P> {
	}

	@Autowired
	protected MessageContext messageContext;

	@Autowired
	protected PMClientConfig pmClientConfig;

	@Autowired
	protected PMEnvironment environment;

	@Autowired
	public CommonMongoTemplate commonMongoTemplate;

	@Autowired
	protected TemplateStore templateStore;

	@Autowired
	protected TmplClient tmplClient;

	@Autowired
	protected ChatLogger logManager;

	@Autowired
	protected ContactStore contactStore;

	@Autowired
	private PMFileStoreClient pmFileStoreClient;

	@Autowired
	protected ChannelClientFactory clientFactory;

	@Autowired(required = false)
	private MessageFailureGuard messageFailureGuard;

	@Override
	public ChannelClient getClient(ChannelConfig channelConfig) {
		return this.clientFactory.get(channelConfig);
	}

	@Override
	public void onException(ChannelConfig channelConfig, ChatContactDoc chatContactDoc, OutboxMessage outboxMessage,
			Exception e) {
		try {
			if (e instanceof AmxApiException) {
				String errorCode = ((AmxApiException) e).getErrorKey();
				outboxMessage.updateStatus(Message.Status.SENT_ERR);
				outboxMessage.logs().add(errorCode);
				if (ApiStatusCodes.API_ERROR.toString().equalsIgnoreCase(errorCode)) {
					logManager.error(outboxMessage, e);
				}
			} else {
				outboxMessage.updateStatus(Message.Status.SENT_EXC);
				logManager.error(outboxMessage, e);
			}
			outboxMessage.logs().add(e.getMessage());
			LOGGER.error("SEND ERROR", e);
		} catch (Exception ex) {
			LOGGER.error("SEND ERROR LOG EXCEPTION", ex);
		}

	}

	public void registerWebhook(ChannelConfig channelConfig, String webhookUrl) {
		ConnectorHandlerFactory.LOGGER.error("WEBHOOK REGISTRATION NOT DEFINED for URL");
	}

	public void registerWebhook(ChannelConfig channelConfig, ChannelConfigLogger channelConfigLogger) {
		String webhookUrl = pmClientConfig.getWebhookUrl(channelConfig, null, null);
		this.registerWebhook(channelConfig, webhookUrl);
	}

	@Override
	public void onChannelUpdate(ChannelConfig channelConfig, ChannelConfigLogger channelConfigLogger) {
		// Register Webhook URL
		this.registerWebhook(channelConfig, channelConfigLogger);
	}

	public ChannelConfig getChannelConfig(String channleType, String lane) {
		String channelId = PostManUtil.CHANNEL_ID(channleType, lane);
		return environment.config().channel(channelId);
	}

	@Override
	public ChannelConfig getChannelConfig(IMessage iMessage) {
		String channelId = PostManUtil.CHANNEL_ID(iMessage.contact());
		ChannelConfig channelConfig = environment.config().channel(channelId);
		if (!ArgUtil.is(channelConfig) && !ContactType.WEBSITE.equals(iMessage.contact().type())) {
			ConnectorHandlerFactory.LOGGER.error(String.format("ChannelConfig not found for %s", channelId));
		}
		return channelConfig;
	}

	public MessageContext context() {
		return messageContext;
	}

	@Override
	public ChatContactDoc getChatContact(IMessage iMessage) {
		return messageContext.contact().getDoc();
	}

	protected CustomerProfileDoc findProfile(ChatContactDoc chatContactDoc) {
		return null;
	}

	@Override
	public void linkProfile(ChatSessionDoc session, InboxMessage inboxMessage) {
		try {
			ChatContactQuery contactQuery = context().contact();
			ChatContactDoc chatContactDoc = contactQuery.getDoc();
			if (!ArgUtil.is(chatContactDoc.profile().getId())) {
				CustomerProfileDoc profile = findProfile(chatContactDoc);
				if (profile != null) {
					contactStore.linkProfile(contactQuery, profile);
				}
			}
		} catch (Exception e) {
			logManager.error(e);
		}
	}

	@Override
	public boolean beforeSend(ChannelConfig channelConfig, ChatContactDoc chatContactDoc, OutboxMessage outboxMessage,
			ApiOutBoundMsg outBoundMsg) {
		outboxMessage.timer().log("send:b4");
		template(channelConfig, chatContactDoc, outboxMessage);

		// Universal template approval check (applies to all channel types)
		if (!validateTemplateApproval(outboxMessage)) {
			return false;
		}

		// After template(): categoryType is known — load contact cached error onto
		// outbox so WacfbClient can skip Meta (XMS/Agent/campaign).
		if (messageFailureGuard != null) {
			try {
				ChatContactDoc contactForMock = chatContactDoc;
				if (!ArgUtil.is(contactForMock)) {
					contactForMock = messageContext.store().getChatContactDoc();
				}
				if (ArgUtil.is(contactForMock)) {
					messageFailureGuard.resolve(outboxMessage, contactForMock);
				}
			} catch (Exception ex) {
				LOGGER.warn("messageFailureGuard.resolve skipped: {}", ex.getMessage());
			}
		}

		if (ArgUtil.is(outboxMessage.getStatus(), Status.BLCKD, Status.LIMIT)) {
			return false;
		}
		environment.messageProcessor().beforeSend(outboxMessage, channelConfig);
		return true;
	}

	@Override
	public OutboxMessage template(ChannelConfig channelConfig, ChatContactDoc chatContactDoc,
			OutboxMessage outboxMessage) {
//	if (ArgUtil.is(outboxMessage.getMedia())) {
//	    QuickMedia templateReply = commonMongoTemplate.findById(outboxMessage.getTemplate().getMedia(),
//		    QuickMedia.class);
//	    if (ArgUtil.is(templateReply)) {
//		if ("image".equalsIgnoreCase(templateReply.getType())) {
//		    outboxMessage.attachment(new Attachment().mediaURL(templateReply.getUrl())
//			    .mediaType(FileType.IMAGE.toString()).mediaCaption(templateReply.getTitle()));
//		    return outboxMessage;
//		}
//	    } else {
//		process(channelConfig, outboxMessage);
//		return outboxMessage;
//	    }
//	} else

		if (ArgUtil.is(outboxMessage.templateId()) || ArgUtil.is(outboxMessage.templateCode())) {
			// outboxMessage.setMessage(tmplClient.process(hsmTemplate.getTemplate(),
			// outboxMessage.getModel()));
			process(channelConfig, chatContactDoc, outboxMessage);
			return outboxMessage;
		} else {
			return outboxMessage;
		}
	}

	public OutboxMessage process(ChannelConfig channelConfig, ChatContactDoc chatContactDoc,
			OutboxMessage outboxMessage) {

		ContactMeta contactMeta = ChatDTOUtil.getContactMeta(chatContactDoc);

//		outboxMessage.model().put("contact", contactMeta);
//		outboxMessage.model().put("global", environment.local().globalVars().toObject());
//
//		Map<String, Object> cp = getProfile(chatContactDoc, outboxMessage);
//
//		if (ArgUtil.is(cp)) {
//			outboxMessage.model().put("profile", JsonUtil.toMap(cp));
//		}

		// Model Data Merge
		MapModel model = MapModel.from(outboxMessage.getModel());

		MapModel data = MapModel.createInstance();
		data.putAll(model.keyEntry(Message.DATA_KEY).asMap());
		data.putAll(outboxMessage.hsm().data());
		model.put(Message.DATA_KEY, data.toMap());

		// outboxMessage.setModel(JsonUtil.deepCopy(model.toMap()));

		if (ArgUtil.isEmpty(outboxMessage.hsm().getLang())) {
			outboxMessage.hsm().lang(chatContactDoc.prefs().getLang());
		}

		tmplClient.process(outboxMessage, contactMeta);

		// Add tptinfo to options if available in template
		// Also store HSM template approval status for XMS API validation
		if (ArgUtil.is(outboxMessage.templateId())) {
			try {
				// Get template from database to access tptinfo and approval status
				HSMTemplateDoc template = templateStore.findById(outboxMessage.templateId());
				if (ArgUtil.is(template)) {
					// Store tptinfo if available
					if (ArgUtil.is(template.getTptinfo())) {
						// Convert TptInfo objects to Map objects for compatibility
						List<Map<String, Object>> tptinfoMaps = new ArrayList<>();
						for (HSMTemplateDoc.TptInfo tptInfo : template.getTptinfo()) {
							Map<String, Object> tptInfoMap = new HashMap<>();
							tptInfoMap.put("channelType", tptInfo.channelType);
							tptInfoMap.put("channelId", tptInfo.channelId);
							tptInfoMap.put("dltContentId", tptInfo.dltContentId);
							tptinfoMaps.add(tptInfoMap);
						}
						outboxMessage.options().put("tptinfo", tptinfoMaps);
						LOGGER.debug("Added tptinfo to options for templateId: {}", outboxMessage.templateId());
					}

					// Store HSM template approval status for this channel
					if (ArgUtil.is(template.getApproved())) {
						String channelId = channelConfig.getChannelId();
						String templateId = outboxMessage.templateId();
						String hsmApprovalStatus = null;

						for (HSMTemplateDoc.ApprovedChannels approved : template.getApproved()) {
							if (ArgUtil.areEqual(approved.channelId, channelId)
									&& ArgUtil.areEqual(approved.templateId, templateId)) {
								hsmApprovalStatus = approved.status;
								break;
							}
						}

						if (ArgUtil.is(hsmApprovalStatus)) {
							outboxMessage.options().put("hsmTemplateApprovalStatus", hsmApprovalStatus);
							LOGGER.debug("Stored HSM template approval status: {} for templateId: {} channelId: {}",
									hsmApprovalStatus, templateId, channelId);
						}
					}
				}
			} catch (Exception e) {
				LOGGER.warn("Could not retrieve template info for templateId: {}", outboxMessage.templateId(), e);
			}
		}

		if (ArgUtil.is(outboxMessage.templateId())) {
			HSMTemplate3rdParty tpTemplate = templateExt(channelConfig, chatContactDoc, outboxMessage);
			if (ArgUtil.is(tpTemplate)) {
				// MapModel varMap = MapModel.from(outboxMessage.getTemplateExt().getVarMap());
				outboxMessage.setTemplateExt(tpTemplate);
			}
		}

		return outboxMessage;
	}

	public HSMTemplate3rdParty templateExt(ChannelConfig channelConfig, ChatContactDoc chatContactDoc,
			OutboxMessage outboxMessage) {
		HSMTemplate3rdParty resolvedTemplate = null;
		if (ArgUtil.is(outboxMessage.hsm().getLinked())) {
			resolvedTemplate = templateStore.templateExtCached(outboxMessage.templateId(), channelConfig.getChannelId(),
					outboxMessage.hsm().getLinked(), outboxMessage.hsm().getLang());
		} else if (outboxMessage.messageMetaWrapper().isTemplateExt()
				|| (MESSAGE_SEND_TYPE.PUSH_MESSAGE.equals(outboxMessage.messageMetaWrapper().sendType())
						&& channelConfig.isPushAllowed() && channelConfig.isPushOnlyApproved())) {
			resolvedTemplate = templateStore.templateExtCached(outboxMessage.templateId(), channelConfig.getChannelId(),
					outboxMessage.hsm().getLang());
		}
		return resolvedTemplate;
		// return templateExt(outboxMessage.hsm().getLang(), temps);
	}

	@Override
	public boolean optin(ChannelConfig channelConfig, ChatContactDoc chatContactDoc) {
		return ArgUtil.is(chatContactDoc.getCsid());
	}

	@Override
	public void prompt(InboxMessage inboxMessage) {

		if (!ArgUtil.is(inboxMessage.form())) {
			return;
		}

		String replyId = ArgUtil.parseAsString(inboxMessage.form().get("reply_id"));

		if (ArgUtil.is(replyId)) {
			if (replyId.startsWith("#")) {
				String[] params = replyId.split("#");
				if (params.length == 4) {
					if (MessagePrompt.TYPE.MOREOPTIONS.equals(params[1])) {
						MessagePrompt prompt = new MessagePrompt();
						prompt.type = params[1];
						prompt.pageIndex = ArgUtil.parseAsInteger(params[2]);
						prompt.messageId = params[3];
						inboxMessage.setPrompt(prompt);
					}
				}
			}
		}
	}

	@Override
	public void reloadMedia(ChannelConfig channelConfig, MessageDoc msg) throws FileNotFoundException, IOException {
		List<Attachment> attach = msg.getAttachments();
		for (Attachment attachment : attach) {
			if (ArgUtil.is(attach) && attach.size() > 0) {
				reloadMedia(channelConfig, msg, attachment);
			}
		}
	}

	@Override
	public CommonFile reloadMedia(ChannelConfig channelConfig, MessageDoc msg, Integer index)
			throws FileNotFoundException, IOException {
		List<Attachment> attach = msg.getAttachments();
		if (ArgUtil.is(attach) && attach.size() > 0) {
			Attachment attachment = attach.get(index);
			return reloadMedia(channelConfig, msg, attachment);
		}
		return null;
	}

	public CommonFile reloadMedia(ChannelConfig channelConfig, MessageDoc msg, Attachment attachment)
			throws MalformedURLException, FileNotFoundException, IOException {
		CommonFileStream srcFile = new CommonFileStream().url(attachment.getMediaSrc())
				// .fileType(attachment.getMediaType())
				.format(FileFormat.from(attachment.getMediaMimeType()))
				// .header(WA360Constants.D360_API_KEY, channelConfig.getWa360d().getApiKey())
				.name(ArgUtil.nonEmpty(attachment.getMediaName(), attachment.getMediaCaption()));

		File fileb = Urly.parse(attachment.getMediaURL()).toFile();

		CommonFile dstFile = new CommonFile().url(attachment.getMediaURL()).path(fileb.getParent())
				.fileType(ArgUtil.parseAsEnumT(attachment.getMediaType(), FileType.class));
		return pmFileStoreClient.commitSessionFileSync(srcFile, dstFile);
	}

	public MessageReport toMessageReport(ChannelConfig channelConfig, InBoundMsgStatus status) {
		MessageReport report = this.createMessageReport(channelConfig);
		report.setMessageId(status.messageId);
		report.setMessageIdExt(status.messageIdExt);
		// report.setMessageIdRef(status.messageId);
		report.setChangeStamp(status.timestamp);
		report.contact().setContactId(status.contactId);
		if (ArgUtil.is(status.contact)) {
			report.contact().setEmail(status.contact.email);
			report.contact().phone(status.contact.phone);
			report.contact().setCsid(status.contact.csid);
		}
		Status st = ArgUtil.parseAsEnumT(status.status, Status.class);
		report.setStatus(st);
		if (ArgUtil.is(status.errors) && status.errors.size() > 0) {
			report.setReason("Code:" + status.errors.get(0).toCode());
			report.setErrors(status.errors);
		}
		return report;
	}

//	protected Map<String, Object> getProfile(ChatContactDoc chatContactDoc, OutboxMessage outboxMessage) {
//		Map<String, Object> profileMap = new HashMap<>();
//
//		try {
//			HSMTemplateDoc template = null;
//			if (ArgUtil.is(outboxMessage.templateId())) {
//				template = templateStore.findById(outboxMessage.templateId());
//			}
//
//			if (ArgUtil.isEmpty(template) && ArgUtil.is(outboxMessage.getHsm())
//					&& ArgUtil.is(outboxMessage.getHsm().getCode())) {
//				template = getTemplateByCode(outboxMessage.getHsm().getCode());
//			}
//
//			return getProfile(chatContactDoc, profileMap, template);
//
//		} catch (Exception e) {
//			logManager.error(e);
//		}
//		return profileMap;
//	}
//
//	private Map<String, Object> getProfile(ChatContactDoc chatContactDoc, Map<String, Object> profileMap,
//			HSMTemplateDoc template) throws IOException {
//		Map<String, Object> model = new HashMap<>();
//		CustomerProfileDoc profile = null;
//		if (ArgUtil.is(chatContactDoc) && ArgUtil.is(template) && template.getTemplate().contains("profile.")) {
//			if (ArgUtil.is(chatContactDoc.getPhone())) {
//				profile = contactStore.findProfileByPhone(chatContactDoc.getPhone());
//			} else if (ArgUtil.is(chatContactDoc.getEmail())) {
//				profile = contactStore.findProfileByEmail(chatContactDoc.getEmail());
//			}
//		}
//
//		if (profile != null && ArgUtil.is(template) && ArgUtil.is(template.getProfileVarMap())) {
//			Map<String, Object> profileVarMap = template.getProfileVarMap();
//
//			model.put("profile", JsonUtil.toMap(profile));
//
//			ObjectMapper mapper = new ObjectMapper();
//			JsonNode root = mapper.readTree(JsonUtil.toJson(model));
//
//			profileVarMap.forEach((key, value) -> {
//				String keyValue = ArgUtil.parseAsString(getValueByPath(root, value), PMConstants.NOT_AVALIABE);
//				profileMap.put(key, keyValue);
//			});
//		}
//
//		return profileMap;
//	}

//	private static String getValueByPath(JsonNode root, Object path) {
//		String[] parts = path.toString().split("\\.");
//		JsonNode current = root;
//		for (String part : parts) {
//			if (current == null)
//				return null;
//			current = current.get(part);
//
//			if (current != null && current.isArray() && current.size() > 0) {
//				current = current.get(0);
//			}
//		}
//		return current != null ? current.asText() : null;
//	}

	/**
	 * Validate template approval status for XMS API sends. Blocks send if template
	 * status exists and is not APPROVED.
	 * 
	 * @param outboxMessage The outbox message
	 * @return true if send should proceed, false if blocked
	 */
	protected boolean validateTemplateApproval(OutboxMessage outboxMessage) {
		String templateStatus = getTemplateApprovalStatus(outboxMessage);
		if (ArgUtil.is(templateStatus) && !ArgUtil.is(templateStatus.toUpperCase(), "APPROVED", "DELETED")) {
			String templateInfo = ArgUtil.nonEmpty(outboxMessage.templateCode(),
					ArgUtil.nonEmpty(outboxMessage.templateId(), "template"));
			String errorMessage = String.format("Template '%s' not approved. Status: %s", templateInfo, templateStatus);

			outboxMessage.updateStatus(OutboxMessage.Status.SENT_ERR);
			outboxMessage.logs().add(errorMessage);
			outboxMessage.options().put("templateNotApproved", errorMessage);

			LOGGER.warn("Blocked send - Template not approved: {} [{}]", templateInfo, templateStatus);
			return false;
		}
		return true;
	}

	/**
	 * Get template approval status from either third-party template or HSM template
	 * 
	 * @param outboxMessage The outbox message
	 * @return Template status (APPROVED, PENDING, REJECTED, etc.) or null if not
	 *         applicable
	 */
	protected String getTemplateApprovalStatus(OutboxMessage outboxMessage) {
		// Check third-party template status (Meta template)
		if (outboxMessage.getTemplateExt() != null && outboxMessage.getTemplateExt().getTemplate() != null) {
			return ArgUtil.parseAsString(outboxMessage.getTemplateExt().getTemplate().get("status"));
		}
		// Check HSM template approval status (regular HSM template)
		if (outboxMessage.options() != null) {
			return ArgUtil.parseAsString(outboxMessage.options().get("hsmTemplateApprovalStatus"));
		}
		return null;
	}

}
