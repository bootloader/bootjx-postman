package com.boot.jx.postman.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.boot.jx.postman.model.Message.Status;
import com.boot.jx.postman.model.MessageDefinitions.Contactable;
import com.boot.jx.postman.model.MessageDefinitions.LogMessage;
import com.boot.jx.postman.model.MessageDefinitions.SessionMessageWithReferral;
import com.boot.jx.postman.model.MessageDefinitions.TraceMessage;
import com.boot.jx.swagger.ApiMockModelProperty;
import com.boot.utils.ArgUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageReport implements LogMessage, Serializable, TraceMessage, SessionMessageWithReferral {

	public MessageReport() {
		super();
		this.origin();
	}

	private static final long serialVersionUID = -9039777977577457215L;

	private String messageId;
	private String messageIdPre;
	private String messageIdExt;
	private String messageIdRef;
	private String sessionId;
	private String sessionBulkId;
	private Contactable contact;

	private long timestamp;
	protected long changeStamp;
	protected long watermarkStamp;
	private Status status = null;
	private String reason = null;
	private List<Object> logs;
	private List<Object> trace;
	private List<MessageReportError> errors;

	private MessageSession session;
	private MessageRouter route;
	private MessageReferral referral;

	/** TP waba con **/
	private Map<String, Object> tpMeta;
	private String type;
	private String templateId;
	private String templateCode;
	private MessageOrigin origin;

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public String getMessageIdExt() {
		return messageIdExt;
	}

	public void setMessageIdExt(String messageIdExt) {
		this.messageIdExt = messageIdExt;
	}

	public String getMessageIdPre() {
		return messageIdPre;
	}

	public void setMessageIdPre(String messageIdPre) {
		this.messageIdPre = messageIdPre;
	}

	public String getMessageIdRef() {
		return messageIdRef;
	}

	public void setMessageIdRef(String messageIdRef) {
		this.messageIdRef = messageIdRef;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public long getChangeStamp() {
		return changeStamp;
	}

	public void setChangeStamp(long changeStamp) {
		this.changeStamp = changeStamp;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
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

	public long getWatermarkStamp() {
		return watermarkStamp;
	}

	public void setWatermarkStamp(long watermarkStamp) {
		this.watermarkStamp = watermarkStamp;
	}

	public List<MessageReportError> getErrors() {
		return errors;
	}

	public void setErrors(List<MessageReportError> errors) {
		this.errors = errors;
	}

	public static class MessageReportError {

		@ApiMockModelProperty(example = "470", value = "Error code.\n")
		public String code;

		@ApiMockModelProperty(
				example = "Failed to send message because you are outside the support window for freeform messages to this user. Please use a valid HSM notification or reconsider.",
				value = "Error code")
		public String title;

		@ApiMockModelProperty(value = "Error details provided, if available/applicable", required = false)
		public String details;

		@ApiMockModelProperty(example = "https://developers.facebook.com/docs/whatsapp/api/errors#error",
				value = "Location for error detail", required = false)
		public String href;

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getDetails() {
			return details;
		}

		public void setDetails(String details) {
			this.details = details;
		}

		public String getHref() {
			return href;
		}

		public void setHref(String href) {
			this.href = href;
		}

		public String toCode() {
			return ArgUtil.nonEmpty(code, title, details, href);
		}
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public MessageSession session() {
		if (session == null) {
			this.session = new MessageSession();
		}
		return this.session;
	}

	public MessageSession getSession() {
		return session;
	}

	public void setSession(MessageSession session) {
		this.session = session;
	}

	public List<Object> getLogs() {
		return logs;
	}

	public void setLogs(List<Object> logs) {
		this.logs = logs;
	}

	@Override
	public String getSubject() {
		return null;
	}

	@Override
	public String getReplyId() {
		return null;
	}

	@Override
	public String getReplyIdExt() {
		return null;
	}

	public String id() {
		return this.getMessageId();
	}

	public void id(String id) {
		this.setMessageId(id);
	}

	public List<Object> getTrace() {
		return trace;
	}

	public void setTrace(List<Object> trace) {
		this.trace = trace;
	}

	@Override
	public List<Object> trace() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MessageRouter route() {
		if (route == null) {
			this.route = new MessageRouter();
		}
		return this.route;
	}

	public MessageRouter getRoute() {
		return route;
	}

	public void setRoute(MessageRouter route) {
		this.route = route;
	}

	public Map<String, Object> getTpMeta() {
		return tpMeta;
	}

	public void setTpMeta(Map<String, Object> tpMeta) {
		this.tpMeta = tpMeta;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTemplateId() {
		return templateId;
	}

	public void setTemplateId(String templateId) {
		this.templateId = templateId;
	}

	public String getTemplateCode() {
		return templateCode;
	}

	public void setTemplateCode(String templateCode) {
		this.templateCode = templateCode;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public MessageTimeout getTimeout() {
		return null;
	}

	public String getSessionBulkId() {
		return sessionBulkId;
	}

	public void setSessionBulkId(String sessionBulkId) {
		this.sessionBulkId = sessionBulkId;
	}

	@Override
	public long getMessageTimestamp() {
		return this.timestamp;
	}

	public MessageReferral getReferral() {
		return referral;
	}

	public void setReferral(MessageReferral referral) {
		this.referral = referral;
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
