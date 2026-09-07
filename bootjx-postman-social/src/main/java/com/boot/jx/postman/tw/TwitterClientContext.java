package com.boot.jx.postman.tw;

import com.boot.utils.ArgUtil;

import twitter4j.DirectMessage;
import twitter4j.DirectMessageList;
import twitter4j.RateLimitStatus;
import twitter4j.Twitter;
import twitter4j.TwitterException;

public class TwitterClientContext {

	private Twitter twitter;
	private long dmSinceId = 0;
	private String dmCursor = null;
	private long myId = -1;
	private long lastAccessStamp;
	RateLimitStatus rateLimit;
	private int lastLotSize;
	private WebhookManager webhookManager;
	private WebhookInfo webhookInfo;

	public TwitterClientContext(Twitter twitter) {
		super();
		this.twitter = twitter;
	}

	public long getDmSinceId() {
		return dmSinceId;
	}

	public void setDmSinceId(long dmSinceId) {
		this.dmSinceId = dmSinceId;
	}

	public String getDmCursor() {
		return dmCursor;
	}

	public void setDmCursor(String dmCursor) {
		this.dmCursor = dmCursor;
	}

	public Twitter getTwitter() {
		return twitter;
	}

	public void setTwitter(Twitter twitter) {
		this.twitter = twitter;
	}

	public DirectMessageList removeDMsNotSentToMe(DirectMessageList list) throws TwitterException {
		if (this.myId == -1) {
			this.myId = this.twitter.verifyCredentials().getId();
		}

		if (!ArgUtil.is(list)) {
			return null;
		}

		// filter direct messages not sent by me
		boolean older = false;
		for (int i = list.size() - 1; i >= 0; i--) {
			DirectMessage dm = list.get(i);
			if (dm.getRecipientId() != this.myId) {
				list.remove(i);
			} else if (dm.getId() == this.dmSinceId && older) {
				list.remove(i);
				older = true;
				continue;
			}
			this.dmSinceId = Math.max(this.dmSinceId, dm.getId());
		}
		this.lastLotSize = list.size();
		return list;
	}

	private DirectMessageList retainCursor(DirectMessageList list) {
		dmCursor = list.getNextCursor();
		lastAccessStamp = System.currentTimeMillis();
		rateLimit = list.getRateLimitStatus();
		return list;
	}

	public boolean isAvailable() {
		if (ArgUtil.is(rateLimit)) {
			if (rateLimit.getRemaining() == 0) {
				long secondsWaited = (System.currentTimeMillis() - lastAccessStamp) / 1000;
				if (rateLimit.getSecondsUntilReset() > secondsWaited) {
					return false;
				}
			}
		}
		return true;
	}

	public DirectMessageList getDirectMessagesReceived(int i) throws TwitterException {
		if (!isAvailable()) {
			return null;
		}
		if (ArgUtil.isEmpty(this.dmCursor)) {
			return retainCursor(removeDMsNotSentToMe(this.getTwitter().getDirectMessages(i)));
		} else {
			return retainCursor(removeDMsNotSentToMe(this.getTwitter().getDirectMessages(i, this.dmCursor)));
		}
	}

	public WebhookManager getWebhookManager() {
		return webhookManager;
	}

	public void setWebhookManager(WebhookManager webhookManager) {
		this.webhookManager = webhookManager;
	}

	public void setWebhookInfo(WebhookInfo webhookInfo) {
		this.webhookInfo = webhookInfo;
	}

	public WebhookInfo getWebhookInfo() {
		return webhookInfo;
	}

	public StatusCode registerWebhook(String webhookUrl) {
		this.webhookInfo = this.webhookManager.getWebhookInfo();
		if (ArgUtil.is(this.webhookInfo) && this.webhookInfo.isValid()
				&& webhookUrl.equals(this.webhookInfo.getUrl())) {
			return StatusCode.OK;
		}
		webhookManager.removeWebhook();
		StatusCode statusCode = webhookManager.addWebhook(java.net.URI.create(webhookUrl));
		if(!statusCode.isError)
		    webhookManager.registerCurrentUser();
		return statusCode;
	}
}
