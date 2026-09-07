package com.boot.jx.postman.store;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import com.boot.jx.AppConfigPackage.AppSharedConfigChange;
import com.boot.jx.mongo.CommonMongoQB.MQB;
import com.boot.jx.mongo.MongoStore;
import com.boot.jx.postman.doc.PMConfigurationDoc;
import com.boot.jx.postman.doc.config.ChannelConfigDoc;
import com.boot.jx.postman.doc.config.ChannelConfigDupsDoc;
import com.boot.jx.postman.doc.config.ClientAppConfigDoc;
import com.boot.jx.postman.doc.config.FeaturesConfigDoc;
import com.boot.jx.postman.doc.config.PrefsConfigDoc;
import com.boot.jx.postman.doc.config.VarsConfigDoc;
import com.boot.jx.utils.PostManUtil;
import com.boot.utils.ArgUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mongodb.client.result.DeleteResult;

@Component
public class ConfigMaster extends MongoStore {

	private Cache<String, List<ChannelConfigDupsDoc>> channelList = CacheBuilder.newBuilder().maximumSize(1000)
			.expireAfterWrite(1, TimeUnit.HOURS).build();

	@Autowired(required = false)
	private ConfigStore configStore;

	public void saveChannelConfig(ChannelConfigDoc configDoc) {
		configStore.saveChannelConfig(configDoc);
		configStore.saveMaster(configDoc);
	}

	public void saveChannelConfig(ChannelConfigDoc configDoc, String action) {
		configStore.saveChannelConfig(configDoc, action);
		configStore.saveMaster(configDoc);
	}

	public DeleteResult remove(ChannelConfigDoc configDoc) {
		DeleteResult r = configStore.remove(configDoc);
		configDoc.setDeleted(true);
		configDoc.setDisabled(true);
		configStore.saveMaster(configDoc);
		return r;
	}

	public void savePrefsConfig(PrefsConfigDoc prefsConfigDoc) {
		configStore.savePrefsConfig(prefsConfigDoc);
		configStore.saveMaster(prefsConfigDoc);
	}

	public void saveConfiguration(PMConfigurationDoc doc) {
		configStore.saveConfiguration(doc);
	}

	public Optional<String> saveClientKeyConfig(ClientAppConfigDoc clientApiKey) {
		return configStore.saveClientKeyConfig(clientApiKey);
	}

	public <T extends VarsConfigDoc> void saveCompanyVar(T companyVarsConfig) {
		configStore.saveCompanyVar(companyVarsConfig);
	}

	public void saveFeatureConfig(FeaturesConfigDoc permConfigDoc) {
		configStore.saveFeatureConfig(permConfigDoc);
		configStore.saveMaster(permConfigDoc);
	}

	public List<ChannelConfigDupsDoc> getChannelMeta(String channelType, String lane, String sublane) {
		String channelId = PostManUtil.CHANNEL_ID(channelType, lane);
		List<ChannelConfigDupsDoc> channels = channelList.getIfPresent(channelId);

		String channelSubId = null;
		if (ArgUtil.is(sublane) && (!ArgUtil.is(channels) || channels.size() < 1)) {
			channelSubId = PostManUtil.CHANNEL_ID(channelType, sublane);
			channels = channelList.getIfPresent(channelSubId);
		}

		if (!ArgUtil.is(channels) || channels.size() < 1) {
			channels = configStore.find(MQB.collection(ChannelConfigDupsDoc.class)
					.where(Criteria.where("lane").is(lane).and("isDisabled").is(false).and("isDeleted").is(false)
							.and("channelType").is(channelType).and("domain").ne("nodomain")));
			if (ArgUtil.is(channels)) {
				channelList.put(channelId, channels);
			}
		}

		if (ArgUtil.is(sublane) && (!ArgUtil.is(channels) || channels.size() < 1)) {
			channels = configStore.find(MQB.collection(ChannelConfigDupsDoc.class)
					.where(Criteria.where("sublane").is(sublane).and("isDisabled").is(false).and("isDeleted").is(false)
							.and("channelType").is(channelType).and("domain").ne("nodomain")));
			if (ArgUtil.is(channels)) {
				channelList.put(channelSubId, channels);
			}
		}

		return channels;
	}
	
	/**
	 * Get channel metadata with laneParam2 (wabaId) fallback for WACFB channels.
	 * 
	 * Template status webhooks do not contain phone number or phone number ID.
	 * This method provides fallback routing using laneParam2 (wabaId).
	 * 
	 * Tries in order: lane → sublane → laneParam2
	 * 
	 * @param channelType Channel type (e.g., "wacfb")
	 * @param lane Phone number (primary routing)
	 * @param sublane Phone number ID (secondary routing)
	 * @param laneParam2 WABA ID for WACFB channels (tertiary routing)
	 * @return List of matching channels
	 */
	public List<ChannelConfigDupsDoc> getChannelMeta(String channelType, String lane, String sublane, String laneParam2) {
		// First, try existing logic (lane and sublane)
		List<ChannelConfigDupsDoc> channels = getChannelMeta(channelType, lane, sublane);
		
		// If found by lane or sublane, return immediately
		if (ArgUtil.is(channels) && channels.size() > 0) {
			return channels;
		}
		
		// FALLBACK: Try laneParam2 (wabaId) for template status webhooks
		if (ArgUtil.is(laneParam2)) {
			String channelParam2Id = PostManUtil.CHANNEL_ID(channelType, laneParam2);
			channels = channelList.getIfPresent(channelParam2Id);
			
			// Query DB if not in cache
			if (!ArgUtil.is(channels) || channels.size() < 1) {
				channels = configStore.find(MQB.collection(ChannelConfigDupsDoc.class)
					.where(Criteria.where("laneParam2").is(laneParam2)
						.and("isDisabled").is(false)
						.and("isDeleted").is(false)
						.and("channelType").is(channelType)
						.and("domain").ne("nodomain")));
				
				if (ArgUtil.is(channels)) {
					channelList.put(channelParam2Id, channels);
				}
			}
		}
		
		return channels;
	}
	
	public void clearChannelMeta(AppSharedConfigChange change) {
		if (ArgUtil.is(change.getConfigType(), ChannelConfigDoc.DOCUMENT_NAME)) {
			channelList.invalidate(change.getConfigId());
		}
	}

}
