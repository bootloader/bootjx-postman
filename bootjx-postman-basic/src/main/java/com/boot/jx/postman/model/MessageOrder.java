package com.boot.jx.postman.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageOrder implements Serializable {
	private static final long serialVersionUID = 1L;

	@JsonAlias({ "order_id" })
	private String orderId;

	private String status;

	@JsonAlias({ "clickpost_status_code" })
	private String statusCode;

	@JsonAlias({ "clickpost_status_bucket" })
	private String statusBucket;

	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}

	public String getStatusBucket() {
		return statusBucket;
	}

	public void setStatusBucket(String statusBucket) {
		this.statusBucket = statusBucket;
	}

	@Override
	public String toString() {
		return "MessageOrder{orderId=" + orderId + ", status=" + status + ", statusCode=" + statusCode
				+ ", statusBucket=" + statusBucket + "}";
	}
}
