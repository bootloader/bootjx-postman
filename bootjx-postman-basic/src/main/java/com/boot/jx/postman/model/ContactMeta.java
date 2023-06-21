package com.boot.jx.postman.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.boot.jx.dict.Language;
import com.boot.jx.postman.model.MessageDefinitions.Contactable;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ContactMeta implements java.io.Serializable, Contactable {

	private static final long serialVersionUID = -2229330167964350550L;
	String tenant;
	String country;
	String userid;
	String prefix;
	String phone;
	String email;
	String name;
	Language lang;

	private String contactType;
	private String channelType;
	private String lane;
	private String csid;
	private String contactId;

	List<Map<String, Object>> filter;
	List<Map<String, String>> keymap;

	public ContactMeta() {
		super();
	}

	public Language getLang() {
		return lang;
	}

	public void setLang(Language lang) {
		this.lang = lang;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String mobile) {
		this.phone = mobile;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getTenant() {
		return tenant;
	}

	public void setTenant(String tenant) {
		this.tenant = tenant;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getUserid() {
		return userid;
	}

	public void setUserid(String userid) {
		this.userid = userid;
	}

	public ContactMeta prefix(String prefix) {
		this.prefix = prefix;
		return this;
	}

	public ContactMeta mobile(String mobile) {
		this.phone = mobile;
		return this;
	}

	public ContactMeta email(String email) {
		this.email = email;
		return this;
	}

	public List<Map<String, String>> getKeymap() {
		return keymap;
	}

	public void setKeymap(List<Map<String, String>> keymap) {
		this.keymap = keymap;
	}

	public List<Map<String, Object>> getFilter() {
		return filter;
	}

	public void setFilter(List<Map<String, Object>> filter) {
		this.filter = filter;
	}

	public List<Map<String, Object>> filter() {
		if (this.filter == null) {
			this.filter = new ArrayList<Map<String, Object>>();
		}
		return this.filter;
	}

	public List<Map<String, String>> keymap() {
		if (this.keymap == null) {
			this.keymap = new ArrayList<Map<String, String>>();
		}
		return this.keymap;
	}

	public ContactMeta or(Map<String, Object> or) {
		this.filter().add(or);
		return this;
	}

	public String getContactType() {
		return contactType;
	}

	public void setContactType(String contactType) {
		this.contactType = contactType;
	}

	public String getLane() {
		return lane;
	}

	public void setLane(String lane) {
		this.lane = lane;
	}

	public String getCsid() {
		return csid;
	}

	public void setCsid(String csid) {
		this.csid = csid;
	}

	public String getContactId() {
		return contactId;
	}

	public void setContactId(String contactId) {
		this.contactId = contactId;
	}

	public String getChannelType() {
		return channelType;
	}

	public void setChannelType(String channelType) {
		this.channelType = channelType;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
