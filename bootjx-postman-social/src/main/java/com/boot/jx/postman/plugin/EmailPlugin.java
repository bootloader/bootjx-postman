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
import com.boot.jx.postman.plugin.EmailPlugin.EmailConfigDetails;
import com.fasterxml.jackson.annotation.JsonView;

public class EmailPlugin implements DefaultChannelPlugin<EmailConfigDetails> {

	@Override
	public String getChannelType() {
		return CHANNEL_TYPE.EMAIL;
	}

	@Override
	public ContactType getContactType() {
		return ContactType.EMAIL;
	}

	public static class EmailConfigDetails extends AChannelDetails {

		private static final long serialVersionUID = -2397678752642150000L;
		private String protocol;

		@ConfigMetaProperty(path = "email.pop3Host", title = "POP3 Host")
		private String pop3Host;

		@ConfigMetaProperty(path = "email.pop3Port", title = "POP3 Port", defaultValue = "995",
				optionValues = { "995", "110" })
		private String pop3Port;

		@ConfigMetaProperty(path = "email.pop3User", createonly = true, title = "POP3 User")
		private String pop3User;

		@JsonView(PMEnvironment.ProtectedProperty.class)
		@ConfigMetaProperty(path = "email.pop3Pass", writeonly = false, title = "POP3 Password")
		private String pop3Pass;

		@ConfigMetaProperty(path = "email.pop3StartTls", title = "Enable POP3 TLS", inputType = INPUT_TYPE.OPTIONS,
				dataType = DATA_TYPE.SWITCH, converterType = CONVERT_TYPE.BOOLEAN)
		private boolean pop3StartTls;

		@ConfigMetaProperty(path = "email.smtpAuth", title = "SMTP Auth", inputType = INPUT_TYPE.OPTIONS,
				dataType = DATA_TYPE.SWITCH, converterType = CONVERT_TYPE.BOOLEAN)
		private boolean smtpAuth;

		@ConfigMetaProperty(path = "email.smtpStartTls", title = "SMTP TLS", inputType = INPUT_TYPE.OPTIONS,
				dataType = DATA_TYPE.SWITCH, converterType = CONVERT_TYPE.BOOLEAN)
		private boolean smtpStartTls;

		@ConfigMetaProperty(path = "email.smtpHost", title = "SMTP Host")
		private String smtpHost;

		@ConfigMetaProperty(path = "email.smtpPort", title = "SMTP Port", defaultValue = "465",
				optionValues = { "465", "587", "25" })
		private String smtpPort;

		@ConfigMetaProperty(path = "email.smtpUser", title = "SMTP User")
		private String smtpUser;

		@JsonView(PMEnvironment.ProtectedProperty.class)
		@ConfigMetaProperty(path = "email.smtpPass", title = "SMTP Password")
		private String smtpPass;

		@Override
		public String getLane() {
			return this.pop3User;
		}

		public String getProtocol() {
			return protocol;
		}

		public void setProtocol(String protocol) {
			this.protocol = protocol;
		}

		public String getPop3Host() {
			return pop3Host;
		}

		public void setPop3Host(String pop3Host) {
			this.pop3Host = pop3Host;
		}

		public boolean isPop3StartTls() {
			return pop3StartTls;
		}

		public void setPop3StartTls(boolean pop3StartTls) {
			this.pop3StartTls = pop3StartTls;
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

		public String getPop3User() {
			return pop3User;
		}

		public void setPop3User(String pop3User) {
			this.pop3User = pop3User;
		}

		public String getPop3Pass() {
			return pop3Pass;
		}

		public void setPop3Pass(String pop3Pass) {
			this.pop3Pass = pop3Pass;
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

		public String getPop3Port() {
			return pop3Port;
		}

		public void setPop3Port(String pop3Port) {
			this.pop3Port = pop3Port;
		}

	}

	@Override
	public void setDetails(ChannelConfig config, EmailConfigDetails details) {
		config.setEmail(details);
	}

	@Override
	public EmailConfigDetails getDetails(ChannelConfig config) {
		return config.getEmail();
	}

	@Override
	public EmailConfigDetails newChannelDetails() {
		return new EmailConfigDetails();
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
