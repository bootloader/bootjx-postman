package com.boot.jx.postman.plugin;

import com.boot.jx.common.impl.ConfigMeta.ConfigMetaProperty;
import com.boot.jx.dict.ContactType;
import com.boot.jx.postman.PMConstants.CHANNEL_TYPE;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.PMEnvironment.AChannelDetails;
import com.boot.jx.postman.plugin.ChannelPluginProvider.DefaultChannelPlugin;
import com.boot.jx.postman.plugin.RCSPlugin.RCSConfigDetails;
import com.boot.utils.ArgUtil;
import com.fasterxml.jackson.annotation.JsonView;

/** Telinfy hub RCS channel (https://hub.telinfy.com/unified/developer). */
public class RCSPlugin implements DefaultChannelPlugin<RCSConfigDetails> {

	/** Default Telinfy hub base URL when channel config omits rcs.baseUrl. */
	public static final String DEFAULT_BASE_URL = "https://hub.telinfy.com/unified/developer";

	public static String resolveBaseUrl(RCSConfigDetails rcs) {
		return ArgUtil.nonEmpty(ArgUtil.is(rcs) ? rcs.getBaseUrl() : null, DEFAULT_BASE_URL).replaceAll("/+$", "");
	}

	@Override
	public String getChannelType() {
		return CHANNEL_TYPE.RCS_TELINFY;
	}

	@Override
	public ContactType getContactType() {
		return ContactType.RCS;
	}

	public static class RCSConfigDetails extends AChannelDetails {

		private static final long serialVersionUID = 1L;

		@ConfigMetaProperty(path = "rcs.agentId", title = "Agent / Bot ID", defaultValue = "default",
				desc = "Lane identifier for this RCS channel (used in channelId).")
		private String agentId;

		@JsonView(PMEnvironment.ProtectedProperty.class)
		@ConfigMetaProperty(path = "rcs.apiKey", writeonly = false, title = "API Key",
				desc = "x-api-key header value from Telinfy hub.")
		private String apiKey;

		@ConfigMetaProperty(path = "rcs.baseUrl", title = "API Base URL", optional = true,
				defaultValue = "https://hub.telinfy.com/unified/developer",
				desc = "Telinfy RCS API base URL for this channel. Falls back to the default hub URL when empty.")
		private String baseUrl;

		@Override
		public String getLane() {
			return ArgUtil.nonEmpty(agentId, "default");
		}

		public String getAgentId() {
			return agentId;
		}

		public void setAgentId(String agentId) {
			this.agentId = agentId;
		}

		public String getApiKey() {
			return apiKey;
		}

		public void setApiKey(String apiKey) {
			this.apiKey = apiKey;
		}

		public String getBaseUrl() {
			return baseUrl;
		}

		public void setBaseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
		}

	}

	@Override
	public void setDetails(ChannelConfig config, RCSConfigDetails details) {
		config.setRcs(details);
	}

	@Override
	public RCSConfigDetails getDetails(ChannelConfig config) {
		return config.getRcs();
	}

	@Override
	public RCSConfigDetails newChannelDetails() {
		return new RCSConfigDetails();
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
