package com.boot.jx.postman.plugin;

import java.util.List;
import java.util.Map;

import com.boot.jx.common.impl.ConfigMeta.ConfigMetaProperty;
import com.boot.jx.common.impl.ConfigMeta.INPUT_TYPE;
import com.boot.jx.dict.ContactType;
import com.boot.jx.postman.PMConstants.CHANNEL_TYPE;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.PMEnvironment.AChannelDetails;
import com.boot.jx.postman.plugin.ChannelPluginProvider.DefaultChannelPlugin;
import com.boot.jx.postman.plugin.PushAppPlugin.PushAppConfigDetails;
import com.fasterxml.jackson.annotation.JsonView;

public class PushAppPlugin implements DefaultChannelPlugin<PushAppConfigDetails> {

	@Override
	public String getChannelType() {
		return CHANNEL_TYPE.PUSHAPP;
	}

	@Override
	public ContactType getContactType() {
		return ContactType.PUSH;
	}

	@ConfigMetaProperty(context = "pushapp")
	public static class PushAppConfigDetails extends AChannelDetails {

		private static final long serialVersionUID = -2397678752642150000L;

		@ConfigMetaProperty(path = "clientId", title = "Client ID", defaultValue = "wecare")
		private String clientId;

		@ConfigMetaProperty(path = "orgId", title = "Organization Id", defaultValue = "638906d42fc1536f19eb5901",
				inputType = INPUT_TYPE.TEXT)
		private String orgId;

		@ConfigMetaProperty(path = "appId", title = "APP Id", desc = "Enter Your PushApp Id")
		private String appId;

		@ConfigMetaProperty(path = "appKey", title = "APP Key", writeonly = true, desc = "Enter Your PushApp Key")
		@JsonView(PMEnvironment.ProtectedProperty.class)
		private String appKey;

		private List<Map<String, Object>> platforms;

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

		public String getAppId() {
			return appId;
		}

		public void setAppId(String appId) {
			this.appId = appId;
		}

		public String getAppKey() {
			return appKey;
		}

		public void setAppKey(String appKey) {
			this.appKey = appKey;
		}

		public List<Map<String, Object>> getPlatforms() {
			return platforms;
		}

		public void setPlatforms(List<Map<String, Object>> platforms) {
			this.platforms = platforms;
		}
	}

	@Override
	public void setDetails(ChannelConfig config, PushAppConfigDetails details) {
		config.setPushapp(details);
	}

	@Override
	public PushAppConfigDetails getDetails(ChannelConfig config) {
		return config.getPushapp();
	}

	@Override
	public PushAppConfigDetails newChannelDetails() {
		return new PushAppConfigDetails();
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

	@Override
	public boolean isPushLocalStore() {
		return false;
	}

}
