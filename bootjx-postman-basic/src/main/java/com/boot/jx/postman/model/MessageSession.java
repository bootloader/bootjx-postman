package com.boot.jx.postman.model;

import java.io.Serializable;

import com.boot.utils.ArgUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageSession implements Serializable {
	
	private static final long serialVersionUID = -5472557093277982501L;
	private String dept;
	private String agent;
	private String bot;
	private String mode;
	private String queue;
	private boolean resolved;
	private boolean firstMessage;
	private boolean initMessage;
	private long sessionStamp;

	private String ticketHash;
	private String routingId;

	public String getDept() {
		return dept;
	}

	public void setDept(String dept) {
		this.dept = dept;
	}

	public String getAgent() {
		return agent;
	}

	public void setAgent(String agent) {
		this.agent = agent;
	}

	public void ifNoAgent(String agent) {
		if (ArgUtil.is(this.agent)) {
			this.agent = agent;
		}
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public boolean isResolved() {
		return resolved;
	}

	public void setResolved(boolean resolved) {
		this.resolved = resolved;
	}

	public String getQueue() {
		return queue;
	}

	public void setQueue(String queue) {
		this.queue = queue;
	}

	public String getBot() {
		return bot;
	}

	public void setBot(String bot) {
		this.bot = bot;
	}

	public boolean isFirstMessage() {
		return firstMessage;
	}

	public void setFirstMessage(boolean firstMessage) {
		this.firstMessage = firstMessage;
	}

	public String getTicketHash() {
		return ticketHash;
	}

	public void setTicketHash(String ticketId) {
		this.ticketHash = ticketId;
	}

	public String getRoutingId() {
		return routingId;
	}

	public void setRoutingId(String routingId) {
		this.routingId = routingId;
	}

	public long getSessionStamp() {
		return sessionStamp;
	}

	public void setSessionStamp(long sessionStamp) {
		this.sessionStamp = sessionStamp;
	}

	public boolean isInitMessage() {
		return initMessage;
	}

	public void setInitMessage(boolean initMessage) {
		this.initMessage = initMessage;
	}

}
