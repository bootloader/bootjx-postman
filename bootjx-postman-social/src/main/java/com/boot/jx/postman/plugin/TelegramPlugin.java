package com.boot.jx.postman.plugin;

import java.util.List;

import com.boot.jx.common.impl.ConfigMeta;
import com.boot.jx.dict.ContactType;
import com.boot.jx.postman.PMConstants.CHANNEL_TYPE;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.PMEnvironment.AChannelDetails;
import com.boot.jx.postman.plugin.ChannelPluginProvider.ChannelPlugin;
import com.boot.jx.postman.plugin.TelegramPlugin.TelegramConfigDetails;
import com.boot.model.MapModel;
import com.fasterxml.jackson.annotation.JsonView;

public class TelegramPlugin implements ChannelPlugin<TelegramConfigDetails> {

	@Override
	public ContactType getContactType() {
		return ContactType.TELEGRAM;
	}

	@Override
	public String getChannelType() {
		return CHANNEL_TYPE.TELEGRAM;
	}

	public static class TelegramConfigDetails extends AChannelDetails {

		private static final long serialVersionUID = -2397678752642150000L;
		private String handler;
		private String type;

		@JsonView(PMEnvironment.ProtectedProperty.class)
		private String accessToken;

		public String getHandler() {
			return handler;
		}

		public void setHandler(String handler) {
			this.handler = handler;
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

		@Override
		public String getLane() {
			return this.handler;
		}

	}

	@Override
	public TelegramConfigDetails newChannelDetails() {
		return new TelegramConfigDetails();
	}

	@Override
	public void setDetails(ChannelConfig config, TelegramConfigDetails details) {
		config.setTelegram(details);
	}

	@Override
	public TelegramConfigDetails getDetails(ChannelConfig config) {
		return config.getTelegram();
	}

	@Override
	public void addConfigMeta(List<ConfigMeta> configMetaList) {
		configMetaList.add(new ConfigMeta().path("telegram.handler").title("BotName").createonly());
		configMetaList.add(new ConfigMeta().path("telegram.type").optionValues("bot").hidden());
		configMetaList.add(new ConfigMeta().path("telegram.accessToken").title("Access Token").writeonly());
	}

	@Override
	public void importChannelDetailsFromMap(TelegramConfigDetails channelDetails, MapModel map) {
		channelDetails.setHandler(map.pathEntry("telegram.handler").asString(channelDetails.getHandler()));
		channelDetails.setType(map.pathEntry("telegram.type").asString(channelDetails.getType()));
		channelDetails.setAccessToken(map.pathEntry("telegram.accessToken").asString(channelDetails.getAccessToken()));
	}

	@Override
	public boolean isPushAllowed() {
		return true;
	}

	@Override
	public boolean isPushOnlyApproved() {
		return false;
	}

	@Override
	public boolean isPushFreeTextAllowed() {
		return true;
	}

	@Override
	public boolean isPushToNewContactAllowed() {
		return false;
	}

	@Override
	public boolean isWebhookManual() {
		return false;
	}
}
