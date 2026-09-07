package com.boot.jx.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.boot.jx.AppContextUtil;
import com.boot.jx.api.ApiFieldError;
import com.boot.jx.api.ApiResponseUtil;
import com.boot.jx.chat.ConnectorHandlerFactory.ConnectorHandler;
import com.boot.jx.inbound.InBound.InBoundHandler;
import com.boot.jx.logger.AuditDetailProvider;
import com.boot.jx.logger.LoggerService;
import com.boot.jx.mongo.CommonDocInterfaces.TimeStampIndex;
import com.boot.jx.mongo.CommonMongoQB.MongoQueryBuilder;
import com.boot.jx.postman.PMConstants;
import com.boot.jx.postman.PMConstants.CAMPAIGN_MODE;
import com.boot.jx.postman.PMConstants.CHAT_STATUS;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.PMEnvironment.AChannelDetails;
import com.boot.jx.postman.PMEnvironment.PMClientConfig;
import com.boot.jx.postman.PMEnvironment.PMDomainConfig;
import com.boot.jx.postman.doc.BulkSessionDoc;
import com.boot.jx.postman.doc.ChatContactDoc;
import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.dto.CampaignDto;
import com.boot.jx.postman.dto.ChatProfileDTO;
import com.boot.jx.postman.dto.ChatProfileDTO.ChatUserProfileRequest;
import com.boot.jx.postman.manager.ChatLogger;
import com.boot.jx.postman.manager.ChatSessionManager;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.model.PMArgs;
import com.boot.jx.postman.model.ext.InBoundEvent;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.plugin.ChannelPluginProvider;
import com.boot.jx.postman.plugin.ChannelPluginProvider.ChannelPlugin;
import com.boot.jx.postman.query.ChatContactQuery;
import com.boot.jx.postman.store.MessageContext;
import com.boot.jx.postman.store.SessionStore;
import com.boot.jx.utils.PostManUtil;
import com.boot.model.MapModel.NodeEntry;
import com.boot.utils.ArgUtil;
import com.boot.utils.Constants;
import com.boot.utils.StringUtils.StringMatcher;
import com.boot.utils.UniqueID;

@Component
public class ChatSessionService {

	private static final Logger LOGGER = LoggerService.getLogger(ChatSessionService.class);

	@Autowired
	private PMDomainConfig pmDomainConfig;

	@Autowired
	private PMClientConfig pmClientConfig;

	@Autowired
	private ChatSessionManager chatSessionManager;

	@Autowired
	private ConnectorHandlerFactory connectorHandlerFactory;

	@Autowired
	private ChatService chatService;

	@Autowired
	private MessageContext messageContext;

	@Autowired
	private SessionStore sessionStore;

	@Autowired
	private ChatClient chatClient;

	@Autowired
	private PMEnvironment enviroment;

	@Lazy
	@Autowired(required = false)
	private InBoundHandler inBoundHandler;

	@Autowired
	private ChatLogger logManager;

	@Autowired
	private ChatUtility chatUtility;

	@Autowired
	private SessionBoundEventQueue sessionBoundEventQueue;

	@Lazy
	@Autowired
	AuditDetailProvider auditDetailProvider;

	public boolean initSession(InboxMessage inboxMessage, ChatSessionDoc session) {
		boolean initd = session.isInitd();
		if (!initd) {
			ConnectorHandler connector = connectorHandlerFactory.get(inboxMessage.contact().type(),
					inboxMessage.contact().getChannelType());
			if (ArgUtil.is(connector)) {
				try {
					ChannelConfig config = connector.getChannelConfig(inboxMessage);
					ChannelPlugin<? extends AChannelDetails> plugin = ChannelPluginProvider
							.get(inboxMessage.contact().getChannelType());
					if (!ArgUtil.is(config)) {
						String channelId = PostManUtil.CHANNEL_ID(inboxMessage.contact());
						logManager.error(inboxMessage, "Channel Config Not Found:" + channelId);
					} else if (!ArgUtil.is(plugin)) {
						logManager.error(inboxMessage, "Channel Plugin Not Found:" + config.getChannelId());
					} else if (!ArgUtil.is(plugin.getDetails(config))) {
						logManager.error(inboxMessage, "Channel Details Not Found:" + config.getChannelId());
					}

					OutboxMessage reply = connector.initSession(session, inboxMessage);
					if (ArgUtil.is(reply)) {
						try {
							if (!OutboxMessage.NO_MESSAGE.equals(reply))
								chatService.reply(inboxMessage, reply);
							initd = false;
						} catch (InterruptedException e) {
							LOGGER.error("Errror While Replying To Sesion Init Message", e);
						}
					} else {
						initd = true;
					}
				} catch (Exception e) {
					logManager.error(inboxMessage, e);
					// e.printStackTrace();
				}
				if (initd) {
					inboxMessage.session().setInitMessage(true);
					connector.linkProfile(session, inboxMessage);
				}
				messageContext.commitChatContactQuery();
				try {
					messageContext.commitChatSessionQuery();
					messageContext.session().update(messageContext.contact().getDoc());
					messageContext.commitChatSessionQuery();
				} catch (Exception e) {
					logManager.error(inboxMessage, e);
				}
			}

			if (initd) { // Inbound Init Method
				InBoundEvent sessionInitEvent = chatSessionManager.initSession(inboxMessage, session);
				if (ArgUtil.is(inBoundHandler)) {
					// inBoundHandler.onSessionInit(sessionInitEvent, session);
					this.sessionEvent(sessionInitEvent, session);
				}
			}
		}

		if (initd) {
			if (chatUtility.isPushOnly(session)) {
				InBoundEvent routEvent = this.routeSession(session);
				inboxMessage.session().setQueue(session.getAssignedToQueue());
				inboxMessage.session().setDept(session.getAssignedToDept());
				inboxMessage.session().setAgent(session.getAssignedToAgent());
				inboxMessage.session().setBot(session.getAssignedToBot());
				inboxMessage.session().setRoutingId(session.getRoutingId());
			}
		}

		return session.isInitd();
	}

	public boolean initSession(OutboxMessage outboxMessage, ChatSessionDoc session) {
		boolean initd = session.isInitd();
		if (initd) {
			return true;
		}
		ConnectorHandler connector = connectorHandlerFactory.get(outboxMessage.contact().type(),
				outboxMessage.contact().getChannelType());

		messageContext.setOutboxMessage(outboxMessage);
		ChatContactQuery contactQuery = messageContext.contact();
		if (ArgUtil.is(connector)) {
			initd = connector.initSession(contactQuery, session, outboxMessage);
			// TODO:-- Validate if saving is required in case of outbound
			// sessionStore.save(contact);
		}
		if (initd) {
			session = sessionStore.initSession(session);
		}
		return session.isInitd();
	}

	@Async
	public void initSessionPost(InboxMessage inboxMessage, ChatSessionDoc session) {
		if (!ArgUtil.is(pmClientConfig.getContactDetailsUrl())) {
			return;
		}

		try {
			ChatContactDoc contact = sessionStore.getContact(inboxMessage);

			ChatUserProfileRequest chatUserProfileRequest = new ChatUserProfileRequest();
			chatUserProfileRequest.setEmail(contact.getEmail());
			chatUserProfileRequest.setMobile(contact.phone());
			chatUserProfileRequest.setContactId(contact.getContactId());
			chatUserProfileRequest.setContactType(contact.getContactType());
			chatUserProfileRequest.setLane(contact.getLane());
			chatUserProfileRequest.setProfileId(contact.getProfileId());
			ChatProfileDTO profile = chatClient.fetchContactDetails(chatUserProfileRequest);

			contact = sessionStore.getContact(inboxMessage);
			if (ArgUtil.is(profile.getProfileId())) {
				sessionStore.save(profile);
				contact.setProfileId(profile.getProfileId());
			} else {
				contact.setProfile(profile);
			}
			sessionStore.save(contact);
		} catch (Exception e) {

		}
	}

	public static final Pattern PING = Pattern.compile("\\/ping\\ ([a-zA-Z0-9_\\-]+)$");

	public boolean globalSession(InboxMessage inboxMessage) {
		String message = ArgUtil.nonEmpty(inboxMessage.getMessage(), Constants.BLANK);
		if ("/proxy".equalsIgnoreCase(message)) {
			try {
				OutboxMessage reply = inboxMessage.replyMessage(
						"DOMAIN :" + AppContextUtil.getTenant() + "." + enviroment.commonConfig().getServiceServer());
				chatService.reply(inboxMessage, reply);
				return true;
			} catch (Exception e) {
				LOGGER.error("Proxy Info Error", e);
			}
		}
		StringMatcher matcher = new StringMatcher(message);
		if (matcher.isMatch(PING)) {
			try {
				String token = matcher.group(1);
				OutboxMessage reply = inboxMessage.replyMessage("DOMAIN :" + AppContextUtil.getTenant() + "."
						+ enviroment.commonConfig().getServiceServer() + "\n" + "PONG  : " + token);
				chatService.reply(inboxMessage, reply);
				return true;
			} catch (Exception e) {
				LOGGER.error("PingPong Error", e);
			}
		}

		return false;
	}

	public InBoundEvent routeSession(ChatSessionDoc sessionDoc, PMArgs pmArgs) {
		InBoundEvent event = chatSessionManager.assignToQueue(sessionDoc, pmArgs);
		event.sessionRouted.params = pmArgs.getParams();
		if (ArgUtil.is(inBoundHandler)) {
			inBoundHandler.onSessionRouteAsync(event, sessionDoc, pmArgs);
		}
		return event;
	}

	public InBoundEvent routeSession(ChatSessionDoc session) {
		String defaultQueue = pmDomainConfig.getDefaultInboundQueue(session.contact());
		return routeSession(session, new PMArgs().assignToQueueCode(defaultQueue)
				// Added Contact Info as it is missing later
				.contact(session.contact()));
	}

	public InBoundEvent routeSession(String sessionId, PMArgs pmArgs) {
		ChatSessionDoc sessionDoc = sessionStore.getSession(sessionId);
		return routeSession(sessionDoc, pmArgs);
	}

	public NodeEntry<InBoundEvent> updateSessionStatus(ChatSessionDoc sessionDoc, CHAT_STATUS status) {
		NodeEntry<InBoundEvent> event = updateSessionStatusinternal(sessionDoc, status);

		if (event.exists()) {
			sessionBoundEventQueue.publish(event.getValue());
		}

		return event;
	}

	private NodeEntry<InBoundEvent> updateSessionStatusinternal(ChatSessionDoc sessionDoc, CHAT_STATUS status) {
		NodeEntry<InBoundEvent> eventEntry = new NodeEntry<InBoundEvent>();

		if (!ArgUtil.is(status)) {
			return eventEntry;
		}

		String oldStatus = sessionDoc.getStatus();
		if (status.toString().equalsIgnoreCase(oldStatus)) {
			return eventEntry;
		}
		if (status == PMConstants.CHAT_STATUS.RESOLVED) {
			NodeEntry<InBoundEvent> eventEntry2 = chatSessionManager.resolveSession(sessionDoc);
			if (ArgUtil.is(inBoundHandler) && eventEntry2.exists()) {
				inBoundHandler.onSessionResolveAsync(eventEntry2.getValue(), sessionDoc);
			}
			return eventEntry2;
		} else if (status == PMConstants.CHAT_STATUS.CLOSED) {
			InBoundEvent event = chatSessionManager.closeSession(sessionDoc);
			if (ArgUtil.is(inBoundHandler) && ArgUtil.is(event)) {
				inBoundHandler.onSessionCloseAsync(event, sessionDoc);
			}
			return eventEntry.value(event);

		} else if (status == PMConstants.CHAT_STATUS.EXPIRED) {
			InBoundEvent event = chatSessionManager.expireSession(sessionDoc);
			if (ArgUtil.is(inBoundHandler) && ArgUtil.is(event)) {
				inBoundHandler.onSessionStatusAsync(event, sessionDoc);
			}
			return eventEntry.value(event);
		} else {
			InBoundEvent event = chatSessionManager.updateStatus(sessionDoc, status);
			if (ArgUtil.is(inBoundHandler) && ArgUtil.is(event)) {
				inBoundHandler.onSessionStatusAsync(event, sessionDoc);
			}
			return eventEntry.value(event);
		}
	}

	public NodeEntry<InBoundEvent> closeSession(ChatSessionDoc chatSessionDoc, CHAT_STATUS reasonStatus) {
		if (!chatSessionDoc.isResolved()) {
			return updateSessionStatus(chatSessionDoc,
					ArgUtil.nonEmpty(reasonStatus, PMConstants.CHAT_STATUS.RESOLVED));
		}
		return updateSessionStatus(chatSessionDoc, PMConstants.CHAT_STATUS.CLOSED);
	}

	public NodeEntry<InBoundEvent> closeSession(String sessionId, CHAT_STATUS reasonStatus) {
		ChatSessionDoc sessionDoc = sessionStore.getSession(sessionId);
		return closeSession(sessionDoc, reasonStatus);
	}

	public NodeEntry<InBoundEvent> resolveSession(ChatSessionDoc chatSessionDoc) {
		NodeEntry<InBoundEvent> eventEntry = new NodeEntry<InBoundEvent>();
		if (!chatSessionDoc.isResolved()) {
			eventEntry = updateSessionStatus(chatSessionDoc, PMConstants.CHAT_STATUS.RESOLVED);
		}
		return eventEntry;
	}

	public NodeEntry<InBoundEvent> resolveSession(String sessionId) {
		ChatSessionDoc sessionDoc = sessionStore.getSession(sessionId);
		return resolveSession(sessionDoc);
	}

	public NodeEntry<InBoundEvent> assignSessionToAgent(PMArgs params) {
		ChatSessionDoc sessionDoc = sessionStore.getSession(params.getSessionId());
		messageContext.session(sessionDoc);
		return inBoundHandler.assignSessionToAgent(params, sessionDoc);
	}

	public NodeEntry<InBoundEvent> assignSessionToAgent(ChatSessionDoc sessionDoc, PMArgs params) {
		return inBoundHandler.assignSessionToAgent(params, sessionDoc);
	}

	public InBoundEvent sessionEvent(InBoundEvent event, PMArgs params) {
		if (ArgUtil.is(inBoundHandler)) {
			return inBoundHandler.onSessionEvent(event, params);
		}
		return event;
	}

	public InBoundEvent sessionEvent(InBoundEvent event, ChatSessionDoc session) {
		if (ArgUtil.is(inBoundHandler)) {
			return inBoundHandler.onSessionEvent(event, new PMArgs().assignToQueueCode(session.getAssignedToQueue())
					.contact(session.contact()).sessionId(session.getSessionId()));
		}
		return event;
	}

	/**
	 * TODO:- Move this mehtod to Campaign Service
	 * 
	 * @param campaignTitle
	 * @param templateCode
	 * @param campaignMode
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public CampaignDto createBulkCampaign(String campaignTitle, String templateCode, CAMPAIGN_MODE campaignMode,
			String contactType, String channelId, String resendType) {
		BulkSessionDoc session = new BulkSessionDoc();
		if (ArgUtil.isEmpty(contactType)) {
			ApiResponseUtil.throwInputException(
					new ApiFieldError().field("channel").codeKey("contactType").description("Contact Type is missing"));
		}
		ChannelConfig channelConfig = null;
		if (ArgUtil.is(channelId)) {
			channelConfig = enviroment.config().channel(channelId);
		}
		session.setChannelId(channelId);
		if (ArgUtil.is(channelConfig)) {
			session.setContactType(channelConfig.getContactType().name());
			session.setLane(channelConfig.getLane());
		} else {
			session.setContactType(contactType);
		}
		session.setBulkSessionId(UniqueID.generateString62());
		session.setCampaignTitle(campaignTitle);
		session.setTemplate(templateCode);

		session.setCreatedBy(auditDetailProvider.getAuditUser());
		session.setCreatedStamp(TimeStampIndex.now().getStamp());
		if (ArgUtil.is(campaignMode)) {
			session.setCampaignMode(campaignMode);
		} else {
			session.setCampaignMode(CAMPAIGN_MODE.DEFAULT);
		}
		// optional; null when not passed by UI/Node
		session.setResendType(resendType);
		sessionStore.save(session);
		CampaignDto dto = new CampaignDto();
		dto.setCampaignId(session.getBulkSessionId());
		dto.setCampaignTitle(session.getCampaignTitle());
		dto.setTemplateCode(session.getTemplate());
		dto.setCampaignMode(session.getCampaignMode());
		dto.setContactType(session.getContactType());
		dto.setResendType(session.getResendType());
		return dto;
	}

	/**
	 * TODO:- Move this mehtod to Campaign Service
	 * 
	 * @param campaignTitle
	 * @param templateCode
	 * @param campaignMode
	 * @return
	 */
	@Deprecated
	public List<CampaignDto> viewBulkCampaign(String campaignTitle, String templateCode, CAMPAIGN_MODE campaignMode) {
		List<CampaignDto> campLst = new ArrayList<>();
		MongoQueryBuilder<BulkSessionDoc> builder = MongoQueryBuilder.collection(BulkSessionDoc.class);

		if (ArgUtil.is(campaignTitle)) {
			builder.where("campaignTitle").is(campaignTitle);
		}
		if (ArgUtil.is(templateCode)) {
			builder.where("template").is(templateCode);
		}

		if (ArgUtil.is(campaignMode)) {
			builder.where("campaignMode").is(campaignMode);
		}

		builder.includeDBRef("bulkSessionId").includeDBRef("campaignTitle").includeDBRef("template")
				.includeDBRef("template").includeDBRef("resendType");
		List<BulkSessionDoc> lst = sessionStore.find(builder);
		if (ArgUtil.is(lst)) {
			for (BulkSessionDoc dc : lst) {
				CampaignDto dt = new CampaignDto();
				dt.setCampaignId(dc.getBulkSessionId());
				dt.setCampaignTitle(dc.getCampaignTitle());
				dt.setTemplateCode(dc.getTemplate());
				dt.setCampaignMode(dc.getCampaignMode());
				dt.setResendType(dc.getResendType());
				if (ArgUtil.is(dt.getTemplateCode())) {
					campLst.add(dt);
				}
			}
		}

		return campLst;
	}

}
