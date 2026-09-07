package com.boot.jx.postman.fb;

import java.io.Serializable;

import com.boot.utils.ArgUtil;
import com.boot.utils.Constants;
import com.boot.utils.StringUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FacebookUserProfile implements Serializable {

	private static final long serialVersionUID = 7610011653823671347L;

	@JsonProperty("name")
	private String name;

	@JsonProperty("first_name")
	private String firstName;

	@JsonProperty("last_name")
	private String lastName;

	@JsonProperty("email")
	private String email;

	@JsonProperty("profile_pic")
	private String profilePic;

	@JsonProperty("locale")
	private String locale;

	@JsonProperty("timezone")
	private Object timezone;

	@JsonProperty("gender")
	private String gender;

	public String getName() {
		if (ArgUtil.is(this.name)) {
			return name;
		}
		return StringUtils.normalizeSpace(ArgUtil.parseAsString(firstName, Constants.BLANK) + " "
				+ ArgUtil.parseAsString(lastName, Constants.BLANK));
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getProfilePic() {
		return profilePic;
	}

	public void setProfilePic(String profilePic) {
		this.profilePic = profilePic;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public Object getTimezone() {
		return timezone;
	}

	public void setTimezone(Object timezone) {
		this.timezone = timezone;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void setName(String name) {
		this.name = name;
	}
}
