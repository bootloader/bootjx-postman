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
import com.boot.jx.postman.plugin.WA360CloudPlugin.WA360CloudConfigDetails;
import com.fasterxml.jackson.annotation.JsonView;

public class WA360CloudPlugin implements DefaultChannelPlugin<WA360CloudConfigDetails> {

	@Override
	public String getChannelType() {
		return CHANNEL_TYPE.WA_360DC;
	}

	@Override
	public ContactType getContactType() {
		return ContactType.WHATSAPP;
	}

	public static class WA360CloudConfigDetails extends AChannelDetails {

		private static final long serialVersionUID = -2397678752642150000L;

		@ConfigMetaProperty(path = "wa360dc.number", title = "Number", createonly = true,
				desc = "Eneter WABA number with country code")
		private String number;

		@ConfigMetaProperty(path = "wa360dc.apiKey", title = "API Key", writeonly = true,
				desc = "Enter Your WABA cloud Key")
		@JsonView(PMEnvironment.ProtectedProperty.class)
		private String apiKey;
		@ConfigMetaProperty(path = "wa360dc.promptEmail", title = "Prompt Email", inputType = INPUT_TYPE.OPTIONS,
				dataType = DATA_TYPE.SWITCH, converterType = CONVERT_TYPE.BOOLEAN, defaultValue = "true")
		private boolean promptEmail;

		@ConfigMetaProperty(path = "wa360dc.promptPhone", title = "Prompt Phone", inputType = INPUT_TYPE.OPTIONS,
				dataType = DATA_TYPE.SWITCH, converterType = CONVERT_TYPE.BOOLEAN, defaultValue = "true")
		private boolean promptPhone;

		@ConfigMetaProperty(path = "wa360dc.promptName", title = "Prompt Name", inputType = INPUT_TYPE.OPTIONS,
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
	public void setDetails(ChannelConfig config, WA360CloudConfigDetails details) {
		config.setWa360dc(details);
	}

	@Override
	public WA360CloudConfigDetails getDetails(ChannelConfig config) {
		return config.getWa360dc();
	}

	@Override
	public WA360CloudConfigDetails newChannelDetails() {
		return new WA360CloudConfigDetails();
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
