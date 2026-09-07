package com.boot.jx.postman.plugin;

import com.boot.jx.common.impl.ConfigMeta.CONVERT_TYPE;
import com.boot.jx.common.impl.ConfigMeta.ConfigMetaProperty;
import com.boot.jx.common.impl.ConfigMeta.DATA_TYPE;
import com.boot.jx.common.impl.ConfigMeta.INPUT_TYPE;
import com.boot.jx.dict.ContactType;
import com.boot.jx.postman.PMConstants.CHANNEL_TYPE;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.plugin.ChannelPluginProvider.DefaultChannelPlugin;
import com.boot.jx.postman.plugin.FacebookPlugin.MetaMasterConfigDetails;
import com.boot.jx.postman.plugin.InstagramPlugin.InstagramConfig;
import com.boot.utils.ArgUtil;
import com.fasterxml.jackson.annotation.JsonView;

public class InstagramPlugin implements DefaultChannelPlugin<InstagramConfig> {

	@Override
	public ContactType getContactType() {
		return ContactType.INSTAGRAM;
	}

	@Override
	public String getChannelType() {
		return CHANNEL_TYPE.INSTAGRAM;
	}

	@ConfigMetaProperty(context = "instagram")
	public static class InstagramConfig extends MetaMasterConfigDetails {

		private static final long serialVersionUID = -2397678752642150000L;
		@ConfigMetaProperty(path = "pageId", title = "Instagram Id", createonly = true)
		private String pageId;

		@ConfigMetaProperty(path = "fbPageId", title = "Facebook Page Id", createonly = true)
		private String fbPageId;

		@ConfigMetaProperty(path = "handler", title = "Handle")
		private String handler;
		@ConfigMetaProperty(path = "type", title = "Type", hidden = true)
		private String type;

		@ConfigMetaProperty(path = "accessToken", title = "Access Token", writeonly = true)
		@JsonView(PMEnvironment.ProtectedProperty.class)
		private String accessToken;
		@ConfigMetaProperty(path = "verifyToken", title = "Verify Token", writeonly = true)
		@JsonView(PMEnvironment.ProtectedProperty.class)
		private String verifyToken;
		@ConfigMetaProperty(path = "appSecret", title = "App Secret", writeonly = true)
		@JsonView(PMEnvironment.ProtectedProperty.class)
		private String appSecret;

		@ConfigMetaProperty(path = "promptEmail", title = "Prompt Email", inputType = INPUT_TYPE.OPTIONS,
				dataType = DATA_TYPE.SWITCH, converterType = CONVERT_TYPE.BOOLEAN, defaultValue = "false")
		private boolean promptEmail;
		@ConfigMetaProperty(path = "promptPhone", title = "Prompt Phone", inputType = INPUT_TYPE.OPTIONS,
				dataType = DATA_TYPE.SWITCH, converterType = CONVERT_TYPE.BOOLEAN, defaultValue = "false")
		private boolean promptPhone;
		@ConfigMetaProperty(path = "promptName", title = "Prompt Name", inputType = INPUT_TYPE.OPTIONS,
				dataType = DATA_TYPE.SWITCH, converterType = CONVERT_TYPE.BOOLEAN, defaultValue = "false")
		private boolean promptName;

		public boolean isPromptName() {
			return promptName;
		}

		public void setPromptName(boolean promptName) {
			this.promptName = promptName;
		}

		public boolean isPromptEmail() {
			return promptEmail;
		}

		public void setPromptEmail(boolean promptEmail) {
			this.promptEmail = promptEmail;
		}

		public boolean isPromptPhone() {
			return promptPhone;
		}

		public void setPromptPhone(boolean promptPhone) {
			this.promptPhone = promptPhone;
		}

		public String getPageId() {
			return pageId;
		}

		public void setPageId(String pageId) {
			this.pageId = pageId;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getAccessToken() {
			return accessToken;
		}

		public void setAccessToken(String accessToken) {
			this.accessToken = accessToken;
		}

		public String getVerifyToken() {
			return verifyToken;
		}

		public void setVerifyToken(String verifyToken) {
			this.verifyToken = verifyToken;
		}

		public String getAppSecret() {
			return appSecret;
		}

		public void setAppSecret(String appSecret) {
			this.appSecret = appSecret;
		}

		@Override
		public String getLane() {
			return this.pageId;
		}

		public String getHandler() {
			return handler;
		}

		public void setHandler(String handler) {
			this.handler = handler;
		}

		public String getFbPageId() {
			return fbPageId;
		}

		public void setFbPageId(String fbPageId) {
			this.fbPageId = fbPageId;
		}

	}

	@Override
	public String getDefaultName(ChannelConfig config) {
		if (!ArgUtil.is(config.getName())) {
			if (ArgUtil.is(config.getInstagram().getHandler())) {
				return config.getInstagram().getHandler();
			}
			return String.format("IG %s", config.getLane());
		}
		return config.getName();
	}

	@Override
	public InstagramConfig newChannelDetails() {
		return new InstagramConfig();
	}

	@Override
	public void setDetails(ChannelConfig config, InstagramConfig details) {
		config.setInstagram(details);
	}

	@Override
	public InstagramConfig getDetails(ChannelConfig config) {
		return config.getInstagram();
	}

	/*
	 * @Override public void addConfigMeta(List<ConfigMeta> list) { list.add(new
	 * ConfigMeta().path("instagram.pageId").title("Instagram Id").createonly());
	 * list.add(new
	 * ConfigMeta().path("instagram.type").title("Type").optionValues("page").hidden
	 * ()); list.add(new ConfigMeta().path("instagram.handler").title("Handle"));
	 * list.add(new
	 * ConfigMeta().path("instagram.verifyToken").title("Verify Token").writeonly())
	 * ; list.add(new
	 * ConfigMeta().path("instagram.accessToken").title("Access Token").writeonly())
	 * ; list.add(new
	 * ConfigMeta().path("instagram.appSecret").title("App Secret").writeonly()); }
	 * 
	 * @Override public void importChannelDetailsFromMap(InstagramConfig
	 * channelDetails, MapModel map) {
	 * channelDetails.setPageId(map.pathEntry("instagram.pageId").asString(
	 * channelDetails.getPageId()));
	 * channelDetails.setHandler(map.pathEntry("instagram.handler").asString(
	 * channelDetails.getHandler()));
	 * channelDetails.setType(map.pathEntry("instagram.type").asString(
	 * channelDetails.getType()));
	 * channelDetails.setVerifyToken(map.pathEntry("instagram.verifyToken").asString
	 * (channelDetails.getVerifyToken()));
	 * channelDetails.setAccessToken(map.pathEntry("instagram.accessToken").asString
	 * (channelDetails.getAccessToken()));
	 * channelDetails.setAppSecret(map.pathEntry("instagram.appSecret").asString(
	 * channelDetails.getAppSecret())); }
	 */

	@Override
	public boolean isPushAllowed() {
		return false;
	}

	@Override
	public boolean isPushOnlyApproved() {
		return true;
	}

	@Override
	public boolean isPushFreeTextAllowed() {
		return false;
	}

	@Override
	public boolean isPushToNewContactAllowed() {
		return false;
	}

	@Override
	public boolean isWebhookManual() {
		return true;
	}
}
