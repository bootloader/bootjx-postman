package com.boot.jx.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.boot.jx.postman.ClientApp;
import com.boot.jx.postman.PMConstants;
import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.model.ext.SessionBoundEvent;
import com.boot.utils.ArgUtil;

@Component
public class ChatUtility {

	private static final Logger LOGGER = LoggerFactory.getLogger(ChatUtility.class);

	public boolean isPushOnly(ChatSessionDoc chatSessionDoc) {
		if (ArgUtil.isEmptyValue(chatSessionDoc.getAssignedToQueue())) {
			return true;
		}
		if (PMConstants.CHAT_MODE.isPushOnly(chatSessionDoc.getMode())) {
			return true;
		}
		return false;
	}

	public boolean isPushOnly(ClientApp app) {
		if (PMConstants.CHAT_MODE.isPushOnly(app.getAppMode())) {
			return true;
		}
		return false;
	}

	public boolean isCampaignPossible(ClientApp app) {
		if (PMConstants.CHAT_MODE.WEBHOOK.toString().equals(app.getAppMode())) {
			return true;
		}
		return false;
	}

	public boolean inQueue(ChatSessionDoc chatSessionDoc) {
		return ArgUtil.is(chatSessionDoc.getAssignedToQueue());
	}

	// Publish
	public boolean isPublishSessionBoundEventPostOutbound(ClientApp app) {
		if (isPushOnly(app) || isCampaignPossible(app)) { // For Campaigns
			return true;
		}

		return false;
	}

	public boolean isPublishSessionBoundEventPostInbound(ClientApp app) {
		if (isPushOnly(app) || isCampaignPossible(app)) { // For Campaigns
			return true;
		}
		return false;
	}

	public boolean isPublishSessionBoundEventPostInbound(ClientApp app, SessionBoundEvent inBoundEvent) {
		if (isPushOnly(app)) { // For Campaigns
			return true;
		}

		if (ArgUtil.is(inBoundEvent.sessionBulkId)) {
			return true;
		}

		return false;
	}

	public boolean isPublishSessionBoundEventPostStatus(ClientApp app) {
		if (isPushOnly(app) || isCampaignPossible(app)) { // For Campaigns
			return true;
		}

		return false;
	}

	public boolean isPublishSessionBoundEventPostStatus(ClientApp app, SessionBoundEvent inBoundEvent) {
		if (isPushOnly(app)) { // For Campaigns
			return true;
		}
		if (ArgUtil.is(inBoundEvent.sessionBulkId)) {
			return true;
		}
		return false;
	}

}
