package com.boot.jx.postman.doc;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.mongo.CommonDocInterfaces.TimeStampIndex;
import com.boot.jx.mongo.CommonDocInterfaces.TimeStampIndex.CreatedTimeStampIndexSupport;

@Document(collection = "VISITOR_ACTIVITY")
@TypeAlias("VisitorActivity")
public class VisitorActivityDoc implements Serializable, CreatedTimeStampIndexSupport {

	private static final long serialVersionUID = -6276462151233358984L;
	@Id
	private String activityId;
	private String visitId;
	private String traceId;
	private TimeStampIndex created;

	@Indexed
	private String sessionId;
	@Indexed
	private String contactId;

	@Indexed
	private String channelId;

	@Indexed
	private String visitorId;

	@Indexed
	private String activity;

	@Indexed
	private List<String> tags;

	private String origin;

	private String referer;

	private Map<String, Object> meta;
	private Map<String, Object> data;

	public String getActivityId() {
		return activityId;
	}

	public void setActivityId(String activityId) {
		this.activityId = activityId;
	}

	public String getVisitId() {
		return visitId;
	}

	public void setVisitId(String visitId) {
		this.visitId = visitId;
	}

	public String getTraceId() {
		return traceId;
	}

	public void setTraceId(String traceId) {
		this.traceId = traceId;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getContactId() {
		return contactId;
	}

	public void setContactId(String contactId) {
		this.contactId = contactId;
	}

	public String getChannelId() {
		return channelId;
	}

	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}

	public String getVisitorId() {
		return visitorId;
	}

	public void setVisitorId(String visitorId) {
		this.visitorId = visitorId;
	}

	public String getActivity() {
		return activity;
	}

	public void setActivity(String activty) {
		this.activity = activty;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

	public String getReferer() {
		return referer;
	}

	public void setReferer(String referer) {
		this.referer = referer;
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

	public Map<String, Object> getData() {
		return data;
	}

	public void setData(Map<String, Object> data) {
		this.data = data;
	}

	public TimeStampIndex getCreated() {
		return created;
	}

	public void setCreated(TimeStampIndex created) {
		this.created = created;
	}

	public VisitorActivityDoc activity(String activity) {
		this.activity = activity;
		return this;
	}

	public VisitorActivityDoc origin(String origin) {
		this.origin = origin;
		return this;
	}

	public VisitorActivityDoc referer(String referer) {
		this.referer = referer;
		return this;
	}

	public VisitorActivityDoc channelId(String channelId) {
		this.channelId = channelId;
		return this;
	}

	public VisitorActivityDoc contactId(String contactId) {
		this.contactId = contactId;
		return this;
	}
}
