package com.boot.jx.postman.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.boot.jx.dict.ContactType;
import com.boot.jx.postman.model.MessageDefinitions.IMessage;
import com.boot.jx.postman.model.MessageDefinitions.IMessageExtended;
import com.boot.jx.postman.model.MessageDefinitions.LogMessage;
import com.boot.jx.postman.model.MessageOptions.WAMessageOptions;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OutboxMessage extends Message<OutboxMessage>
		implements WAMessageOptions, IMessage, LogMessage, IMessageExtended {

	private static final long serialVersionUID = 3115992767625612005L;

	public static final OutboxMessage NO_MESSAGE = new OutboxMessage();

	private BigDecimal queue;
	private MessageSession session;
	private MessageRouter route;
	private MessagePrompt prompt;
	private List<Object> logs;
	private List<Object> trace;
	/** csv refernce key**/
	private String referenceKey;
	
	/** **/
	private Map<String,Object> rawMessageFormat;


	public OutboxMessage(ContactType contactType) {
		super(contactType);
		this.updateStatus(Status.SCHLD);
	}

	public OutboxMessage() {
		super();
	}

	public BigDecimal getQueue() {
		return queue;
	}

	public void setQueue(BigDecimal queue) {
		this.queue = queue;
	}

	public MessageSession getSession() {
		return session;
	}

	public void setSession(MessageSession session) {
		this.session = session;
	}

	@Override
	public MessageSession session() {
		if (session == null) {
			this.session = new MessageSession();
		}
		return this.session;
	}

	public List<Object> getLogs() {
		return logs;
	}

	public void setLogs(List<Object> logs) {
		this.logs = logs;
	}

	public List<Object> logs() {
		if (this.logs == null) {
			this.logs = new ArrayList<Object>();
		}
		return this.logs;
	}

	public String getCsid() {
		return this.contact().getCsid();
	}

	public void setCsid(String csid) {
		this.contact().setCsid(csid);
	}

	public MessagePrompt getPrompt() {
		return prompt;
	}

	public void setPrompt(MessagePrompt prompt) {
		this.prompt = prompt;
	}

	public OutboxMessage prompt(MessagePrompt prompt) {
		this.prompt = prompt;
		return this;
	}

	public MessageRouter getRoute() {
		return route;
	}

	public void setRoute(MessageRouter route) {
		this.route = route;
	}

	@Override
	public MessageRouter route() {
		if (route == null) {
			this.route = new MessageRouter();
		}
		return this.route;
	}

	@Override
	public String getFrom() {
		return null;
	}

	@Override
	public String getFromName() {
		return null;
	}

	@Override
	public Message<?> replyMessage(String message) {
		return null;
	}

	public List<Object> trace() {
		if (this.trace == null) {
			this.trace = new ArrayList<Object>();
		}
		return this.trace;
	}

	public List<Object> getTrace() {
		return trace;
	}

	public void setTrace(List<Object> trace) {
		this.trace = trace;
	}

	public String getReferenceKey() {
		return referenceKey;
	}

	public void setReferenceKey(String referenceKey) {
		this.referenceKey = referenceKey;
	}

	public Map<String, Object> getRawMessageFormat() {
		return rawMessageFormat;
	}

	public void setRawMessageFormat(Map<String, Object> rawMessageFormat) {
		this.rawMessageFormat = rawMessageFormat;
	}

}
