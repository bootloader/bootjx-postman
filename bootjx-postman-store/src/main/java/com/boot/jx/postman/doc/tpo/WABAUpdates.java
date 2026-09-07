package com.boot.jx.postman.doc.tpo;

import java.io.Serializable;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.mongo.CommonDocInterfaces.TimeStampIndex;
import com.boot.jx.mongo.CommonDocInterfaces.TimeStampIndex.CreatedTimeStampIndexSupport;
import com.boot.jx.mongo.CommonDocInterfaces.TimeStampIndex.UpdatedTimeStampIndexSupport;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Document(collection = WABAUpdates.COLLECTION_NAME)
@TypeAlias(WABAUpdates.COLLECTION_NAME)
@JsonIgnoreProperties(ignoreUnknown = true)
public class WABAUpdates implements Serializable, CreatedTimeStampIndexSupport, UpdatedTimeStampIndexSupport {

	private static final long serialVersionUID = 4116849214262304471L;

	public static final String COLLECTION_NAME = "TP_WABA_UPDATES";

	@Id
	private String id;

	private TimeStampIndex created;
	private TimeStampIndex updated;

	private String channelId;
	private String field;
	private Map<String, Object> value;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public TimeStampIndex getCreated() {
		return created;
	}

	public void setCreated(TimeStampIndex created) {
		this.created = created;
	}

	public TimeStampIndex getUpdated() {
		return updated;
	}

	public void setUpdated(TimeStampIndex updated) {
		this.updated = updated;
	}

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}

	public Map<String, Object> getValue() {
		return value;
	}

	public void setValue(Map<String, Object> value) {
		this.value = value;
	}

	public String getChannelId() {
		return channelId;
	}

	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}

}
