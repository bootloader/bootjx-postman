package com.boot.jx.postman.model.ext;

import java.util.List;

import com.boot.jx.postman.model.ContactMeta;
import com.boot.jx.postman.model.MessageDefinitions.Contactable;
import com.boot.jx.postman.model.MessageDefinitions.LoggableEntity;
import com.boot.jx.postman.model.MessageDefinitions.SessionInfo;
import com.boot.jx.postman.model.MessageDefinitions.SessionMessageWithReferral;
import com.boot.jx.postman.model.MessageDefinitions.SessionObject;
import com.boot.jx.postman.model.MessageDefinitions.TraceMessage;
import com.boot.jx.postman.model.MessageRouter;
import com.boot.jx.postman.model.MessageSession;
import com.boot.jx.postman.model.MessageTimeout;
import com.boot.jx.swagger.ApiMockModelProperty;
import com.boot.jx.utils.PostManUtil;
import com.boot.utils.ArgUtil;
import com.boot.utils.StringUtils;

public class SessionBoundEvent implements SessionInfo, TraceMessage, LoggableEntity {

	public static class TRIGGER_TYPE {
		public static final String MESSAGE = "MESSAGE";
		public static final String SESSION = "SESSION";
		public static final String ACTION = "ACTION";
		public static final String STATUS = "STATUS";
		public static final String SYSTEM = "SYSTEM";
		public static final String TIMEOUT = "TIMEOUT";
	}

	public static class EVENT_TYPE {
		public static final String SESSION_ROUTED = "SESSION_ROUTED";
		public static final String SESSION_INIT = "SESSION_INIT";
		public static final String SESSION_RESOLVED = "SESSION_RESOLVED";
		public static final String SESSION_EXPIRED = "SESSION_EXPIRED";
		public static final String SESSION_CLOSED = "SESSION_CLOSED";
		public static final String SESSION_STATUS = "SESSION_STATUS";
		public static final String SESSION_ASSIGNED = "SESSION_ASSIGNED";
		public static final String CONTACT_UPDATE = "CONTACT_UPDATE";
		public static final String MESSAGE_INBOUND = "INBOUND";
		public static final String MESSAGE_OUTBOUND = "OUTBOUND";
		public static final String REPLY_TIMEOUT = "REPLY_TIMEOUT";
		public static final String INBOUND_TIMEOUT = "INBOUND_TIMEOUT";
		public static final String OUTBOUND_TIMEOUT = "OUTBOUND_TIMEOUT";
		public static final String SEND_TIMEOUT = "SEND_TIMEOUT";
		/** ClickPost shipment tracking webhook forwarded to Scriptus. */
		public static final String CLICKPOST_TRACKING = "CLICKPOST_TRACKING";
	}

	private static final long serialVersionUID = 5398462708633545227L;
	private MessageSession session;
	public Contactable contact;
	private MessageRouter route;
	public String contactId;
	protected List<Object> trace;

	public String sessionId;
	public String sessionBulkId;
	public List<String> sessionIds;
	public String messageId;
	public long messageTimestamp;
	public String eventId;
	private String checksum;
	private long timestamp;
	private MessageTimeout timeout;

	@ApiMockModelProperty(example = "SESSION_ROUTED", value = "Event Triggered by App/Service",
			allowableValues = "SESSION_ROUTED,SESSION_INIT,SESSION_CLOSED,SESSION_STATUS,"
					+ "SESSION_ASSIGNED,CONTACT_UPDATE,MESSAGE_INBOUND,MESSAGE_OUTBOUND,"
					+ "REPLY_TIMEOUT,INBOUND_TIMEOUT,OUTBOUND_TIMEOUT,SEND_TIMEOUT,CLICKPOST_TRACKING")
	public String type;

	@ApiMockModelProperty(example = "MESSAGE", value = "Bound Type of Event Triggered by App/Service",
			allowableValues = "MESSAGE,ACTION,STATUS,SYSTEM,TIMEOUT")
	public String triggerType;

	public SessionBoundEvent type(String type, String triggerType) {
		this.type = type;
		this.triggerType = triggerType;
		return this;
	}

	public MessageSession getSession() {
		return session;
	}

	public void setSession(MessageSession session) {
		this.session = session;
	}

	@Override
	public String getSessionId() {
		return this.sessionId;
	}

	@Override
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	@Override
	public MessageSession session() {
		if (session == null) {
			this.session = new MessageSession();
		}
		return this.session;
	}

	@Override
	public Contactable contact() {
		if (this.contact == null) {
			this.contact = new ContactMeta();
		}
		return this.contact;
	}

	public MessageRouter getRoute() {
		return route;
	}

	public void setRoute(MessageRouter route) {
		this.route = route;
	}

	@Override
	public MessageRouter route() {
		if (route == null) {
			this.route = new MessageRouter();
		}
		return this.route;
	}

	@Override
	public List<Object> getTrace() {
		return trace;
	}

	@Override
	public void setTrace(List<Object> trace) {
		this.trace = trace;
	}

	@Override
	public String getContactId() {
		if (ArgUtil.is(this.contactId)) {
			return this.contactId;
		}
		return PostManUtil.CONTACT_ID(this.contact());
	}

	public String eventId() {
		if (!ArgUtil.is(this.eventId)) {
			this.eventId = String.format("%s/%s", this.messageId, this.type);
		}
		return this.eventId;
	}

	@Override
	public String id() {
		return this.eventId;
	}

	@Override
	public void id(String id) {
		this.eventId = id;
	}

	public Contactable getContact() {
		return contact;
	}

	public void setContact(Contactable contact) {
		this.contact = contact;
	}

	public String getEventId() {
		return eventId;
	}

	public void setEventId(String eventId) {
		this.eventId = eventId;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public String getEventCode() {
		return type;
	}

	public void setEventCode(String eventCode) {
		this.type = eventCode;
	}

	public void setContactId(String contactId) {
		this.contactId = contactId;
	}

	public String getChecksum() {
		return checksum;
	}

	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

	public String getTriggerType() {
		return triggerType;
	}

	public void setTriggerType(String triggerType) {
		this.triggerType = triggerType;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void from(SessionMessageWithReferral message) {
		this.timestamp = message.getTimestamp();
		this.messageId = message.getMessageId();
		this.messageTimestamp = message.getMessageTimestamp();
		this.sessionId = message.getSessionId();
		this.sessionBulkId = ArgUtil.anyOf(message.getSessionBulkId(), message.referral().getBulkId());
		this.setSession(message.session());
		this.contact().copyFrom(message.contact());
		this.timeout = message.getTimeout();
		message.session();
	}

	public SessionBoundEvent from(SessionObject session) {
		this.timestamp = System.currentTimeMillis();
		this.messageId = null;
		this.sessionId = session.getSessionId();
		this.session().from(session);
		this.contactId = session.contact().getContactId();
		this.contact().copyFrom(session.contact());
		this.timeout = null;
		return this;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public MessageTimeout getTimeout() {
		return timeout;
	}

	public void setTimeout(MessageTimeout timeout) {
		this.timeout = timeout;
	}

	public List<String> getSessionIds() {
		return sessionIds;
	}

	public void setSessionIds(List<String> sessionIds) {
		this.sessionIds = sessionIds;
	}

	public void fixEvent() {
		if (!ArgUtil.is(triggerType)) {
			switch (type) {
			case EVENT_TYPE.SESSION_ROUTED:
			case EVENT_TYPE.SESSION_INIT:
			case EVENT_TYPE.SESSION_RESOLVED:
			case EVENT_TYPE.SESSION_CLOSED:
			case EVENT_TYPE.SESSION_STATUS:
			case EVENT_TYPE.SESSION_ASSIGNED:
				this.triggerType = TRIGGER_TYPE.SESSION;
				break;
			case EVENT_TYPE.CLICKPOST_TRACKING:
				this.triggerType = TRIGGER_TYPE.STATUS;
				break;
			default:
				break;
			}
		}
	}

	public String uniqueId() {
		switch (type) {
		case EVENT_TYPE.SESSION_INIT:
		case EVENT_TYPE.SESSION_RESOLVED:
		case EVENT_TYPE.SESSION_CLOSED:
			// One Time Events
			return StringUtils.join(":", this.sessionId, this.triggerType, this.type);
		case EVENT_TYPE.SESSION_ROUTED:
			return StringUtils.join(":", this.sessionId, this.triggerType, this.type, this.route().getRouterId());
		case EVENT_TYPE.SESSION_STATUS:
			return StringUtils.join(":", this.sessionId, this.triggerType, this.type, this.session().getStatus());
		default:
			return StringUtils.join(":", this.sessionId, this.triggerType, this.type, this.getMessageId());
		}
	}

}
