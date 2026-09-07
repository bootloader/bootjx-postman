package com.boot.jx.postman.model;

import java.io.Serializable;

public class GeoLocation implements Serializable {

	private static final long serialVersionUID = 4616644115138392356L;

	private String ipAddress = null;
	private String cityId = null;
	private String cityName = null;
	private String stateCode = null;
	private String countryCode = null;
	private String continentCode = null;
	private String tenant = null;

	public String getTenant() {
		return tenant;
	}

	public void setTenant(String tenant) {
		this.tenant = tenant;
	}

	public GeoLocation() {
	}
	public GeoLocation(String ip) {
		this.ipAddress = ip;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getCityId() {
		return cityId;
	}

	public void setCityId(String cityId) {
		this.cityId = cityId;
	}

	public String getCityName() {
		return cityName;
	}

	public void setCityName(String cityName) {
		this.cityName = cityName;
	}

	public String getStateCode() {
		return stateCode;
	}

	public void setStateCode(String stateCode) {
		this.stateCode = stateCode;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	public String getContinentCode() {
		return continentCode;
	}

	public void setContinentCode(String continentCode) {
		this.continentCode = continentCode;
	}

}
