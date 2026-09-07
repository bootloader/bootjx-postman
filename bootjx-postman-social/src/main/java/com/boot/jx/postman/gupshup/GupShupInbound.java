package com.boot.jx.postman.gupshup;

import java.io.Serializable;

import com.boot.jx.swagger.ApiMockModelProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GupShupInbound implements Serializable {

	private static final long serialVersionUID = -5985194546657716271L;

	@ApiMockModelProperty(example = "919560222091")
	String waNumber;

	@ApiMockModelProperty(example = "919004371797")
	String mobile;

	@ApiMockModelProperty(example = "3900363981641897487")
	String replyId;
	@ApiMockModelProperty(example = "custom Message ID")
	String messageId;

	@ApiMockModelProperty(example = "Hola Amigo")
	String text;

	@ApiMockModelProperty(example = "John Smith")
	String name;

	@ApiMockModelProperty(example = "text", allowableValues = "text,image,document,voice,audio,video,location,contacts")
	String type;

	@ApiMockModelProperty(example = "1564471290000")
	String timestamp;

	MediaObject image;
	MediaObject document;
	MediaObject voice;
	MediaObject audio;
	MediaObject video;

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class MediaObject implements Serializable {
		private static final long serialVersionUID = 8950956506213782726L;

		@JsonProperty("mime_type")
		@ApiMockModelProperty(example = "image/jpeg")
		String mimeType;

		@ApiMockModelProperty(example = "c4f82d0d148dbc31d4e0b107e4057053348e7803a0d6efb168d0ec656f233a5d")
		@JsonProperty("signature")
		String signature;

		@ApiMockModelProperty(example = "https://gs-datareceiver-whatsapp.s3.ap-south-1.amazonaws.com/4366511661603789899_845f8118-6f04-48ae-bbe8-76a81008a725?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20210503T111300Z&X-Amz-SignedHeaders=host&X-Amz-Expires=172799&X-Amz-Credential=AKIAV4FTFRLFCLI4BR77%2F20210503%2Fap-south-1%2Fs3%2Faws4_request&X-Amz-Signature=")
		@JsonProperty("url")
		String url;

		@ApiMockModelProperty(example = "This is a caption message")
		@JsonProperty("caption")
		String caption;

		public String getMimeType() {
			return mimeType;
		}

		public void setMimeType(String mimeType) {
			this.mimeType = mimeType;
		}

		public String getSignature() {
			return signature;
		}

		public void setSignature(String signature) {
			this.signature = signature;
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getCaption() {
			return caption;
		}

		public void setCaption(String caption) {
			this.caption = caption;
		}
	}

	public String getWaNumber() {
		return waNumber;
	}

	public void setWaNumber(String waNumber) {
		this.waNumber = waNumber;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public String getReplyId() {
		return replyId;
	}

	public void setReplyId(String replyId) {
		this.replyId = replyId;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public MediaObject getImage() {
		return image;
	}

	public void setImage(MediaObject image) {
		this.image = image;
	}

	public MediaObject getDocument() {
		return document;
	}

	public void setDocument(MediaObject document) {
		this.document = document;
	}

	public MediaObject getVoice() {
		return voice;
	}

	public void setVoice(MediaObject voice) {
		this.voice = voice;
	}

	public MediaObject getAudio() {
		return audio;
	}

	public void setAudio(MediaObject audio) {
		this.audio = audio;
	}

	public MediaObject getVideo() {
		return video;
	}

	public void setVideo(MediaObject video) {
		this.video = video;
	}
}
