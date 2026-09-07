package com.boot.jx.postman.events;

import java.math.BigDecimal;

import com.boot.jx.dict.ContactType;
import com.boot.jx.postman.model.Message;
import com.boot.jx.postman.model.TGMessage;
import com.boot.jx.postman.model.WAMessage;
import com.boot.jx.tunnel.ITunnelDefs.ITunnelEvent;
import com.boot.utils.ArgUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserInboxEvent implements ITunnelEvent {

	private static final long serialVersionUID = -4488174520614920589L;

	private String to;
	private String from;
	private String message;
	private ContactType contactType;
	private WAMessage.Channel waChannel;
	private TGMessage.Channel tgChannel;
	private BigDecimal queue;

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public WAMessage.Channel getWaChannel() {
		return waChannel;
	}

	public void setWaChannel(WAMessage.Channel waChannel) {
		this.waChannel = waChannel;
	}

	public BigDecimal getQueue() {
		return queue;
	}

	public void setQueue(BigDecimal queue) {
		this.queue = queue;
	}

	public WAMessage replyWAMessage(String message) {
		WAMessage reply = new WAMessage();
		reply.setQueue(this.getQueue());
		reply.setIChannel(this.getWaChannel());
		reply.addTo(this.getFrom());
		reply.setMessage(message);
		return reply;
	}

	public Message<?> replyMessage(String message) {
		if (ArgUtil.is(this.getWaChannel())) {
			WAMessage reply = new WAMessage();
			reply.setQueue(this.getQueue());
			reply.setIChannel(this.getWaChannel());
			reply.addTo(this.getFrom());
			reply.setMessage(message);
			return reply;
		} else if (ArgUtil.is(this.getTgChannel())) {
			TGMessage reply = new TGMessage();
			reply.setQueue(this.getQueue());
			reply.setIChannel(this.getTgChannel());
			reply.addTo(this.getFrom());
			reply.setMessage(message);
			return reply;
		}
		return null;
	}

	// Builder Functions
	public UserInboxEvent to(String to) {
		this.setTo(to);
		return this;
	}

	public UserInboxEvent from(String from) {
		this.setFrom(from);
		return this;
	}

	public UserInboxEvent message(String message) {
		this.setMessage(message);
		return this;
	}

	public UserInboxEvent waChannel(WAMessage.Channel waChannel) {
		this.setWaChannel(waChannel);
		return this;
	}

	public TGMessage.Channel getTgChannel() {
		return tgChannel;
	}

	public void setTgChannel(TGMessage.Channel tgChannel) {
		this.tgChannel = tgChannel;
	}

	public ContactType getContactType() {
		return contactType;
	}

	public void setContactType(ContactType contactType) {
		this.contactType = contactType;
	}
}
