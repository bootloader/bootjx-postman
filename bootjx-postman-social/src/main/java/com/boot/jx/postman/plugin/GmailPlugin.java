package com.boot.jx.postman.plugin;

import com.boot.jx.common.impl.ConfigMeta.ConfigMetaProperty;
import com.boot.jx.dict.ContactType;
import com.boot.jx.postman.PMConstants.CHANNEL_TYPE;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.PMEnvironment.AChannelDetails;
import com.boot.jx.postman.plugin.ChannelPluginProvider.DefaultChannelPlugin;
import com.boot.jx.postman.plugin.GmailPlugin.GmailConfigDetails;
import com.boot.jx.postman.plugin.OutlookPlugin.OutlookMasterConfigDetails;
import com.boot.utils.ArgUtil;
import com.fasterxml.jackson.annotation.JsonView;

public class GmailPlugin implements DefaultChannelPlugin<GmailConfigDetails> {

	@Override
	public String getChannelType() {
		return CHANNEL_TYPE.GMAIL;
	}

	@Override
	public ContactType getContactType() {
		return ContactType.EMAIL;
	}

	public static abstract class GmailMasterConfigDetails extends AChannelDetails {
		private static final long serialVersionUID = -2174315212703629204L;
		@ConfigMetaProperty(path = "masterAppTitle", title = "masterAppTitle", createonly = true, hidden = true,
				readonly = true, desc = "masterAppTitle")
		private String masterAppTitle;

		@ConfigMetaProperty(path = "masterClientId", title = "masterClientId", createonly = true, hidden = true,
				readonly = true, desc = "masterClientId")
		private String masterClientId;

		@ConfigMetaProperty(path = "masterClientSecret", title = "masterClientSecret", createonly = true, hidden = true,
				readonly = true, desc = "masterClientSecret")
		@JsonView(PMEnvironment.ProtectedProperty.class)
		private String masterClientSecret;

		@ConfigMetaProperty(path = "masterTenantId", title = "masterTenantId", createonly = true, hidden = true,
				readonly = true, desc = "masterTenantId")
		private String masterTenantId;

		public String getMasterAppTitle() {
			return masterAppTitle;
		}

		public void setMasterAppTitle(String masterAppTitle) {
			this.masterAppTitle = masterAppTitle;
		}

		public String getMasterClientId() {
			return masterClientId;
		}

		public void setMasterClientId(String masterClientId) {
			this.masterClientId = masterClientId;
		}

		public String getMasterClientSecret() {
			return masterClientSecret;
		}

		public void setMasterClientSecret(String masterClientSecret) {
			this.masterClientSecret = masterClientSecret;
		}

		public String getMasterTenantId() {
			return masterTenantId;
		}

		public void setMasterTenantId(String masterTenantId) {
			this.masterTenantId = masterTenantId;
		}

	}

	@ConfigMetaProperty(context = "gmail")
	public static class GmailConfigDetails extends OutlookMasterConfigDetails {

		private static final long serialVersionUID = -2397678752642150000L;
		@ConfigMetaProperty(path = "email", title = "Email")
		private String email;

		@ConfigMetaProperty(path = "accessToken", title = "Access Token", writeonly = true)
		@JsonView(PMEnvironment.ProtectedProperty.class)
		private String accessToken;
		@ConfigMetaProperty(path = "refreshToken", title = "Refresh Token", writeonly = true)
		@JsonView(PMEnvironment.ProtectedProperty.class)
		private String refreshToken;

		public String getAccessToken() {
			return accessToken;
		}

		public void setAccessToken(String accessToken) {
			this.accessToken = accessToken;
		}

		@Override
		public String getLane() {
			return this.email;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public String getRefreshToken() {
			return refreshToken;
		}

		public void setRefreshToken(String refreshToken) {
			this.refreshToken = refreshToken;
		}

	}

	@Override
	public String getDefaultName(ChannelConfig config) {
		if (!ArgUtil.is(config.getName())) {
			if (ArgUtil.is(config.getOutlook().getEmail())) {
				return config.getOutlook().getEmail();
			}
			return String.format("Gmail %s", config.getLane());
		}
		return config.getName();
	}

	@Override
	public GmailConfigDetails newChannelDetails() {
		return new GmailConfigDetails();
	}

	@Override
	public void setDetails(ChannelConfig config, GmailConfigDetails details) {
		config.setGmail(details);
	}

	@Override
	public GmailConfigDetails getDetails(ChannelConfig config) {
		return config.getGmail();
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
		return true;
	}

}
