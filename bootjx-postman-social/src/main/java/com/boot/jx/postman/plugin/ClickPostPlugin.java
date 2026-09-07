package com.boot.jx.postman.plugin;

import com.boot.jx.common.impl.ConfigMeta.ConfigMetaProperty;
import com.boot.jx.dict.ContactType;
import com.boot.jx.postman.PMConstants.CHANNEL_TYPE;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.PMEnvironment.AChannelDetails;
import com.boot.jx.postman.plugin.ChannelPluginProvider.DefaultChannelPlugin;
import com.boot.jx.utils.PostManUtil;
import com.boot.model.MapModel;
import com.boot.utils.ArgUtil;
import com.boot.utils.Random;
import com.fasterxml.jackson.annotation.JsonView;

/** ClickPost shipment tracking webhook channel (manual URL, inbound only). */
public class ClickPostPlugin implements DefaultChannelPlugin<ClickPostPlugin.ClickPostConfigDetails> {

	@ConfigMetaProperty(context = "clickpost")
	public static class ClickPostConfigDetails extends AChannelDetails {

		private static final long serialVersionUID = 1L;

		@ConfigMetaProperty(path = "webhookApiKey", title = "Webhook API Key", writeonly = true, optional = true,
				desc = "Secret for ClickPost X-Api-Key auth (auto-generated if empty).")
		@JsonView(PMEnvironment.ProtectedProperty.class)
		private String webhookApiKey;

		@ConfigMetaProperty(path = "shopifyUrl", title = "Shopify Store URL", optional = true,
				desc = "e.g. https://yourstore.myshopify.com — used to resolve customer from additional.order_id")
		private String shopifyUrl;

		@ConfigMetaProperty(path = "shopifyToken", title = "Shopify Access Token", writeonly = true, optional = true,
				desc = "Admin API access token for Shopify GraphQL order lookup")
		@JsonView(PMEnvironment.ProtectedProperty.class)
		private String shopifyToken;

		public String getWebhookApiKey() {
			return webhookApiKey;
		}

		public void setWebhookApiKey(String webhookApiKey) {
			this.webhookApiKey = webhookApiKey;
		}

		public String getShopifyUrl() {
			return shopifyUrl;
		}

		public void setShopifyUrl(String shopifyUrl) {
			this.shopifyUrl = shopifyUrl;
		}

		public String getShopifyToken() {
			return shopifyToken;
		}

		public void setShopifyToken(String shopifyToken) {
			this.shopifyToken = shopifyToken;
		}

		/** Lane lives on {@link ChannelConfig}; */
		@Override
		public String getLane() {
			return null;
		}
	}

	@Override
	public String getChannelType() {
		return CHANNEL_TYPE.CLICKPOST;
	}

	@Override
	public void importChannelConfigFromMap(ChannelConfig config, MapModel map, String channelType) {
		// channelId = clickpost:{lane}; lane lives on ChannelConfig (not
		// ClickPostConfigDetails)
		if (!ArgUtil.is(config.getLane()) && !ArgUtil.is(map.getString("lane"))) {
			String lane = "cp" + Random.randomAlphaNumeric(6).toLowerCase();
			map.put("lane", lane);
			config.setLane(lane);
		} else if (!ArgUtil.is(config.getLane()) && ArgUtil.is(map.getString("lane"))) {
			config.setLane(map.getString("lane"));
		}
		DefaultChannelPlugin.super.importChannelConfigFromMap(config, map, channelType);

		ClickPostConfigDetails details = getDetails(config);
		if (ArgUtil.is(details) && !ArgUtil.is(details.getWebhookApiKey())) {
			details.setWebhookApiKey(PostManUtil.UNIQUE_API_KEY());
		}
	}

	@Override
	public ContactType getContactType() {
		return ContactType.APPLICATION;
	}

	@Override
	public void setDetails(ChannelConfig config, ClickPostConfigDetails details) {
		config.setClickpost(details);
	}

	@Override
	public ClickPostConfigDetails getDetails(ChannelConfig config) {
		return config.getClickpost();
	}

	@Override
	public ClickPostConfigDetails newChannelDetails() {
		return new ClickPostConfigDetails();
	}

	@Override
	public boolean isWebhookManual() {
		return true;
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

	public boolean isOutboundAllowed() {
		return false;
	}

}
