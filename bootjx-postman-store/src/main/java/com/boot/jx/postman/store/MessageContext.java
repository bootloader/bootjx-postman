package com.boot.jx.postman.store;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.boot.jx.AppContextUtil;
import com.boot.jx.mongo.CommonMongoTemplate;
import com.boot.jx.postman.ClientApp;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.PMEnvironment.PMDomainConfig;
import com.boot.jx.postman.doc.ChatContactDoc;
import com.boot.jx.postman.doc.ChatContextDoc;
import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.doc.MessageDoc;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.MessageDefinitions.Contactable;
import com.boot.jx.postman.model.MessageDefinitions.IMessageLoggable;
import com.boot.jx.postman.model.MessageDefinitions.SessionInfo;
import com.boot.jx.postman.model.MessageReport;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.model.ext.InBoundEvent;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.query.ChatContactQuery;
import com.boot.jx.postman.query.ChatContextQuery;
import com.boot.jx.postman.query.ChatSessionQuery;
import com.boot.jx.scope.ThreadScoped;
import com.boot.jx.utils.PostManUtil;
import com.boot.utils.ArgUtil;

@Component
@ThreadScoped
public class MessageContext {

	private static final Logger LOGGER = LoggerFactory.getLogger(MessageContext.class);

	private String currentHandler;

	@Autowired
	public MongoTemplate mongoTemplate;

	@Autowired
	public CommonMongoTemplate commonMongoTemplate;

	@Autowired
	private PMEnvironment pmEnvironment;

	@Autowired
	private PMDomainConfig pmDomainConfig;

	// DTOs
	private OutboxMessage outboxMessage;
	private InboxMessage inboxMessage;
	private InBoundEvent event;
	private MessageReport messageReport;
	private Contactable contactable;

	// QUERYs
	private ChatContactQuery chatContactQuery;
	private ChatSessionQuery chatSessionQuery;
	private ChatContextQuery chatContextQuery;

	@Autowired
	private SessionStore sessionStore;
	@Autowired
	private ContactStore contactStore;

	@Autowired
	private MessageContextStore messageContextStore;

	private MessageContextStore localStore;

	public MessageContextStore store() {
		if (this.messageContextStore != null) {
			return this.messageContextStore;
		}
		if (this.localStore == null) {
			this.localStore = new MessageContextStore();
		}
		return this.localStore;
	}

	@Autowired
	private MessageStore messageStore;

	private ChannelConfig channel;

	public MessageContext from(MessageContext context) {
		this.outboxMessage = context.getOutboxMessage();
		this.inboxMessage = context.getInboxMessage();
		// this.messageDoc = context.getMessageDoc();
		this.store().from(context.store());
		this.messageReport = context.messageReport;
		this.event = context.getEvent();
		this.contactable = context.getContactable();
		this.chatContactQuery = context.getChatContactQuery();
		this.chatSessionQuery = context.getChatSessionQuery();
		this.chatContextQuery = context.getChatContextQuery();
		return this;
	}

	public void setOutboxMessage(OutboxMessage message) {
		this.outboxMessage = message;
	}

	public InboxMessage getInboxMessage() {
		return inboxMessage;
	}

	public void setInboxMessage(InboxMessage inboxMessage) {
		this.inboxMessage = inboxMessage;
	}

	public void setMessageDoc(MessageDoc messageDoc) {
		this.store().setMessageDoc(messageDoc);
	}

	public MessageDoc getMessageDoc() {
		return this.store().getMessageDoc();
	}

	public void setMessageReport(MessageReport messageReport) {
		this.messageReport = messageReport;
	}

	public MessageReport getMessageReport() {
		return messageReport;
	}

	public MessageDoc messageDoc() {
		MessageDoc messageDoc = this.store().getMessageDoc();
		if (!ArgUtil.is(messageDoc)) {
			if (messageStore == null) {
				LOGGER.warn("messageStore is null");
				return messageDoc;
			}
			messageDoc = messageStore.findMessageDoc(getMessage());
			this.store().setMessageDoc(messageDoc);
		}
		return messageDoc;
	}

	public IMessageLoggable getMessage() {
		if (ArgUtil.is(this.inboxMessage)) {
			return this.inboxMessage;
		} else {
			return this.outboxMessage;
		}
	}

	public SessionInfo getSessionInfoMessage() {
		if (ArgUtil.is(this.inboxMessage)) {
			return this.inboxMessage;
		} else if (ArgUtil.is(this.outboxMessage)) {
			return this.outboxMessage;
		} else if (ArgUtil.is(this.event)) {
			return this.event;
		}
		return null;
	}

	public void setInBoundEvent(InBoundEvent event) {
		this.event = event;
	}

	public InBoundEvent getInBoundEvent() {
		return this.event;
	}

	private String getSessionId() {
		String sessionId = AppContextUtil.get(ChatSessionDoc.class.getName());
		if (ArgUtil.is(sessionId)) {
			return sessionId;
		} else {
			if (getMessage() != null) {
				sessionId = getMessage().getSessionId();
			} else if (this.event != null) {
				sessionId = this.event.sessionId;
			}
			if (ArgUtil.is(sessionId)) {
				AppContextUtil.set(ChatSessionDoc.class.getName(), sessionId);
			}
		}
		return sessionId;
	}

	public ChatSessionQuery session() {
		if (chatSessionQuery == null && ArgUtil.is(getSessionId())) {
			ChatSessionDoc chatSessionDoc = sessionStore.getSession(getSessionId());
			chatSessionQuery = new ChatSessionQuery(chatSessionDoc);
		}
		return chatSessionQuery;
	}

	public void session(ChatSessionDoc sessionDoc) {
		if (ArgUtil.is(sessionDoc)) {
			chatSessionQuery = new ChatSessionQuery(sessionDoc);
		}
	}

	private Contactable getContactable() {
		return this.contactable;
	}

	private Contactable contactable() {
		if (this.contactable == null) {
			if (getMessage() != null) {
				this.contactable = PostManUtil.getContactMeta(getMessage().contact());
			} else if (messageDoc() != null) {
				this.contactable = PostManUtil.getContactMeta(messageDoc().getContact());
			} else if (this.event != null) {
				this.contactable = PostManUtil.getContactMeta(this.event.contact(), this.event.contactId);
			}
		}
		return this.contactable;
	}

	private ChatContactDoc getChatContactDoc() {
		Contactable c = contactable();
		return contactStore.findContactById(c.getContactId());
	}

	public ChatContactQuery contact() {
		if (this.chatContactQuery == null) {
			ChatContactDoc chatContactDoc = this.getChatContactDoc();
			if (!ArgUtil.is(chatContactDoc)) {
				LOGGER.error("NO CONTACT FOUND");
			} else {
				store().setChatContactDoc(chatContactDoc);
				this.chatContactQuery = new ChatContactQuery(chatContactDoc);
			}
		}
		return this.chatContactQuery;
	}

	private String getQueueCode() {
		if (getMessage() != null) {
			return getMessage().session().getQueue();
		} else if (messageDoc() != null) {
			return messageDoc().getQueue();
		} else if (this.event != null && this.event.sessionRouted != null) {
			return this.event.sessionRouted.targetQueue;
		} else if (ArgUtil.notNull(this.session()) && ArgUtil.notNull(this.session().getDoc())) {
			return this.session().getDoc().getAssignedToQueue();
		}
		return null;
	}

	public String getActiveQueueCode() {
		if (getMessage() != null) {
			return getMessage().session().getQueue();
		} else if (this.event != null && ArgUtil.is(this.event.session().getQueue())) {
			return event.session().getQueue();
		} else if (ArgUtil.notNull(this.session()) && ArgUtil.notNull(this.session().getDoc())) {
			return this.session().getDoc().getAssignedToQueue();
		}
		return null;
	}

	public void commitChatSessionQuery() {
		if (this.chatSessionQuery != null) {
			commonMongoTemplate.updateFirst(this.chatSessionQuery);
		}
	}

	public void commitChatContactQuery() {
		if (this.chatContactQuery != null) {
			commonMongoTemplate.updateFirst(this.chatContactQuery);
		}
	}

	public void commitChatContextQuery() {
		if (this.chatContextQuery != null) {
			commonMongoTemplate.updateFirst(this.chatContextQuery);
		}
	}

	public ChatContactDoc commit() {
		if (chatContactQuery != null) {
			sessionStore.update(chatContactQuery);
		}
		if (chatSessionQuery != null) {
			sessionStore.update(chatSessionQuery);
		}
		if (chatContextQuery != null) {
			sessionStore.upsert(chatContextQuery);
		}
		return null;
	}

	public ClientApp clientApp(String assignedQueue, Contactable contactable) {
		ClientApp defaultClient = null;
		if (ArgUtil.is(assignedQueue)) {
			defaultClient = pmEnvironment.config().clientApiKey(assignedQueue);

			if (ArgUtil.is(defaultClient)) {
				return defaultClient;
			}
		}

		if (!ArgUtil.is(contactable)) {
			return defaultClient;
		}

		assignedQueue = pmDomainConfig.getDefaultInboundQueue(contactable);

		if (ArgUtil.is(assignedQueue)) {
			defaultClient = pmEnvironment.config().clientApiKey(assignedQueue);

			if (ArgUtil.is(defaultClient)) {
				return defaultClient;
			}
		}

		return defaultClient;
	}

	public ClientApp clientApp(String assignedQueue) {
		return this.clientApp(assignedQueue, null);
	}

	public ClientApp clientApp() {
		if (ArgUtil.is(getMessage())) {
			return this.clientApp(getQueueCode(), contactable());
		}

		if (ArgUtil.is(this.messageReport)) {
			return this.clientApp(this.messageReport.session().getQueue(), this.messageReport.contact());
		}

		if (ArgUtil.is(messageDoc())) {
			return this.clientApp(getQueueCode(), contactable());
		}

		if (ArgUtil.notNull(session()) && ArgUtil.notNull(session().getDoc())) {
			return this.clientApp(session().getDoc().getAssignedToQueue(), session().getDoc().contact());
		}

		return this.clientApp(null, null);
	}

	public ChatContextQuery chat() {
		if (this.chatContextQuery == null) {
			Contactable c = contactable();
			String contactId = c.getContactId();
			if (ArgUtil.is(contactId)) {
				ChatContextDoc doc = commonMongoTemplate.findByIdSafeCheck(contactId, ChatContextDoc.class);
				if (!ArgUtil.is(doc)) {
					this.chatContextQuery = new ChatContextQuery(contactId);
				} else {
					this.chatContextQuery = new ChatContextQuery(doc);
				}
			} else {
				LOGGER.error("NO CONTACT FOUND");
			}
			if (!ArgUtil.is(this.chatContextQuery)) {
				LOGGER.error("NOT Ablet to Build chatContextQuery");
			}
		}
		return this.chatContextQuery;
	}

	public String getCurrentHandler() {
		return currentHandler;
	}

	public void setCurrentHandler(String currentHandler) {
		this.currentHandler = currentHandler;
	}

	public void setChatConext(ChatContextDoc doc) {
		this.chatContextQuery = new ChatContextQuery(doc);
	}

	public OutboxMessage getOutboxMessage() {
		return outboxMessage;
	}

	public InBoundEvent getEvent() {
		return event;
	}

	public ChatContactQuery getChatContactQuery() {
		return chatContactQuery;
	}

	public ChatSessionQuery getChatSessionQuery() {
		return chatSessionQuery;
	}

	public ChatContextQuery getChatContextQuery() {
		return chatContextQuery;
	}

	public MessageContext channel(ChannelConfig channel) {
		this.channel = channel;
		return this;
	}

	public ChannelConfig getChannel() {
		return channel;
	}

	@PostConstruct
	public void init() {
		// System.out.println("MessageContext:init");
	}

	@PreDestroy
	public void destroy() {
		// System.out.println("MessageContext:destroy");
	}

}
