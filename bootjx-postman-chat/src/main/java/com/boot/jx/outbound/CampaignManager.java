package com.boot.jx.outbound;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.boot.jx.AppConfigPackage.AppSharedConfig;
import com.boot.jx.AppConfigPackage.AppSharedConfigChange;
import com.boot.jx.AppConfigPackage.SharedConfigChangeBuilder;
import com.boot.jx.AppContextUtil;
import com.boot.jx.api.ApiFieldError;
import com.boot.jx.api.ApiResponseUtil;
import com.boot.jx.cache.TenantAwareCacheLoader;
import com.boot.jx.def.TenantAwareKey;
import com.boot.jx.mongo.MongoStore;
import com.boot.jx.postman.doc.BulkSessionDoc;
import com.boot.jx.postman.store.TemplateStore;
import com.boot.utils.ArgUtil;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheLoader.InvalidCacheLoadException;
import com.google.common.cache.LoadingCache;

@Component
public class CampaignManager implements AppSharedConfig {

	private static final Logger LOGGER = LoggerFactory.getLogger(TemplateStore.class);

	// Need to update toime based on schedule update
	private final LoadingCache<TenantAwareKey, BulkSessionDoc> cacheCampaign = CacheBuilder.newBuilder()
			.maximumSize(1_000).expireAfterWrite(10, TimeUnit.MINUTES)
			.build(TenantAwareCacheLoader.from(this::loadCampaign));

	@Override
	public void clear(AppSharedConfigChange change) {
		if (ArgUtil.is(change.getConfigId())) {
			String tenant = AppContextUtil.getTenant();
			cacheCampaign.invalidateAll(cacheCampaign.asMap().keySet().stream()
					.filter(k -> k.tenant().equals(tenant) && k.code().equals(change.getConfigId()))
					.collect(Collectors.toList()));
		} else if (ArgUtil.is(change.getName(), name())) {
			String tenant = AppContextUtil.getTenant();
			cacheCampaign.invalidateAll(cacheCampaign.asMap().keySet().stream().filter(k -> k.tenant().equals(tenant))
					.collect(Collectors.toList()));
		}
	}

	private BulkSessionDoc loadCampaign(TenantAwareKey cacheKey) {
		String tenant = cacheKey.tenant();
		String scheduleCode = cacheKey.code();
		BulkSessionDoc schedule = findById(scheduleCode);
		if (schedule == null) {
			throw new CacheLoader.InvalidCacheLoadException("Campaign not found " + scheduleCode);
		}
		return schedule;
	}

	@Autowired
	protected MongoStore mongoStore;

	public BulkSessionDoc findById(String campaignId) {
		return mongoStore.readOnly().findByIdString(campaignId, BulkSessionDoc.class);
	}

	public BulkSessionDoc getById(String campaignId) {
		if (campaignId == null) {
			ApiResponseUtil.throwMissinInputException(new ApiFieldError().field("schedule"));
			return null;
		}
		try {
			TenantAwareKey key = TenantAwareKey.fromCode(campaignId);
			return cacheCampaign.getUnchecked(key);
		} catch (InvalidCacheLoadException e) {
			return null;
		}
	}

	public void saveAndPublish(BulkSessionDoc doc) {
		mongoStore.save(doc);
		publishUpdate(SharedConfigChangeBuilder.newChange().id(doc.getBulkSessionId()).build());
	}

	public void saveWithoutPublish(BulkSessionDoc doc) {
		mongoStore.save(doc);
	}

}
