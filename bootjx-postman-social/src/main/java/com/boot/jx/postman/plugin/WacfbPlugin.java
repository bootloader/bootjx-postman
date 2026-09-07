package com.boot.jx.postman.plugin;





import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.boot.jx.common.impl.ConfigMeta.CONVERT_TYPE;
import com.boot.jx.common.impl.ConfigMeta.ConfigMetaProperty;
import com.boot.jx.common.impl.ConfigMeta.DATA_TYPE;
import com.boot.jx.common.impl.ConfigMeta.INPUT_TYPE;
import com.boot.jx.dict.ContactType;
import com.boot.jx.postman.PMConstants.CHANNEL_TYPE;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.plugin.ChannelPluginProvider.DefaultChannelPlugin;
import com.boot.jx.postman.plugin.FacebookPlugin.MetaMasterConfigDetails;
import com.boot.utils.ArgUtil;
import com.fasterxml.jackson.annotation.JsonView;



public class WacfbPlugin implements DefaultChannelPlugin<com.boot.jx.postman.plugin.WacfbPlugin.WACFBConfigDetails> {
	private static final Logger LOGGER = LoggerFactory.getLogger(WacfbPlugin.class);
	
	@Override
	public String getChannelType() {
		return CHANNEL_TYPE.WACFB;
	}

	@Override
	public ContactType getContactType() {
		return ContactType.WHATSAPP;
	}

	@ConfigMetaProperty(context = "wacfb")
	public static class WACFBConfigDetails extends MetaMasterConfigDetails {
		
		
		private static final long serialVersionUID = 5956062194604118502L;

		@ConfigMetaProperty(path = "number", title = "Phone Number", createonly = true,
				desc = "Enter WABA number with country code")
		private String number;

		@ConfigMetaProperty(path = "accessToken", title = "Access Token", writeonly = true,
				desc = "Enter Your Access Token")
		@JsonView(PMEnvironment.ProtectedProperty.class)
		private String accessToken;

		@ConfigMetaProperty(path = "phoneNumberId", title = "Phone Number Id", desc = "Enter Your Phone Number Id")
		private String phoneNumberId;

		@ConfigMetaProperty(path = "verificationPin", title = "Verification Pin", writeonly = true, optional = true,
				desc = "Enter Your Phone Number Id")
		@JsonView(PMEnvironment.ProtectedProperty.class)
		private String verificationPin;

		@ConfigMetaProperty(path = "verifyToken", title = "Verification Token", writeonly = true, optional = true,
				desc = "Enter Your Phone Number Id")
		@JsonView(PMEnvironment.ProtectedProperty.class)
		private String verifyToken;

		@ConfigMetaProperty(path = "wabaId", title = "WaBa Id", desc = "Enter Your WaBa Id", optional = true)
		private String wabaId;

		@ConfigMetaProperty(path = "bmId", title = "BM Id", desc = "Enter Your Business Manager Id", optional = true)
		private String bmId;

		@ConfigMetaProperty(path = "mmlite", title = "Mmlite", desc = "mmlite", optional = true, hidden = true)
		private boolean mmlite;

		public boolean isMmlite() {
			return mmlite;
		}

		public void setMmlite(boolean mmlite) {
			this.mmlite = mmlite;
		}
		
		@ConfigMetaProperty(path = "configcall", title = "Configcall", desc = "configcall", optional = true, hidden = true)
		private boolean configcall;

		public boolean isConfigcall() {
			return configcall;
		}

		public void setConfigcall(boolean configcall) {
			this.configcall = configcall;
		}

		@ConfigMetaProperty(path = "promptEmail", title = "Prompt Email", inputType = INPUT_TYPE.OPTIONS,
				dataType = DATA_TYPE.SWITCH, converterType = CONVERT_TYPE.BOOLEAN, defaultValue = "true")
		private boolean promptEmail;

		@ConfigMetaProperty(path = "promptPhone", title = "Prompt Phone", inputType = INPUT_TYPE.OPTIONS,
				dataType = DATA_TYPE.SWITCH, converterType = CONVERT_TYPE.BOOLEAN, defaultValue = "true")
		private boolean promptPhone;

		@ConfigMetaProperty(path = "promptName", title = "Prompt Name", inputType = INPUT_TYPE.OPTIONS,
				dataType = DATA_TYPE.SWITCH, converterType = CONVERT_TYPE.BOOLEAN, defaultValue = "true")
		private boolean promptName;

		@Override
		public String getLane() {
			return number;
		}

		@Override
		public String getSublane() {
			return phoneNumberId;
		}

		public String getNumber() {
			return number;
		}

		public void setNumber(String phoneNumber) {
			this.number = phoneNumber;
		}

		public String getAccessToken() {
			return accessToken;
		}

		public void setAccessToken(String acessToken) {
			this.accessToken = acessToken;
		}

		public String getPhoneNumberId() {
			return phoneNumberId;
		}

		public void setPhoneNumberId(String phoneNumberId) {
			this.phoneNumberId = phoneNumberId;
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

		public String getWabaId() {
			return wabaId;
		}

		public void setWabaId(String wabaId) {
			this.wabaId = wabaId;
		}

		public String getVerificationPin() {
			return verificationPin;
		}

		public void setVerificationPin(String verificationPin) {
			this.verificationPin = verificationPin;
		}

		public String getVerifyToken() {
			return verifyToken;
		}

		public void setVerifyToken(String verifyToken) {
			this.verifyToken = verifyToken;
		}

		public String getBmId() {
			return bmId;
		}

		public void setBmId(String bmId) {
			this.bmId = bmId;
		}

	}

	@Override
	public WACFBConfigDetails newChannelDetails() {
		return new WACFBConfigDetails();
	}

	@Override
	public void setDetails(ChannelConfig config, WACFBConfigDetails details) {
		config.setWacfb(details);
	}

	@Override
	public WACFBConfigDetails getDetails(ChannelConfig config) {
		return config.getWacfb();
	}
	
//	/**
//	 * Returns WABA ID for storing in master collection (laneParam2).
//	 * Used for routing template status webhooks which contain WABA ID but not phone number.
//	 * 
//	 * Called from ConfigStore.saveMaster() when saving channel to master DB.
//	 * 
//	 * @param config ChannelConfig with WACFB details
//	 * @return wabaId or null
//	 */
//	public String getLaneParam2(ChannelConfig config) {
//		if (config == null || !CHANNEL_TYPE.WACFB.equalsIgnoreCase(config.getChannelType())) {
//			return null;
//		}
//		
//		WACFBConfigDetails details = getDetails(config);
//		if (details == null || !ArgUtil.is(details.getWabaId())) {
//			return null;
//		}
//		
//		return details.getWabaId();
//	}
	
	/**
	 * Returns WABA ID for storing in master collection (laneParam2).
	 * Used for routing template status webhooks which contain WABA ID but not phone number.
	 * 
	 * Called from ConfigStore.saveMaster() when saving channel to master DB.
	 * 
	 * @param config ChannelConfig with WACFB details
	 * @return wabaId or null
	 */
	public String getLaneParam2(ChannelConfig config) {
		
		if (config == null || !CHANNEL_TYPE.WACFB.equalsIgnoreCase(config.getChannelType())) {
			return null;
		}
		
		WACFBConfigDetails details = getDetails(config);
		
		if (details == null) {
			LOGGER.warn("getLaneParam2 - details is null!");
			return null;
		}
		
		String wabaId = details.getWabaId();
		
		if (wabaId == null || wabaId.trim().isEmpty()) {
			LOGGER.warn("getLaneParam2 - wabaId is null or empty!");
			return null;
		}
		
		LOGGER.debug("getLaneParam2 - returning wabaId: {}", wabaId);
		return wabaId;
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
