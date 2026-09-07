package com.boot.jx.postman.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.boot.jx.swagger.ApiMockModelProperty;
import com.boot.utils.ArgUtil;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageReferral implements Serializable {
	private static final long serialVersionUID = 1875887497925865671L;

	String messageId;
	String messageIdExt;

	@JsonAlias({ "bulkId", "bulkSessionId" })
	String bulkId;

	String sourceUrl;
	String sourceId;
	
	String channelId;
	String sessionId;
	
	@ApiMockModelProperty(example = "campaign", value = "Category of Referral Source",
			allowableValues = "message,social,ads,campaign")
	String sourceCategory;

	@ApiMockModelProperty(example = "feedback", value = "Type of Referral Source",
			allowableValues = "inbound,outbound,feedback,story,post")
	String sourceType;

	String title;
	String body;
	String templateCode;

	String mediaType;
	String mediaUrl;
	String thumbUrl;
	String imageUrl;
	String mediaName;
	String mediaCaption;

	private Map<String, Object> info;

	public void putAll(MessageReferral referral) {
		this.messageId = ArgUtil.nonEmpty(referral.messageId, this.messageId);
		this.messageIdExt = ArgUtil.nonEmpty(referral.messageIdExt, this.messageIdExt);
		this.channelId = ArgUtil.nonEmpty(referral.channelId, this.channelId);
		this.sessionId = ArgUtil.nonEmpty(referral.sessionId, this.sessionId);
		this.bulkId = ArgUtil.nonEmpty(referral.bulkId, this.bulkId);
		this.sourceUrl = ArgUtil.nonEmpty(referral.sourceUrl, this.sourceUrl);
		this.sourceId = ArgUtil.nonEmpty(referral.sourceId, this.sourceId);
		this.sourceCategory = ArgUtil.nonEmpty(referral.sourceCategory, this.sourceCategory);
		this.sourceType = ArgUtil.nonEmpty(referral.sourceType, this.sourceType);
		this.sourceId = ArgUtil.nonEmpty(referral.sourceId, this.sourceId);
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public String getSourceUrl() {
		return sourceUrl;
	}

	public void setSourceUrl(String sourceUrl) {
		this.sourceUrl = sourceUrl;
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	public String getSourceType() {
		return sourceType;
	}

	public void setSourceType(String sourceType) {
		this.sourceType = sourceType;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}
	
	public String getTemplateCode() {
		return templateCode;
	}

	public void setTemplateCode(String templateCode) {
		this.templateCode = templateCode;
	}

	public Map<String, Object> info() {
		if (this.info == null) {
			this.info = new HashMap<String, Object>();
		}
		return this.info;
	}

	@Override
	public String toString() {
		return "MessageReferral{ \n" + "sourceUrl=" + sourceUrl + ", sourceId=" + sourceId + ", sourceType="
				+ sourceType + ", body=" + body + "}";
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getMediaType() {
		return mediaType;
	}

	public void setMediaType(String mediaType) {
		this.mediaType = mediaType;
	}

	public String getMediaUrl() {
		return mediaUrl;
	}

	public void setMediaUrl(String mediaUrl) {
		this.mediaUrl = mediaUrl;
	}

	public String getThumbUrl() {
		return thumbUrl;
	}

	public void setThumbUrl(String thumbUrl) {
		this.thumbUrl = thumbUrl;
	}

	public Map<String, Object> getInfo() {
		return info;
	}

	public void setInfo(Map<String, Object> info) {
		this.info = info;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public String getMessageIdExt() {
		return messageIdExt;
	}

	public void setMessageIdExt(String messageIdExt) {
		this.messageIdExt = messageIdExt;
	}
	
	public String getChannelId() {
		return channelId;
	}

	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getSourceCategory() {
		return sourceCategory;
	}

	public void setSourceCategory(String sourceCategory) {
		this.sourceCategory = sourceCategory;
	}

	public String getBulkId() {
		return bulkId;
	}

	public void setBulkId(String bulkId) {
		this.bulkId = bulkId;
	}

	public String getMediaName() {
		return mediaName;
	}

	public void setMediaName(String mediaName) {
		this.mediaName = mediaName;
	}

	public String getMediaCaption() {
		return mediaCaption;
	}

	public void setMediaCaption(String mediaCaption) {
		this.mediaCaption = mediaCaption;
	}

}
