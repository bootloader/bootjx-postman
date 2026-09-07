package com.boot.jx.postman.model;

import com.boot.jx.dict.ContactType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DefaultMessage extends Message<DefaultMessage> {

	private static final long serialVersionUID = 3115992767625612005L;

	public DefaultMessage(ContactType contactType) {
		super(contactType);
	}

	public DefaultMessage() {
		super();
	}

}
