package com.boot.jx.postman.fb;

import java.io.Serializable;
import java.util.Map;

import com.boot.utils.ArgUtil;

public class FacebookMessaging implements Serializable {
	private static final long serialVersionUID = 4410460271776560521L;
	private Map<String, String> sender;
	private Map<String, String> recipient;
	private Long timestamp;
	private FacebookMessage message;
	private FacebookPostback postBack;
	private FacebookReaction reaction;
	private Object delivery;

	private Map<String, Object> read;

	public Map<String, String> getSender() {
		return sender;
	}

	public void setSender(Map<String, String> sender) {
		this.sender = sender;
	}

	public Map<String, String> getRecipient() {
		return recipient;
	}

	public void setRecipient(Map<String, String> recipient) {
		this.recipient = recipient;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	public FacebookMessage getMessage() {
		return message;
	}

	public void setMessage(FacebookMessage message) {
		this.message = message;
	}

	public FacebookPostback getPostBack() {
		return postBack;
	}

	public void setPostback(FacebookPostback postBack) {
		this.postBack = postBack;
	}

	public Map<String, Object> getRead() {
		return read;
	}

	public void setRead(Map<String, Object> read) {
		this.read = read;
	}

	public long getReadWatermark() {
		if (this.read == null) {
			return 0L;
		}
		if (ArgUtil.is(read.get("watermark"))) {
			return ArgUtil.parseAsLong(this.read.get("watermark"));
		}
		return 0L;
	}

	public boolean isValidCustomerMessage() {
		return (ArgUtil.is(this.message) && !this.message.isIs_echo());
	}

	public FacebookReaction getReaction() {
		return reaction;
	}

	public void setReaction(FacebookReaction reaction) {
		this.reaction = reaction;
	}

	public void setPostBack(FacebookPostback postBack) {
		this.postBack = postBack;
	}

	public Object getDelivery() {
		return this.delivery;
	}

	public void setDelivery(Object delivery) {
		this.delivery = delivery;
	}
}
