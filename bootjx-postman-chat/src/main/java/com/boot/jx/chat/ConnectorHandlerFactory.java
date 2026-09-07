package com.boot.jx.chat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;

import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.boot.jx.AppConfig;
import com.boot.jx.AppContextUtil;
import com.boot.jx.auth.AuthStateManager.AuthState;
import com.boot.jx.chat.ConnectorHandlerFactory.ConnectorHandler;
import com.boot.jx.connectors.AbstractConnector.DefaultConnector;
import com.boot.jx.dict.ContactType;
import com.boot.jx.exception.AmxApiException;
import com.boot.jx.inbound.InBound.MessageEvents;
import com.boot.jx.logger.LoggerService;
import com.boot.jx.model.CommonFile;
import com.boot.jx.mongo.CommonMongoTemplate;
import com.boot.jx.postman.PMConfiguration;
import com.boot.jx.postman.PMConstants;
import com.boot.jx.postman.PMConstants.MESSAGE_COMPOSE_TYPE;
import com.boot.jx.postman.PMConstants.MESSAGE_SEND_TYPE;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.channel.ChannelClientFactory.ChannelClient;
import com.boot.jx.postman.doc.ChatContactDoc;
import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.doc.MessageDoc;
import com.boot.jx.postman.doc.config.ChannelConfigDoc;
import com.boot.jx.postman.doc.config.ChannelConfigLogger;
import com.boot.jx.postman.dto.ChatMessageDTO;
import com.boot.jx.postman.guard.MessageFailureGuard;
import com.boot.jx.postman.manager.ChatLogger;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.Message;
import com.boot.jx.postman.model.Message.Status;
import com.boot.jx.postman.model.MessageBoxEvent;
import com.boot.jx.postman.model.MessageDefinitions.IMessage;
import com.boot.jx.postman.model.MessageDefinitions.IMessageExtended;
import com.boot.jx.postman.model.MessageReport;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.model.outbound.OutBoundMsgBasic.ApiOutBoundMsg;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.plugin.ChannelPluginProvider.ChannelBasedFactory;
import com.boot.jx.postman.query.ChatContactQuery;
import com.boot.jx.postman.query.ChatSessionQuery;
import com.boot.jx.postman.service.BillingCounterService;
import com.boot.jx.postman.service.ChatDTOUtil;
import com.boot.jx.postman.store.MessageContext;
import com.boot.jx.postman.store.MessageStore;
import com.boot.jx.stomp.StompTunnelService;
import com.boot.jx.tunnel.TunnelService;
import com.boot.jx.tunnel.task.BatchJobProvider.BatchJobPool;
import com.boot.jx.utils.PostManUtil;
import com.boot.model.MapModel;
import com.boot.utils.ArgUtil;
import com.boot.utils.Constants;
import com.boot.utils.TimeUtils;

@Component
public class ConnectorHandlerFactory extends ChannelBasedFactory<ConnectorHandler> {

	private static final long serialVersionUID = 4007091611441725719L;

	public static Logger LOGGER = LoggerService.getLogger(ConnectorHandlerFactory.class);

	public interface ConnectorHandler {

		public static final long DEFAULT_SESISON_PERIOD = TimeUtils.toMillis("24h");

		default public Status reply(ChannelConfig channelConfig, ChatContactDoc chatContactDoc,
				OutboxMessage outboxMessage, IMessageExtended inboxMessage) {
			if (!ArgUtil.is(channelConfig)) {
				channelConfig = getChannelConfig(outboxMessage);
			}

			if (!ArgUtil.is(chatContactDoc)) {
				chatContactDoc = getChatContact(outboxMessage);
			}

			outboxMessage.addTo(inboxMessage.getFrom());
			outboxMessage.contact().setLane(inboxMessage.contact().getLane());
			if (this.beforeSend(channelConfig, chatContactDoc, outboxMessage, null)) {
				return this.onSend(channelConfig, chatContactDoc, outboxMessage, null);
			}
			return null;
		}

		/**
		 * Message to be send while initiating new session
		 * 
		 * @param channelConfig
		 * @param chatContactDoc
		 * @param outboxMessage
		 * @return TODO
		 */
		default public Status send(ChannelConfig channelConfig, ChatContactDoc chatContactDoc,
				OutboxMessage outboxMessage) {
			if (!ArgUtil.is(channelConfig)) {
				channelConfig = getChannelConfig(outboxMessage);
			}

			if (!ArgUtil.is(chatContactDoc)) {
				chatContactDoc = getChatContact(outboxMessage);
			}
			outboxMessage.addTo(chatContactDoc.getCsid());
			outboxMessage.contact().setLane(chatContactDoc.getLane());
			if (this.beforeSend(channelConfig, chatContactDoc, outboxMessage, null)) {
				return this.onSend(channelConfig, chatContactDoc, outboxMessage, null);
			} else {
				if (!ArgUtil.is(outboxMessage.logs().size())) {
					outboxMessage.logs().add("blocked");
				}
			}
			return null;
		}

		/**
		 * Message to be send while initiating new session
		 * 
		 * @param channelConfig
		 * @param chatContactDoc
		 * @param outboxMessage
		 * @param outBoundMsg
		 * @return TODO
		 */
		default public Status push(ChannelConfig channelConfig, ChatContactDoc chatContactDoc,
				OutboxMessage outboxMessage, ApiOutBoundMsg outBoundMsg) {
			if (!ArgUtil.is(channelConfig)) {
				channelConfig = getChannelConfig(outboxMessage);
			}

			if (!ArgUtil.is(chatContactDoc)) {
				chatContactDoc = getChatContact(outboxMessage);
			}
			outboxMessage.addTo(chatContactDoc.getCsid());
			outboxMessage.contact().setLane(chatContactDoc.getLane());
			if (this.beforeSend(channelConfig, chatContactDoc, outboxMessage, outBoundMsg)) {
				return this.onSend(channelConfig, chatContactDoc, outboxMessage, outBoundMsg);
			}
			return null;
		}

		default public InboxMessage assignToAgent(InboxMessage inboxMessage) {
			return inboxMessage;
		}

		default public OutboxMessage initSession(ChatSessionDoc session, InboxMessage inboxMessage) {
			return null;
		}

		default public boolean initSession(ChatContactQuery contactQuery, ChatSessionDoc session,
				OutboxMessage outboxMessage) {
			return true;
		}

		default public void meta(ChannelConfig channelConfig, String messageType, ChatContactDoc chatContactDoc,
				IMessageExtended inboxMessage, OutboxMessage outboxMessage) {
			if (TimeUtils.isExpired(chatContactDoc.getLastInBoundStamp(), DEFAULT_SESISON_PERIOD)) {
				outboxMessage.messageMetaWrapper().sendType(MESSAGE_SEND_TYPE.PUSH_MESSAGE); // Push Message
			} else {
				outboxMessage.messageMetaWrapper().sendType(MESSAGE_SEND_TYPE.SESSION_MESSAGE); // Session Message
			}
			outboxMessage.messageMetaWrapper().channelId(channelConfig.getChannelId());
		}

		default public void message(ChannelConfig channelConfig, String messageType, ChatContactDoc chatContactDoc,
				OutboxMessage outboxMessage, IMessageExtended inboxMessage, ApiOutBoundMsg outBoundMsg) {
			LOGGER.debug("message(String {}, ChatContactDoc {}, SessionMessage {}, OutboxMessage {})", messageType,
					chatContactDoc, inboxMessage, outboxMessage);
			try {
				outboxMessage.updateStatus(Status.SEND);
				Status status = null;
				switch (messageType) {
				case MESSAGE_COMPOSE_TYPE.SEND:
					outboxMessage.messageMetaWrapper().composeType(MESSAGE_COMPOSE_TYPE.SEND_CODE); // is a New Message
					this.meta(channelConfig, messageType, chatContactDoc, inboxMessage, outboxMessage);
					status = this.send(channelConfig, chatContactDoc, outboxMessage);
					outboxMessage.updateStatus(ArgUtil.anyOf(status, Status.SENT));
					break;
				case MESSAGE_COMPOSE_TYPE.REPLY:
					outboxMessage.messageMetaWrapper().composeType(MESSAGE_COMPOSE_TYPE.REPLY_CODE); // Its a Reply
					this.meta(channelConfig, messageType, chatContactDoc, inboxMessage, outboxMessage);
					status = this.reply(channelConfig, chatContactDoc, outboxMessage, inboxMessage);
					outboxMessage.updateStatus(ArgUtil.anyOf(status, Status.SENT));
					break;
				case MESSAGE_COMPOSE_TYPE.PUSH:
					outboxMessage.messageMetaWrapper().composeType(MESSAGE_COMPOSE_TYPE.PUSH_CODE); // Its a Push
					this.meta(channelConfig, messageType, chatContactDoc, inboxMessage, outboxMessage);
					status = this.push(channelConfig, chatContactDoc, outboxMessage, outBoundMsg);
					outboxMessage.updateStatus(ArgUtil.anyOf(status, Status.SENT));
					break;
				default:
					break;
				}
			} catch (Exception e) {
				onException(channelConfig, chatContactDoc, outboxMessage, e);
			}
			outboxMessage.timer().log("chm:z");

		}

		default public void onException(ChannelConfig channelConfig, ChatContactDoc chatContactDoc,
				OutboxMessage outboxMessage, Exception e) {
			if (e instanceof AmxApiException) {
				outboxMessage.updateStatus(OutboxMessage.Status.SENT_ERR);
				outboxMessage.logs().add(((AmxApiException) e).getErrorKey());
			} else {
				outboxMessage.updateStatus(Message.Status.SENT_EXC);
				outboxMessage.logs().add(e.getMessage());
				LOGGER.error("SEND ERROR", e);
			}
		}

		boolean beforeSend(ChannelConfig channelConfig, ChatContactDoc chatContactDoc, OutboxMessage outboxMessage,
				ApiOutBoundMsg outBoundMsg);

		Status onSend(ChannelConfig channelConfig, ChatContactDoc chatContactDoc, OutboxMessage outboxMessage,
				ApiOutBoundMsg outBoundMsg);

		default void onChannelUpdate(ChannelConfig channelConfig, ChannelConfigLogger channelConfigLogger) {
			LOGGER.error("WEBHOOK onChannelUpdate NOT FOUND ");
		}

		default InboxMessage createInboxMessage(ChannelConfig channelConfig, InboxMessage inboxMessage) {
			if (ArgUtil.is(channelConfig)) {
				inboxMessage.contact().type(channelConfig.getContactType());
				inboxMessage.contact().setChannelType(channelConfig.getChannelType());
				inboxMessage.contact().setLane(channelConfig.getLane());
			}
			return inboxMessage;
		}

		default InboxMessage createInboxMessage(ChannelConfig channelConfig) {
			InboxMessage inboxMessage = new InboxMessage();
			createInboxMessage(channelConfig, inboxMessage);
			return inboxMessage;
		}

		default MessageReport createMessageReport(ChannelConfig channelConfig) {
			MessageReport messageReport = new MessageReport();
			if (ArgUtil.is(channelConfig)) {
				messageReport.contact().type(channelConfig.getContactType());
				messageReport.contact().setChannelType(channelConfig.getChannelType());
				messageReport.contact().setLane(channelConfig.getLane());
			}
			return messageReport;
		}

		/**
		 * 
		 * This method is invoked when message from ChannelProvider is recvd and is to
		 * be formatted into MessageBoxEvent format Basically this method should be
		 * implemented to convert channel message formats to internal formats
		 * 
		 * @param channelConfig
		 * @param requestMap
		 * @param messageBoxEvent TODO
		 * @return
		 */
		public MessageBoxEvent inboundMessageBoxEvent(ChannelConfig channelConfig, MapModel requestMap,
				MessageBoxEvent messageBoxEvent);

		default List<InboxMessage> beforeReceiveInboxMessage(List<InboxMessage> inboxMessages) {
			return inboxMessages;
		}

		/**
		 * This method is invoked after message from ChannelProvider has been processed
		 * 
		 * @param inboxMessages
		 * 
		 * @return
		 */
		default List<InboxMessage> onReceiveInboxMessage(List<InboxMessage> inboxMessages) {
			return inboxMessages;
		}

		default void onMessageReports(List<MessageReport> messageReports) {
			// DO Nothing this method is optional
		}

		public ChannelConfig getChannelConfig(IMessage outboxMessage);

		public ChatContactDoc getChatContact(IMessage outboxMessage);

		public OutboxMessage template(ChannelConfig channelConfig, ChatContactDoc chatContactDoc,
				OutboxMessage outboxMessage);

		boolean optin(ChannelConfig channelConfig, ChatContactDoc chatContactDoc);

		void prompt(InboxMessage inboxMessage);

		void linkProfile(ChatSessionDoc session, InboxMessage inboxMessage);

		CommonFile reloadMedia(ChannelConfig channelConfig, MessageDoc msg, Integer index)
				throws FileNotFoundException, IOException;

		void reloadMedia(ChannelConfig channelConfig, MessageDoc msg) throws FileNotFoundException, IOException;

		default List<ChannelConfig> onRegister(ChannelConfig setup, ChannelConfigLogger resp, AuthState state) {
			LOGGER.error("Channel onRegister NOT FOUND ");
			return null;
		}

		public ChannelClient getClient(ChannelConfig channelConfig);

		default public String createAuthUrl(ChannelConfig setup, ChannelConfigLogger channelConfigTemp, AuthState state)
				throws URISyntaxException, MalformedURLException {
			return Constants.BLANK;
		}

	}

	public ConnectorHandlerFactory(List<ConnectorHandler> libs) {
		super(libs);
	}

	@Override
	public ConnectorHandler getDefault() {
		return this.defaultConnector;
	}

	@Autowired(required = false)
	private DefaultConnector<?, ?> defaultConnector;

	@Autowired
	private MessageStore messageStore;

	@Autowired
	private StompTunnelService stompTunnelService;

	@Autowired
	public CommonMongoTemplate commonMongoTemplate;

	@Autowired
	private PMEnvironment environment;

	@Autowired
	protected MessageContext messageContext;

	@Lazy
	@Autowired(required = false)
	private MessageEvents messageEvents;

	@Autowired
	protected ChatLogger logManager;

	@Autowired(required = false)
	private BillingCounterService billingCounterService;

	@Autowired
	private TunnelService tunnelService;

	@Autowired(required = false)
	private RedissonClient redisson;

	@Autowired(required = false)
	private AppConfig appConfig;

	@Autowired(required = false)
	private MessageFailureGuard messageFailureGuard;

	private BatchJobPool jobPool;

	public BatchJobPool jobPool() {
		if (this.jobPool == null) {
			this.jobPool = new BatchJobPool("DISPATCHER", tunnelService, redisson, appConfig.getAppName());
		}
		return this.jobPool;
	}

	/**
	 * 
	 * @param channelType
	 * @param lane
	 * @return TODO
	 */
	public ChannelConfig onChannelUpdate(String channelType, String lane) {
		LOGGER.info("onChannelUpdate({},{})", channelType, lane);
		String channelId = PostManUtil.CHANNEL_ID(channelType, lane);
		PMConfiguration config = environment.local();
		ChannelConfig channelConfig = config.channel(channelId);
		if (!ArgUtil.is(channelConfig)) {
			channelConfig = commonMongoTemplate.findById(channelId, ChannelConfigDoc.class);
		}
		return onChannelUpdate(channelConfig);
	}

	public ChannelConfig onChannelUpdate(ChannelConfig channelConfig) {
		LOGGER.info("onChannelUpdate");
		if (ArgUtil.is(channelConfig)) {
			ConnectorHandler connector = get(channelConfig.getContactType(), channelConfig.getChannelType());
			if (ArgUtil.is(connector)) {
				try {
					ChannelConfigLogger channelConfigTemp = commonMongoTemplate
							.findById(channelConfig.getChannelConfigTempId(), ChannelConfigLogger.class);
					if (!ArgUtil.is(channelConfigTemp)) {
						channelConfigTemp = new ChannelConfigLogger();
						channelConfigTemp.setChannelConfigId(channelConfig.getMasterChannelId());
						channelConfigTemp.setChannelType(channelConfig.getChannelType());
						channelConfigTemp.setDomain(AppContextUtil.getTenant());
					}
					connector.onChannelUpdate(channelConfig, channelConfigTemp);
					commonMongoTemplate.save(channelConfigTemp);
				} catch (Exception e) {
					LOGGER.error("error onChannelUpdate " + channelConfig, e);
				}
			} else {
				LOGGER.info("connector:NOT_FOUND {} {}", channelConfig.getContactType(),
						channelConfig.getChannelType());
			}
		} else {
			LOGGER.info("channelConfig:NOT_FOUND");
		}
		return channelConfig;
	}

	/**
	 * THis message is just to fix values which are missing for any reason, add only
	 * if you are sure it is required
	 * 
	 * @param outboxMessage
	 * @param inboxMessage
	 */
	private void fixMessageBodyPostSendAndBeforeEvent(OutboxMessage outboxMessage, IMessageExtended inboxMessage) {
		if (!ArgUtil.is(outboxMessage.session().getQueue()) && ArgUtil.is(inboxMessage)) {
			outboxMessage.session().setQueue(inboxMessage.session().getQueue());
		}
	}

	/**
	 * 
	 * Should always be last method or not changes in chatContactDoc or
	 * outboxMessage after this;
	 * 
	 * @param messageType
	 * @param chatContactDoc
	 * @param outboxMessage
	 * @param inboxMessage
	 */
//	@Async
//	public void message(MessageContext context, String messageType, ChatContactDoc chatContactDoc,
//			OutboxMessage outboxMessage, IMessageExtended inboxMessage) {
//		messageContext.from(context);
//		LOGGER.debug("message(String {}, ChatContactDoc {}, IMessageExtended {}, OutboxMessage {})", messageType,
//				chatContactDoc, inboxMessage, outboxMessage);
//
//		messageByConnector(messageType, chatContactDoc, outboxMessage, inboxMessage, null);
//
//		MessageDoc messageDoc = messageStore.createOrUpdate(outboxMessage);
//
//		if (ArgUtil.isEqual(messageType, "REPLY", "SEND")) {
//			ChatContactQuery contactQuery = ArgUtil.is(chatContactDoc) ? new ChatContactQuery(chatContactDoc)
//					: new ChatContactQuery(outboxMessage.contact().getContactId());
//			ChatSessionQuery sessionQuery = new ChatSessionQuery(outboxMessage.getSessionId());
//			long now = System.currentTimeMillis();
//			contactQuery.setLastOutBoundStamp(now);
//			sessionQuery.setLastOutGoingStamp(now);
//			
//			// DAU Counter: Increment counter for outbound messages (reuse chatContactDoc to avoid DB query)
//			if (billingCounterService != null) {
//				String contactId = outboxMessage.contact().getContactId();
//				boolean incremented = billingCounterService.incrementDAUCounter(contactId, chatContactDoc);
//				
//				// If counter was incremented and chatContactDoc exists, update lastDAUStamp in the query
//				if (incremented && chatContactDoc != null) {
//					contactQuery.setLastDAUStamp(chatContactDoc.getLastDAUStamp());
//				}
//			}
//			
//			if (ArgUtil.is(chatContactDoc)) {
//				if (ArgUtil.isEmptyValue(chatContactDoc.getFirstOutBoundStamp())) {
//					contactQuery.setFirstOutBoundStamp(now);
//				}
//			}
//
//			switch (messageType) {
//			case MESSAGE_COMPOSE_TYPE.REPLY:
//				contactQuery.setLastReplyStamp(now);
//				sessionQuery.setLastResponseStamp(now);
//				break;
//			case MESSAGE_COMPOSE_TYPE.SEND:
//				contactQuery.setLastPushStamp(now);
//				break;
//			default:
//				break;
//			}
//
//			if (ArgUtil.is(inboxMessage) && ArgUtil.is(inboxMessage.contact())) {
//				contactQuery.update(inboxMessage.contact());
//			}
//
//			sessionQuery.setLastMsg(ChatDTOUtil.getChatMessageDTO(messageDoc));
//			commonMongoTemplate.updateFirst(sessionQuery);
//			commonMongoTemplate.updateFirst(contactQuery);
//		}
//
//		if (PMConstants.CHAT_MODE.AGENT.toString().equals(outboxMessage.session().getMode())
//				&& ArgUtil.is(outboxMessage.session().getDept())) {
//			ChatMessageDTO messageDto = ChatDTOUtil.getChatMessageDTO(messageDoc);
//			stompTunnelService.sendToTag(outboxMessage.session().getDept(), "/message/sent/new", messageDto);
//		}
//
//		if (messageEvents != null) {
//			fixMessageBodyPostSendAndBeforeEvent(outboxMessage, inboxMessage);
//			if (ArgUtil.is(messageType, MESSAGE_COMPOSE_TYPE.ACTION)) {
//				messageEvents.postAction(outboxMessage);
//			} else {
//				messageEvents.postMessageOutBound(outboxMessage);
//			}
//		}
//	}

	@Async
	public void message(MessageContext context, String messageType, ChatContactDoc chatContactDoc,
			OutboxMessage outboxMessage, IMessageExtended inboxMessage) {
		outboxMessage.timer().log("cfm:0");
		messageContext.from(context);

		if (LOGGER.isDebugEnabled())
			LOGGER.debug("message(String {}, ChatContactDoc {}, IMessageExtended {}, OutboxMessage {})", messageType,
					chatContactDoc, inboxMessage, outboxMessage);

		messageByConnector(messageType, chatContactDoc, outboxMessage, inboxMessage, null);

		MessageDoc messageDoc = messageStore.createOrUpdate(outboxMessage);

		if (ArgUtil.isEqual(messageType, "REPLY", "SEND")) {
			ChatSessionQuery sessionQuery = new ChatSessionQuery(outboxMessage.getSessionId());
			long now = System.currentTimeMillis();
			sessionQuery.setLastOutGoingStamp(now);

			// OPTIMIZATION: Try to reuse contactQuery from MessageContext (includes
			// lastDAUStamp/lastMAUStamp updates from billing counter)
			ChatContactQuery contactQuery = messageContext.getChatContactQuery();
			// Original code: Create new query if not available in MessageContext
			if (contactQuery == null) {
				contactQuery = ArgUtil.is(chatContactDoc) ? new ChatContactQuery(chatContactDoc)
						: new ChatContactQuery(outboxMessage.contact().getContactId());
			}
			contactQuery.setLastOutBoundStamp(now);

			if (ArgUtil.is(chatContactDoc)) {
				if (ArgUtil.isEmptyValue(chatContactDoc.getFirstOutBoundStamp())) {
					contactQuery.setFirstOutBoundStamp(now);
				}
			}

			switch (messageType) {
			case MESSAGE_COMPOSE_TYPE.REPLY:
				contactQuery.setLastReplyStamp(now);
				sessionQuery.setLastResponseStamp(now);
				break;
			case MESSAGE_COMPOSE_TYPE.SEND:
				contactQuery.setLastPushStamp(now);
				break;
			default:
				break;
			}

			if (ArgUtil.is(inboxMessage) && ArgUtil.is(inboxMessage.contact())) {
				contactQuery.update(inboxMessage.contact());
			}

			if (messageFailureGuard != null) {
				messageFailureGuard.capture(outboxMessage, contactQuery);
			}
			sessionQuery.setLastMsg(ChatDTOUtil.getChatMessageDTO(messageDoc));
			commonMongoTemplate.updateFirst(sessionQuery);
			// Commit contactQuery with all updates: lastDAUStamp/lastMAUStamp (from billing
			// counter) + lastOutBoundStamp + other fields
			commonMongoTemplate.updateFirst(contactQuery);
		}

		if (PMConstants.CHAT_MODE.AGENT.toString().equals(outboxMessage.session().getMode())
				&& ArgUtil.is(outboxMessage.session().getDept())) {
			ChatMessageDTO messageDto = ChatDTOUtil.getChatMessageDTO(messageDoc);
			stompTunnelService.sendToTag(outboxMessage.session().getDept(), "/message/sent/new", messageDto);
		}

		if (messageEvents != null) {
			fixMessageBodyPostSendAndBeforeEvent(outboxMessage, inboxMessage);
			if (ArgUtil.is(messageType, MESSAGE_COMPOSE_TYPE.ACTION)) {
				messageEvents.postAction(outboxMessage);
			} else {
				messageEvents.postMessageOutBound(outboxMessage);
			}
		}
		outboxMessage.timer().log("cfm:z");
	}

	public void messageSync(MessageContext context, String messageType, ChatContactDoc chatContactDoc,
			OutboxMessage outboxMessage, IMessageExtended inboxMessage) {
		this.message(context, messageType, chatContactDoc, outboxMessage, inboxMessage);
	}
//	private void messageByConnector(String messageType, ChatContactDoc chatContactDoc, OutboxMessage outboxMessage,
//			IMessageExtended inboxMessage, ApiOutBoundMsg outBoundMsg) {
//		String channelId = PostManUtil.CHANNEL_ID(outboxMessage.contact());
//		ChannelConfig channelConfig = environment.config().channel(channelId);
//
//		try {
//			if (!environment.gateKeeper().canSendMessage(outboxMessage, chatContactDoc)) {
//				// outboxMessage.logs().add(String.format("Insufficient Balance"));
//			} else if (ArgUtil.is(channelConfig) || ContactType.WEBSITE.equals(outboxMessage.contact().type())) {
//				ConnectorHandler connector = get(channelConfig);
//				if (ArgUtil.is(connector)) {
//					connector.message(channelConfig, messageType, chatContactDoc, outboxMessage, inboxMessage,
//							outBoundMsg);
//				} else {
//					outboxMessage.logs().add(String.format("Connector not defined for %s", channelId));
//				}
//			} else {
//				outboxMessage.logs().add(String.format("ChannelConfig not found for %s", channelId));
//			}
//
//		} catch (Exception e) {
//			logManager.error(outboxMessage, e);
//		}
//	}

	private void messageByConnector(String messageType, ChatContactDoc chatContactDoc, OutboxMessage outboxMessage,
			IMessageExtended inboxMessage, ApiOutBoundMsg outBoundMsg) {

		String channelId = PostManUtil.CHANNEL_ID(outboxMessage.contact());
		ChannelConfig channelConfig = environment.config().channel(channelId);

		// Add fallback logic for legacy channels (e.g., wa360 -> wacfb)
		if (!ArgUtil.is(channelConfig)) {
			String fallbackChannelId = PostManUtil.CHANNEL_ID_FALLBACK(channelId);
			if (ArgUtil.is(fallbackChannelId)) {
				channelConfig = environment.config().channel(fallbackChannelId);

				if (ArgUtil.is(channelConfig)) {
					channelId = fallbackChannelId;

					outboxMessage.contact().setChannelType(channelConfig.getChannelType());
					if (chatContactDoc != null) {
						chatContactDoc.setChannelType(channelConfig.getChannelType());
					}
				}
			}
		}

		String channelType = ArgUtil.is(channelConfig) ? channelConfig.getChannelType() : PMConstants.CHANNEL_TYPE.NONE;

		boolean dispatched = false;
		try {
			if (!environment.gateKeeper().canSendMessage(outboxMessage, chatContactDoc)) {
				if (!ArgUtil.is(outboxMessage.logs())) {
					outboxMessage.logs().add("blocked:csm");
				}
			} else {
				// DAU/MAU Counter: Count after validation passes, before sending
				if (billingCounterService != null) {
					messageContext.setOutboxMessage(outboxMessage);
					billingCounterService.incrementCounters(messageContext, "O");
				}

				if (ArgUtil.is(channelConfig) || ContactType.WEBSITE.equals(outboxMessage.contact().type())) {
					ConnectorHandler connector = get(channelConfig);
					if (ArgUtil.is(connector)) {
						connector.message(channelConfig, messageType, chatContactDoc, outboxMessage, inboxMessage,
								outBoundMsg);
						dispatched = true;
					} else {
						outboxMessage.logs().add(String.format("Connector not defined for %s", channelId));
					}
				} else {
					outboxMessage.logs().add(String.format("ChannelConfig not found for %s", channelId));
				}
			}
		} catch (Exception e) {
			try {
				logManager.error(outboxMessage, e);
			} catch (Exception e1) {
				LOGGER.error("FAILED TO LOG MESSAGE SEND EXCEPTION");
			}
		}

		// capture here to overwrite sent_err
		if (messageFailureGuard != null) {
			try {
				dispatched = dispatched && messageFailureGuard.applyReplayFailure(outboxMessage);
			} catch (Exception ex) {
				LOGGER.warn("messageFailureGuard.applyReplayFailure skipped: {}", ex.getMessage());
			}
		}

		if (dispatched) {
			jobPool().dispatchCounter(jobPool().globalJobName(), 1, channelType, outboxMessage.origin().getAppType());
		} else {
			jobPool().failedCounter(jobPool().globalJobName(), 1, channelType, outboxMessage.origin().getAppType());
		}

	}

	/**
	 * This method does not store it in db
	 * 
	 * Currently not used anywhere
	 * 
	 * @param context
	 * @param messageType
	 * @param outboxMessage
	 * @param inboxMessage
	 */
	public void messageNoStore(MessageContext context, String messageType, OutboxMessage outboxMessage,
			IMessageExtended inboxMessage) {
		LOGGER.debug("message(String {}, ChatContactDoc {}, IMessageExtended {}, OutboxMessage {})", messageType, null,
				inboxMessage, outboxMessage);
		messageByConnector(messageType, null, outboxMessage, inboxMessage, null);
	}

	/**
	 * This method is used only for push
	 * 
	 * Currently not used anywhere
	 * 
	 * @param context
	 * @param messageType
	 * @param outboxMessage
	 * @param inboxMessage
	 * @param outBoundMsg
	 */
	public void push(MessageContext context, String messageType, OutboxMessage outboxMessage,
			IMessageExtended inboxMessage, ApiOutBoundMsg outBoundMsg) {
		LOGGER.debug("message(String {}, ChatContactDoc {}, IMessageExtended {}, OutboxMessage {})", messageType, null,
				inboxMessage, outboxMessage);
		messageByConnector(messageType, null, outboxMessage, inboxMessage, outBoundMsg);
	}
}
