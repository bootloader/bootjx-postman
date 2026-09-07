package com.boot.jx.postman.dms;

import java.util.Map;

public class DMSObject {
	private long timestamp;
	private String publicId;
	private String apiKey;
	private String url;
	private String signature;
	private Map<String, Object> params;

	private String callbackUrl;
	private String uploadPageUrl;

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public String getPublicId() {
		return publicId;
	}

	public void setPublicId(String publicId) {
		this.publicId = publicId;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getUploadPageUrl() {
		return uploadPageUrl;
	}

	public void setUploadPageUrl(String uploadPageUrl) {
		this.uploadPageUrl = uploadPageUrl;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	public Map<String, Object> getParams() {
		return params;
	}

	public void setParams(Map<String, Object> params) {
		this.params = params;
	}

	public String getCallbackUrl() {
		return callbackUrl;
	}

	public void setCallbackUrl(String callbackUrl) {
		this.callbackUrl = callbackUrl;
	}
}
