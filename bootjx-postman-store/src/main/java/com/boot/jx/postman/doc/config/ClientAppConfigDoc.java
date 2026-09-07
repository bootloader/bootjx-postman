package com.boot.jx.postman.doc.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.AppContextUtil;
import com.boot.jx.mongo.CommonDocInterfaces.AuditableByIdEntity;
import com.boot.jx.mongo.CommonDocInterfaces.IDocument;
import com.boot.jx.mongo.CommonDocInterfaces.SimpleDocument;
import com.boot.jx.postman.ClientApp;
import com.boot.jx.postman.PMConstants.APP_TYPE;
import com.boot.jx.postman.PMConstants.CHAT_MODE;
import com.boot.jx.postman.model.WebhookHealth;
import com.boot.model.UtilityModels.JsonIgnoreUnknown;
import com.boot.utils.ArgUtil;
import com.fasterxml.jackson.annotation.JsonProperty;

@Document(collection = "CONFIG_CLIENT_KEY")
@TypeAlias("ClientAppConfigDoc")
public class ClientAppConfigDoc
		implements IDocument, AuditableByIdEntity, ClientApp, JsonIgnoreUnknown, SimpleDocument {

	private static final long serialVersionUID = -3070718912315245729L;

	@Id
	private String id;

	@JsonProperty("name")
	@Indexed(unique = true)
	private String keyName;

	@JsonProperty("code")
	@Indexed(unique = true)
	private String queue;

	private String traceId;

	private String createdBy;
	private Long createdStamp;

	private String updatedBy;
	private Long updatedStamp;

	private String key;
	private String keyVersion;

	private Long version;

	private String appMode;
	private String appType;
	private String webhook;

	private String forward;

	private String appHook;
	private String appHookFrwrd;

	private Map<String, Object> secret;
	private Map<String, Object> props;
	private Map<String, Object> config;
	private Map<String, Object> custom;

	private WebhookHealth health;

	private String domain;
	private boolean isShared;
	private boolean isDisabled;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTraceId() {
		return traceId;
	}

	public void setTraceId(String traceId) {
		this.traceId = traceId;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public Long getCreatedStamp() {
		return createdStamp;
	}

	public void setCreatedStamp(Long createdStamp) {
		this.createdStamp = createdStamp;
	}

	@Override
	public String getKeyName() {
		return keyName;
	}

	public void setKeyName(String keyName) {
		this.keyName = keyName;
	}

	@Override
	public String getKeyVersion() {
		return keyVersion;
	}

	public void setKeyVersion(String keyVersion) {
		this.keyVersion = keyVersion;
	}

	@Override
	public String getKey() {
		return key;
	}

	public void setKey(String apiKey) {
		this.key = apiKey;
	}

	public String getAppType() {
		return appType;
	}

	public void setAppType(String appType) {
		this.appType = appType;
	}

	public String getWebhook() {
		return webhook;
	}

	public void setWebhook(String webhook) {
		this.webhook = webhook;
	}

	public String getQueue() {
		return queue;
	}

	public void setQueue(String queue) {
		this.queue = queue;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	public Long getUpdatedStamp() {
		return updatedStamp;
	}

	public void setUpdatedStamp(Long updatedStamp) {
		this.updatedStamp = updatedStamp;
	}

	public String getForward() {
		return forward;
	}

	public void setForward(String forward) {
		this.forward = forward;
	}

	public Map<String, Object> getSecret() {
		return secret;
	}

	public void setSecret(Map<String, Object> secret) {
		this.secret = secret;
	}

	public Map<String, Object> getProps() {
		return props;
	}

	public void setProps(Map<String, Object> props) {
		this.props = props;
	}

	public Map<String, Object> props() {
		if (this.props == null) {
			this.props = new HashMap<String, Object>();
		}
		return props;
	}

	public Map<String, Object> secret() {
		if (this.secret == null) {
			this.secret = new HashMap<String, Object>();
		}
		return secret;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	@Override
	public boolean isShared() {
		return isShared;
	}

	public void setShared(boolean isShared) {
		this.isShared = isShared;
	}

	@Override
	public boolean isReadOnly() {
		return (this.isShared()) && !ArgUtil.areEqual(domain, AppContextUtil.getTenant());
	}

	@Override
	public boolean isAgentApp() {
		return CHAT_MODE.AGENT.toString().equals(getAppMode());
	}

	@Override
	public boolean isFeedbackApp() {
		if (ArgUtil.isEqual(getAppType(), APP_TYPE.FEEDBACK, APP_TYPE.FEEDBACK_V2)) {
			return true;
		}
		// App Script opt-in via props.feedbackApp (Include in Default Feedback Queue)
		if (ArgUtil.isEqual(getAppType(), APP_TYPE.APP_SCRIPT)) {
			return Boolean.TRUE.equals(ArgUtil.parseAsBoolean(props().get("feedbackApp"), Boolean.FALSE));
		}
		return false;
	}

	@Override
	public boolean isWebhookApp() {
		return ArgUtil.isEqual(getAppType(), APP_TYPE.WEBHOOK);
	}

	@Override
	public boolean isCustomApp() {
		return ArgUtil.isEqual(getAppType(), APP_TYPE.APP_SCRIPT, APP_TYPE.BOTFLOW);
	}

	@Override
	public boolean isPreDefinedBot() {
		return ArgUtil.isEqual(getAppMode(), CHAT_MODE.BOT, CHAT_MODE.SCRIPTUS);
	}

	@Override
	public boolean isTimeOutPossible() {
		return this.isCustomApp() || this.isPreDefinedBot();
	}

	@Override
	public boolean equals(CHAT_MODE mode) {
		return mode.toString().equals(getAppMode());
	}

	@Override
	public boolean equals(APP_TYPE appType) {
		return appType.toString().equals(getAppType());
	}

	@Override
	public String getAppMode() {
		if (appMode == null) {
			this.appMode = APP_TYPE.from(getAppType()).getMode().toString();
		}
		return appMode;
	}

	public void setAppMode(String appMode) {
		this.appMode = appMode;
	}

	public String getAppHook() {
		return appHook;
	}

	public void setAppHook(String appHook) {
		this.appHook = appHook;
	}

	public String getAppHookFrwrd() {
		return appHookFrwrd;
	}

	public void setAppHookFrwrd(String appHookFrwrd) {
		this.appHookFrwrd = appHookFrwrd;
	}

	public Map<String, Object> getConfig() {
		return config;
	}

	public void setConfig(Map<String, Object> config) {
		this.config = config;
	}

	@Override
	public Map<String, Object> config() {
		if (this.config == null) {
			this.config = new HashMap<String, Object>();
		}
		return config;
	}

	@Override
	public boolean isDisabled() {
		return isDisabled;
	}

	public void setDisabled(boolean isDisabled) {
		this.isDisabled = isDisabled;
	}

	public Map<String, Object> getCustom() {
		return custom;
	}

	public void setCustom(Map<String, Object> custom) {
		this.custom = custom;
	}

	@Override
	public Map<String, Object> custom() {
		if (this.custom == null) {
			this.custom = new HashMap<String, Object>();
		}
		return custom;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	public WebhookHealth getHealth() {
		return health;
	}

	public void setHealth(WebhookHealth health) {
		this.health = health;
	}

}
