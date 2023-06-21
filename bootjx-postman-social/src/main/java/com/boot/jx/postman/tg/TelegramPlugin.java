package com.boot.jx.postman.tg;

import com.boot.jx.postman.PostmanChannels;
import com.boot.jx.postman.PostmanChannels.AChannelDetails;
import com.boot.jx.postman.PostmanChannels.ChannelPlugin;
import com.boot.jx.postman.tg.TelegramPlugin.TelegramConfigDetails;
import com.fasterxml.jackson.annotation.JsonView;

public class TelegramPlugin implements ChannelPlugin<TelegramConfigDetails> {

	public static class TelegramConfigDetails extends AChannelDetails {

		private static final long serialVersionUID = -2397678752642150000L;
		private String handler;
		private String type;

		@JsonView(PostmanChannels.ProtectedProperty.class)
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

}
