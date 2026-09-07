package com.boot.jx.postman.doc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.mongo.CommonDocInterfaces.TimeStampIndex.TimeStampDoc;
import com.boot.jx.postman.model.Attachment;
import com.boot.jx.postman.model.MessageReferral;

@Document(collection = MessageTempInbound.COLLECTION_NAME)
@TypeAlias("MessageTempInbound")
public class MessageTempInbound extends TimeStampDoc implements Serializable {

	private static final long serialVersionUID = -1916969779141145310L;

	public static final String COLLECTION_NAME = "MESSAGE_TEMP_INBOUND";

	@Id
	private String tempId;

	@Indexed
	private String channelType;

	private Map<String, Object> data;
	private List<Attachment> attachments = null;
	private MessageReferral referral;
	protected Map<String, Object> form = new HashMap<String, Object>();

	private List<Object> logs;

	public String getTempId() {
		return tempId;
	}

	public void setTempId(String tempId) {
		this.tempId = tempId;
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

	public String getChannelType() {
		return channelType;
	}

	public void setChannelType(String channelType) {
		this.channelType = channelType;
	}

	public Map<String, Object> getData() {
		return data;
	}

	public void setData(Map<String, Object> data) {
		this.data = data;
	}

	public List<Attachment> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<Attachment> attachments) {
		this.attachments = attachments;
	}

	public MessageReferral getReferral() {
		return referral;
	}

	public void setReferral(MessageReferral referral) {
		this.referral = referral;
	}

	public Map<String, Object> getForm() {
		return form;
	}

	public void setForm(Map<String, Object> form) {
		this.form = form;
	}

}
