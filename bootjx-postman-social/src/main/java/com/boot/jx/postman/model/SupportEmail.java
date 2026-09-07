package com.boot.jx.postman.model;

public class SupportEmail extends Email {

	private static final long serialVersionUID = -2802497711144618686L;
	private String visitorName = null;
	private String visitorEmail = null;
	private String visitorPhone = null;
	private String visitorMessage = null;
	private String captchaCode = null;
	private String identity = null;

	public SupportEmail() {
		super();
	}

	public String getVisitorName() {
		return visitorName;
	}

	public void setVisitorName(String visitorName) {
		this.visitorName = visitorName;
	}

	public String getVisitorEmail() {
		return visitorEmail;
	}

	public void setVisitorEmail(String visitorEmail) {
		this.visitorEmail = visitorEmail;
	}

	public String getVisitorPhone() {
		return visitorPhone;
	}

	public void setVisitorPhone(String visitorPhone) {
		this.visitorPhone = visitorPhone;
	}

	public String getVisitorMessage() {
		return visitorMessage;
	}

	public void setVisitorMessage(String visitorMessage) {
		this.visitorMessage = visitorMessage;
	}

	public String getCaptchaCode() {
		return captchaCode;
	}

	public void setCaptchaCode(String captchaCode) {
		this.captchaCode = captchaCode;
	}

	public String getIdentity() {
		return identity;
	}

	public void setIdentity(String identity) {
		this.identity = identity;
	}

}