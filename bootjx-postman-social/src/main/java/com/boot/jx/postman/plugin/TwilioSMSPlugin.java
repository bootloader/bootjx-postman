package com.boot.jx.postman.plugin;

import com.boot.jx.common.impl.ConfigMeta.ConfigMetaProperty;
import com.boot.jx.dict.ContactType;
import com.boot.jx.postman.PMConstants.CHANNEL_TYPE;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.PMEnvironment.AChannelDetails;
import com.boot.jx.postman.plugin.ChannelPluginProvider.DefaultChannelPlugin;
import com.boot.jx.postman.plugin.TwilioSMSPlugin.TwilioConfigDetails;
import com.fasterxml.jackson.annotation.JsonView;

public class TwilioSMSPlugin implements DefaultChannelPlugin<TwilioConfigDetails> {

	@Override
	public String getChannelType() {
		return CHANNEL_TYPE.SMS_TWILIO;
	}

	@Override
	public ContactType getContactType() {
		return ContactType.SMS;
	}

	public static class TwilioConfigDetails extends AChannelDetails {

		private static final long serialVersionUID = -2397678752642150000L;

		@ConfigMetaProperty(path = "twilio.sid", title = "Account SID")
		private String sid;

		@JsonView(PMEnvironment.ProtectedProperty.class)
		@ConfigMetaProperty(path = "twilio.token", writeonly = false, title = "Auth Token")
		private String token;

		@ConfigMetaProperty(path = "twilio.number", title = "My Twilio phone number", defaultValue = "995")
		private String number;

		@Override
		public String getLane() {
			return number;
		}

		public String getSid() {
			return sid;
		}

		public void setSid(String sid) {
			this.sid = sid;
		}

		public String getToken() {
			return token;
		}

		public void setToken(String token) {
			this.token = token;
		}

		public String getNumber() {
			return number;
		}

		public void setNumber(String number) {
			this.number = number;
		}

	}

	@Override
	public void setDetails(ChannelConfig config, TwilioConfigDetails details) {
		config.setTwilio(details);
	}

	@Override
	public TwilioConfigDetails getDetails(ChannelConfig config) {
		return config.getTwilio();
	}

	@Override
	public TwilioConfigDetails newChannelDetails() {
		return new TwilioConfigDetails();
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
