package com.boot.jx.postman.doc.config;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.AppContextUtil;
import com.boot.jx.mongo.CommonDocInterfaces.TimeStampIndex.TimeStampDocSupport;
import com.boot.jx.postman.PMEnvironment.AChannelDetails;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.plugin.ChannelPluginProvider.ChannelPlugin;
import com.boot.utils.ArgUtil;

@Document(collection = ChannelConfigDoc.DOCUMENT_NAME)
@TypeAlias("ConfigChannel")
public class ChannelConfigDoc extends ChannelConfig implements TimeStampDocSupport {
	public static final String DOCUMENT_NAME = "CONFIG_CHANNEL";

	private static final long serialVersionUID = -6368905475787041196L;

	@Id
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getSubId() {
		return subId;
	}

	public void setSubId(String subId) {
		this.subId = subId;
	}

	public boolean isReadOnly() {
		return (this.isSandbox() || this.isShared()) && !ArgUtil.areEqual(domain, AppContextUtil.getTenant());
	}

	@Override
	@Transient
	public ChannelPlugin<? extends AChannelDetails> getPlugin() {
		return super.getPlugin();
	}

}
