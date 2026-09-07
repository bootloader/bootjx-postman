package com.boot.jx.postman.doc;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ChatMeta implements Serializable {

	private static final long serialVersionUID = 1L;

	long updateStamp;
	String nextHandler;
	String prevHandler;
	boolean agentEnabled;
	String queueCode;
	String sessionId;
	String routingId;

	private Map<String, ChatPromise> promise;

	public Map<String, ChatPromise> getPromise() {
		return promise;
	}

	public void setPromise(Map<String, ChatPromise> promise) {
		this.promise = promise;
	}

	public Map<String, ChatPromise> promise() {
		if (this.promise == null) {
			this.promise = new HashMap<String, ChatPromise>();
		}
		return this.promise;
	}

	public String getNextHandler() {
		return nextHandler;
	}

	public void setNextHandler(String nextHandler) {
		this.nextHandler = nextHandler;
	}

	public String getPrevHandler() {
		return prevHandler;
	}

	public void setPrevHandler(String prevHandler) {
		this.prevHandler = prevHandler;
	}

	public long getUpdateStamp() {
		return updateStamp;
	}

	public void setUpdateStamp(long updateStamp) {
		this.updateStamp = updateStamp;
	}

	public boolean isAgentEnabled() {
		return agentEnabled;
	}

	public void setAgentEnabled(boolean agentEnabled) {
		this.agentEnabled = agentEnabled;
	}

	public String getQueueCode() {
		return queueCode;
	}

	public void setQueueCode(String queueCode) {
		this.queueCode = queueCode;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getRoutingId() {
		return routingId;
	}

	public void setRoutingId(String routingId) {
		this.routingId = routingId;
	}

}
