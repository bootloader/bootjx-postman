package com.boot.jx.postman.plugin;

import com.boot.jx.common.impl.ConfigMeta.CONVERT_TYPE;
import com.boot.jx.common.impl.ConfigMeta.ConfigMetaProperty;
import com.boot.jx.common.impl.ConfigMeta.DATA_TYPE;
import com.boot.jx.common.impl.ConfigMeta.INPUT_TYPE;
import com.boot.jx.dict.ContactType;
import com.boot.jx.postman.PMConstants.CHANNEL_TYPE;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.PMEnvironment.AChannelDetails;
import com.boot.jx.postman.plugin.ChannelPluginProvider.DefaultChannelPlugin;
import com.boot.jx.postman.plugin.WA360Plugin.WA360ConfigDetails;
import com.fasterxml.jackson.annotation.JsonView;

public class WA360Plugin implements DefaultChannelPlugin<WA360ConfigDetails> {

	@Override
	public String getChannelType() {
		return CHANNEL_TYPE.WA_360D;
	}

	@Override
	public ContactType getContactType() {
		return ContactType.WHATSAPP;
	}

	public static class WA360ConfigDetails extends AChannelDetails {

		private static final long serialVersionUID = -2397678752642150000L;

		@ConfigMetaProperty(path = "wa360d.number", title = "Number", createonly = true,
				desc = "Eneter WABA number with country code")
		private String number;

		@ConfigMetaProperty(path = "wa360d.apiKey", title = "API Key", writeonly = true, desc = "Enter Your WABA Key")
		@JsonView(PMEnvironment.ProtectedProperty.class)
		private String apiKey;
		
		@ConfigMetaProperty(path = "wa360d.promptEmail", title = "Prompt Email", inputType = INPUT_TYPE.OPTIONS,
				dataType = DATA_TYPE.SWITCH, converterType = CONVERT_TYPE.BOOLEAN, defaultValue = "true")
		private boolean promptEmail;
		

		@ConfigMetaProperty(path = "wa360d.promptPhone", title = "Prompt Phone", inputType = INPUT_TYPE.OPTIONS,
				dataType = DATA_TYPE.SWITCH, converterType = CONVERT_TYPE.BOOLEAN, defaultValue = "true")
		private boolean promptPhone;
		
		@ConfigMetaProperty(path = "wa360d.promptName", title = "Prompt Name", inputType = INPUT_TYPE.OPTIONS,
				dataType = DATA_TYPE.SWITCH, converterType = CONVERT_TYPE.BOOLEAN, defaultValue = "true")
		private boolean promptName;
		

		@Override
		public String getLane() {
			return this.number;
		}

		public String getNumber() {
			return number;
		}

		public void setNumber(String number) {
			this.number = number;
		}

		public String getApiKey() {
			return apiKey;
		}

		public void setApiKey(String apiKey) {
			this.apiKey = apiKey;
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

		public boolean isPromptName() {
			return promptName;
		}

		public void setPromptName(boolean promptName) {
			this.promptName = promptName;
		}
	}

	@Override
	public void setDetails(ChannelConfig config, WA360ConfigDetails details) {
		config.setWa360d(details);
	}

	@Override
	public WA360ConfigDetails getDetails(ChannelConfig config) {
		return config.getWa360d();
	}

	@Override
	public WA360ConfigDetails newChannelDetails() {
		return new WA360ConfigDetails();
	}

//	@Override
//	public void addConfigMeta(List<ConfigMeta> configMetaList) {
//		configMetaList.add(new ConfigMeta().path("wa360d.number").title("Number").createonly());
//		configMetaList.add(new ConfigMeta().path("wa360d.apiKey").title("API Key").writeonly());
//	}

//	@Override
//	public void importChannelDetailsFromMap(WA360ConfigDetails channelDetails, MapModel map) {
//		channelDetails.setNumber(map.pathEntry("wa360d.number").asString(channelDetails.getNumber()));
//		channelDetails.setApiKey(map.pathEntry("wa360d.apiKey").asString(channelDetails.getApiKey()));
//	}

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
		return false;
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
