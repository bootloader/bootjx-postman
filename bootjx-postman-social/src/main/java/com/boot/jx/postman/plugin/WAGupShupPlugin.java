package com.boot.jx.postman.plugin;

import java.util.List;

import com.boot.jx.common.impl.ConfigMeta;
import com.boot.jx.dict.ContactType;
import com.boot.jx.postman.PMConstants.CHANNEL_TYPE;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.PMEnvironment.AChannelDetails;
import com.boot.jx.postman.plugin.ChannelPluginProvider.ChannelPlugin;
import com.boot.jx.postman.plugin.WAGupShupPlugin.GupShupConfigDetails;
import com.boot.model.MapModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;

public class WAGupShupPlugin implements ChannelPlugin<GupShupConfigDetails> {

	@Override
	public String getChannelType() {
		return CHANNEL_TYPE.WA_GUPSHUP;
	}

	@Override
	public ContactType getContactType() {
		return ContactType.WHATSAPP;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class GupShupConfigDetails extends AChannelDetails {

		private static final long serialVersionUID = -2397678752642150000L;
		private String number;
		@JsonView(PMEnvironment.PublicProperty.class)
		private String notifyId;
		@JsonView(PMEnvironment.ProtectedProperty.class)
		private String notifyPass;
		@JsonView(PMEnvironment.PublicProperty.class)
		private String chatId;
		@JsonView(PMEnvironment.ProtectedProperty.class)
		private String chatPass;

		@JsonView(PMEnvironment.PublicProperty.class)
		private String agentUrl;

		public String getNumber() {
			return number;
		}

		public void setNumber(String number) {
			this.number = number;
		}

		public String getNotifyId() {
			return notifyId;
		}

		public void setNotifyId(String notifyId) {
			this.notifyId = notifyId;
		}

		public String getNotifyPass() {
			return notifyPass;
		}

		public void setNotifyPass(String notifyPass) {
			this.notifyPass = notifyPass;
		}

		public String getChatId() {
			return chatId;
		}

		public void setChatId(String chatId) {
			this.chatId = chatId;
		}

		public String getChatPass() {
			return chatPass;
		}

		public void setChatPass(String chatPass) {
			this.chatPass = chatPass;
		}

		@Override
		public String getLane() {
			return this.number;
		}

		public String getAgentUrl() {
			return agentUrl;
		}

		public void setAgentUrl(String agentUrl) {
			this.agentUrl = agentUrl;
		}
	}

	@Override
	public void setDetails(ChannelConfig config, GupShupConfigDetails details) {
		config.setGupshup(details);
	}

	@Override
	public GupShupConfigDetails getDetails(ChannelConfig config) {
		return config.getGupshup();
	}

	@Override
	public GupShupConfigDetails newChannelDetails() {
		return new GupShupConfigDetails();
	}

	@Override
	public void addConfigMeta(List<ConfigMeta> list) {
		list.add(new ConfigMeta().path("gupshup.number").title("Number").createonly());
		list.add(new ConfigMeta().path("gupshup.chatId").title("Chat Id"));
		list.add(new ConfigMeta().path("gupshup.chatPass").title("Chat Password").writeonly());
		list.add(new ConfigMeta().path("gupshup.notifyId").title("Notification Id"));
		list.add(new ConfigMeta().path("gupshup.notifyPass").title("Notification Password").writeonly());
	}

	@Override
	public void importChannelDetailsFromMap(GupShupConfigDetails channelDetails, MapModel map) {
		channelDetails.setNumber(map.pathEntry("gupshup.number").asString(channelDetails.getNumber()));
		channelDetails.setChatId(map.pathEntry("gupshup.chatId").asString(channelDetails.getChatId()));
		channelDetails.setChatPass(map.pathEntry("gupshup.chatPass").asString(channelDetails.getChatPass()));
		channelDetails.setNotifyId(map.pathEntry("gupshup.notifyId").asString(channelDetails.getNotifyId()));
		channelDetails.setNotifyPass(map.pathEntry("gupshup.notifyPass").asString(channelDetails.getNotifyPass()));
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
		return true;
	}
}
