package com.boot.jx.postman.model.outbound;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.boot.jx.model.CommonTemplateMeta;
import com.boot.jx.postman.model.Attachment;
import com.boot.jx.postman.model.MessageReferral;
import com.boot.jx.postman.model.MessageTimeout;
import com.boot.jx.postman.model.ext.CommonMsgLocation;
import com.boot.jx.postman.model.ext.CommonMsgOptions;
import com.boot.jx.postman.model.ext.CommonMsgText.OutBoundMsgText;
import com.boot.jx.swagger.ApiMockModelProperty;
import com.boot.model.UtilityModels.JsonIgnoreUnknown;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OutBoundMsgBasic implements Serializable, JsonIgnoreUnknown {
	private static final long serialVersionUID = 8095410793094848402L;

	@JsonProperty("channelId")
	@ApiMockModelProperty(example = "91SERVICENUMBER",
			value = "The ID that identifies the channel over which the message should be sent.")
	public String channelId;

	@JsonProperty("to")
	@ApiMockModelProperty(value = "Either a channel-specific identifier for the receiver"
			+ " (e.g. MSISDN for SMS or WhatsApp channels), or email.")
	public OutBoundContact toContact;

	@ApiMockModelProperty(example = "text", value = "message-type",
			allowableValues = "audio,document,image,location,system,text,video,voice,contacts,template,optin,optout")
	public String type;

	@ApiMockModelProperty(example = "false", value = "Mask the outgoing message data")
	public boolean mask;
	
	@ApiMockModelProperty(example = "campaignId", value = "campaign",
			allowableValues = "Represents the details associated with a specific campaign",required = false)
	public String campaignId;
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class OutBoundMsgActivity implements Serializable {
		private static final long serialVersionUID = -2956206106138171770L;

		@ApiMockModelProperty(required = false)
		public String activityId;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ApiOutBoundMsg extends OutBoundMsgBasic {
		private static final long serialVersionUID = 5358190519262995012L;

		@ApiMockModelProperty(example = "gBEGkYiEB1VXAglK1ZEqA1YKPrU", value = "Messsage to be resent",
				required = false)
		public String messageIdResend;

		@ApiMockModelProperty(required = false)
		public OutBoundMsgText text;

		@ApiMockModelProperty(required = false)
		public OutBoundMsgMedia audio;

		@ApiMockModelProperty(required = false)
		public OutBoundMsgMedia document;

		@ApiMockModelProperty(required = false)
		public OutBoundMsgMedia image;
		// public Media sticker;
		@ApiMockModelProperty(required = false)
		public OutBoundMsgMedia video;

		@ApiMockModelProperty(hidden = false)
		public List<CommonMsgContactCard> contacts;

		@ApiMockModelProperty(hidden = false)
		public CommonMsgLocation location;

		@ApiMockModelProperty(required = false)
		public CommonTemplateMeta template;

		@ApiMockModelProperty(required = false)
		public Map<String, Object> push;

		@ApiMockModelProperty(required = false)
		public OutBoundMsgActivity activity;

		@ApiMockModelProperty(required = false)
		public CommonMsgOptions options;

		@ApiMockModelProperty(required = false)
		public Map<String, Object> style;

		@ApiMockModelProperty(required = false)
		public MessageTimeout timeout;
		
		@ApiMockModelProperty(required = false)
		public List<Attachment> attachments;
		
		@ApiMockModelProperty(required = false)
		public MessageReferral referral;
		
		public OutBoundMsgText getText() {
			return text;
		}

		public void setText(OutBoundMsgText text) {
			this.text = text;
		}

		public OutBoundMsgMedia getAudio() {
			return audio;
		}

		public void setAudio(OutBoundMsgMedia audio) {
			this.audio = audio;
		}

		public OutBoundMsgMedia getDocument() {
			return document;
		}

		public void setDocument(OutBoundMsgMedia document) {
			this.document = document;
		}

		public OutBoundMsgMedia getImage() {
			return image;
		}

		public void setImage(OutBoundMsgMedia image) {
			this.image = image;
		}

		public OutBoundMsgMedia getVideo() {
			return video;
		}

		public void setVideo(OutBoundMsgMedia video) {
			this.video = video;
		}

		public List<CommonMsgContactCard> getContacts() {
			return contacts;
		}

		public void setContacts(List<CommonMsgContactCard> contacts) {
			this.contacts = contacts;
		}

		public CommonMsgLocation getLocation() {
			return location;
		}

		public void setLocation(CommonMsgLocation location) {
			this.location = location;
		}

		public CommonTemplateMeta getTemplate() {
			return template;
		}

		public void setTemplate(CommonTemplateMeta template) {
			this.template = template;
		}

		public CommonMsgOptions getOptions() {
			return options;
		}

		public void setOptions(CommonMsgOptions options) {
			this.options = options;
		}

		public String getMessageIdResend() {
			return messageIdResend;
		}

		public void setMessageIdResend(String messageIdResend) {
			this.messageIdResend = messageIdResend;
		}

		public MessageTimeout getTimeout() {
			return timeout;
		}

		public void setTimeout(MessageTimeout timeout) {
			this.timeout = timeout;
		}
		
		public List<Attachment> getAttachments() {
			return attachments;
		}

		public void setAttachments(List<Attachment> attachments) {
			this.attachments = attachments;
		}
		
		public MessageReferral getReferral() {
			return referral;
		}

		public void setReferral(MessageReferral referral) {
			this.referral = referral;
		}
	}

	public String getChannelId() {
		return channelId;
	}

	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public boolean isMask() {
		return mask;
	}

	public void setMask(boolean mask) {
		this.mask = mask;
	}

	public OutBoundContact getToContact() {
		return toContact;
	}

	public void setToContact(OutBoundContact toContact) {
		this.toContact = toContact;
	}

	public String getCampaignId() {
		return campaignId;
	}

	public void setCampaignId(String campaignId) {
		this.campaignId = campaignId;
	}

}
