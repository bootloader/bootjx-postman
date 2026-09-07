package com.boot.jx.postman.doc.config;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.mongo.CommonDocInterfaces.IDocument;
import com.boot.jx.postman.PMEnvironment.PMConfigurationObject;
import com.boot.model.UtilityModels.JsonIgnoreUnknown;

@Document(collection = "CONFIG_FEATURES")
@TypeAlias("FeaturesConfig")
public class FeaturesConfigDoc extends PMConfigurationObject implements IDocument, JsonIgnoreUnknown {

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
