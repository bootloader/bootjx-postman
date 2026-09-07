package com.boot.jx.postman.doc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.mongo.CommonDocInterfaces.TimeStampIndex.UpdatedTimeStampDoc;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.PMArgs;
import com.boot.jx.postman.model.ext.InBoundEvent;

@Document(collection = MessageHold.COLLECTION_NAME)
@TypeAlias("MessageHold")
public class MessageHold extends UpdatedTimeStampDoc implements Serializable {

	private static final long serialVersionUID = -1916969779141145310L;

	public static final String COLLECTION_NAME = "MESSAGE_HOLD";

	public static final String COLLECTION_ORIGINAL = "MESSAGE_ORIGINAL";
	public static final String COLLECTION_REJECTED = "MESSAGE_REJECTED";
	public static final String COLLECTION_QUEUED = "MESSAGE_QUEUED";

	public enum MESSAGE_QUEUE_TYPE {
		HOLD, ORIGINAL, REJECTED, QUEUED
	}

	@Id
	private String tempId;

	@Indexed
	private String contactId;

	@Indexed
	private String sessionId;

	@Indexed
	private String appType;

	@Indexed
	private String appVenv;

	@Indexed
	private String batch;

	private long timestamp;

	private InboxMessage inboxMessage;

	private InBoundEvent event;

	private PMArgs pmArgs;

	private Map<String, Object> map;
	private List<Object> logs;

	private Object httpReq;
	private Object httpResp;

	public String getTempId() {
		return tempId;
	}

	public void setTempId(String tempId) {
		this.tempId = tempId;
	}

	public String getContactId() {
		return contactId;
	}

	public void setContactId(String contactId) {
		this.contactId = contactId;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public InboxMessage getInboxMessage() {
		return inboxMessage;
	}

	public void setInboxMessage(InboxMessage inboxMessage) {
		this.inboxMessage = inboxMessage;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public String getAppType() {
		return appType;
	}

	public void setAppType(String appType) {
		this.appType = appType;
	}

	public InBoundEvent getEvent() {
		return event;
	}

	public void setEvent(InBoundEvent event) {
		this.event = event;
	}

	public PMArgs getPmArgs() {
		return pmArgs;
	}

	public void setPmArgs(PMArgs pmArgs) {
		this.pmArgs = pmArgs;
	}

	public MessageHold event(InBoundEvent event) {
		this.event = event;
		return this;
	}

	public MessageHold pmArgs(PMArgs pmArgs) {
		this.pmArgs = pmArgs;
		return this;
	}

	public MessageHold inboxMessage(InboxMessage inboxMessage) {
		this.inboxMessage = inboxMessage;
		return this;
	}

	public String getBatch() {
		return batch;
	}

	public void setBatch(String batch) {
		this.batch = batch;
	}

	@Document(collection = COLLECTION_ORIGINAL)
	@TypeAlias("MessageHoldOriginal")
	public static class MessageHoldOriginal extends MessageHold {
		private static final long serialVersionUID = -4164969609975765804L;
	}

	@Document(collection = COLLECTION_REJECTED)
	@TypeAlias("MessageHoldRejected")
	public static class MessageHoldRejected extends MessageHold {
		private static final long serialVersionUID = 5700536999322313441L;
		private String reason;

		public String getReason() {
			return reason;
		}

		public void setReason(String reason) {
			this.reason = reason;
		}

	}

	@Document(collection = COLLECTION_QUEUED)
	@TypeAlias("MessageHoldQueue")
	public static class MessageHoldQueue extends MessageHold {
		private static final long serialVersionUID = 1137079051032041202L;
	}

	public String getAppVenv() {
		return appVenv;
	}

	public void setAppVenv(String appVenv) {
		this.appVenv = appVenv;
	}

	public List<Object> getLogs() {
		return logs;
	}

	public void setLogs(List<Object> logs) {
		this.logs = logs;
	}

	public List<Object> logs() {
		if (this.getLogs() == null) {
			this.setLogs(new ArrayList<Object>());
		}
		return this.getLogs();
	}

	public Object getHttpReq() {
		return httpReq;
	}

	public void setHttpReq(Object httpReq) {
		this.httpReq = httpReq;
	}

	public Object getHttpResp() {
		return httpResp;
	}

	public void setHttpResp(Object httpResp) {
		this.httpResp = httpResp;
	}
}
