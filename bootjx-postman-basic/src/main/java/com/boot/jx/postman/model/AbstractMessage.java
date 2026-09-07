package com.boot.jx.postman.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.boot.jx.logger.LoggerService.LogTimer;
import com.boot.jx.postman.model.MessageDefinitions.Contactable;
import com.boot.jx.postman.model.MessageDefinitions.IMessageExtended;

public abstract class AbstractMessage implements IMessageExtended {

	private static final long serialVersionUID = -3592771922137876177L;

	private String messageId;
	private String messageIdPre;
	private String messageIdExt;
	private String messageIdRef;
	private String messageIdResend;

	protected String replyId;
	protected String replyIdExt;

	private String sessionId;
	protected long timestamp;

	private String from;
	private String fromName;
	private Contactable contact;

	protected List<String> to = null;

	protected String subject;
	private String formatType;
	private String formatSubType;

	private BigDecimal queue;
	private LogTimer timer;
	private MessageSession session;
	private List<Object> trace;
	private MessageRouter route;
	private MessageTimeout timeout;
	private MessageOrigin origin;
	private MessageReferral referral;
	private MessageOrder order;

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public String getMessageIdPre() {
		return messageIdPre;
	}

	public void setMessageIdPre(String messageIdPre) {
		this.messageIdPre = messageIdPre;
	}

	public String getMessageIdExt() {
		return messageIdExt;
	}

	public void setMessageIdExt(String messageIdExt) {
		this.messageIdExt = messageIdExt;
	}

	public String getMessageIdRef() {
		return messageIdRef;
	}

	public void setMessageIdRef(String messageIdRef) {
		this.messageIdRef = messageIdRef;
	}

	public String getMessageIdResend() {
		return messageIdResend;
	}

	public void setMessageIdResend(String messageIdResend) {
		this.messageIdResend = messageIdResend;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getReplyId() {
		return replyId;
	}

	public void setReplyId(String replyId) {
		this.replyId = replyId;
	}

	public String getReplyIdExt() {
		return replyIdExt;
	}

	public void setReplyIdExt(String replyIdExt) {
		this.replyIdExt = replyIdExt;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getFromName() {
		return fromName;
	}

	public void setFromName(String fromName) {
		this.fromName = fromName;
	}

	@Override
	public String toString() {
		return String.format("[messageId:%s]", this.messageId);
	}

	public Contactable getContact() {
		return contact;
	}

	public void setContact(Contactable contact) {
		this.contact = contact;
	}

	public Contactable contact() {
		if (this.contact == null) {
			this.contact = new ContactMeta();
		}
		return this.contact;
	}

	public LogTimer getTimer() {
		return timer;
	}

	public void setTimer(LogTimer timer) {
		this.timer = timer;
	}

	public LogTimer timer() {
		if (this.timer == null) {
			this.timer = new LogTimer();
		}
		return this.timer;
	}

	public MessageSession getSession() {
		return session;
	}

	public void setSession(MessageSession session) {
		this.session = session;
	}

	@Override
	public MessageSession session() {
		if (session == null) {
			this.session = new MessageSession();
		}
		return this.session;
	}

	public List<Object> trace() {
		if (this.trace == null) {
			this.trace = new ArrayList<Object>();
		}
		return this.trace;
	}

	public List<Object> getTrace() {
		return trace;
	}

	public void setTrace(List<Object> trace) {
		this.trace = trace;
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
	public MessageTimeout getTimeout() {
		return this.timeout;
	}

	public void setTimeout(MessageTimeout timeout) {
		this.timeout = timeout;
	}

	public MessageOrigin getOrigin() {
		return origin;
	}

	public void setOrigin(MessageOrigin origin) {
		this.origin = origin;
	}

	public MessageOrigin origin() {
		if (this.origin == null) {
			this.origin = new MessageOrigin();
		}
		return origin;
	}

	@Override
	public MessageReferral getReferral() {
		return referral;
	}

	@Override
	public void setReferral(MessageReferral referral) {
		this.referral = referral;
	}
	
	@Override
	public MessageOrder getOrder() {
		return order;
	}

	@Override
	public void setOrder(MessageOrder order) {
		this.order = order;
	}


	/**
	 * @return the to
	 */
	public List<String> getTo() {
		return to;
	}

	/**
	 * @param to the to to set
	 */
	public void setTo(List<String> to) {
		this.to = to;
	}

	public List<String> to() {
		if (this.to == null) {
			this.to = new ArrayList<String>();
		}
		return to;
	}

	@Override
	public String getType() {
		return null;
	}

	public String getFormatType() {
		return formatType;
	}

	public void setFormatType(String formatType) {
		this.formatType = formatType;
	}

	public String getFormatSubType() {
		return formatSubType;
	}

	public void setFormatSubType(String formatSubType) {
		this.formatSubType = formatSubType;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public BigDecimal getQueue() {
		return queue;
	}

	public void setQueue(BigDecimal queue) {
		this.queue = queue;
	}

	@Override
	public Message<?> replyMessage(String message) {
		// TODO Auto-generated method stub
		return null;
	}

}
