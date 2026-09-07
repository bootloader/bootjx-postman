package com.boot.jx.postman;

import java.io.Serializable;
import java.util.Map;

import com.boot.jx.model.AuditCreateEntity.AuditIdentifier;
import com.boot.jx.postman.PMConstants.APP_TYPE;
import com.boot.jx.postman.PMConstants.CHAT_MODE;
import com.boot.model.MapModel.EntryMeta;
import com.boot.model.MapModel.MapPathEntry;
import com.fasterxml.jackson.annotation.JsonView;

public interface ClientApp extends Serializable, AuditIdentifier {

	public static final String APP_TYPE_WEBHOOK = "WEBHOOK";
	public static final String APP_TYPE_AGENT = "AGENT";
	public static final String APP_TYPE_BOT = "BOT";

	public String getId();

	@JsonView(PMEnvironment.OneTimeVisibleProperty.class)
	public String getKey();

	@JsonView(PMEnvironment.OneTimeVisibleProperty.class)
	public String getAppHook();

	public String getAppHookFrwrd();

	public String getKeyName();

	public String getQueue();

	public String getKeyVersion();

	public Long getVersion();

	public String getAppType();

	public String getAppMode();

	public String getWebhook();

	public String getForward();

	public Map<String, Object> getProps();

	public Map<String, Object> props();

	@JsonView(PMEnvironment.ProtectedProperty.class)
	public Map<String, Object> getSecret();

	public Map<String, Object> secret();

	boolean isShared();

	boolean isReadOnly();

	boolean isAgentApp();

	boolean isPreDefinedBot();

	boolean isFeedbackApp();

	boolean isWebhookApp();

	public boolean isCustomApp();

	boolean equals(CHAT_MODE mode);

	boolean equals(APP_TYPE appType);

	public Map<String, Object> getConfig();

	public Map<String, Object> config();

	public Map<String, Object> custom();

	public default MapPathEntry keyEntry(EntryMeta metaKey) {
		return new MapPathEntry().map(this.config()).key(metaKey.getUkey()).load(null);
	}

	@Override
	public default String auditIdentifier() {
		return this.getQueue();
	}

	public default String getType() {
		return this.getAppType();
	}

	boolean isDisabled();

	boolean isTimeOutPossible();
}
