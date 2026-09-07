
package com.boot.jx.postman.dto;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.boot.jx.logger.LoggerService.LogTimer;
import com.boot.jx.postman.model.Attachment;
import com.boot.jx.postman.model.MessageCall;
import com.boot.jx.postman.model.MessageOrigin;
import com.boot.jx.postman.model.MessageDefinitions.IMessageId;
import com.boot.jx.postman.model.MessageOrder;
import com.boot.jx.postman.model.MessageReferral;
import com.boot.jx.postman.model.MessageRouter;
import com.boot.jx.postman.model.MessageTimeout;
import com.boot.jx.postman.model.TagDocument;
import com.boot.jx.postman.pbook.PBVCard;
import com.boot.jx.swagger.ApiMockModelProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatMessageDTO implements Serializable, IMessageId {
	private static final long serialVersionUID = 7766790295486098869L;
	private String text;
	private String messageTrail;
	private String template;
	private String templateId;
	private String action;
	private String name;
	private String sessionId;
	private String bulkSessionId;
	private String messageId;
	private String messageIdExt;
	private String messageIdRef;
	private String messageIdResend;

	private String replyIdExt;
	private String replyId;

	private long timestamp;
	private String type;
	private TagDocument tags;
	private List<Attachment> attachments;
	private List<PBVCard> vccards;

	private String sender;
	private String status;
	private ContactDTO contact;

	private Map<String, Object> meta;
	private Map<String, Object> options;
	private LogTimer timer;

	/**
	 * @deprecated use referral() to store info
	 * @return
	 */
	@Deprecated
	@ApiMockModelProperty(example = "",
			value = "Information for reply to but deprecated in favor of referral() instead", required = false,
			hidden = true)
	private Map<String, Object> replyTo;

	protected Map<String, Object> form;
	private MessageRouter route;
	private Map<String, Long> stamps;
	public List<Object> logs;

	private MessageReferral referral;
	private MessageOrder order;
	private MessageTimeout timeout;
	private MessageCall call;
	private MessageOrigin origin;

	private String messageIdPre;

	public String getMessageIdPre() {
		return messageIdPre;
	}

	public void setMessageIdPre(String messageIdPre) {
		this.messageIdPre = messageIdPre;
	}

	public MessageReferral getReferral() {
		return referral;
	}

	public void setReferral(MessageReferral referral) {
		this.referral = referral;
	}
	
	public MessageOrder getOrder() {
		return order;
	}

	public void setOrder(MessageOrder order) {
		this.order = order;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public TagDocument getTags() {
		return tags;
	}

	public void setTags(TagDocument tags) {
		this.tags = tags;
	}

	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public List<Attachment> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<Attachment> attachments) {
		this.attachments = attachments;
	}

	public String getSender() {
		return sender;
	}

	public void setSender(String sender) {
		this.sender = sender;
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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public ContactDTO getContact() {
		return contact;
	}

	public void setContact(ContactDTO contact) {
		this.contact = contact;
	}

	public String getTemplateId() {
		return templateId;
	}

	public void setTemplateId(String templateId) {
		this.templateId = templateId;
	}

	public String getBulkSessionId() {
		return bulkSessionId;
	}

	public void setBulkSessionId(String bulkSessionId) {
		this.bulkSessionId = bulkSessionId;
	}

	public Map<String, Object> getMeta() {
		return meta;
	}

	public void setMeta(Map<String, Object> meta) {
		this.meta = meta;
	}

	public Map<String, Object> meta() {
		if (this.meta == null) {
			this.meta = new HashMap<String, Object>();
		}
		return this.meta;
	}

	public String getReplyIdExt() {
		return replyIdExt;
	}

	public void setReplyIdExt(String replyIdExt) {
		this.replyIdExt = replyIdExt;
	}

	public String getReplyId() {
		return replyId;
	}

	public void setReplyId(String replyId) {
		this.replyId = replyId;
	}

	/**
	 * @deprecated use referral() to store info
	 * @return
	 */
	@Deprecated
	public Map<String, Object> getReplyTo() {
		return replyTo;
	}

	/**
	 * @deprecated use referral() to store info
	 * @return
	 */
	@Deprecated
	public void setReplyTo(Map<String, Object> replyTo) {
		this.replyTo = replyTo;
	}

	public MessageRouter getRoute() {
		return route;
	}

	public void setRoute(MessageRouter route) {
		this.route = route;
	}

	public MessageRouter route() {
		if (route == null) {
			this.route = new MessageRouter();
		}
		return this.route;
	}

	public List<PBVCard> getVccards() {
		return vccards;
	}

	public void setVccards(List<PBVCard> vccards) {
		this.vccards = vccards;
	}

	public Map<String, Object> getOptions() {
		return options;
	}

	public void setOptions(Map<String, Object> options) {
		this.options = options;
	}

	public Map<String, Long> getStamps() {
		return stamps;
	}

	public void setStamps(Map<String, Long> stamps) {
		this.stamps = stamps;
	}

	public List<Object> getLogs() {
		return logs;
	}

	public void setLogs(List<Object> logs) {
		this.logs = logs;
	}

	public Map<String, Object> getForm() {
		return form;
	}

	public void setForm(Map<String, Object> form) {
		this.form = form;
	}

	public String getMessageTrail() {
		return messageTrail;
	}

	public void setMessageTrail(String messageTrail) {
		this.messageTrail = messageTrail;
	}

	public String getMessageIdResend() {
		return messageIdResend;
	}

	public void setMessageIdResend(String messageIdResend) {
		this.messageIdResend = messageIdResend;
	}

	public MessageTimeout getTimeout() {
		return timeout;
	}

	public void setTimeout(MessageTimeout timeout) {
		this.timeout = timeout;
	}

	public MessageCall getCall() {
		return call;
	}

	public void setCall(MessageCall call) {
		this.call = call;
	}

	public LogTimer getTimer() {
		return timer;
	}

	public void setTimer(LogTimer timer) {
		this.timer = timer;
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

}
