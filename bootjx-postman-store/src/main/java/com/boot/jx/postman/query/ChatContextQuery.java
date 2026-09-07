package com.boot.jx.postman.query;

import java.util.Map;

import com.boot.jx.mongo.CommonMongoQueryBuilder.DocQueryBuilder;
import com.boot.jx.postman.doc.ChatContextDoc;
import com.boot.jx.postman.doc.ChatMeta;
import com.boot.jx.postman.doc.ChatPromise;

public class ChatContextQuery extends DocQueryBuilder<ChatContextDoc> {

	public ChatContextQuery(ChatContextDoc doc) {
		super(doc);
	}

	public ChatContextQuery(String contactId) {
		super(contactId);
	}

	@Override
	public String getId(ChatContextDoc doc) {
		return doc.getContactId();
	}

	@Override
	public ChatContextDoc newDoc(String id) {
		ChatContextDoc doc = new ChatContextDoc();
		doc.setContactId(id);
		return doc;
	}

	public void setMeta(ChatMeta chatMeta) {
		this.doc.setMeta(chatMeta);
		this.set("meta", chatMeta);
	}

	public void setPrevHandler(String prevHandler) {
		this.doc.meta().setPrevHandler(prevHandler);
		this.set("meta.prevHandler", prevHandler);
	}

	public void setUpdateStamp(long currentTimeMillis) {
		this.doc.meta().setUpdateStamp(currentTimeMillis);
		this.set("meta.updateStamp", currentTimeMillis);
	}

	public String getNextHandler() {
		return this.doc.meta().getNextHandler();
	}

	public void setNextHandler(String nextHandler) {
		this.doc.meta().setNextHandler(nextHandler);
		this.set("meta.nextHandler", nextHandler);
	}

	public void setQueueCode(String queueCode) {
		this.doc.meta().setQueueCode(queueCode);
		this.set("meta.queueCode", queueCode);
	}

	public void setSessionId(String sessionId) {
		this.doc.meta().setSessionId(sessionId);
		this.set("meta.sessionId", sessionId);
	}

	public void setRoutingId(String routingId) {
		this.doc.meta().setRoutingId(routingId);
		this.set("meta.routingId", routingId);
	}

	public String getQueueCode() {
		return this.doc.meta().getQueueCode();
	}

	public Map<String, ChatPromise> getPromise() {
		return this.doc.meta().getPromise();
	}

	public Map<String, ChatPromise> promise() {
		return this.doc.meta().promise();
	}

	public String getPrevHandler() {
		return this.doc.meta().getPrevHandler();
	}

}
