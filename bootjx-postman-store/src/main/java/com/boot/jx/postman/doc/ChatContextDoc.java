package com.boot.jx.postman.doc;

import java.io.Serializable;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.swagger.ApiMockModelProperty;
import com.fasterxml.jackson.annotation.JsonProperty;

@Document(collection = "CHAT_CONTEXT")
@TypeAlias("ChatContextDoc")
public class ChatContextDoc implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@ApiMockModelProperty(example = "wa919930104050", required = false)
	@JsonProperty("contactId")
	private String contactId;

	ChatMeta meta;

	public ChatMeta getMeta() {
		return meta;
	}

	public void setMeta(ChatMeta meta) {
		this.meta = meta;
	}

	public String getContactId() {
		return contactId;
	}

	public void setContactId(String contactId) {
		this.contactId = contactId;
	}

	public ChatMeta meta() {
		if (this.meta == null) {
			this.meta = new ChatMeta();
		}
		return this.meta;
	}

}
