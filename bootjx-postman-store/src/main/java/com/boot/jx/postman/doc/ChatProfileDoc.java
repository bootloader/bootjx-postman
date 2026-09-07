package com.boot.jx.postman.doc;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.postman.dto.ChatProfileDTO;

@Document(collection = "CHAT_PROFILE")
@TypeAlias("ChatUserProfile")
public class ChatProfileDoc extends ChatProfileDTO {

	private static final long serialVersionUID = -7243968513050670522L;

	@Id
	private String id;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

}
