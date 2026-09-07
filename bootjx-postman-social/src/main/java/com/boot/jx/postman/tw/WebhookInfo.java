package com.boot.jx.postman.tw;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookInfo {
	private String id;

	private boolean valid;

	private String url;

	@JsonProperty("created_at")
	private String createdAt;

	public long idAsLong() {
		return Long.parseLong(id);
	}

	public boolean isValid() {
		return valid;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}
}
