package com.boot.jx.postman.doc.tpo;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.mongo.CommonDocInterfaces.TimeStampIndex;
import com.boot.jx.mongo.CommonDocInterfaces.TimeStampIndex.CreatedTimeStampIndexSupport;
import com.boot.jx.mongo.CommonDocInterfaces.TimeStampIndex.UpdatedTimeStampIndexSupport;
import com.boot.jx.postman.doc.ContactDetailDoc;
import com.boot.jx.postman.model.MessageDefinitions.Contactable;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Document(collection = WABAConversation.COLLECTION_NAME)
@TypeAlias("TP_WABA_CONVERSATIONS")
@JsonIgnoreProperties(ignoreUnknown = true)
public class WABAConversation implements Serializable, CreatedTimeStampIndexSupport, UpdatedTimeStampIndexSupport {

	private static final long serialVersionUID = 4116849214262304471L;

	public static final String COLLECTION_NAME = "TP_WABA_CONVERSATIONS";

	@Id
	private String id;

	private ContactDetailDoc contact;
	private TimeStampIndex created;
	private TimeStampIndex updated;

	private Map<String, Object> conversation;
	private Map<String, Object> pricing;
	private Map<String, Object> meta;
	LinkedHashMap<String, Object> lMap = null;
	/** Chat session Id **/
	private Map<String, Object> chatSession;
	

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Map<String, Object> getConversation() {
		return conversation;
	}

	public void setConversation(Map<String, Object> conversation) {
		this.conversation = conversation;
	}

	public Map<String, Object> getPricing() {
		return pricing;
	}

	public void setPricing(Map<String, Object> pricing) {
		this.pricing = pricing;
	}

	public ContactDetailDoc getContact() {
		return contact;
	}

	public void setContact(ContactDetailDoc contact) {
		this.contact = contact;
	}

	public Contactable contact() {
		if (this.contact == null) {
			this.contact = new ContactDetailDoc();
		}
		return this.contact;
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

	public Map<String, Object> getMeta() {
		return meta;
	}

	public void setMeta(Map<String, Object> meta) {
		this.meta = meta;
	}

	public Map<String, Object> getChatSession() {
		return chatSession;
	}

	public void setChatSession(Map<String, Object> chatSession) {
		this.chatSession = chatSession;
	}
}
