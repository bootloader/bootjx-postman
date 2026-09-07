package com.boot.jx.postman.store;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.boot.jx.AppContextUtil;
import com.boot.jx.mongo.CommonMongoTemplateAbstract;
import com.boot.jx.postman.PMConstants.CHANNEL_TYPE;
import com.boot.jx.postman.doc.PMConfigurationDoc;
import com.boot.jx.postman.doc.config.ChannelConfigDoc;
import com.boot.jx.postman.doc.config.ChannelConfigDupsDoc;
import com.boot.jx.postman.doc.config.ClientAppConfigDoc;
import com.boot.jx.postman.doc.config.FeaturesConfigDoc;
import com.boot.jx.postman.doc.config.OnboardConfigDoc;
import com.boot.jx.postman.doc.config.PrefsConfigDoc;
import com.boot.jx.postman.doc.config.VarsConfigDoc;
import com.boot.jx.postman.doc.config.VarsConfigDoc.CompanyTokenKeyDoc;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.plugin.WacfbPlugin;
import com.boot.jx.scope.tnt.Tenants;
import com.boot.jx.utils.PostManUtil;
import com.boot.utils.ArgUtil;
import com.boot.utils.CryptoUtil;
import com.boot.utils.EntityDtoUtil;
import com.mongodb.client.result.DeleteResult;

@Component
public class ConfigStore extends CommonMongoTemplateAbstract<ConfigStore> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigStore.class);

	public void saveConfiguration(PMConfigurationDoc doc) {
		doc.getAccountKey(); // Populate Keys of not exists
		save(doc);
		log(doc, "updated");
	}

	public void savePrefsConfig(PrefsConfigDoc prefsConfigDoc) {
		save(prefsConfigDoc);
		log(prefsConfigDoc, "updated");
	}

	public void saveFeatureConfig(FeaturesConfigDoc prefsConfigDoc) {
		save(prefsConfigDoc);
		log(prefsConfigDoc, "updated");
	}

	public OnboardConfigDoc saveOnboardConfig(OnboardConfigDoc onboardConfigDoc) {
		save(onboardConfigDoc);
		log(onboardConfigDoc, "updated");
		return onboardConfigDoc;
	}

	public OnboardConfigDoc findOnboardConfig(String taskCode) {
		return findByIdString(taskCode, OnboardConfigDoc.class);
	}

	public java.util.List<OnboardConfigDoc> findAllOnboardConfigs() {
		return findAll(OnboardConfigDoc.class);
	}

	public void saveChannelConfig(ChannelConfig configDoc, String action) {
		if ("disable".equalsIgnoreCase(action)) {
			configDoc.disabled(true);
		} else if ("enable".equalsIgnoreCase(action)) {
			configDoc.disabled(false);
		} else if ("sandbox_enable".equalsIgnoreCase(action)) {
			configDoc.setSandbox(true);
		} else if ("sandbox_disable".equalsIgnoreCase(action)) {
			configDoc.setSandbox(false);
		} else if ("shared_enable".equalsIgnoreCase(action)) {
			configDoc.setShared(true);
		} else if ("shared_disable".equalsIgnoreCase(action)) {
			configDoc.setShared(false);
		} else if ("mock_disable".equalsIgnoreCase(action)) {
			configDoc.setMockChannel(false);
		} else if ("mock_enable".equalsIgnoreCase(action)) {
			configDoc.setMockChannel(true);
		}
		configDoc.getChannelKey(); // Populate Keys of not exists
		save(configDoc);
		log(configDoc, "updated", action);
	}

	public void saveChannelConfig(ChannelConfig doc) {
		saveChannelConfig(doc, null);
	}

	public Optional<String> saveClientKeyConfig(ClientAppConfigDoc clientApiKey) {
		try {
			boolean generated = false;
			String plainKey = null;
			if (!ArgUtil.is(clientApiKey.getId()) || !ArgUtil.is(clientApiKey.getKey())) {
				plainKey = PostManUtil.UNIQUE_API_KEY();
				clientApiKey.setKey(CryptoUtil.getSHA2HashUnchecked(plainKey));
				generated = true;
			} else {
				ClientAppConfigDoc oldDoc = findByIdString(clientApiKey.getId(), ClientAppConfigDoc.class);
				clientApiKey.setKey(oldDoc.getKey());
			}

			clientApiKey.setAppHook(String.format("https://{{domain}}.{{server}}/bot/ext/app/%s/{{id}}/%s",
					clientApiKey.getQueue(), CryptoUtil.getMD5Hash(clientApiKey.getKey())));

			save(clientApiKey);
			log(clientApiKey, "updated", generated ? "Key Updated" : "");
			return generated ? Optional.of(plainKey) : Optional.empty();
		} catch (org.springframework.dao.DuplicateKeyException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.error("saveClientKeyConfig", e);
			return Optional.empty();
		}
	}

	public DeleteResult remove(Object object) {
		DeleteResult r = super.remove(object);
		log(object, "deleted");
		return r;
	}

	public void saveCompanyVar(VarsConfigDoc refreshableConfigDoc) {
		try {

			VarsConfigDoc companyVarOld = findById(refreshableConfigDoc.getId(), refreshableConfigDoc.getClass());
			if (ArgUtil.is(companyVarOld)) {
				if (refreshableConfigDoc.getValue() == null) {
					refreshableConfigDoc.setValue(companyVarOld.getValue());
				}

				if (refreshableConfigDoc instanceof CompanyTokenKeyDoc) {
					CompanyTokenKeyDoc companyTokenKeyDoc = (CompanyTokenKeyDoc) refreshableConfigDoc;
					CompanyTokenKeyDoc companyTokenKeyDocOld = (CompanyTokenKeyDoc) companyVarOld;
					companyTokenKeyDocOld.secret().putAll(companyTokenKeyDoc.secret());
					companyTokenKeyDoc.secret().putAll(companyTokenKeyDocOld.secret());
				}
			}

			save(refreshableConfigDoc);
			log(refreshableConfigDoc, "updated");
		} catch (Exception e) {
			LOGGER.error("saveClientKeyConfig", e);
		}
	}

	@Async
	public void saveMaster(ChannelConfigDoc configDoc) {
		LOGGER.info("saveMaster called for channelId: {}, channelType: {}", configDoc.getChannelId(),
				configDoc.getChannelType());

		String tnt = AppContextUtil.getTenant();
		if (Tenants.isDefault(tnt)) {
			return;
		}
		ChannelConfigDupsDoc masterDoc = EntityDtoUtil.dtoToEntity(configDoc, new ChannelConfigDupsDoc());
		String domain = ArgUtil.nonEmpty(masterDoc.getDomain(), AppContextUtil.getTenant());
		masterDoc.setId(domain + ":" + configDoc.getId());
		masterDoc.setChannelId(configDoc.getChannelId());
		masterDoc.setName(configDoc.getName());
		masterDoc.setDomain(domain);
		masterDoc.setDomainProxy(configDoc.getDomainProxy());
		masterDoc.setContactType(configDoc.getContactType());
		masterDoc.setMeta(configDoc.getMeta());

		// Explicitly set lane and sublane
		masterDoc.setLane(configDoc.getLane());
		masterDoc.setSublane(configDoc.getSublane());

		// For WACFB channels, extract and set laneParam2 (wabaId)
		if (CHANNEL_TYPE.WACFB.equalsIgnoreCase(configDoc.getChannelType())) {
			try {
				WacfbPlugin wacfbPlugin = new WacfbPlugin();
				LOGGER.debug("WacfbPlugin instance created successfully");

				String laneParam2 = wacfbPlugin.getLaneParam2(configDoc);
				LOGGER.debug("Extracted laneParam2 (wabaId): {}", laneParam2);

				if (ArgUtil.is(laneParam2)) {
					masterDoc.setLaneParam2(laneParam2);
				} else {
					LOGGER.warn("laneParam2 is null or empty!");
				}
			} catch (Exception e) {
				LOGGER.error("Error while extracting laneParam2 for WACFB channel", e);
			}
		}

		AppContextUtil.clear();
		AppContextUtil.setTenant(Tenants.getDefault());
		AppContextUtil.init();
		save(masterDoc);
	}

	@Async
	public void saveMaster(PrefsConfigDoc prefsConfigDoc) {
		String tnt = AppContextUtil.getTenant();
		if (Tenants.isDefault(tnt)) {
			return;
		}
		PrefsConfigDoc masterDoc = EntityDtoUtil.dtoToEntity(prefsConfigDoc, new PrefsConfigDoc());
		String domain = ArgUtil.nonEmpty(masterDoc.getDomain(), AppContextUtil.getTenant());
		masterDoc.setId(domain + ":" + prefsConfigDoc.getId());
		masterDoc.setDomain(domain);
		AppContextUtil.clear();
		AppContextUtil.setTenant(Tenants.getDefault());
		AppContextUtil.init();
		save(masterDoc, "DUPS_CONFIG_PREFS");
	}

	@Async
	public void saveMaster(FeaturesConfigDoc featureConfigDoc) {
		String tnt = AppContextUtil.getTenant();
		if (Tenants.isDefault(tnt)) {
			return;
		}
		FeaturesConfigDoc masterDoc = EntityDtoUtil.dtoToEntity(featureConfigDoc, new FeaturesConfigDoc());
		String domain = ArgUtil.nonEmpty(masterDoc.getDomain(), AppContextUtil.getTenant());
		masterDoc.setId(domain + ":" + featureConfigDoc.getId());
		masterDoc.setDomain(domain);
		AppContextUtil.clear();
		AppContextUtil.setTenant(Tenants.getDefault());
		AppContextUtil.init();
		save(masterDoc, "DUPS_CONFIG_FEATURES");
	}

}
