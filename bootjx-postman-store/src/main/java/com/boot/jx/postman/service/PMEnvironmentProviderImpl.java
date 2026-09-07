package com.boot.jx.postman.service;

import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.boot.jx.AppConfigPackage.AppSharedConfig;
import com.boot.jx.AppConfigPackage.AppSharedConfigChange;
import com.boot.jx.AppContextUtil;
import com.boot.jx.http.CommonHttpRequest.ApiRequestDetail;
import com.boot.jx.logger.AuditDetailProvider;
import com.boot.jx.logger.LoggerService;
import com.boot.jx.mongo.CommonMongoSource;
import com.boot.jx.postman.PMConfiguration.PMConfigurationModel;
import com.boot.jx.postman.PMEnvironment.PMConfigurationObject;
import com.boot.jx.postman.PMEnvironment.PMEnvironmentProvider;
import com.boot.jx.postman.client.PostManClient;
import com.boot.jx.postman.doc.PMConfigurationDoc;
import com.boot.jx.postman.doc.config.ChannelConfigDoc;
import com.boot.jx.postman.doc.config.ClientAppConfigDoc;
import com.boot.jx.postman.doc.config.FeaturesConfigDoc;
import com.boot.jx.postman.doc.config.PrefsConfigDoc;
import com.boot.jx.postman.doc.config.VarsConfigDoc.CompanyVarsConfigDoc;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.store.ConfigMaster;
import com.boot.jx.scope.tnt.Tenants;
import com.boot.model.SafeKeyHashMap;
import com.boot.utils.ArgUtil;
import com.boot.utils.EntityDtoUtil;
import com.boot.utils.StringUtils;
import com.boot.utils.UniqueID;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

@Component
public class PMEnvironmentProviderImpl implements PMEnvironmentProvider, AppSharedConfig {
	private static final Logger LOGGER = LoggerService.getLogger(PMEnvironmentProviderImpl.class);

	private Cache<String, PMConfigurationDoc> localConfigMap = CacheBuilder.newBuilder().maximumSize(1000)
			.expireAfterWrite(1, TimeUnit.HOURS).build();

	// private Map<String, PMConfigurationDoc> localConfigMap = new HashMap<String,
	// PMConfigurationDoc>();

	PMConfigurationDoc sharedConfiguration = null;
	PMConfigurationDoc defaultConfiguration = null;

	@Autowired(required = false)
	private ConfigMaster configMaster;

	public String name() {
		return "CONFIG_SETUP";
	};

	@Autowired
	private PostManClient postManClient;

	@Lazy
	@Autowired
	AuditDetailProvider auditDetailProvider;

	@Value("${mry.prop.service.server}")
	private String serviceServer;

	private boolean hasRule(String useNoDb) {
		ApiRequestDetail apiDetails = AppContextUtil.getApiRequestDetail();
		return ArgUtil.is(apiDetails) && apiDetails.hasRule(useNoDb);
	}

	@Override
	public PMConfigurationModel local() {
		String tnt = AppContextUtil.getTenant();

		String mappedTo = hasRule(CommonMongoSource.USE_NO_DB) ? "nodb" : tnt;

		if (Tenants.isDefault(tnt)) {
			if (defaultConfiguration != null && sharedConfiguration != null) {
				return defaultConfiguration;
			}
		} else {
			PMConfigurationModel presentConfig = localConfigMap.getIfPresent(mappedTo);
			if (presentConfig != null) {
				return presentConfig;
			}
		}

		if (ArgUtil.is(configMaster)) {
			PMConfigurationDoc localConfiguration = getPMConfigurationDoc();
			localConfiguration.setPrefs(null);

			List<PrefsConfigDoc> prefsConfigs = configMaster.readOnly().findAll(PrefsConfigDoc.class);
			for (PrefsConfigDoc prefsConfig : prefsConfigs) {
				localConfiguration.setPref(prefsConfig, serviceServer);
			}

			List<FeaturesConfigDoc> featuresConfigs = configMaster.readOnly().findAll(FeaturesConfigDoc.class);
			for (FeaturesConfigDoc featureConfig : featuresConfigs) {
				localConfiguration.setFeature(featureConfig, serviceServer);
			}

			List<ChannelConfigDoc> channels = configMaster.readOnly().findAll(ChannelConfigDoc.class);
			// System.out.println("TENE==" + tnt + "=====" + mappedTo + "====" +
			// channels.size());
			for (ChannelConfig channel : channels) {
				channel.setDomain(tnt);
				localConfiguration.channels(channel);
			}

			List<ClientAppConfigDoc> clientKeys = configMaster.readOnly().findAll(ClientAppConfigDoc.class);
			for (ClientAppConfigDoc clientKey : clientKeys) {
				localConfiguration.clientApiKey(clientKey);
			}

			List<CompanyVarsConfigDoc> companyVars = configMaster.readOnly().findAll(CompanyVarsConfigDoc.class);

			SafeKeyHashMap<Object> company = localConfiguration.globalVars();

			for (CompanyVarsConfigDoc companyVar : companyVars) {
				company.put(companyVar.getKey(), companyVar.getValue());
			}

			if (ArgUtil.is(localConfiguration)) {
				localConfiguration.setUpdateStamp(System.currentTimeMillis());
				if (!Tenants.isDefault(tnt)) {
					localConfigMap.put(mappedTo, localConfiguration);
				} else {
					defaultConfiguration = localConfiguration;
				}
			}

			if (Tenants.isDefault(tnt)) {
				PMConfigurationDoc newSharedConfiguration = new PMConfigurationDoc();
				for (Entry<String, PMConfigurationObject> entry : localConfiguration.prefs().entrySet()) {
					newSharedConfiguration.setPref(entry.getValue(), serviceServer);
				}
				for (Entry<String, PMConfigurationObject> entry : localConfiguration.features().entrySet()) {
					newSharedConfiguration.setFeature(entry.getValue(), serviceServer);
				}
				List<ChannelConfigDoc> sandboxChannels = configMaster.readOnly().findAll(ChannelConfigDoc.class);
				for (ChannelConfig channel : sandboxChannels) {
					channel.setDomain(tnt);
					if (channel.isSandbox() || channel.isShared() || channel.isMaster()) {
						newSharedConfiguration.channels(channel, serviceServer);
					}
				}

				List<ClientAppConfigDoc> sharedApps = configMaster.readOnly().findAll(ClientAppConfigDoc.class);
				for (ClientAppConfigDoc sharedApp : sharedApps) {
					sharedApp.setDomain(tnt);
					if (sharedApp.isShared()) {
						newSharedConfiguration.clientApiKey(sharedApp);
					}
				}
				sharedConfiguration = newSharedConfiguration;
			}

			return localConfiguration;
		}
		return null;
	}

	public ChannelConfig configInternal(ChannelConfig config) {
		ChannelConfigDoc doc = EntityDtoUtil.dtoToEntity(config, new ChannelConfigDoc());
		doc.setId(StringUtils.toLowerCase(doc.getChannelId()));
		ChannelConfigDoc configUpdated = configMaster.findById(config.getChannelId(), ChannelConfigDoc.class);
		if (ArgUtil.is(configUpdated)) {
			doc.meta().putAll(configUpdated.meta());
		}
		configMaster.saveChannelConfig(doc);
		return doc;
	}

	@Override
	public ChannelConfig addChannel(ChannelConfig config) {
		if (!ArgUtil.is(config.getServer())) {
			config.setServer(serviceServer);
		}
		return configInternal(config);
	}

	@Override
	public void updateChannel(ChannelConfig config, String action) {
		if (ArgUtil.is(config)) {
			ChannelConfigDoc configDoc = EntityDtoUtil.dtoToEntity(config, new ChannelConfigDoc());
			if ("remove".equalsIgnoreCase(action)) {
				configMaster.remove(configDoc);
				PMConfigurationDoc doc = getPMConfigurationDoc();
				doc.channels().remove(config.getChannelId());
				configMaster.save(doc);

			} else {
				if (!ArgUtil.is(config.getServer())) {
					config.setServer(serviceServer);
				}
				configMaster.saveChannelConfig(configDoc, action);
			}
		} else {
			System.out.println("No Channel to delete");
		}
	}

	private PMConfigurationDoc getPMConfigurationDoc() {
		PMConfigurationDoc doc = configMaster.findById(AppContextUtil.getTenant(), PMConfigurationDoc.class);
		if (ArgUtil.isEmpty(doc)) {
			doc = new PMConfigurationDoc();
			doc.setTenant(AppContextUtil.getTenant());
		}
		return doc;
	}

	@Override
	public PMConfigurationModel shared() {
		return sharedConfiguration;
	}

	@Override
	public void initConfig() {
		LOGGER.info("=======================initConfig");
		String sessionId = UniqueID.generateString();
		AppContextUtil.setSessionId(sessionId);
		AppContextUtil.getTraceId(true, true);
		AppContextUtil.resetTraceTime();
		AppContextUtil.init();
		local();
	}

	@Override
	public void clear(AppSharedConfigChange change) {
		String tnt = AppContextUtil.getTenant();
		localConfigMap.invalidate(tnt);
		if (Tenants.isDefault(tnt)) {
			this.sharedConfiguration = null;
			this.defaultConfiguration = null;
			this.initConfig();
		}
		if (ArgUtil.is(this.configMaster) && ArgUtil.is(change)) {
			this.configMaster.clearChannelMeta(change);
		}
	}
}
