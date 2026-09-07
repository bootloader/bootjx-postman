package com.boot.jx.postman;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import com.boot.jx.AppConfig;
import com.boot.jx.AppContextUtil;
import com.boot.jx.agent.AgentConfig;
import com.boot.jx.postman.PMEnvironment.AChannelConfig;
import com.boot.jx.postman.PMEnvironment.PMConfigurationObject;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.scope.tnt.Tenants;
import com.boot.jx.utils.PostManUtil;
import com.boot.model.MapModel.EntryMeta;
import com.boot.model.MapModel.NodeEntry;
import com.boot.model.SafeKeyHashMap;
import com.boot.utils.ArgUtil;
import com.boot.utils.CryptoUtil;
import com.boot.utils.Random;

public interface PMConfiguration extends Serializable {

	public ChannelConfig channel(String channelId);

	PMConfigurationObject prefsEntry(EntryMeta entry);

	public ClientApp clientApiKey(String assignedQueue);

	public NodeEntry<Object> prefsEntry(String string);

	public Set<AChannelConfig> listChannels();

	public List<ClientApp> listApps();

	public static class PMConfigurationModel implements PMConfiguration {

		private static final long serialVersionUID = -5432956433673368768L;

		private Map<String, ChannelConfig> channels;
		private Map<String, ClientApp> clientApiKeys;

		private Map<String, PMConfigurationObject> prefs;
		private Map<String, PMConfigurationObject> perms;
		private Map<String, Object> globalVars;

		private AgentConfig agent;
		private String accountKey;
		private long updateStamp;

		// All Channels
		public SafeKeyHashMap<ChannelConfig> channels() {
			if (ArgUtil.isEmpty(channels)) {
				channels = new HashMap<String, ChannelConfig>();
			}
			return new SafeKeyHashMap<ChannelConfig>(channels);
		}

		public Map<String, ChannelConfig> getChannels() {
			return channels;
		}

		public void setChannels(Map<String, ChannelConfig> channels) {
			this.channels = channels;
		}

		public ChannelConfig channel(String channelId) {
			if (!ArgUtil.is(channelId)) {
				return null;
			}
			ChannelConfig channel = channels().get(channelId);
			if (ArgUtil.is(channel)) {
				return channel;
			}

			channelId = PostManUtil.CHANNEL_ID_FALLBACK(channelId);
			channel = channels().get(channelId);
			if (ArgUtil.is(channel)) {
				return channel;
			}

			return null;
		}

		public PMConfiguration channels(ChannelConfig channel) {
			this.channels().put(channel.getChannelId(), channel);
			this.channels().put(PostManUtil.CHANNEL_ID(channel), channel);
			return this;
		}

		public PMConfiguration channels(ChannelConfig channel, String server) {
			if (ArgUtil.is(channel.getServer())) {
				if (ArgUtil.is(channel.getServer(), server)) {
					return this.channels(channel);
				}
				return this;
			} else {
				ChannelConfig existing = this.channel(channel.getChannelId());
				if (!ArgUtil.is(existing) || !ArgUtil.is(existing.getServer())) {
					return this.channels(channel);
				}
			}
			return this;
		}

		// All A:PI Ckeys
		public SafeKeyHashMap<ClientApp> clientApiKeys() {
			if (ArgUtil.isEmpty(clientApiKeys)) {
				clientApiKeys = new HashMap<String, ClientApp>();
			}
			return new SafeKeyHashMap<ClientApp>(clientApiKeys);
		}
		
		public ClientApp clientApiKey(String apiKey) {
			ClientApp app = clientApiKeys().get(apiKey);
			if (app == null) {
				app = clientApiKeys().get(CryptoUtil.getSHA2HashUnchecked(apiKey));
			}
			return app;
		}

		public PMConfiguration clientApiKey(ClientApp clientApiKey) {
			this.clientApiKeys().put(clientApiKey.getKey(), clientApiKey);
			if (ArgUtil.is(clientApiKey.getQueue())) {
				this.clientApiKeys().put(clientApiKey.getQueue(), clientApiKey);
			}
			if (ArgUtil.is(clientApiKey.getId())) {
				this.clientApiKeys().put(clientApiKey.getId(), clientApiKey);
			}
			return this;
		}

		// Agent
		public AgentConfig agent() {
			if (ArgUtil.isEmpty(agent)) {
				agent = new AgentConfig();
			}
			return agent;
		}

		public AgentConfig getAgent() {
			return agent;
		}

		public void setAgent(AgentConfig agent) {
			this.agent = agent;
		}

		public PMConfiguration agent(AgentConfig agent) {
			this.agent = agent;
			return this;
		}

		// Config
		public Map<String, PMConfigurationObject> getPrefs() {
			return prefs;
		}

		public void setPrefs(Map<String, PMConfigurationObject> prefs) {
			this.prefs = prefs;
		}

		public SafeKeyHashMap<PMConfigurationObject> prefs() {
			if (ArgUtil.isEmpty(prefs)) {
				prefs = new HashMap<String, PMConfigurationObject>();
			}
			return new SafeKeyHashMap<PMConfigurationObject>(prefs);
		}

		public SafeKeyHashMap<PMConfigurationObject> features() {
			if (ArgUtil.isEmpty(perms)) {
				perms = new HashMap<String, PMConfigurationObject>();
			}
			return new SafeKeyHashMap<PMConfigurationObject>(perms);
		}

		@Override
		public PMConfigurationObject prefsEntry(String key) {
			return prefs().getOrDefault(key, new PMConfigurationObject(key, null));
		}

		@Override
		public PMConfigurationObject prefsEntry(EntryMeta entry) {
			return this.prefsEntry(entry.getKey());
		}

		public PMConfigurationObject getPref(String key, Object value) {
			return prefs().getOrDefault(key, new PMConfigurationObject(key, value));
		}

		public PMConfiguration setPref(PMConfigurationObject map) {
			this.prefs().put(map.getKey(), map);
			return this;
		}

		public PMConfiguration setPref(PMConfigurationObject map, String server) {
			if (ArgUtil.is(map.getServer())) {
				if (ArgUtil.is(map.getServer(), server)) {
					return this.setPref(map);
				}
				return this;
			} else {
				PMConfigurationObject existing = this.prefs().get(map.getKey());
				if (!ArgUtil.is(existing) || !ArgUtil.is(existing.getServer())) {
					return this.setPref(map);
				}
			}
			return this;
		}

		public PMConfigurationObject featureEntry(String key) {
			return features().getOrDefault(key, new PMConfigurationObject(key, null));
		}

		public PMConfigurationObject featureEntry(EntryMeta entry) {
			return this.featureEntry(entry.getKey());
		}

		public PMConfigurationObject getFeature(String key, Object value) {
			return features().getOrDefault(key, new PMConfigurationObject(key, value));
		}

		public PMConfiguration setFeature(PMConfigurationObject map) {
			this.features().put(map.getKey(), map);
			return this;
		}

		public PMConfiguration setFeature(PMConfigurationObject map, String server) {
			if (ArgUtil.is(map.getServer())) {
				if (ArgUtil.is(map.getServer(), server)) {
					return this.setFeature(map);
				}
				return this;
			} else {
				PMConfigurationObject existing = this.features().get(map.getKey());
				if (!ArgUtil.is(existing) || !ArgUtil.is(existing.getServer())) {
					return this.setFeature(map);
				}
			}
			return this;
		}

		public Set<AChannelConfig> listChannels() {
			Set<AChannelConfig> list = new TreeSet<AChannelConfig>();
			for (Entry<String, ChannelConfig> aChannelDetails : this.channels().entrySet()) {
				list.add(aChannelDetails.getValue());
			}
			return list;
		}

		@Override
		public List<ClientApp> listApps() {
			List<ClientApp> list = new ArrayList<ClientApp>();
			for (Entry<String, ClientApp> aChannelDetails : this.clientApiKeys().entrySet()) {
				if (!list.contains(aChannelDetails.getValue())) {
					list.add(aChannelDetails.getValue());
				}
			}
			return list;
		}

		public String getAccountKey() {
			if (!ArgUtil.is(this.accountKey)) {
				this.accountKey = Random.randomAlphaNumeric(10);
			}
			return accountKey;
		}

		public void setAccountKey(String accountKey) {
			this.accountKey = accountKey;
		}

		public long getUpdateStamp() {
			return updateStamp;
		}

		public void setUpdateStamp(long updateStamp) {
			this.updateStamp = updateStamp;
		}

		public SafeKeyHashMap<Object> globalVars() {
			if (ArgUtil.isEmpty(globalVars)) {
				globalVars = new HashMap<String, Object>();
			}
			return new SafeKeyHashMap<Object>(globalVars);
		}

	}

	public static PMConfigurationModel instance() {
		return new PMConfigurationModel();
	}

	public static class PMConfigurationWrappper implements PMConfiguration {

		private static final long serialVersionUID = -5108277431528919820L;
		private static PMConfigurationModel DEFAULT = PMConfiguration.instance();

		private PMConfigurationModel local;
		private PMConfigurationModel shared;
		private boolean domainProxy = false;

		private AppConfig appConfig;

		public PMConfigurationWrappper domainProxy() {
			this.domainProxy = true;
			return this;
		}

		public PMConfigurationWrappper local(PMConfigurationModel local) {
			this.local = local;
			return this;
		}

		public PMConfigurationWrappper shared(PMConfigurationModel shared) {
			this.shared = shared;
			return this;
		}

		public PMConfigurationWrappper appConfig(AppConfig appConfig) {
			this.appConfig = appConfig;
			return this;
		}

		public PMConfigurationModel local() {
			return (this.local != null) ? this.local : DEFAULT;
		}

		public PMConfigurationModel shared() {
			return (this.shared != null) ? this.shared : DEFAULT;
		}

		@Override
		public ChannelConfig channel(String channelId) {
			ChannelConfig x = this.local().channel(channelId);
			if (ArgUtil.is(x)) {
				return x;
			}
			if (this.domainProxy) {
				x = this.shared().channel(channelId);
				if (ArgUtil.is(x.getDomainProxy(), AppContextUtil.getTenant())) {
					return x;
				}
				return null;
			} else {
				return this.shared().channel(channelId);
			}
		}

		@Override
		public ClientApp clientApiKey(String assignedQueue) {
			ClientApp x = this.local().clientApiKey(assignedQueue);
			if (ArgUtil.is(x)) {
				return x;
			}
			return this.shared().clientApiKey(assignedQueue);
		}

		@Override
		public PMConfigurationObject prefsEntry(String key) {
			PMConfigurationObject configObject = this.local().prefs().get(key);
			String tnt = AppContextUtil.getTenant();
			if (ArgUtil.isEmpty(configObject) && !Tenants.isDefault(tnt)) {
				PMConfigurationObject sharedConfigObject = this.shared().prefs().get(key);
				if (ArgUtil.is(sharedConfigObject)) {
					return sharedConfigObject;
				}
			}

			if (ArgUtil.isEmpty(configObject)) {
				String value = appConfig.prop(key);
				configObject = new PMConfigurationObject(key, value);
				// this.config().map().put(key, configObject);
			}

			return configObject;
		}

		@Override
		public PMConfigurationObject prefsEntry(EntryMeta entryMeta) {
			return prefsEntry(entryMeta.getKey());
		}

		public PMConfigurationObject featureEntry(String key) {
			PMConfigurationObject configObject = this.local().features().get(key);
			String tnt = AppContextUtil.getTenant();
			if (ArgUtil.isEmpty(configObject) && !Tenants.isDefault(tnt)) {
				PMConfigurationObject sharedConfigObject = this.shared().features().get(key);
				if (ArgUtil.is(sharedConfigObject)) {
					return sharedConfigObject;
				}
			}
			if (ArgUtil.isEmpty(configObject)) {
				String value = appConfig.prop(key);
				configObject = new PMConfigurationObject(key, value);
				// this.config().map().put(key, configObject);
			}
			return configObject;
		}

		public PMConfigurationObject featureEntry(EntryMeta entryMeta) {
			return featureEntry(entryMeta.getKey());
		}

		@Override
		public Set<AChannelConfig> listChannels() {
			Set<AChannelConfig> list = this.local().listChannels();
			if (!Tenants.isDefault(AppContextUtil.getTenant())) {
				Set<AChannelConfig> cs = this.shared().listChannels();
				for (AChannelConfig aChannelConfig : cs) {
					if (aChannelConfig.isShared()
							|| (aChannelConfig.isSandbox() && prefsEntry("postman.chat.channel.sandbox").asBoolean())) {
						list.add(aChannelConfig);
					}
				}
			}
			return list;
		}

		@Override
		public List<ClientApp> listApps() {
			List<ClientApp> list = this.local().listApps();
			if (!Tenants.isDefault(AppContextUtil.getTenant())) {
				List<ClientApp> cs = this.shared().listApps();
				for (ClientApp aChannelConfig : cs) {
					if (aChannelConfig.isShared()) {
						list.add(aChannelConfig);
					}
				}
			}
			return list;
		}

	}

}
