
package com.boot.jx.postman;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import com.boot.utils.CommonHashCodeBuilder;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.boot.jx.AppConfig;
import com.boot.jx.AppConfigPackage.AppCommonConfig;
import com.boot.jx.AppConfigPackage.AppSharedConfig;
import com.boot.jx.AppConfigPackage.AppSharedConfigChange;
import com.boot.jx.dict.ContactType;
import com.boot.jx.logger.LoggerService;
import com.boot.jx.model.AuditCreateEntity.AuditIdentifier;
import com.boot.jx.postman.PMConfiguration.PMConfigurationModel;
import com.boot.jx.postman.PMConfiguration.PMConfigurationWrappper;
import com.boot.jx.postman.PMConstants.CHAT_MODE;
import com.boot.jx.postman.model.MessageDefinitions.ChatContact;
import com.boot.jx.postman.model.MessageDefinitions.Contactable;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.plugin.ChannelPluginProvider;
import com.boot.jx.postman.plugin.ChannelPluginProvider.ChannelPlugin;
import com.boot.jx.utils.PostManUtil;
import com.boot.model.MapModel.MapEntry;
import com.boot.model.TimeModels.TimeStampSupportedModel;
import com.boot.utils.ArgUtil;
import com.boot.utils.TimeUtils.TimePeriod;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;

@Component
public class PMEnvironment {

	private static final Logger LOGGER = LoggerService.getLogger(PMEnvironment.class);

	/**
	 * Field will be visible in [TinyView,SummaryView,FullView] View
	 * 
	 * All the Fields with [TinyView,None] are going to be visible
	 * 
	 * @author lalittanwar
	 *
	 */
	public static interface TinyView {
	}

	/**
	 * Field will be visible in [SummaryView,FullView] View.
	 * 
	 * All the Fields with [SummaryView,TinyView,None] are going to be visible
	 * 
	 * @author lalittanwar
	 *
	 */
	public static interface SummaryView extends TinyView {
	}

	/**
	 * Field will be visible only in [FullView] View.
	 * 
	 * All the Fields with [FullView,SummaryView,TinyView,None] are going to be
	 * visible
	 * 
	 * @author lalittanwar
	 *
	 */
	public static interface FullView extends SummaryView {
	}

	public static interface PublicProperty {
	}

	public static interface ProtectedProperty extends PublicProperty {
	}

	public static interface OneTimeVisibleProperty extends ProtectedProperty {
	}

	public static interface PMEnvironmentProvider extends AppSharedConfig {

		public PMConfigurationModel local();

		public PMConfigurationModel shared();

		public ChannelConfig addChannel(ChannelConfig config);

		public void updateChannel(ChannelConfig config, String action);

		public void initConfig();
	}

	public static interface ChannelTypeSpecificProps {

		@JsonView(PublicProperty.class)
		default public boolean isOutboundAllowed() {
			return true;
		}

		@JsonView(PublicProperty.class)
		default public boolean isInboundAllowed() {
			return true;
		}

		@JsonView(PublicProperty.class)
		public boolean isPushAllowed();

		@JsonView(PublicProperty.class)
		public boolean isPushOnlyApproved();

		@JsonView(PublicProperty.class)
		public boolean isPushFreeTextAllowed();

		@JsonView(PublicProperty.class)
		public boolean isPushToNewContactAllowed();

		@JsonView(PublicProperty.class)
		public boolean isWebhookManual();

		@JsonView(PublicProperty.class)
		default public boolean isPushLocalStore() {
			return true;
		}

		@Deprecated
		@JsonView(PublicProperty.class)
		public default String getChannel() {
			return null;
		}

		@JsonView(PublicProperty.class)
		public ContactType getContactType();

		@JsonView(PublicProperty.class)
		public String getChannelType();

	}

	public static interface ChannelDetails extends Serializable {

		@JsonView(PublicProperty.class)
		public String getLane();

		@JsonView(PublicProperty.class)
		default public String getSublane() {
			return null;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static abstract class AChannelDetails extends TimeStampSupportedModel implements ChannelDetails {
		private static final long serialVersionUID = -5531902306230415784L;
	}

	public static abstract class AChannelConfig extends AChannelDetails
			implements ChannelTypeSpecificProps, AuditIdentifier, Comparable<AChannelConfig> {

		private static final long serialVersionUID = 1950315645271368433L;

		protected ContactType contactType;
		protected String channelType;

		@JsonView(PMEnvironment.ProtectedProperty.class)
		protected String channelKey;
		protected String channelCode;

		protected String id;
		protected String subId;
		protected String name;
		protected String inboundQueue;

		private boolean isProxyEnabled;
		private boolean isSandbox;
		private boolean isShared;
		private boolean isDisabled;
		private boolean isDeleted;
		private boolean isHidden;
		private String apiVersion;
		private boolean isMockEnabled;

		@JsonView(PMEnvironment.PublicProperty.class)
		private String server;

		@JsonView(PMEnvironment.PublicProperty.class)
		protected String webhookUrl;

		public AChannelConfig() {
			this.channelType = "WEBSITE";
		}

		public ContactType getContactType() {
			return contactType;
		}

		public void setContactType(ContactType contactType) {
			this.contactType = contactType;
		}

		@Override
		public String getChannelType() {
			return channelType;
		}

		public void setChannelType(String channelType) {
			this.channelType = channelType;
		}

		public String getChannelId() {
			if (ArgUtil.is(this.id)) {
				return this.id;
			}
			return PostManUtil.CHANNEL_ID_FORMAT(this.getChannelType(), this.getLane());
		}

		public String getChannelSubId() {
			if (ArgUtil.is(this.subId)) {
				return this.subId;
			}
			return PostManUtil.CHANNEL_ID_FORMAT(this.getChannelType(), this.getSublane());
		}

		public String getChannelKey() {
			return channelKey;
		}

		public void setChannelKey(String channelKey) {
			this.channelKey = channelKey;
		}

		public String getName() {
			if (!ArgUtil.is(this.name)) {
				return String.format("%s %s", this.getContactType(), this.getLane());
			}
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getWebhookUrl() {
			return webhookUrl;
		}

		public void setWebhookUrl(String webhookUrl) {
			this.webhookUrl = webhookUrl;
		}

		@Override
		public String toString() {
			return this.getChannelId();
		}

		public boolean isSandbox() {
			return isSandbox;
		}

		public void setSandbox(boolean isSandbox) {
			this.isSandbox = isSandbox;
		}

		public boolean isDisabled() {
			return isDisabled;
		}

		public void setDisabled(boolean isDisabled) {
			this.isDisabled = isDisabled;
		}

		public boolean isReadOnly() {
			return false;
		}

		public boolean isShared() {
			return isShared;
		}

		public void setShared(boolean isShared) {
			this.isShared = isShared;
		}

		public String getInboundQueue() {
			return inboundQueue;
		}

		public void setInboundQueue(String inboundQueue) {
			this.inboundQueue = inboundQueue;
		}

		public String getChannelCode() {
			return channelCode;
		}

		public void setChannelCode(String channelCode) {
			this.channelCode = channelCode;
		}

		public String getServer() {
			return server;
		}

		public void setServer(String server) {
			this.server = server;
		}

		public boolean isProxyEnabled() {
			return isProxyEnabled;
		}

		public void setProxyEnabled(boolean isProxyEnabled) {
			this.isProxyEnabled = isProxyEnabled;
		}

		public boolean equals(ContactType type) {
			return this.contactType == type;
		}

		public boolean isDeleted() {
			return isDeleted;
		}

		public void setDeleted(boolean isDeleted) {
			this.isDeleted = isDeleted;
		}

		@Override
		public String auditIdentifier() {
			return this.getChannelId();
		}

		public boolean isHidden() {
			return isHidden;
		}

		public void setHidden(boolean isHidden) {
			this.isHidden = isHidden;
		}

		public String getApiVersion() {
			return apiVersion;
		}

		public void setApiVersion(String apiVersion) {
			this.apiVersion = apiVersion;
		}

		public boolean isMockEnabled() {
			return isMockEnabled;
		}

		public void setMockChannel(boolean isMockChannel) {
			this.isMockEnabled = isMockChannel;
		}

		@Override
		public int hashCode() {
			return new CommonHashCodeBuilder(17, 31).append(this.toString()).toHashCode();
		}

		@Override
		public int compareTo(AChannelConfig o) {
			if (o == null) {
				return 1;
			}
			return this.toString().compareTo(o.toString());
		}

		@JsonView(PublicProperty.class)
		public ChannelPlugin<? extends AChannelDetails> getPlugin() {
			return ChannelPluginProvider.getOrDefault(channelType);
		}

	}

	public static class PMConfigurationObject extends MapEntry implements Serializable {

		private static final long serialVersionUID = 2678154770516185408L;
		String key;
		String description;
		String domain;
		String server;
		boolean shared;
		boolean disabled;
		boolean primary;

		public PMConfigurationObject(String key, Object value) {
			super(value);
			this.key = key;
		}

		public PMConfigurationObject() {
			super(null);
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public boolean isShared() {
			return shared;
		}

		public void setShared(boolean shared) {
			this.shared = shared;
		}

		public String getDomain() {
			return domain;
		}

		public void setDomain(String domain) {
			this.domain = domain;
		}

		public String getServer() {
			return server;
		}

		public void setServer(String server) {
			this.server = server;
		}

		public boolean isDisabled() {
			return disabled;
		}

		public void setDisabled(boolean disabled) {
			this.disabled = disabled;
		}

		public boolean isPrimary() {
			return primary;
		}

		public void setPrimary(boolean primary) {
			this.primary = primary;
		}

	}

	public static class UrlPath {
		public String v1;
		public String v2;

		public UrlPath v1(String v1) {
			this.v1 = v1;
			return this;
		}

		public UrlPath v2(String v2) {
			this.v2 = v2;
			return this;
		}

		public String getV2orV1() {
			return ArgUtil.anyOf(v2, v1);
		}
	}

	@Lazy
	@Autowired(required = false)
	private PMEnvironmentProvider provider;

	public PMConfigurationModel local() {
		PMConfigurationModel config = null;
		if (ArgUtil.is(provider)) {
			config = provider.local();
		}
		if (config == null) {
			config = PMConfiguration.instance();
		}
		return config;
	}

	public PMConfigurationModel shared() {
		PMConfigurationModel config = null;
		if (ArgUtil.is(provider)) {
			config = provider.shared();
		}
		if (config == null) {
			config = PMConfiguration.instance();
		}
		return config;
	}

	public PMConfigurationWrappper config() {
		PMConfigurationWrappper config = new PMConfigurationWrappper().appConfig(appConfig);
		if (ArgUtil.is(provider)) {
			return config.local(provider.local()).shared(provider.shared());
		}
		return config;
	}

	public void initConfig() {
		if (ArgUtil.is(provider)) {
			LOGGER.info("=======================initConfig");
			provider.initConfig();
		}
	}

	@Autowired
	private AppConfig appConfig;

	public void addChannel(ChannelConfig config) {
		if (ArgUtil.is(provider)) {
			provider.addChannel(config);
		}
	}

	public void updateChannel(ChannelConfig config, String action) {
		if (ArgUtil.is(provider)) {
			provider.updateChannel(config, action);
		}
	}

	/**
	 * Find master channel by channel type from shared environment
	 *
	 * @param channelType The channel type to search for (e.g., "wacfb", "wac360")
	 * @return ChannelConfig if found, null otherwise
	 */
	public ChannelConfig findMasterChannelByType(String channelType) {
		if (!ArgUtil.is(channelType)) {
			return null;
		}
		Set<AChannelConfig> allChannels = shared().listChannels();
		return allChannels.stream().map(channel -> (ChannelConfig) channel).filter(channel -> channel.isMaster())
				.filter(channel -> channelType.equalsIgnoreCase(channel.getChannelType())).findFirst().orElse(null);
	}

	public interface PMCommonConfig extends AppCommonConfig {
		public String getCdnServerDebug();

		public String getCdnServer();

		public String getBotUrl(ClientApp app);

		public String getAgentUrl();

		public String getServiceServer();

		public String getServiceServerByRequest();

		public String getScriptusUrl();

		public String getScriptusUrl(ClientApp app, UrlPath path);

		public String getScriptusSecret();

		public boolean isValidDomain();

		public boolean isDefaultDomain();

		public String mainDomainRedirect();

		public String mainDomainRedirect(String path);

		public String getMediaTemplateVersion();

	}

	public interface PMDomainConfig {
		public String getDefaultInboundQueue();

		public String getDefaultInboundQueue(String channelId, CHAT_MODE mode);

		public String getDefaultInboundQueue(Contactable contact);

		String getDomainUrl();

		PMConfigurationObject getAgentHistoryPeriod();

		PMConfigurationObject getChatIdleTimeout();

		PMConfigurationObject getAgentHistoryCount();

		String getDefaultInboundQueue(Contactable contact, CHAT_MODE mode);

		PMConfigurationObject getAgentChatDisable();

		String getTimeZoneFromSetup();

		boolean getMessageOutboundDump();

		String getCampaignQueue();

	}

	public interface PMClientConfig {

		String getWebhookBase(ChannelConfig channelConfig, String appPrefix);

		String getChatSessionTimeout();

		TimePeriod getAgentSessionTimeout();

		String getWebhookUrl(ChannelConfig channelConfig, String appPrefix, Map<String, Object> query);

		boolean isLocalDummyBotEnabled();

		String getDefaultSender();

		String getContactDetailsUrl();

	}

	public interface PMGateKeeper {

		boolean canSendTemplateMedia(OutboxMessage outboxMessage, ChatContact chatContact);

		boolean canSendMessage(OutboxMessage outboxMessage, ChatContact chatContact);

		default boolean canSendTemplateMedia(OutboxMessage outboxMessage) {
			return this.canSendTemplateMedia(outboxMessage, null);
		};
	}

	public interface MessageProcessor {
		OutboxMessage beforeSend(OutboxMessage outboxMessage, ChannelConfig channelConfig);
	}

	// @Lazy on these three, same as messageProcessor/pmGateKeeper below: their
	// implementations (PMCommonConfigImpl/PMDomainConfigImpl/PMClientConfigImpl)
	// each hold an @Autowired PMEnvironment back-reference, forming a bean
	// creation cycle that Spring Boot 2.6+ rejects by default
	// (spring.main.allow-circular-references=false).
	@Lazy
	@Autowired(required = false)
	private PMCommonConfig pmCommonConfig;
	@Lazy
	@Autowired(required = false)
	private PMDomainConfig pmDomainConfig;
	@Lazy
	@Autowired(required = false)
	private PMClientConfig pmClientConfig;

	@Autowired
	@Lazy // Delays initialization to break circular dependency
	private MessageProcessor messageProcessor;

	@Autowired
	@Lazy // Delays initialization to break circular dependency
	private PMGateKeeper pmGateKeeper;

	public PMCommonConfig commonConfig() {
		return pmCommonConfig;
	}

	public PMDomainConfig domainConfig() {
		return pmDomainConfig;
	}

	public PMClientConfig clientConfig() {
		return pmClientConfig;
	}

	public PMGateKeeper gateKeeper() {
		return pmGateKeeper;
	}

	public MessageProcessor messageProcessor() {
		return messageProcessor;
	}

	public void publishUpdate() {
		if (ArgUtil.is(provider)) {
			provider.publishUpdate();
		}
	}

	public void publishUpdate(AppSharedConfigChange change) {
		if (ArgUtil.is(provider)) {
			provider.publishUpdate(change);
		}
	}
}
