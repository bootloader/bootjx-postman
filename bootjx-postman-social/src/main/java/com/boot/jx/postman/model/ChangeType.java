package com.boot.jx.postman.model;

public enum ChangeType {

	PASSWORD_CHANGE("password_change"), 
	SECURITY_QUESTION_CHANGE("secutiry_que_change"), 
	IMAGE_CHANGE("image_change"), 
	MOBILE_CHANGE("mobile_change"),
	EMAIL_CHANGE("email_change"),;

	String fileName;

	public String getFileName() {
		return fileName;
	}

	ChangeType(String fileName) {
		this.fileName = fileName;
	}
}

