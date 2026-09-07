package com.boot.jx.postman.doc;

import java.io.Serializable;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Maps a Shopify order id to a Mehery {@code contactId} so repeat ClickPost webhooks
 * can skip Shopify customer lookup. Identity only — tracking status lives on session/message.
 */
@Document(collection = "ORDER_CONTACT")
@TypeAlias("OrderContactDoc")
public class OrderContactDoc implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	private String orderId;

	private String contactId;

	private Long createdAt;

	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public String getContactId() {
		return contactId;
	}

	public void setContactId(String contactId) {
		this.contactId = contactId;
	}

	public Long getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Long createdAt) {
		this.createdAt = createdAt;
	}

}
