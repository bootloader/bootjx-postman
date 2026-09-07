package com.boot.jx.postman.doc.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.mongo.CommonDocInterfaces.SimpleDocument;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.PMEnvironment.PMConfigurationObject;
import com.boot.utils.ArgUtil;
import com.fasterxml.jackson.annotation.JsonView;

@Document(collection = "CONFIG_VARS")
@TypeAlias("VarsConfigDoc")
public class VarsConfigDoc extends PMConfigurationObject implements SimpleDocument {

	private static final long serialVersionUID = -4251710793999219993L;

	@Id
	private String id;

	private String group;

	private String type;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getGroup() {
		return ArgUtil.nonEmpty(this.group, "default");
	}

	public void setGroup(String group) {
		this.group = group;
	}

	@Document(collection = "CONFIG_COMP_VARS")
	@TypeAlias("CompanyVarsConfig")
	public static class CompanyVarsConfigDoc extends VarsConfigDoc {
		private static final long serialVersionUID = -782517850590239631L;

	}

	@Document(collection = "CONFIG_TOKEN_KEYS")
	@TypeAlias("CompanyTokenKey")
	public static class CompanyTokenKeyDoc extends VarsConfigDoc {
		private static final long serialVersionUID = -4251710793999219993L;

		@JsonView(PMEnvironment.ProtectedProperty.class)
		private Map<String, Object> secret;

		public Map<String, Object> getSecret() {
			return secret;
		}

		public void setSecret(Map<String, Object> secret) {
			this.secret = secret;
		}

		public Map<String, Object> secret() {
			if (this.secret == null) {
				this.secret = new HashMap<String, Object>();
			}
			return secret;
		}
	}
}
