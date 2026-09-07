package com.boot.jx.postman.dto;

import com.boot.jx.postman.pbook.PBName;

public class CustomerProfileRequest{

	/**
	 * 
	 */
	
	String id;
	String chatcontactId;
	PBName name;
	String phone;
	String altPhone;
	String email;
	String altEmail;
	String code;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getChatcontactId() {
		return chatcontactId;
	}
	public void setChatcontactId(String chatcontactId) {
		this.chatcontactId = chatcontactId;
	}
	
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}
	public String getAltPhone() {
		return altPhone;
	}
	public void setAltPhone(String altPhone) {
		this.altPhone = altPhone;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getAltEmail() {
		return altEmail;
	}
	public void setAltEmail(String altEmail) {
		this.altEmail = altEmail;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public void setName(PBName name) {
		this.name = name;
	}
	public PBName getName() {
		return name;
	}

}
