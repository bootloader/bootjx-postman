package com.boot.jx.postman.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.boot.jx.swagger.ApiMockModelProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatProfileDTO implements Serializable {

	private static final long serialVersionUID = 3000520290601093027L;

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ChatUserDTO implements Serializable {
		private static final long serialVersionUID = -4545613020712475879L;
		String name;
		String mobile;
		String email;
		String code;
		String token;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getMobile() {
			return mobile;
		}

		public void setMobile(String mobile) {
			this.mobile = mobile;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public String getToken() {
			return token;
		}

		public void setToken(String token) {
			this.token = token;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ChatUserProfileRequest implements Serializable {
		private static final long serialVersionUID = -4573847059953057257L;
		String contactId;
		String contactType;
		String lane;
		String profileId;
		String mobile;
		String email;

		public String getMobile() {
			return mobile;
		}

		public void setMobile(String mobileNo) {
			this.mobile = mobileNo;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String emailId) {
			this.email = emailId;
		}

		public String getContactId() {
			return contactId;
		}

		public void setContactId(String contactId) {
			this.contactId = contactId;
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

		public String getProfileId() {
			return profileId;
		}

		public void setProfileId(String profileId) {
			this.profileId = profileId;
		}

	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class CustomerLabel implements Serializable {

		private static final long serialVersionUID = 3113597210867635972L;

		@ApiMockModelProperty(example = "CUSTOMER_TYPE", value = "Unique LabelType")
		String key;

		@ApiMockModelProperty(example = "Customer Type",
				value = "Display LabelType, Labels with same name will be grouped together,"
						+ " if not provided key will be used instead",
				required = false)
		String name;

		@ApiMockModelProperty(example = "TEXT", value = "Type of Label", allowableValues = "TEXT,DATE,NUMBER,LIST",
				required = false)
		String type;

		@ApiMockModelProperty(example = "MM/DD/YY", value = "Data Format", allowableValues = "MM/DD/YY",
				required = false)
		String format;

		@ApiMockModelProperty(example = "Platinum", value = "Value of Label Value")
		Object value;

		public Object getValue() {
			return value;
		}

		public void setValue(Object value) {
			this.value = value;
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public CustomerLabel type(String type) {
			this.type = type;
			return this;
		}

		public CustomerLabel value(String value) {
			this.value = value;
			return this;
		}

		public CustomerLabel name(String name) {
			this.name = name;
			return this;
		}

		public CustomerLabel key(String key) {
			this.key = key;
			return this;
		}

		public CustomerLabel format(String format) {
			this.format = format;
			return this;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getFormat() {
			return format;
		}

		public void setFormat(String format) {
			this.format = format;
		}
	}

	String id;
	String code;

	String contactId;

	String profileId;

	String userId;

	String mobile;

	String email;

	String name;

	List<CustomerLabel> labels;

	public ChatProfileDTO label(String key, String value, String name) {
		if (this.labels == null) {
			this.labels = new ArrayList<CustomerLabel>();
		}

		CustomerLabel label = new CustomerLabel();
		label.setKey(key);
		label.setValue(value);
		label.setName(name);

		this.labels.add(label);
		return this;
	}

	public ChatProfileDTO label(CustomerLabel label) {
		if (this.labels == null) {
			this.labels = new ArrayList<CustomerLabel>();
		}
		this.labels.add(label);
		return this;
	}

	public String getContactId() {
		return contactId;
	}

	public void setContactId(String contactId) {
		this.contactId = contactId;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<CustomerLabel> getLabels() {
		return labels;
	}

	public void setLabels(List<CustomerLabel> labels) {
		this.labels = labels;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getProfileId() {
		return profileId;
	}

	public void setProfileId(String profileId) {
		this.profileId = profileId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
	
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ChatInfoDTO implements Serializable {
		private static final long serialVersionUID = -4545613020712475879L;
		String name;
		String phone;
		String email;
		String code;
		String token;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getPhone() {
			return phone;
		}

		public void setPhone(String phone) {
			this.phone = phone;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public String getToken() {
			return token;
		}

		public void setToken(String token) {
			this.token = token;
		}
	}

}
