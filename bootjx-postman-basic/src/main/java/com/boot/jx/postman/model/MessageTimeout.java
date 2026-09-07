package com.boot.jx.postman.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageTimeout implements Serializable {
	private static final long serialVersionUID = 1875887497925865671L;
	long read;
	long delivery;
	long reply;

	public long getRead() {
		return read;
	}

	public void setRead(long read) {
		this.read = read;
	}

	public long getDelivery() {
		return delivery;
	}

	public void setDelivery(long delivery) {
		this.delivery = delivery;
	}

	public long getReply() {
		return reply;
	}

	public void setReply(long reply) {
		this.reply = reply;
	}

}
