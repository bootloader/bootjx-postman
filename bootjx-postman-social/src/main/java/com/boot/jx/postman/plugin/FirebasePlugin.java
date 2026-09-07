package com.boot.jx.postman.plugin;

import com.boot.jx.common.impl.ConfigMeta.ConfigMetaProperty;
import com.boot.jx.dict.ContactType;
import com.boot.jx.postman.PMConstants.CHANNEL_TYPE;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.PMEnvironment.AChannelDetails;
import com.boot.jx.postman.plugin.ChannelPluginProvider.DefaultChannelPlugin;
import com.boot.jx.postman.plugin.FirebasePlugin.FirebaseConfigDetails;
import com.fasterxml.jackson.annotation.JsonView;

public class FirebasePlugin implements DefaultChannelPlugin<FirebaseConfigDetails> {

	@Override
	public ContactType getContactType() {
		return ContactType.PUSH;
	}

	@Override
	public String getChannelType() {
		return CHANNEL_TYPE.FIREBASE;
	}

	public static final class FirebaseConfigDetails extends AChannelDetails {

		private static final long serialVersionUID = -1204213453486344023L;

		@ConfigMetaProperty(path = "firebase.appName", title = "App Name", createonly = true,
				desc = "App Name")
		private String appName;

		@ConfigMetaProperty(path = "firebase.serverKey", title = "API Key", writeonly = true,
				desc = "Enter Server WABA Key")
		@JsonView(PMEnvironment.ProtectedProperty.class)
		private String serverKey;

		@Override
		public String getLane() {
			return this.appName;
		}

		public String getServerKey() {
			return serverKey;
		}

		public void setServerKey(String serverKey) {
			this.serverKey = serverKey;
		}

		public String getAppName() {
			return appName;
		}

		public void setAppName(String appName) {
			this.appName = appName;
		}

	}

	@Override
	public void setDetails(ChannelConfig config, FirebaseConfigDetails details) {
		config.setFirebase(details);
	}

	@Override
	public FirebaseConfigDetails getDetails(ChannelConfig config) {
		return config.getFirebase();
	}

	@Override
	public FirebaseConfigDetails newChannelDetails() {
		return new FirebaseConfigDetails();
	}

	@Override
	public boolean isPushAllowed() {
		return false;
	}

	@Override
	public boolean isPushOnlyApproved() {
		return false;
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
