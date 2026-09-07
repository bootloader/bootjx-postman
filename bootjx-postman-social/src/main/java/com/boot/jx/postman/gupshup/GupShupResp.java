package com.boot.jx.postman.gupshup;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GupShupResp implements Serializable {

	private static final long serialVersionUID = -7425134915725975228L;

	GupShupRespMsg response;

	GupShupRespData<GupShupRespMsg> data;

	String status;

	String description;

	public GupShupRespMsg getResponse() {
		return response;
	}

	public void setResponse(GupShupRespMsg response) {
		this.response = response;
	}

	public GupShupRespData<GupShupRespMsg> getData() {
		return data;
	}

	public void setData(GupShupRespData<GupShupRespMsg> data) {
		this.data = data;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
