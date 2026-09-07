package com.boot.jx.postman.query;

import java.util.Map;

import com.boot.jx.mongo.CommonMongoQueryBuilder.DocQueryBuilder;
import com.boot.jx.postman.doc.tpo.WABAConversation;
import com.boot.jx.postman.model.MessageDefinitions.Contactable;

public class WABAConversationQuery extends DocQueryBuilder<WABAConversation> {

	public WABAConversationQuery(WABAConversation doc) {
		super(doc);
	}

	public WABAConversationQuery(String docId) {
		super(docId);
	}

	@Override
	public WABAConversation newDoc(String id) {
		WABAConversation doc = new WABAConversation();
		doc.setId(id);
		return doc;
	}

	@Override
	public String getId(WABAConversation doc) {
		return doc.getId();
	}

	public WABAConversationQuery setContact(Contactable contact) {
		this.set("contact", contact);
		return this;
	}

	public WABAConversationQuery setConversation(Map<String, Object> conversation) {
		this.set("conversation", conversation);
		return this;
	}

	public WABAConversationQuery setPricing(Map<String, Object> pricing) {
		this.set("pricing", pricing);
		return this;
	}

	public WABAConversationQuery setMeta(Map<String, Object> meta) {
		this.set("meta", meta);
		return this;
	}
	
	
	public WABAConversationQuery setChatSession(Map<String, Object> chatSession) {
		this.set("chatSessionId", chatSession);
		return this;
	}
}
