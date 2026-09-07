package com.boot.jx.chat;

import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.boot.jx.AppConfig;
import com.boot.jx.AppConfigPackage.AppSharedConfig;
import com.boot.jx.AppConfigPackage.AppSharedConfigChange;
import com.boot.jx.AppContextUtil;
import com.boot.jx.cache.CacheBox;
import com.boot.jx.def.ICacheBox;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.utils.ArgUtil;
import com.boot.utils.JsonUtil;

@Component
public class ChatProxyManager implements AppSharedConfig {

	@Autowired(required = false)
	private RedissonClient redisson;

	private CacheBox<String> proxyManager;

	@Autowired
	private AppConfig appConfig;

	public CacheBox<String> proxy() {
		if (redisson != null && proxyManager == null) {
			// this.proxyManager = new HashMap<String, String>();
			this.proxyManager = CacheBox.getInstance("ChatProxyManager-" + appConfig.getAppType() + "-Proxy-",
					redisson);
		}
		return this.proxyManager;
	}

	public void put(String contactId, String proxy) {
		proxy().put(contactId, proxy);
	}

	public void fastRemove(String contactId) {
		proxy().remove(contactId);
	}

	public String get(String contactId) {
		return proxy().get(contactId);
	}

	// Holder
	private CacheBox<String> holdManager;

	public ICacheBox<String> hold() {
		if (redisson != null && holdManager == null) {
			this.holdManager = CacheBox.getInstance("ChatProxyManager-" + appConfig.getAppType() + "-Hold-", redisson);
		}
		return this.holdManager;
	}

	public void hold(String contactId) {
		hold().put(contactId, "HOLDING");
	}

	public void release(String contactId) {
		hold().put(contactId, "RELEASING");
	}

	public boolean onhold(String contactId) {
		String status = hold().get(contactId);
		return ArgUtil.isEqual(status, "HOLDING");
	}

	// Holder
	private CacheBox<String> firstMessage;

	public ICacheBox<String> firstMessage() {
		if (redisson != null && firstMessage == null) {
			this.firstMessage = CacheBox.getInstance("ChatProxyManager-" + appConfig.getAppType() + "-first-",
					redisson);
		}
		return this.firstMessage;
	}

	public void first(InboxMessage inboxMessageOriginal) {
		if (ArgUtil.is(inboxMessageOriginal.getSessionId())) {
			this.firstMessage().put(inboxMessageOriginal.getSessionId(), JsonUtil.toJson(inboxMessageOriginal));
		}
	}

	public InboxMessage first(String sessionId) {
		if (ArgUtil.is(sessionId)) {
			String x = this.firstMessage().remove(sessionId);
			if (ArgUtil.is(x)) {
				return JsonUtil.parse(x, InboxMessage.class);
			}
		}
		return null;
	}

	@Override
	public void clear(AppSharedConfigChange change) {
		String tnt = AppContextUtil.getTenant();

	}

}
