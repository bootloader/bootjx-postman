package com.boot.jx.postman.gupshup;

import java.io.Serializable;

import com.boot.jx.postman.gupshup.GupShupConstants.Method;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GupShupReq implements Serializable {

	private static final long serialVersionUID = -1435240788130671232L;
	GupShupConstants.Method method;
	String format;
	String userid;
	String password;

	String waNumber;

	@JsonProperty("phone_number")
	String phoneNumber;

	@JsonProperty("send_to")
	String sendTo;

	String v;

	@JsonProperty("auth_scheme")
	String authScheme;

	String channel;

	@JsonProperty("msg")
	String message;

	@JsonProperty("msg_type")
	String messageType;

	@JsonProperty("data_encoding")
	String dataEncoding;

	@JsonProperty("isTemplate")
	Boolean isTemplate;

	@JsonProperty("isHSM")
	Boolean hsm;

	@JsonProperty("msg_id")
	String messageId;

	String extra;

	@JsonProperty("media_url")
	String mediaURL;

	@JsonProperty("media_id")
	String mediaId;

	String caption;

	public GupShupReq() {
		this.v = "1.1";
		this.format = "json";
		this.authScheme = "plain";
	}

	public GupShupReq(GupShupConstants.Method method) {
		this();
		this.method = method;
	}

	public GupShupConstants.Method getMethod() {
		return method;
	}

	public void setMethod(GupShupConstants.Method method) {
		this.method = method;
	}

	public String getUserid() {
		return userid;
	}

	public void setUserid(String userid) {
		this.userid = userid;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getV() {
		return v;
	}

	public void setV(String v) {
		this.v = v;
	}

	public String getAuthScheme() {
		return authScheme;
	}

	public void setAuthScheme(String authScheme) {
		this.authScheme = authScheme;
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public String getSendTo() {
		return sendTo;
	}

	public void setSendTo(String sendTo) {
		this.sendTo = sendTo;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public GupShupReq phoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
		return this;
	}

	public GupShupReq password(String password) {
		this.password = password;
		return this;
	}

	public GupShupReq sendTo(String sendTo) {
		this.sendTo = sendTo;
		return this;
	}

	public GupShupReq message(String message) {
		this.message = message;
		return this;
	}

	public GupShupReq messageType(String messageType) {
		this.messageType = messageType;
		return this;
	}

	public GupShupReq hsm(Boolean isHSM) {
		this.hsm = isHSM;
		return this;
	}

	public String getMessageType() {
		return messageType;
	}

	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}

	public Boolean isHsm() {
		return hsm;
	}

	public void setHsm(Boolean hsm) {
		this.hsm = hsm;
	}

	public String getDataEncoding() {
		return dataEncoding;
	}

	public void setDataEncoding(String dataEncoding) {
		this.dataEncoding = dataEncoding;
	}

	public GupShupReq dataEncoding(String dataEncoding) {
		this.dataEncoding = dataEncoding;
		return this;
	}

	/**
	 * @see GupShupReq#messageId(String)
	 * @param messageId
	 */
	public String getMessageId() {
		return messageId;
	}

	/**
	 * @see GupShupReq#messageId(String)
	 * @param messageId
	 */
	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	/**
	 * A Custom message ID that can be specified by the business. This will be
	 * attached to Message Status Callbacks and can help you track messages using
	 * your internal IDs. 200 characters alphanumeric values are allowed for msg_id
	 * and it must be unique for every message sent. eg:- 134389132153571381
	 */
	public GupShupReq messageId(String messageId) {
		this.messageId = messageId;
		return this;
	}

	/**
	 * @See GupShupReq#extra(String)
	 * 
	 * @param extra
	 */
	public String getExtra() {
		return extra;
	}

	/**
	 * {@link GupShupReq#extra(String)}
	 * 
	 * @param extra
	 */
	public void setExtra(String extra) {
		this.extra = extra;
	}

	/**
	 * A Custom parameter that can be used as an identifier for reporting purposes.
	 * You can input any text in this parameter and the same value will be forwarded
	 * in the Status Callback. 50 alphanumeric characters are allowed for this
	 * parameter. eg :- SUPER100SEGMENT
	 * 
	 * @param extra
	 * @return
	 */
	public GupShupReq extra(String extra) {
		this.extra = extra;
		return this;
	}

	public GupShupReq mediaURL(String mediaURL) {
		this.mediaURL = mediaURL;
		return this;
	}

	public String getMediaURL() {
		return mediaURL;
	}

	public void setMediaURL(String mediaURL) {
		this.mediaURL = mediaURL;
	}

	public GupShupReq caption(String caption) {
		this.caption = caption;
		return this;
	}

	public String getCaption() {
		return caption;
	}

	public void setCaption(String caption) {
		this.caption = caption;
	}

	public GupShupReq mediaId(String mediaId) {
		this.mediaId = mediaId;
		return this;
	}

	public String getMediaId() {
		return mediaId;
	}

	public void setMediaId(String mediaId) {
		this.mediaId = mediaId;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public Boolean getHsm() {
		return hsm;
	}

	public Boolean getIsTemplate() {
		return isTemplate;
	}

	public void setIsTemplate(Boolean isTemplate) {
		this.isTemplate = isTemplate;
	}

	public GupShupReq isTemplate(boolean isTemplate) {
		this.isTemplate = isTemplate;
		return this;
	}

	public GupShupReq method(Method method) {
		this.method = method;
		return this;
	}

	public String getWaNumber() {
		return waNumber;
	}

	public void setWaNumber(String waNumber) {
		this.waNumber = waNumber;
	}
}
