package com.boot.jx.postman.dto;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CampaignDto implements Serializable {
	private static final long serialVersionUID = -8534094564803770857L;
	private String campaignId;
	private String campaignTitle;
	private String templateCode;
	private Enum campaignMode;
	private String channelId;
	private String contactType;

	/** optional audit:RFMM | RSMM | RAMM; set only when passed */
	private String resendType;

	public String getCampaignId() {
		return campaignId;
	}

	public void setCampaignId(String campaignId) {
		this.campaignId = campaignId;
	}

	public String getCampaignTitle() {
		return campaignTitle;
	}

	public void setCampaignTitle(String campaignTitle) {
		this.campaignTitle = campaignTitle;
	}

	public String getTemplateCode() {
		return templateCode;
	}

	public void setTemplateCode(String templateCode) {
		this.templateCode = templateCode;
	}

	public Enum getCampaignMode() {
		return campaignMode;
	}

	public void setCampaignMode(Enum campaignMode) {
		this.campaignMode = campaignMode;
	}

	public String getContactType() {
		return contactType;
	}

	public void setContactType(String contactType) {
		this.contactType = contactType;
	}

	public String getChannelId() {
		return channelId;
	}

	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}

	public String getResendType() {
		return resendType;
	}

	public void setResendType(String resendType) {
		this.resendType = resendType;
	}
}
