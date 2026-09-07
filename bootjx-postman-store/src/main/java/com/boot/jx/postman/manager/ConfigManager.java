package com.boot.jx.postman.manager;

import java.util.Map;

import com.boot.jx.model.ModelPatch.ModelPatches;
import com.boot.jx.postman.ClientApp;
import com.boot.jx.postman.PMEnvironment.PMConfigurationObject;
import com.boot.jx.postman.doc.config.ClientAppConfigDoc;
import com.boot.jx.postman.plugin.ChannelConfig;

public interface ConfigManager {
	ClientAppConfigDoc save(ClientAppConfigDoc xo);

	ClientAppConfigDoc create(ClientAppConfigDoc xo);

	void refresh();

	void refresh(String configType, String configId);

	void save(ChannelConfig config);

	ChannelConfig saveChannelConfig(String channelType, Map<String, Object> data);

	ChannelConfig patchChannelConfig(ModelPatches req);

	/**
	 * This mehthod will add channel for specidfied domain, in asyn manner
	 * 
	 * @param authState
	 * @param config
	 * @param domain
	 */
	void saveForDomain(ChannelConfig config, String domain);

	void save(PMConfigurationObject config);

	/**
	 * Update webhook health (centralized tracking for webhook calls)
	 * @param e 
	 */
	void updateWebhookHealth(ClientApp clientApp, boolean success, Exception e);

}
