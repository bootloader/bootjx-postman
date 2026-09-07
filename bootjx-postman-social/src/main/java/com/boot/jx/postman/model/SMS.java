package com.boot.jx.postman.model;

import com.boot.jx.dict.ContactType;
import com.boot.utils.ArgUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SMS extends Message<SMS> {

	private static final long serialVersionUID = 7854194104138401905L;

	public static enum Channel implements IChannel {
		P, T;

		public static Channel DEFAULT = T;
	}

	public String toText() {
		return this.getMessage();
	}

	public SMS() {
		super(ContactType.SMS);
		this.contact().setChannelType(Channel.DEFAULT.toString());
	}

	public void setIChannel(Channel channel) {
		this.contact().setChannelType(ArgUtil.parseAsString(channel));
	}

}
