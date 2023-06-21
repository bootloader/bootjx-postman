package com.boot.jx.postman.pbook;

import java.io.Serializable;
import java.util.Arrays;

import com.boot.model.UtilityModels.JsonIgnoreUnknown;
import com.boot.utils.ArgUtil;
import com.boot.utils.StringUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PBName implements Serializable, JsonIgnoreUnknown {
	private static final long serialVersionUID = -2023402204499331768L;
	public String firstName;
	public String middleName;
	public String lastName;
	public String formattedName;

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getFormattedName() {
		return formattedName;
	}

	public void setFormattedName(String formattedName) {
		this.formattedName = formattedName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public PBName fix() {
		if (ArgUtil.not(formattedName)) {
			this.formattedName = StringUtils.join(" ", firstName, middleName, lastName);
		} else if (ArgUtil.is(this.formattedName) && ArgUtil.not(firstName)) {
			String[] names = this.formattedName.split(" ");
			this.firstName = names[0];
			if (names.length >= 2) {
				int lastIndex = names.length - 1;
				this.lastName = names[lastIndex];
				if (names.length > 2) {
					this.middleName = StringUtils.join(" ", Arrays.copyOfRange(names, 1, lastIndex));
				}
			}

		}
		return this;
	}

	public String getMiddleName() {
		return middleName;
	}

	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}
}
