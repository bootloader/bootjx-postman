package com.boot.jx.postman.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.boot.jx.postman.PMBasic;
import com.boot.jx.postman.model.MessageDefinitions.Contactable;
import com.boot.jx.postman.model.MessageDefinitions.IMessageExtended;
import com.boot.jx.postman.model.MessageDefinitions.LogMessage;
import com.boot.jx.postman.model.MessageDefinitions.TraceMessage;
import com.boot.jx.postman.pbook.PBVCard;
import com.boot.utils.ArgUtil;
import com.boot.utils.StringUtils.StringMatcher;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InboxMessage implements Serializable, IMessageExtended, LogMessage, TraceMessage {

	private static final long serialVersionUID = -4488174520614920589L;
	public static final String REPLY_ID = "reply_id";

	private String messageId;
	private String messageIdExt;
	private String messageIdRef;
	protected List<String> to;
	private String from;
	private String fromName;
	private String sessionId;

	private Contactable contact;
	private BigDecimal queue;

	private long timestamp;
	private String message;
	private String subject;
	private String formatType;
	private String formatSubType;

	@JsonIgnore
	private StringMatcher matcher;

	private String checksum;

	private Object originalMessage;
	private MessageSession session;
	private MessageRouter route;
	private MessagePrompt prompt;

	protected Map<String, Object> form = new HashMap<String, Object>();
	protected Map<String, Object> data = new HashMap<String, Object>();
	private Map<String, Object> replyTo = new HashMap<String, Object>();
	protected TagDocument tags;
	private List<Attachment> attachments = null;
	private List<PBVCard> vccards = null;

	private List<Object> logs;

	private String replyId;
	private String replyIdExt;
	private List<Object> trace;

	public InboxMessage() {
		this.timestamp = System.currentTimeMillis();
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public BigDecimal getQueue() {
		return queue;
	}

	public void setQueue(BigDecimal queue) {
		this.queue = queue;
	}

	public OutboxMessage replyMessage(String message) {
		OutboxMessage reply = new OutboxMessage();
		reply.setQueue(this.getQueue());
		reply.contact().setChannelType(this.contact().getChannelType());
		reply.addTo(this.getFrom());
		reply.setMessage(message);
		reply.contact().copyFrom(this.contact());
		return reply;
	}

	// Builder Functions
	public InboxMessage to(String to) {
		this.to().add(to);
		return this;
	}

	public InboxMessage from(String from) {
		this.setFrom(from);
		return this;
	}

	public InboxMessage message(String message) {
		this.setMessage(message);
		return this;
	}

	@JsonIgnore
	public StringMatcher getMatcher() {
		return matcher;
	}

	@JsonIgnore
	public void setMatcher(StringMatcher matcher) {
		this.matcher = matcher;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public String getFromName() {
		return fromName;
	}

	public void setFromName(String fromName) {
		this.fromName = fromName;
	}

	public String getMessageIdExt() {
		return messageIdExt;
	}

	public void setMessageIdExt(String messageIdExt) {
		this.messageIdExt = messageIdExt;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getChecksum() {
		return checksum;
	}

	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

	public Map<String, Object> getForm() {
		return form;
	}

	public void setForm(Map<String, Object> form) {
		this.form = form;
	}

	public Map<String, Object> form() {
		if (form == null) {
			this.form = new HashMap<String, Object>();
		}
		return this.form;
	}

	public Map<String, Object> getData() {
		return data;
	}

	public void setData(Map<String, Object> data) {
		this.data = data;
	}

	public Map<String, Object> data() {
		if (data == null) {
			this.data = new HashMap<String, Object>();
		}
		return this.data;
	}

	public void setReplyTo(Map<String, Object> reply) {
		this.replyTo = reply;
	}

	public Map<String, Object> getReplyTo() {
		return this.replyTo;
	}

	public Map<String, Object> replyTo() {
		if (replyTo == null) {
			this.replyTo = new HashMap<String, Object>();
		}
		return this.replyTo;
	}

	public Object getOriginalMessage() {
		return originalMessage;
	}

	public void setOriginalMessage(Object originalMessage) {
		this.originalMessage = originalMessage;
	}

	public TagDocument getTags() {
		return tags;
	}

	public void setTags(TagDocument tags) {
		this.tags = tags;
	}

	public MessageSession getSession() {
		return session;
	}

	public void setSession(MessageSession session) {
		this.session = session;
	}

	@Override
	public List<String> to() {
		if (to == null) {
			this.to = new ArrayList<String>();
		}
		return this.to;
	}

	public MessageSession session() {
		if (session == null) {
			this.session = new MessageSession();
		}
		return this.session;
	}

	public List<Attachment> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<Attachment> attachments) {
		this.attachments = attachments;
	}

	public List<Attachment> attachments() {
		if (this.attachments == null) {
			this.attachments = new ArrayList<Attachment>();
		}
		return attachments;
	}

	public InboxMessage attachment(Attachment... attachments) {
		for (Attachment file : attachments) {
			this.attachments().add(file);
		}
		return this;
	}

	@Override
	public String getType() {
		return PMBasic.MESSAGE_BOUND_TYPE.INBOUND;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public List<String> getTo() {
		return to;
	}

	public void setTo(List<String> to) {
		this.to = to;
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

	@Override
	public String toString() {
		return String.format("[messageId:%s]", this.messageId);
	}

	@Override
	public String getReplyId() {
		return replyId;
	}

	public void setReplyId(String replyId) {
		this.replyId = replyId;
	}

	@Override
	public String getReplyIdExt() {
		return replyIdExt;
	}

	public void setReplyIdExt(String replyIdExt) {
		this.replyIdExt = replyIdExt;
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

	public String getMessageIdRef() {
		return messageIdRef;
	}

	public void setMessageIdRef(String messageIdRef) {
		this.messageIdRef = messageIdRef;
	}

	public MessagePrompt getPrompt() {
		return prompt;
	}

	public void setPrompt(MessagePrompt prompt) {
		this.prompt = prompt;
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

	public List<Object> getLogs() {
		return logs;
	}

	public void setLogs(List<Object> logs) {
		this.logs = logs;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public List<PBVCard> getVccards() {
		return vccards;
	}

	public void setVccards(List<PBVCard> vccards) {
		this.vccards = vccards;
	}

	public List<PBVCard> vccards() {
		if (vccards == null) {
			this.vccards = new ArrayList<PBVCard>();
		}
		return this.vccards;
	}

	public String toReplyEnum() {
		String codeValue = this.form().get(REPLY_ID) == null ? this.getMessage() : this.form().get(REPLY_ID).toString();
		if (ArgUtil.is(codeValue)) {
			codeValue = codeValue.toLowerCase().trim();
		}
		return codeValue;
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

	@Override
	public String id() {
		return this.getMessageId();
	}

	@Override
	public void id(String id) {
		this.setMessageId(id);
	}

}
