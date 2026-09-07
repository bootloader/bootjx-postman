package com.boot.jx.postman.doc.config;

import java.io.Serializable;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.postman.PMEnvironment;
import com.boot.model.TimeModels.TimeStampSupportedModel;
import com.fasterxml.jackson.annotation.JsonView;

@Document(collection = "DUPS_CONFIG_CHANNEL")
@TypeAlias("ChannelConfigDups")
public class ChannelConfigDupsDoc extends TimeStampSupportedModel implements Serializable {

	private static final long serialVersionUID = -6368905475787041196L;

	@Id
	private String id;

	@Indexed
	private String domain;
	private String domainProxy;

	@Indexed
	private String lane;

	@Indexed
	private String sublane;
	
	// Stores WABA ID for WACFB channels (used for template status routing)
	@Indexed
	private String laneParam2;

	@Indexed
	private String channelType;

	@Indexed
	private Object contactType;

	@Indexed
	private String channelId;

	private String name;

	@JsonView(PMEnvironment.ProtectedProperty.class)
	protected String channelKey;

	private boolean isAutoCreated;
	private boolean isProxyEnabled;
	private boolean isSandbox;
	private boolean isShared;
	@Indexed
	private boolean isDisabled;
	@Indexed
	private boolean isDeleted;
	private String server;

	private Map<String, Object> meta;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getLane() {
		return lane;
	}

	public void setLane(String lane) {
		this.lane = lane;
	}

	public boolean isProxyEnabled() {
		return isProxyEnabled;
	}

	public void setProxyEnabled(boolean isProxyEnabled) {
		this.isProxyEnabled = isProxyEnabled;
	}

	public boolean isSandbox() {
		return isSandbox;
	}

	public void setSandbox(boolean isSandbox) {
		this.isSandbox = isSandbox;
	}

	public boolean isShared() {
		return isShared;
	}

	public void setShared(boolean isShared) {
		this.isShared = isShared;
	}

	public boolean isDisabled() {
		return isDisabled;
	}

	public void setDisabled(boolean isDisabled) {
		this.isDisabled = isDisabled;
	}

	public boolean isDeleted() {
		return isDeleted;
	}

	public void setDeleted(boolean isDeleted) {
		this.isDeleted = isDeleted;
	}

	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public String getChannelType() {
		return channelType;
	}

	public void setChannelType(String channelType) {
		this.channelType = channelType;
	}

	public String getChannelId() {
		return channelId;
	}

	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}

	public String getChannelKey() {
		return channelKey;
	}

	public void setChannelKey(String channelKey) {
		this.channelKey = channelKey;
	}

	public boolean isAutoCreated() {
		return isAutoCreated;
	}

	public void setAutoCreated(boolean isAutoCreated) {
		this.isAutoCreated = isAutoCreated;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Object getContactType() {
		return contactType;
	}

	public void setContactType(Object contactType) {
		this.contactType = contactType;
	}

	public Map<String, Object> getMeta() {
		return meta;
	}

	public void setMeta(Map<String, Object> meta) {
		this.meta = meta;
	}

	public String getDomainProxy() {
		return domainProxy;
	}

	public void setDomainProxy(String domainProxy) {
		this.domainProxy = domainProxy;
	}

	public String getSublane() {
		return sublane;
	}

	public void setSublane(String sublane) {
		this.sublane = sublane;
	}
	
	public String getLaneParam2() {
		return laneParam2;
	}

	public void setLaneParam2(String laneParam2) {
		this.laneParam2 = laneParam2;
	}

}
