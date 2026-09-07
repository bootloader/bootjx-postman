package com.boot.jx.postman.model;

import java.io.Serializable;

public class WebhookHealth implements Serializable {
	private static final long serialVersionUID = 1L;

	public static final String STATUS_ONLINE = "ONLINE";
	public static final String STATUS_ERROR = "ERROR";

	private String status = STATUS_ONLINE;
	private Long failedAt;
	private String failedMessage;

	public String getFailedMessage() {
		return failedMessage;
	}

	public void setFailedMessage(String failedMessage) {
		this.failedMessage = failedMessage;
	}

	private Long restoredAt;

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Long getFailedAt() {
		return failedAt;
	}

	public void setFailedAt(Long failedAt) {
		this.failedAt = failedAt;
	}

	public Long getRestoredAt() {
		return restoredAt;
	}

	public void setRestoredAt(Long restoredAt) {
		this.restoredAt = restoredAt;
	}

	/**
	 * Mark webhook as failed. Only updates if status changes from ONLINE to ERROR.
	 */
	public void markFailed(String failedMessage) {
		if (!STATUS_ERROR.equals(this.status)) {
			this.status = STATUS_ERROR;
			if (this.failedAt == null) {
				this.failedAt = System.currentTimeMillis();
				this.failedMessage = failedMessage;
			}
		}
	}

	/**
	 * Mark webhook as restored. Only updates if status changes from ERROR to
	 * ONLINE.
	 */
	public void markRestored() {
		if (!STATUS_ONLINE.equals(this.status)) {
			this.status = STATUS_ONLINE;
			this.restoredAt = System.currentTimeMillis();
		}
	}
}
