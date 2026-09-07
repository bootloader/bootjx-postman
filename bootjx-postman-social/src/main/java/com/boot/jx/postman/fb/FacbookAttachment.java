package com.boot.jx.postman.fb;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FacbookAttachment implements Serializable {
    private static final long serialVersionUID = 260395496229953084L;
    private String type;
    private FacebookPayload payload;

    public String getType() {
	return type;
    }

    public void setType(String type) {
	this.type = type;
    }

    public FacebookPayload getPayload() {
	return payload;
    }

    public void setPayload(FacebookPayload payload) {
	this.payload = payload;
    }
}
