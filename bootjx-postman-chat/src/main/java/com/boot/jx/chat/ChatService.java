package com.boot.jx.chat;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.boot.jx.AppContextUtil;
import com.boot.jx.inbound.InBound.MessageEvents;
import com.boot.jx.logger.LoggerService;
import com.boot.jx.postman.PMConstants;
import com.boot.jx.postman.PMConstants.MESSAGE_COMPOSE_TYPE;
import com.boot.jx.postman.PMEnvironment.PMClientConfig;
import com.boot.jx.postman.PostManException;
import com.boot.jx.postman.doc.ChatContactDoc;
import com.boot.jx.postman.doc.ChatContextDoc;
import com.boot.jx.postman.doc.ChatMeta;
import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.doc.MessageDoc;
import com.boot.jx.postman.manager.ChatLogger;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.Message;
import com.boot.jx.postman.model.Message.Status;
import com.boot.jx.postman.model.MessageDefinitions.IMessageExtended;
import com.boot.jx.postman.model.MessageDefinitions.TraceMessage;
import com.boot.jx.postman.model.MessageReferral;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.model.ext.InBoundEvent;
import com.boot.jx.postman.model.outbound.OutBoundMsgBasic.ApiOutBoundMsg;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.store.ContactStore;
import com.boot.jx.postman.store.MessageContext;
import com.boot.jx.postman.store.MessageStore;
import com.boot.jx.postman.store.SessionStore;
import com.boot.jx.utils.PostManUtil;
import com.boot.utils.ArgUtil;

@Component
public class ChatService {

	public static Logger LOGGER = LoggerService.getLogger(ChatService.class);

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private MessageContext messageContext;

	@Autowired
	private ChatClient chatClient;

	@Autowired
	private PMClientConfig chatClientConfig;

	@Autowired
	private MessageStore messageStore;

	@Autowired
	private SessionStore sessionStore;

	@Autowired
	private ContactStore contactStore;

	@Autowired
	private ChatLogger chatLogger;

	@Autowired
	private ChatSessionFactory chatSessionFactory;

	@Lazy
	@Autowired(required = false)
	private MessageEvents messageEvents;

	public InboxMessage getInboxMessage() {
		return messageContext.getInboxMessage();
	}

	@Autowired
	private ConnectorHandlerFactory connectorHandlerFactory;

	public MessageContext context() {
		return messageContext;
	}

	public ChatClient getClient() {
		return chatClient;
	}

	public PMClientConfig getClientConfig() {
		return chatClientConfig;
	}

	@Autowired
	private ChatLogger logManager;

	private MessageDoc message(String messageType, ChatContactDoc chatContactDoc, OutboxMessage outboxMessage,
			IMessageExtended inboxMessage, ApiOutBoundMsg outBoundMsg) {

		MessageDoc messageDoc = messageStore.createOrUpdate(outboxMessage);

		//messageContext.store().getChatContactDoc();
		
		if (AppContextUtil.batchTask()) {
			connectorHandlerFactory.messageSync(new MessageContext().from(context()), messageType, chatContactDoc,
					outboxMessage, inboxMessage);
		} else {
			connectorHandlerFactory.message(new MessageContext().from(context()), messageType, chatContactDoc,
					outboxMessage, inboxMessage);
		}
		chatSessionFactory.push(messageDoc, outboxMessage);

		if (messageEvents != null) {
			messageEvents.onMessageOutbound(outboxMessage);
		}

		logCrossMessage(outboxMessage);
		outboxMessage.timer().log("csm:z");
		return messageDoc;
	}

	private MessageDoc push(String messageType, ChatContactDoc chatContactDoc, OutboxMessage outboxMessage,
			IMessageExtended inboxMessage, ApiOutBoundMsg outBoundMsg, ChannelConfig channel) {

		MessageDoc messageDoc = null;

		if (ArgUtil.is(channel) && channel.isPushLocalStore()) {
			messageStore.createOrUpdate(outboxMessage);
		}
		connectorHandlerFactory.push(new MessageContext().channel(channel), messageType, outboxMessage, inboxMessage,
				outBoundMsg);
		// chatSessionFactory.push(messageDoc, outboxMessage);

		if (messageEvents != null) {
			messageEvents.onMessageOutbound(outboxMessage);
		}
		outboxMessage.timer().log("csp:z");
		return messageDoc;
	}

	private MessageDoc actionIntenal(ChatContactDoc chatContactDoc, OutboxMessage outboxMessage) {
		if (!ArgUtil.is(outboxMessage.getAction())) {
			return null;
		}

		if (!ArgUtil.is(chatContactDoc)) {
			throw new PostManException("Destination Not Specified : chatContactDoc Empty");
		}

		outboxMessage.updateStatus(Status.RECEIVD);
		outboxMessage.updateStatus(Message.Status.INIT);
		outboxMessage.contact().setContactType(chatContactDoc.getContactType());
		outboxMessage.contact().setChannelType(
				ArgUtil.nonEmpty(outboxMessage.contact().getChannelType(), chatContactDoc.getChannelType()));
		outboxMessage.contact().setLane(chatContactDoc.getLane());
		outboxMessage.contact().setCsid(chatContactDoc.getCsid());
		outboxMessage.contact().setContactId(chatContactDoc.getContactId());
		outboxMessage.setSessionId(chatContactDoc.getSessionId());

		return message(MESSAGE_COMPOSE_TYPE.ACTION, chatContactDoc, outboxMessage, null, null);
	}

	private MessageDoc replyIntenal(ChatContactDoc chatContactDoc, OutboxMessage outboxMessage,
			IMessageExtended inboxMessage) {
		LOGGER.debug("replyIntenal(ChatContactDoc {}, OutboxMessage {})", inboxMessage, outboxMessage);

		if (!ArgUtil.is(inboxMessage)) {
			throw new PostManException("Destination Not Specified : inboxMessage Empty");
		}

		outboxMessage.updateStatus(Message.Status.INIT);
		outboxMessage.contact().setContactType(inboxMessage.contact().getContactType());
		outboxMessage.contact().setChannelType(
				ArgUtil.nonEmpty(outboxMessage.contact().getChannelType(), chatContactDoc.getChannelType()));
		outboxMessage.contact().setLane(inboxMessage.contact().getLane());
		outboxMessage.contact().setCsid(inboxMessage.contact().getCsid());
		outboxMessage.contact().setContactId(inboxMessage.contact().getContactId());
		outboxMessage.setQueue(inboxMessage.getQueue());
		outboxMessage.addTo(inboxMessage.getFrom());
		outboxMessage.setSessionId(inboxMessage.getSessionId());

		if (!ArgUtil.is(outboxMessage.session().getMode())) {
			outboxMessage.session().setMode(inboxMessage.session().getMode());
		}

		// outboxMessage.model().put("contact",
		// ChatDTOUtil.getContactMeta(chatContactDoc));
		return message(MESSAGE_COMPOSE_TYPE.REPLY, chatContactDoc, outboxMessage, inboxMessage, null);
	}

	private MessageDoc sendIntenal(ChatContactDoc chatContactDoc, OutboxMessage outboxMessage) {
		LOGGER.debug("sendIntenal(ChatContactDoc {}, OutboxMessage {})", chatContactDoc, outboxMessage);

		if (!ArgUtil.is(chatContactDoc)) {
			throw new PostManException("Destination Not Specified : chatContactDoc Empty");
		}

		outboxMessage.updateStatus(Message.Status.INIT);
		outboxMessage.contact().setContactType(chatContactDoc.getContactType());
		outboxMessage.contact().setChannelType(
				ArgUtil.nonEmpty(outboxMessage.contact().getChannelType(), chatContactDoc.getChannelType()));
		outboxMessage.contact().setLane(chatContactDoc.getLane());
		outboxMessage.contact().setCsid(chatContactDoc.getCsid());
		outboxMessage.contact().setContactId(chatContactDoc.getContactId());
		outboxMessage.setSessionId(chatContactDoc.getSessionId());

		// outboxMessage.model().put("contact",
		// ChatDTOUtil.getContactMeta(chatContactDoc));
		return message(MESSAGE_COMPOSE_TYPE.SEND, chatContactDoc, outboxMessage, null, null);
	}

	private MessageDoc pushIntenal(ApiOutBoundMsg message, OutboxMessage outboxMessage, ChannelConfig channel) {
		LOGGER.debug("sendIntenal(ChatContactDoc {}, OutboxMessage {})", message, outboxMessage);
		outboxMessage.updateStatus(Message.Status.INIT);
		// outboxMessage.model().put("contact",
		// ChatDTOUtil.getContactMeta(chatContactDoc));
		return push(MESSAGE_COMPOSE_TYPE.PUSH, null, outboxMessage, null, message, channel);
	}

	public MessageDoc reply(ChatSessionDoc sessionDoc, OutboxMessage outboxMessage) throws InterruptedException {
		context().session(sessionDoc);

		ChatContactDoc chatContactDoc = contactStore.findContactById(sessionDoc.contact().getContactId());
		IMessageExtended inboxMessage = sessionStore.toSessionMessage(sessionDoc);

		if (ArgUtil.isEmpty(outboxMessage.session().getAgent())) {
			outboxMessage.session().setAgent(chatClientConfig.getDefaultSender());
		}

		// Action Only
		MessageDoc actionDco = actionIntenal(chatContactDoc, outboxMessage);
		if (ArgUtil.is(actionDco)) {
			return actionDco;
		}
		return replyIntenal(chatContactDoc, outboxMessage, inboxMessage);
	}

	public MessageDoc reply(IMessageExtended inboxMessage, OutboxMessage outboxMessage) throws InterruptedException {

		if (!ArgUtil.is(inboxMessage)) {
			ChatSessionDoc sessionDoc = messageContext.session().getDoc();
			if (ArgUtil.is(sessionDoc)) {
				return reply(sessionDoc, outboxMessage);
			}
		}

		ChatContactDoc chatContactDoc = contactStore.findContactById(inboxMessage.contact().getContactId());
		contactStore.store(chatContactDoc);

		if (ArgUtil.isEmpty(outboxMessage.session().getAgent())) {
			outboxMessage.session().setAgent(chatClientConfig.getDefaultSender());
		}

		// Action Only
		MessageDoc actionDco = actionIntenal(chatContactDoc, outboxMessage);
		if (ArgUtil.is(actionDco)) {
			return actionDco;
		}
		return replyIntenal(chatContactDoc, outboxMessage, inboxMessage);
	}

	public MessageDoc send(ChatSessionDoc sessionDoc, OutboxMessage outboxMessage) {
		LOGGER.debug("send(ChatSessionDoc {}, OutboxMessage {})", sessionDoc, outboxMessage);

		// LOGGER.info("send ---"+JsonUtil.toJson(outboxMessage)); TODO:- @Rabil not to
		// leave these info statements

		context().session(sessionDoc);

		ChatContactDoc chatContactDoc = contactStore.findContactById(sessionDoc.getContactId());
		contactStore.store(chatContactDoc);

		if (ArgUtil.isEmpty(outboxMessage.session().getAgent())) {
			outboxMessage.session().setAgent(sessionDoc.getAssignedToAgent());
		}

		if (ArgUtil.isEmpty(outboxMessage.session().getDept())) {
			outboxMessage.session().setDept(sessionDoc.getAssignedToDept());
		}

		// Action Only
		MessageDoc actionDto = actionIntenal(chatContactDoc, outboxMessage);
		if (ArgUtil.is(actionDto)) {
			return actionDto;
		}

		if (ArgUtil.isEmptyValue(sessionDoc.getLastInComingStamp())) {
			return sendIntenal(chatContactDoc, outboxMessage);
		} else {
			IMessageExtended inboxMessage = sessionStore.toSessionMessage(sessionDoc);
			return replyIntenal(chatContactDoc, outboxMessage, inboxMessage);
		}
	}

	public MessageDoc send(ChatContactDoc chatContactDoc, OutboxMessage outboxMessage) {
		if (ArgUtil.isEmpty(outboxMessage.session().getAgent())) {
			outboxMessage.session().setAgent(chatClientConfig.getDefaultSender());
		}

		// Action Only
		MessageDoc actionDto = actionIntenal(chatContactDoc, outboxMessage);
		if (ArgUtil.is(actionDto)) {
			return actionDto;
		}

		return sendIntenal(chatContactDoc, outboxMessage);
	}

	public MessageDoc push(ApiOutBoundMsg message, OutboxMessage outboxMessage, ChannelConfig channel) {
		return pushIntenal(message, outboxMessage, channel);
	}

	public boolean beforeMessageHandler() {
		InboxMessage inboxMessage = messageContext.getInboxMessage();
		if (ArgUtil.is(inboxMessage)) {
		}
		return true;
	}

	private MessageContext loadChatContextInternal(String contactId, TraceMessage inboxMessage) {
		LOGGER.debug("Loading chat conewxt");
		if (!ArgUtil.is(inboxMessage.session().getMode())) {
			ChatSessionDoc sessionDoc = messageContext.session().getDoc();

			if (!ArgUtil.is(sessionDoc)) {
				LOGGER.error("No Session Found for {}/{}", contactId, inboxMessage.getSessionId());
			}

			inboxMessage.session().setMode(PMConstants.CHAT_MODE.BOT.toString());
			inboxMessage.session().setAgent(chatClientConfig.getDefaultSender());

			sessionStore.assignToBot(sessionDoc, chatClientConfig.getDefaultSender());
		}

		ChatContextDoc doc = mongoTemplate.findById(contactId, ChatContextDoc.class);

		if (!ArgUtil.is(doc)) {
			doc = new ChatContextDoc();
			doc.setContactId(contactId);
		}
		messageContext.setChatConext(doc);

		if (!ArgUtil.is(doc.getMeta())
				|| !ArgUtil.is(doc.getMeta().getRoutingId(), inboxMessage.session().getRoutingId())) {
			LOGGER.debug("Loading chat conewxt:newSession");
			chatLogger.addTrace(inboxMessage, "NewSession", doc.getMeta(), inboxMessage.session());
			messageContext.chat().setMeta(new ChatMeta());
			messageContext.commitChatContextQuery();
			messageContext.chat().setQueueCode(inboxMessage.session().getQueue());
			messageContext.chat().setSessionId(inboxMessage.getSessionId());
			messageContext.chat().setRoutingId(inboxMessage.session().getRoutingId());
		} else {
			chatLogger.addTrace(inboxMessage, "ContinueOldSession", doc.getMeta());
		}

		// messageStore.create(inboxMessage);
		return messageContext;
	}

	public MessageContext loadChatContext(String contactId, InboxMessage inboxMessage) {
		messageContext.setInboxMessage(inboxMessage);
		return loadChatContextInternal(contactId, inboxMessage);
	}

	public MessageContext loadChatContext(String contactId, InBoundEvent assignEvent) {
		messageContext.setInBoundEvent(assignEvent);
		return loadChatContextInternal(contactId, assignEvent);
	}

	public void commitChatContext(String contactId, String prevHandler, InboxMessage inboxMessage) {
		messageContext.chat().setPrevHandler(prevHandler);
		messageContext.chat().setUpdateStamp(System.currentTimeMillis());
		if (ArgUtil.is(prevHandler)) {
			messageStore.setHandler(inboxMessage, prevHandler);
		}
		messageContext.commit();
	}

	public void commitChatContext(ChatSessionDoc sessionDoc, InBoundEvent assignEvent) {
		messageContext.commit();
	}

	public boolean botScore(ChatSessionDoc session, Integer botScore) {
		session = sessionStore.botScore(session, botScore);
		return true;
	}

	public boolean agentScore(ChatSessionDoc session, Integer botScore) {
		session = sessionStore.agentScore(session, botScore);
		return true;
	}
	
	/** Lxo log in referral.sessionId when cross-channel (referral.channelId ≠ outbound channel). */
	private void logCrossMessage(OutboxMessage channelOutbox) {
		if (!ArgUtil.is(channelOutbox) || !ArgUtil.is(channelOutbox.getReferral())) {
			return;
		}
		MessageReferral referral = channelOutbox.getReferral();
		String outboundChannelId = PostManUtil.CHANNEL_ID(channelOutbox.contact());
		if (!PostManUtil.isCrossChannelReferral(referral, outboundChannelId) || !ArgUtil.is(referral.getSessionId())) {
			return;
		}

		String referralSessionId = referral.getSessionId();
		ChatSessionDoc referralSession = sessionStore.getValidSession(referralSessionId);
		if (!ArgUtil.is(referralSession)) {
			LOGGER.warn("Cross-channel visibility log skipped: invalid or expired referralSessionId={}",
					referralSessionId);
			return;
		}

		chatLogger.logCrossOutbound(referralSession, crossMessageText(channelOutbox));
	}

	private static String crossMessageText(OutboxMessage channelOutbox) {
		String channelId = PostManUtil.CHANNEL_ID(channelOutbox.contact());
		String templateCode = channelOutbox.templateCode();
		if (!ArgUtil.is(templateCode) && ArgUtil.is(channelOutbox.getHsm())) {
			templateCode = channelOutbox.getHsm().getCode();
		}
		if (ArgUtil.is(templateCode)) {
			return templateCode + " sent on " + channelId;
		}
		return "sent on " + channelId;
	}
}
