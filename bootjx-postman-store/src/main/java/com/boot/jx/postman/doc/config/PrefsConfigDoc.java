package com.boot.jx.postman.doc.config;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.postman.PMEnvironment.PMConfigurationObject;

@Document(collection = "CONFIG_PREFS")
@TypeAlias("PrefsConfig")
public class PrefsConfigDoc extends PMConfigurationObject {

	private static final long serialVersionUID = -4251710793999219993L;

	@Id
	private String id;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

}
