package com.boot.jx.postman.plugin;

import java.util.Map;

import com.boot.jx.common.impl.ConfigMeta.ConfigMetaProperty;
import com.boot.jx.common.impl.ConfigMeta.INPUT_TYPE;
import com.boot.jx.dict.ContactType;
import com.boot.jx.postman.PMConstants.CHANNEL_TYPE;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.PMEnvironment.AChannelDetails;
import com.boot.jx.postman.plugin.ChannelPluginProvider.DefaultChannelPlugin;
import com.boot.jx.postman.plugin.SMSPlugin.SMSConfigDetails;
import com.boot.utils.ArgUtil;
import com.boot.utils.BashUtil.CurlCommand;
import com.boot.utils.Constants;
import com.fasterxml.jackson.annotation.JsonView;

public class SMSPlugin implements DefaultChannelPlugin<SMSConfigDetails> {

	@Override
	public String getChannelType() {
		return CHANNEL_TYPE.SMS;
	}

	@Override
	public ContactType getContactType() {
		return ContactType.SMS;
	}

	public static class SMSConfigDetails extends AChannelDetails {

		private static final long serialVersionUID = -2397678752642150000L;

		@ConfigMetaProperty(path = "sms.number", title = "My phone number / Header / Sender", defaultValue = "ABCLTD")
		private String number;

		@ConfigMetaProperty(path = "sms.country", title = "SMS Country", defaultValue = "IN", searchable = true,
				filterable = true, inputType = INPUT_TYPE.OPTIONS, optionsSource = "getx:/pub/meta/options/isdcode")
		private String country;

		@ConfigMetaProperty(path = "sms.provider", title = "SMS Provider", defaultValue = "TEXTLOCAL",
				inputType = INPUT_TYPE.OPTIONS, optionsSource = "data:/config/sms_provider")
		private String provider;

		@ConfigMetaProperty(path = "sms.pub", writeonly = false, title = "Identifiers JSON", hidden = true,
				desc = "A json with all public identifiers to be used in api request", inputType = INPUT_TYPE.JSON)
		private Map<String, Object> pub;

		@JsonView(PMEnvironment.ProtectedProperty.class)
		@ConfigMetaProperty(path = "sms.secret", writeonly = false, title = "Secret Keys JSON", hidden = true,
				desc = "A json with all secret keys to be used in api request", inputType = INPUT_TYPE.JSON)
		private Map<String, Object> secret;

		@ConfigMetaProperty(path = "sms.request", pathRaw = "sms.request.bash", writeonly = false,
				title = "Request Object", hidden = false, desc = "Request in curl format", optional = true,
				inputType = INPUT_TYPE.TEXTAREA)
		private CurlCommand request;

		@Override
		public String getLane() {
			
			 String lane = number + "_" + provider + "_" + ArgUtil.parseAsString(country, Constants.BLANK);
			  // Replace SMARTPING with MEHERYPING in the lane name for display purposes only
			  if (lane.contains("SMARTPING")) {
			      lane = lane.replace("SMARTPING", "MEHERYPING");
			  }
			  return lane;
		}

		public String getNumber() {
			return number;
		}

		public void setNumber(String number) {
			this.number = number;
		}

		public String getProvider() {
			return provider;
		}

		public void setProvider(String provider) {
			this.provider = provider;
		}

		public Map<String, Object> getSecret() {
			return secret;
		}

		public void setSecret(Map<String, Object> secret) {
			this.secret = secret;
		}

		public CurlCommand getRequest() {
			return request;
		}

		public void setRequest(CurlCommand request) {
			this.request = request;
		}

		public Map<String, Object> getPub() {
			return pub;
		}

		public void setPub(Map<String, Object> pub) {
			this.pub = pub;
		}

		public String getCountry() {
			return country;
		}

		public void setCountry(String country) {
			this.country = country;
		}

	}

	@Override
	public void setDetails(ChannelConfig config, SMSConfigDetails details) {
		config.setSms(details);
	}

	@Override
	public SMSConfigDetails getDetails(ChannelConfig config) {
		return config.getSms();
	}

	@Override
	public SMSConfigDetails newChannelDetails() {
		return new SMSConfigDetails();
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
