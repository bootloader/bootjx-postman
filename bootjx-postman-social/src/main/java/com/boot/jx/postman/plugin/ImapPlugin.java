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
import com.boot.jx.postman.plugin.ImapPlugin.ImapConfigDetails;
import com.fasterxml.jackson.annotation.JsonView;

public class ImapPlugin implements DefaultChannelPlugin<ImapConfigDetails> {

	@Override
	public String getChannelType() {
		return CHANNEL_TYPE.IMAP;
	}

	@Override
	public ContactType getContactType() {
		return ContactType.EMAIL;
	}

	public static class ImapConfigDetails extends AChannelDetails {

		private static final long serialVersionUID = -2397678752642150000L;
		private String protocol;

		@ConfigMetaProperty(path = "imap.imapHost", title = "IMAP Host")
		private String imapHost;

		@ConfigMetaProperty(path = "imap.imapPort", title = "IMAP Port", defaultValue = "993",
				optionValues = { "993", "143" })
		private String imapPort;

		@ConfigMetaProperty(path = "imap.imapUser", createonly = true, title = "IMAP User")
		private String imapUser;

		@JsonView(PMEnvironment.ProtectedProperty.class)
		@ConfigMetaProperty(path = "imap.imapPass", writeonly = false, title = "IMAP Password")
		private String imapPass;

		@ConfigMetaProperty(path = "imap.imapStartTls", title = "Enable IMAP TLS", inputType = INPUT_TYPE.OPTIONS,
				dataType = DATA_TYPE.SWITCH, converterType = CONVERT_TYPE.BOOLEAN)
		private boolean imapStartTls;

		@ConfigMetaProperty(path = "imap.smtpAuth", title = "SMTP Auth", inputType = INPUT_TYPE.OPTIONS,
				dataType = DATA_TYPE.SWITCH, converterType = CONVERT_TYPE.BOOLEAN)
		private boolean smtpAuth;

		@ConfigMetaProperty(path = "imap.smtpStartTls", title = "SMTP TLS", inputType = INPUT_TYPE.OPTIONS,
				dataType = DATA_TYPE.SWITCH, converterType = CONVERT_TYPE.BOOLEAN)
		private boolean smtpStartTls;

		@ConfigMetaProperty(path = "imap.smtpHost", title = "SMTP Host")
		private String smtpHost;

		@ConfigMetaProperty(path = "imap.smtpPort", title = "SMTP Port", defaultValue = "465",
				optionValues = { "465", "587", "25" })
		private String smtpPort;

		@ConfigMetaProperty(path = "imap.smtpUser", title = "SMTP User")
		private String smtpUser;

		@JsonView(PMEnvironment.ProtectedProperty.class)
		@ConfigMetaProperty(path = "imap.smtpPass", title = "SMTP Password")
		private String smtpPass;

		@Override
		public String getLane() {
			return this.imapUser;
		}

		public String getProtocol() {
			return protocol;
		}

		public void setProtocol(String protocol) {
			this.protocol = protocol;
		}

		public boolean isSmtpAuth() {
			return smtpAuth;
		}

		public void setSmtpAuth(boolean smtpAuth) {
			this.smtpAuth = smtpAuth;
		}

		public boolean isSmtpStartTls() {
			return smtpStartTls;
		}

		public void setSmtpStartTls(boolean smtpStartTls) {
			this.smtpStartTls = smtpStartTls;
		}

		public String getSmtpHost() {
			return smtpHost;
		}

		public void setSmtpHost(String smtpHost) {
			this.smtpHost = smtpHost;
		}

		public String getSmtpPort() {
			return smtpPort;
		}

		public void setSmtpPort(String smtpPort) {
			this.smtpPort = smtpPort;
		}

		public String getSmtpUser() {
			return smtpUser;
		}

		public void setSmtpUser(String smtpUser) {
			this.smtpUser = smtpUser;
		}

		public String getSmtpPass() {
			return smtpPass;
		}

		public void setSmtpPass(String smtpPass) {
			this.smtpPass = smtpPass;
		}

		public String getImapHost() {
			return imapHost;
		}

		public void setImapHost(String imapHost) {
			this.imapHost = imapHost;
		}

		public String getImapPort() {
			return imapPort;
		}

		public void setImapPort(String imapPort) {
			this.imapPort = imapPort;
		}

		public String getImapUser() {
			return imapUser;
		}

		public void setImapUser(String imapUser) {
			this.imapUser = imapUser;
		}

		public String getImapPass() {
			return imapPass;
		}

		public void setImapPass(String imapPass) {
			this.imapPass = imapPass;
		}

		public boolean isImapStartTls() {
			return imapStartTls;
		}

		public void setImapStartTls(boolean imapStartTls) {
			this.imapStartTls = imapStartTls;
		}

	}

	@Override
	public void setDetails(ChannelConfig config, ImapConfigDetails details) {
		config.setImap(details);
	}

	@Override
	public ImapConfigDetails getDetails(ChannelConfig config) {
		return config.getImap();
	}

	@Override
	public ImapConfigDetails newChannelDetails() {
		return new ImapConfigDetails();
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
