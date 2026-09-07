package com.boot.jx.postman.model;

import com.boot.jx.dict.ContactType;
import com.boot.jx.postman.model.MessageOptions.WAMessageOptions;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WAMessage extends Message<WAMessage> implements WAMessageOptions {
	private static final long serialVersionUID = 2765644882757156928L;

	public static enum Channel implements IChannel {
		TWILIO, APIWHA, DEFAULT, GUPSHUPAGENT, DUMMY, RAPIWHA
	}

	public WAMessage() {
		super(ContactType.WHATSAPP);
	}

	@Override
	public Message<?> replyMessage(String message) {
		return null;
	}

}
