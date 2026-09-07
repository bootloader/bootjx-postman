package com.boot.jx.postman.model;

import java.math.BigDecimal;

import com.boot.jx.dict.ContactType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TGMessage extends Message<TGMessage> {
	private static final long serialVersionUID = 2765644882757156928L;

	public static enum Channel implements IChannel {
		DEFAULT
	}

	private BigDecimal queue;

	public TGMessage() {
		super(ContactType.WHATSAPP);
	}

	public BigDecimal getQueue() {
		return queue;
	}

	public void setQueue(BigDecimal queue) {
		this.queue = queue;
	}

}
