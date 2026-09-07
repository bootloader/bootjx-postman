package com.boot.jx.postman.model.outbound;

import java.util.Map;

import com.boot.jx.postman.model.MessageDefinitions.ContactID;
import com.boot.jx.swagger.ApiMockModelProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(as = OutBoundContact.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OutBoundContact implements ContactID {

	private static final long serialVersionUID = -4577777772782792298L;

	@ApiMockModelProperty(value = "Contact Id is unique id for any contact, you will recieve it in Inbound.contact")
	String contactId;

	@ApiMockModelProperty(value = "Phone in caseo of SMS/WHATSAPP")
	String phone;

	@ApiMockModelProperty(value = "Name of to be used will, override the name in message")
	String name;
	
	@ApiMockModelProperty(value = "Email Address for email message.")
	String email;

	@ApiMockModelProperty(required = false, value = "Filter on contact profile/user/device")
	public Map<String, Object> filter;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String phone() {
		return phone;
	}

	public void phone(String phone) {
		this.phone = phone;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getContactId() {
		return contactId;
	}

	public void setContactId(String contactId) {
		this.contactId = contactId;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public Map<String, Object> getFilter() {
		return filter;
	}

	public void setFilter(Map<String, Object> filter) {
		this.filter = filter;
	}

}