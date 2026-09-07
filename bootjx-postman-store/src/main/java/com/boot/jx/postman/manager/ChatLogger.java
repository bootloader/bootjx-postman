package com.boot.jx.postman.manager;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;

import com.boot.jx.AppConfig;
import com.boot.jx.AppContextUtil;
import com.boot.jx.api.ApiFieldError;
import com.boot.jx.api.ApiResponseUtil;
import com.boot.jx.exception.AmxApiError;
import com.boot.jx.exception.ApiHttpExceptions.ApiHttpException;
import com.boot.jx.exception.ApiHttpExceptions.ApiHttpServerException;
import com.boot.jx.logger.AuditDetailProvider;
import com.boot.jx.mongo.CommonMongoQueryBuilder;
import com.boot.jx.postman.PMConstants.MESSAGE_BOUND_TYPE;
import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.doc.MessageDoc;
import com.boot.jx.postman.doc.MessageDoc.MessageDocLogs;
import com.boot.jx.postman.doc.MessageDocAbstract;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.Message.Status;
import com.boot.jx.postman.model.MessageCall;
import com.boot.jx.postman.model.MessageDefinitions.IMessageExtended;
import com.boot.jx.postman.model.MessageDefinitions.LogMessage;
import com.boot.jx.postman.model.MessageDefinitions.LoggableEntity;
import com.boot.jx.postman.model.MessageDefinitions.SessionInfo;
import com.boot.jx.postman.model.MessageDefinitions.TraceMessage;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.model.ext.InBoundEvent;
import com.boot.jx.postman.store.ContactStore;
import com.boot.jx.postman.store.MessageContext;
import com.boot.jx.postman.store.MessageStore;
import com.boot.jx.postman.store.MessageStore.EVENTS;
import com.boot.jx.postman.store.SessionStore;
import com.boot.jx.utils.PostManUtil;
import com.boot.model.MapModel;
import com.boot.utils.ArgUtil;

@Component
public class ChatLogger {

	private static final Logger LOGGER = LoggerFactory.getLogger(ChatLogger.class);

	@Lazy
	@Autowired(required = false)
	private AuditDetailProvider auditDetailProvider;

	public String getCurrenUser() {
		return ArgUtil.is(auditDetailProvider) ? auditDetailProvider.getAuditUser() : "_SYSTEM_";
	}

	@Autowired
	private SessionStore sessionStore;

	@Autowired
	private MessageStore messageStore;

	@Autowired
	private ContactStore contactStore;

	@Autowired
	private MessageContext messageContext;

	@Autowired
	private AppConfig appConfig;

	public MessageDoc note(ChatSessionDoc sessionDoc, OutboxMessage outboxMessage) {
		outboxMessage.contact().setContactType(sessionDoc.getContactType());
		outboxMessage.contact().setChannelType(sessionDoc.getChannel());
		outboxMessage.contact().setLane(sessionDoc.getLane());
		outboxMessage.contact().setContactId(sessionDoc.getContactId());
		outboxMessage.contact().copyFrom(sessionDoc.getContact());

		outboxMessage.setSessionId(sessionDoc.getSessionId());
		outboxMessage.setType("N");
		return messageStore.note(outboxMessage, getCurrenUser());
	}

	/**
	 * This method can create only object but not save it, saving it dob is
	 * responsibilty of caller, (Purpose to reduce number of DB Writes)
	 * 
	 * @param inboxMessage
	 * @param actorAgent
	 * @param eventName
	 * @param logMessage
	 * @return
	 */
	private MessageDoc createEventDocument(SessionInfo inboxMessage, String actorAgent, EVENTS eventName,
			Object... logMessage) {
		MessageDoc doc = new MessageDoc();
		doc.setContactId(PostManUtil.createContactId(inboxMessage.contact()));
		doc.setType(MESSAGE_BOUND_TYPE.LOG);
		doc.setTimestamp(System.currentTimeMillis());
		if (ArgUtil.is(logMessage)) {
			for (Object string : logMessage) {
				doc.logs().add(string);
			}
		}
		doc.setAction(ArgUtil.parseAsString(eventName));
		doc.setSessionId(inboxMessage.getSessionId());
		doc.setAgent(ArgUtil.nonEmpty(actorAgent, AppContextUtil.getActorId()));
		doc.setTraceId(AppContextUtil.getTraceId());
		return doc;
	}
	
	/**
	 * Creates a type-{@code Lxo} document only; caller or {@link #logCrossOutbound} saves it.
	 */
	private MessageDoc createCrossOutboundDocument(SessionInfo session, String actorAgent, String messageText) {
		MessageDoc doc = new MessageDoc();
		doc.setContactId(PostManUtil.createContactId(session.contact()));
		doc.setType(MESSAGE_BOUND_TYPE.LOG_CROSS_OUTBOUND);
		doc.setTimestamp(System.currentTimeMillis());
		doc.setSessionId(session.getSessionId());
		doc.setAgent(ArgUtil.nonEmpty(actorAgent, AppContextUtil.getActorId()));
		doc.setTraceId(AppContextUtil.getTraceId());
		doc.setMessage(messageText);
		return doc;
	}

	/**
	 * Saves an Lxo log line into {@code sessionDoc} (the referral session from {@code referral.sessionId}).
	 *
	 * @param sessionDoc session to attach the visibility log to (not the outbound message's own session)
	 */
	public MessageDoc logCrossOutbound(ChatSessionDoc sessionDoc, String messageText) {
		IMessageExtended sessionMsg = sessionStore.toSessionMessage(sessionDoc);
		MessageDoc doc = createCrossOutboundDocument(sessionMsg, getCurrenUser(), messageText);
		messageStore.saveMessage(doc, sessionMsg.contact().type());
		return doc;
	}

	public MessageDoc event(SessionInfo inboxMessage, String actorAgent, EVENTS eventName, Object... logMessage) {
		MessageDoc doc = createEventDocument(inboxMessage, actorAgent, eventName, logMessage);
		messageStore.saveMessage(doc, inboxMessage.contact().type());
		return doc;
	}

	public MessageDoc event(SessionInfo inboxMessage, EVENTS event, Object... logs) {
		return event(inboxMessage, inboxMessage.session().getAgent(), event, logs);
	}

	public MessageDoc event(ChatSessionDoc sessionDoc, String auditAgent, EVENTS event, Object... logs) {
		IMessageExtended inboxMessage = sessionStore.toSessionMessage(sessionDoc);
		return event(inboxMessage, auditAgent, event, logs);
	}

	public MessageDoc event(ChatSessionDoc sessionDoc, EVENTS event, Object... logs) {
		return event(sessionDoc, getCurrenUser(), event, logs);
	}

	public void error(LogMessage inboxMessage, Throwable e) {
		this.error(inboxMessage, null, e);
	}

	public void error(LogMessage inboxMessage, String erromessage) {
		this.error(inboxMessage, null, null, erromessage);
	}

	private boolean isHttpException(Throwable e) {
		Throwable t = e;

		while (t != null) {
			if (t instanceof HttpStatusCodeException // Spring RestTemplate
					|| t instanceof IOException) { // network level
				return true;
			}
			t = t.getCause();
		}

		return false;
	}

	public void error(LogMessage inboxMessage, Status status, Throwable e, String message) {
		MessageDocLogs doc = new MessageDocLogs();
		doc.setSessionId(inboxMessage.getSessionId());
		doc.setMessageId(inboxMessage.getMessageId());
		doc.setMessageIdExt(inboxMessage.getMessageIdExt());
		doc.setMessageIdRef(inboxMessage.getMessageIdRef());
		doc.setContactId(PostManUtil.createContactId(inboxMessage.contact()));
		doc.setType("E");
		doc.setTimestamp(System.currentTimeMillis());
		doc.setTraceId(AppContextUtil.getTraceId());
		doc.setMessage(e.getMessage());
		doc.setStatus(ArgUtil.parseAsString(status));

		toLogs(e, doc);

		messageStore.save(doc);

		// Extract error code from HTTP response and add to WhatsApp logs
		String errorCode = getErrorCode(doc.getHttpResp());
		if (errorCode != null && !inboxMessage.logs().contains(errorCode)) {
			inboxMessage.logs().add(errorCode);
		}
		inboxMessage.logs().add(message);
		inboxMessage.logs().add("trail_id:" + doc.getMessageId());
		if (isHttpException(e)) {
			if (e instanceof HttpStatusCodeException) {
				HttpStatusCodeException ex = (HttpStatusCodeException) e;
				int httpstatus = ex.getStatusCode().value();

				if (httpstatus >= 500) {
					inboxMessage.logs().add("HTTP_SERVER_ERROR");
				} else if (httpstatus >= 400) {
					inboxMessage.logs().add("HTTP_CLIENT_ERROR");
				}
			} else if (e instanceof SocketTimeoutException) {
				inboxMessage.logs().add("HTTP_TIMEOUT");
			} else if (e instanceof ConnectException) {
				inboxMessage.logs().add("HTTP_CONNECTION");
			}
		} else {
			inboxMessage.logs().add("APPLICATION_ERROR");
		}

	}

	public void error(LogMessage inboxMessage, Status status, Throwable e) {
		this.error(inboxMessage, status, e, e.getMessage());
	}

	public void error(InBoundEvent inBoundEvent, Throwable e) {
		MessageDocLogs doc = new MessageDocLogs();
		doc.setSessionId(inBoundEvent.sessionId);
		doc.setContactId(inBoundEvent.contactId);
		doc.setType("E");
		doc.setTimestamp(System.currentTimeMillis());
		doc.setTraceId(AppContextUtil.getTraceId());
		doc.setMessage(e.getMessage());
		doc.setQueue(inBoundEvent.session().getQueue());

		toLogs(e, doc);
		messageStore.save(doc);
	}

	private void toLogs(Throwable e, MessageDocLogs doc) {
		if (e == null) {
			return;
		}

		doc.logs().add(e.getMessage());

		StackTraceElement[] traces = e.getStackTrace();

		if (traces.length > 0 && traces[0].toString().length() > 0) {
			for (StackTraceElement trace : traces) {
				doc.logs().add(trace.toString());
			}
		}

		if (e instanceof ApiHttpServerException || e instanceof ApiHttpException) {
			AmxApiError r = ((ApiHttpException) e).getResponse();
			doc.setHttpResp(MapModel.from(r.getBody()).toMap());
			doc.setHttpStatusCode(r.getRawStatusCode());
		}

		List<ApiFieldError> errors = ApiResponseUtil.getErrors();
		if (ArgUtil.is(errors)) {
			doc.trace().add(errors);
		}

	}

	public void error(String message, Throwable e) {
		if (ArgUtil.is(messageContext.getMessage())) {
			this.error(messageContext.getMessage(), e);
		} else if (ArgUtil.is(messageContext.getInBoundEvent())) {
			this.error(messageContext.getInBoundEvent(), e);
		} else {
			MessageDocLogs doc = new MessageDocLogs();
			doc.setType("E");
			doc.setTimestamp(System.currentTimeMillis());
			doc.setTraceId(AppContextUtil.getTraceId());
			doc.setMessage(message);
			toLogs(e, doc);
			messageStore.save(doc);
		}
	}

	public void error(Throwable e) {
		this.error(e.getMessage(), e);
	}

	private void log(MessageDocAbstract doc, String message, Object[] debugMessage) {
		doc.setTimestamp(System.currentTimeMillis());
		doc.setTraceId(AppContextUtil.getTraceId());
		doc.setMessage(message);
		if (ArgUtil.is(debugMessage)) {
			for (int i = 0; i < debugMessage.length; i++) {
				doc.logs().add(ArgUtil.parseAsString(debugMessage[i]));
			}
		}
		messageStore.save(doc);
	}

	private MessageDocAbstract messageDoc(String type, LoggableEntity inBoundEvent) {
		MessageDocLogs doc = new MessageDocLogs();
		if (ArgUtil.is(inBoundEvent)) {
			doc.setSessionId(inBoundEvent.getSessionId());
			doc.setContactId(inBoundEvent.getContactId());
		}
		return doc;
	}

	private MessageDocAbstract messageDoc(String type, LogMessage message) {
		MessageDocAbstract doc = new MessageDocLogs();
		doc.setType(type);
		if (ArgUtil.is(message)) {
			doc.setSessionId(message.getSessionId());
			doc.setMessageId(message.getMessageId());
			doc.setMessageIdExt(message.getMessageIdExt());
			doc.setMessageIdRef(message.getMessageIdRef());
			doc.setContactId(PostManUtil.createContactId(message.contact()));
		}
		return doc;
	}

	public void debug(String message, Object... debugMessage) {
		if (!LOGGER.isDebugEnabled())
			return;
		if (ArgUtil.is(messageContext.getMessage())) {
			this.log(messageDoc("D", messageContext.getMessage()), message, debugMessage);
		} else if (ArgUtil.is(messageContext.getInBoundEvent())) {
			this.log(messageDoc("D", messageContext.getInBoundEvent()), message, debugMessage);
		} else {
			this.log(messageDoc("D", new InBoundEvent()), message, debugMessage);
		}
	}

	public void debug(InBoundEvent assignEvent, String message, Object... debugMessage) {
		if (!LOGGER.isDebugEnabled())
			return;
		this.log(messageDoc("D", assignEvent), message, debugMessage);
	}

	public void warn(String message, Object... debugMessage) {
		if (!LOGGER.isWarnEnabled())
			return;
		if (ArgUtil.is(messageContext.getMessage())) {
			this.log(messageDoc("W", messageContext.getMessage()), message, debugMessage);
		} else if (ArgUtil.is(messageContext.getInBoundEvent())) {
			this.log(messageDoc("W", messageContext.getInBoundEvent()), message, debugMessage);
		} else {
			this.log(messageDoc("W", new InBoundEvent()), message, debugMessage);
		}
	}

	public void addTrace(TraceMessage inboxMessage, Object... msg) {
		if (msg == null || msg.length == 0 || inboxMessage == null) {
			return;
		}
		Object[] result = new Object[msg.length + 1];
		result[0] = appConfig.getAppInstanceType();
		System.arraycopy(msg, 0, result, 1, msg.length);
		CommonMongoQueryBuilder builder = new CommonMongoQueryBuilder();

		if (ArgUtil.is(inboxMessage.id())) {
			builder.whereIdSafe(inboxMessage.id());
			inboxMessage.trace().add(result);
			builder.update().push("trace", result);
			messageStore.updateFirst(builder.getQuery(), builder.getUpdate(), MessageDoc.class,
					MessageStore.getCollectionName(inboxMessage.contact().type()));
		} else {
			inboxMessage.trace().add(result);
		}
	}

	/**
	 * Extract error code from HTTP response and format as "Code:XXXXX"
	 */
	private String getErrorCode(Object httpResp) {
		if (httpResp == null)
			return null;

		try {
			// Cast to Map if possible
			@SuppressWarnings("unchecked")
			Map<String, Object> respMap = (Map<String, Object>) httpResp;

			// Check Facebook/WhatsApp API format: {"error": {"code": 131000}}
			Object errorObj = respMap.get("error");
			if (errorObj instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, Object> error = (Map<String, Object>) errorObj;
				Object code = error.get("code");
				if (code != null)
					return "Code:" + code.toString();
			}

			// Check direct code field: {"code": 131000}
			Object code = respMap.get("code");
			if (code != null)
				return "Code:" + code.toString();

		} catch (Exception e) {
			// Safe fallback
		}

		return null;
	}

	/**
	 * Create type L message for call_permission_reply. Other call events are
	 * handled by regular createOrUpdate flow.
	 */
	public MessageDoc createOrUpdateCall(MessageCall call, InboxMessage inboxMessage) {

		if (MessageCall.EVENTS.CALL_PERMISSION_REPLY.equals(call.getEvent())) {

			// Zero-DB-call deduplication: If messageId is already set, type L was already
			// created
//			if (ArgUtil.isNotEmpty(inboxMessage.getMessageId())) {
//				not required as 			
//				return;
//			}

			String response = ArgUtil.parseAsString(inboxMessage.form().get("call_permission_response"));
			Long grantedAt = call.getTimestamp();
			contactStore.applyCallPermissionReply(PostManUtil.createContactId(inboxMessage.contact()), response,
					grantedAt);
			String messageText = "User call permission: " + (ArgUtil.isNotEmpty(response) ? response : "N/A");
			MessageDoc doc = createEventDocument(inboxMessage, getCurrenUser(), EVENTS.CALL_PERMISSION_REPLY,
					messageText);

			doc.setType(MESSAGE_BOUND_TYPE.LOG_CALL);
			doc.setMessageIdExt(inboxMessage.getMessageIdExt());
			doc.setMessage(messageText);
			messageStore.saveMessage(doc, inboxMessage.contact().type());

			if (ArgUtil.is(doc.getMessageId())) {
				inboxMessage.setMessageId(doc.getMessageId());
			}

			return doc;
		} else {
			return messageStore.createOrUpdate(inboxMessage);
		}

	}
}
