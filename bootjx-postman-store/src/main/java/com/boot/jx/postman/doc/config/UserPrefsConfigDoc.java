package com.boot.jx.postman.doc.config;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.postman.PMEnvironment.PMConfigurationObject;

@Document(collection = "CONFIG_USER_PREFS")
@TypeAlias("UserPrefsConfig")
public class UserPrefsConfigDoc extends PMConfigurationObject {

	private static final long serialVersionUID = -4251710793999219993L;

	@Id
	private String id;

	private String user;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

}
