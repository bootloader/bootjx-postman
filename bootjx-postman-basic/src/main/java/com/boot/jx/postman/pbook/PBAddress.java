package com.boot.jx.postman.pbook;

import java.io.Serializable;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.boot.model.UtilityModels.JsonIgnoreUnknown;
import com.boot.model.UtilityModels.UniqueIndex;
import com.boot.utils.ArgUtil;
import com.boot.utils.Constants;
import com.boot.utils.StringUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PBAddress implements Serializable, Comparable<PBAddress>, JsonIgnoreUnknown, UniqueIndex<PBAddress> {
	private static final long serialVersionUID = -1967541446184852363L;
	public String uuid;
	public String type;
	public String street;
	public String city;
	public String state;
	public String zip;
	public String country;
	public String countryCode;

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getZip() {
		return zip;
	}

	public void setZip(String zip) {
		this.zip = zip;
	}

	public PBAddress country(String country) {
		this.country = country;
		return this;
	}

	@Override
	public String toString() {
		return StringUtils.join(Constants.BLANK, street, city, state, zip, country, countryCode);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 31).append(this.toString()).toHashCode();
	}

	@Override
	public int compareTo(PBAddress o) {
		if (o == null) {
			return 1;
		}
		return this.toString().compareTo(o.toString());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		PBAddress that = (PBAddress) o;

		if (ArgUtil.equalsIgnoreCase(this.city, that.city)) {
			return false;
		}
		if (ArgUtil.equalsIgnoreCase(this.country, that.country)) {
			return false;
		}
		if (ArgUtil.equalsIgnoreCase(this.countryCode, that.countryCode)) {
			return false;
		}
		if (ArgUtil.equalsIgnoreCase(this.state, that.state)) {
			return false;
		}
		if (ArgUtil.equalsIgnoreCase(this.street, that.street)) {
			return false;
		}
		if (ArgUtil.equalsIgnoreCase(this.zip, that.zip)) {
			return false;
		}
		return true;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	@Override
	public String uuid() {
		return uuid;
	}

	@Override
	public String uuid(String uuid) {
		if (ArgUtil.not(this.uuid)) {
			this.uuid = uuid;
		}
		return this.uuid;
	}

	@Override
	public PBAddress update(PBAddress fromObject) {
		this.type = fromObject.getType();
		this.street = fromObject.getStreet();
		this.city = fromObject.getCity();
		this.state = fromObject.getState();
		this.zip = fromObject.getZip();
		this.country = fromObject.getCountry();
		this.countryCode = fromObject.getCountryCode();
		return this;
	}
}
