package com.boot.jx.postman.dto;

import java.io.Serializable;

public class ContactPrefsDTO implements Serializable {

	private static final long serialVersionUID = -7190205572166712885L;

	private String lang;

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

}
