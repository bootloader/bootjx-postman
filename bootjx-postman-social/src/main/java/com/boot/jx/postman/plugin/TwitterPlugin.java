package com.boot.jx.postman.plugin;

import java.util.List;

import com.boot.jx.common.impl.ConfigMeta;
import com.boot.jx.dict.ContactType;
import com.boot.jx.postman.PMConstants.CHANNEL_TYPE;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.PMEnvironment.AChannelDetails;
import com.boot.jx.postman.plugin.ChannelPluginProvider.ChannelPlugin;
import com.boot.jx.postman.plugin.TwitterPlugin.TwitterConfigDetails;
import com.boot.model.MapModel;
import com.fasterxml.jackson.annotation.JsonView;

public class TwitterPlugin implements ChannelPlugin<TwitterConfigDetails> {

	@Override
	public ContactType getContactType() {
		return ContactType.TWITTER;
	}

	@Override
	public String getChannelType() {
		return CHANNEL_TYPE.TWITTER;
	}

	public static class TwitterConfigDetails extends AChannelDetails {

		private static final long serialVersionUID = -2397678752642150000L;
		private String handler;
		private String numericId;
		private String type;
		private String envName;

		@JsonView(PMEnvironment.ProtectedProperty.class)
		private String consumerKey;
		@JsonView(PMEnvironment.ProtectedProperty.class)
		private String consumerSecret;
		@JsonView(PMEnvironment.ProtectedProperty.class)
		private String accessToken;
		@JsonView(PMEnvironment.ProtectedProperty.class)
		private String accessTokenSecret;

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

		public String getEnvName() {
			return envName;
		}

		public void setEnvName(String envName) {
			this.envName = envName;
		}

		public String getConsumerKey() {
			return consumerKey;
		}

		public void setConsumerKey(String consumerKey) {
			this.consumerKey = consumerKey;
		}

		public String getConsumerSecret() {
			return consumerSecret;
		}

		public void setConsumerSecret(String consumerSecret) {
			this.consumerSecret = consumerSecret;
		}

		public String getAccessToken() {
			return accessToken;
		}

		public void setAccessToken(String accessToken) {
			this.accessToken = accessToken;
		}

		public String getAccessTokenSecret() {
			return accessTokenSecret;
		}

		public void setAccessTokenSecret(String accessTokenSecret) {
			this.accessTokenSecret = accessTokenSecret;
		}

		@Override
		public String getLane() {
			return this.handler;
		}

		public String getNumericId() {
			return numericId;
		}

		public void setNumericId(String numericId) {
			this.numericId = numericId;
		}

	}

	@Override
	public void setDetails(ChannelConfig config, TwitterConfigDetails details) {
		config.setTwitter(details);
	}

	@Override
	public TwitterConfigDetails getDetails(ChannelConfig config) {
		return config.getTwitter();
	}

	@Override
	public TwitterConfigDetails newChannelDetails() {
		return new TwitterConfigDetails();
	}

	@Override
	public void addConfigMeta(List<ConfigMeta> configMetaList) {
		configMetaList.add(new ConfigMeta().path("twitter.handler").title("Handler").createonly());
		configMetaList.add(new ConfigMeta().path("twitter.numericId").title("Numberic Id").optional());
		configMetaList.add(new ConfigMeta().path("twitter.type").title("Type").hidden());
		configMetaList.add(new ConfigMeta().path("twitter.envName").title("Env").optional());
		configMetaList.add(new ConfigMeta().path("twitter.accessToken").title("Access Token").writeonly());
		configMetaList.add(new ConfigMeta().path("twitter.accessTokenSecret").title("Access Token Secret").writeonly());
		configMetaList.add(new ConfigMeta().path("twitter.consumerKey").title("Consumer/Api Key").writeonly());
		configMetaList.add(new ConfigMeta().path("twitter.consumerSecret").title("Consumer/Api Secret").writeonly());
	}

	@Override
	public void importChannelDetailsFromMap(TwitterConfigDetails channelDetails, MapModel map) {
		channelDetails.setHandler(map.pathEntry("twitter.handler").asString(channelDetails.getHandler()));
		channelDetails.setNumericId(map.pathEntry("twitter.numericId").asString(channelDetails.getNumericId()));
		channelDetails.setType(map.pathEntry("twitter.type").asString(channelDetails.getType()));
		channelDetails.setEnvName(map.pathEntry("twitter.envName").asString(channelDetails.getEnvName()));
		channelDetails.setAccessToken(map.pathEntry("twitter.accessToken").asString(channelDetails.getAccessToken()));
		channelDetails.setAccessTokenSecret(
				map.pathEntry("twitter.accessTokenSecret").asString(channelDetails.getAccessTokenSecret()));
		channelDetails.setConsumerKey(map.pathEntry("twitter.consumerKey").asString(channelDetails.getConsumerKey()));
		channelDetails.setConsumerSecret(
				map.pathEntry("twitter.consumerSecret").asString(channelDetails.getConsumerSecret()));
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
