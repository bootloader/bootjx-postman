package com.boot.jx.postman.model;

import java.io.Serializable;
import java.util.List;

import com.boot.jx.tunnel.ITunnelDefs.ITunnelEvent;

/**
 * Event published when Meta WhatsApp API returns payment errors (131042).
 * Used to pause campaigns, block channels, and alert admins.
 */
public class PaymentAlertEvent implements ITunnelEvent {
	
	private static final long serialVersionUID = 1L;
	
	private String channelId;
	private String campaignId;
	private String errorCode;
	private String errorMessage;
	private List<String> pausedCampaignIds;
	private long timestamp;
	
	public PaymentAlertEvent() {
		this.timestamp = System.currentTimeMillis();
	}
	
	public String getChannelId() {
		return channelId;
	}
	
	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}
	
	public String getCampaignId() {
		return campaignId;
	}
	
	public void setCampaignId(String campaignId) {
		this.campaignId = campaignId;
	}
	
	public String getErrorCode() {
		return errorCode;
	}
	
	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}
	
	public String getErrorMessage() {
		return errorMessage;
	}
	
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	
	public List<String> getPausedCampaignIds() {
		return pausedCampaignIds;
	}
	
	public void setPausedCampaignIds(List<String> pausedCampaignIds) {
		this.pausedCampaignIds = pausedCampaignIds;
	}
	
	public long getTimestamp() {
		return timestamp;
	}
	
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	/**
	 * Get user-friendly explanation for error code.
	 */
	public String getExplanation() {
		if ("131042".equals(errorCode)) {
			return "Your WhatsApp Business Account requires a valid payment method. "
				+ "Please add or update payment details in your Meta Business Manager.";
		}
		return "Payment-related issue with your WhatsApp Business Account.";
	}
	
	@Override
	public String toString() {
		return String.format("PaymentAlertEvent[channel=%s, campaign=%s, code=%s, cleared=%s]",
				channelId, campaignId, errorCode);
	}
}

