package com.boot.jx.postman.gupshup;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GupShupRespData<T> implements Serializable {

	private static final long serialVersionUID = -1719356809980411974L;

	@JsonProperty("response_messages")
	List<T> responseMessage;

	public List<T> getResponseMessage() {
		return responseMessage;
	}

	public void setResponseMessage(List<T> responseMessage) {
		this.responseMessage = responseMessage;
	}
}
