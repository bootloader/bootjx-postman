package com.boot.jx.postman.doc.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.mongo.CommonDocInterfaces.AuditableByIdEntity;
import com.boot.jx.mongo.CommonDocInterfaces.IDocument;
import com.boot.model.UtilityModels.JsonIgnoreUnknown;

/**
 * One document per onboarding task in {@code CONFIG_ONBOARD}. Id is the task
 * code (e.g. REPORTING_TIMEZONE).
 */
@Document(collection = OnboardConfigDoc.COLLECTION)
@TypeAlias("OnboardConfig")
public class OnboardConfigDoc implements IDocument, AuditableByIdEntity, JsonIgnoreUnknown {

	public static final String COLLECTION = "CONFIG_ONBOARD";

	private static final long serialVersionUID = 1L;

	@Id
	private String id;

	/** TODO / DONE / SKIPPED */
	private String status;

	private boolean mandatory;

	private String comment;

	/** Related refs / stamps (deptId, agentId, channelId, messageId, timezone, …) */
	private Map<String, Object> data;

	private String createdBy;
	private Long createdStamp;
	private String updatedBy;
	private Long updatedStamp;
	private String traceId;

	public OnboardConfigDoc() {
	}

	public OnboardConfigDoc(String taskCode) {
		this.id = taskCode;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public boolean isMandatory() {
		return mandatory;
	}

	public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public Map<String, Object> getData() {
		if (data == null) {
			data = new HashMap<>();
		}
		return data;
	}

	public void setData(Map<String, Object> data) {
		this.data = data;
	}

	public OnboardConfigDoc data(String key, Object value) {
		getData().put(key, value);
		return this;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public Long getCreatedStamp() {
		return createdStamp;
	}

	public void setCreatedStamp(Long createdStamp) {
		this.createdStamp = createdStamp;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	public Long getUpdatedStamp() {
		return updatedStamp;
	}

	public void setUpdatedStamp(Long updatedStamp) {
		this.updatedStamp = updatedStamp;
	}

	public String getTraceId() {
		return traceId;
	}

	public void setTraceId(String traceId) {
		this.traceId = traceId;
	}
}
