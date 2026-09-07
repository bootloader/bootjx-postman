package com.boot.jx.postman.plugin;

import com.boot.jx.common.impl.ConfigMeta.ConfigMetaProperty;
import com.boot.jx.common.impl.ConfigMeta.INPUT_TYPE;
import com.boot.jx.dict.ContactType;
import com.boot.jx.postman.PMConstants.CHANNEL_TYPE;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.PMEnvironment.AChannelDetails;
import com.boot.jx.postman.plugin.ChannelPluginProvider.DefaultChannelPlugin;
import com.boot.jx.postman.plugin.OAPlugin.OAConfigDetails;
import com.fasterxml.jackson.annotation.JsonView;

public class OAPlugin implements DefaultChannelPlugin<OAConfigDetails> {

	@Override
	public String getChannelType() {
		return CHANNEL_TYPE.OA;
	}

	@Override
	public ContactType getContactType() {
		return ContactType.OA;
	}

	@ConfigMetaProperty(context = "oa")
	public static class OAConfigDetails extends AChannelDetails {

		private static final long serialVersionUID = -2397678752642150000L;

		@ConfigMetaProperty(path = "clientId", title = "Client ID", defaultValue = "wecare")
		private String clientId;

		@ConfigMetaProperty(path = "orgId", title = "Organization Id", defaultValue = "638906d42fc1536f19eb5901",
				inputType = INPUT_TYPE.TEXT)
		private String orgId;

		@ConfigMetaProperty(path = "apiId", title = "API Id", desc = "Enter Your OA API Id")
		private String apiId;

		@ConfigMetaProperty(path = "apiKey", title = "API Key", writeonly = true, desc = "Enter Your OA API Key")
		@JsonView(PMEnvironment.ProtectedProperty.class)
		private String apiKey;

		@Override
		public String getLane() {
			return clientId;
		}

		public String getClientId() {
			return clientId;
		}

		public void setClientId(String clientId) {
			this.clientId = clientId;
		}

		public String getOrgId() {
			return orgId;
		}

		public void setOrgId(String orgId) {
			this.orgId = orgId;
		}

		public String getApiId() {
			return apiId;
		}

		public void setApiId(String apiId) {
			this.apiId = apiId;
		}

		public String getApiKey() {
			return apiKey;
		}

		public void setApiKey(String apiKey) {
			this.apiKey = apiKey;
		}

	}

	@Override
	public void setDetails(ChannelConfig config, OAConfigDetails details) {
		config.setOa(details);
	}

	@Override
	public OAConfigDetails getDetails(ChannelConfig config) {
		return config.getOa();
	}

	@Override
	public OAConfigDetails newChannelDetails() {
		return new OAConfigDetails();
	}

	@Override
	public boolean isPushAllowed() {
		return true;
	}

	@Override
	public boolean isPushOnlyApproved() {
		return true;
	}

	@Override
	public boolean isPushFreeTextAllowed() {
		return true;
	}

	@Override
	public boolean isPushToNewContactAllowed() {
		return true;
	}

	@Override
	public boolean isWebhookManual() {
		return false;
	}

}
