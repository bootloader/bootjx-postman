package com.boot.jx.postman.pbook;

import java.io.Serializable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.boot.model.UtilityModels.JsonIgnoreUnknown;
import com.boot.model.UtilityModels.UniqueIndex;
import com.boot.utils.ArgUtil;
import com.boot.utils.Constants;
import com.boot.utils.StringUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PBPhone implements Serializable, Comparable<PBPhone>, JsonIgnoreUnknown, UniqueIndex<PBPhone> {

	private static final long serialVersionUID = 1772318013635615811L;
	public String uuid;
	public String phone;
	public String type;
	public String label;

	public String country;
	public String countryCallingCode;
	public String nationalNumber;
	public String ext;

	// Social
	public String whatsAppId;

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getCountryCallingCode() {
		return countryCallingCode;
	}

	public void setCountryCallingCode(String countryCallingCode) {
		this.countryCallingCode = countryCallingCode;
	}

	public String getNationalNumber() {
		return nationalNumber;
	}

	public void setNationalNumber(String nationalNumber) {
		this.nationalNumber = nationalNumber;
	}

	public String getExt() {
		return ext;
	}

	public void setExt(String ext) {
		this.ext = ext;
	}

	public String getWhatsAppId() {
		return whatsAppId;
	}

	public void setWhatsAppId(String whatsAppId) {
		this.whatsAppId = whatsAppId;
	}

	public PBPhone phone(String phone) {
		this.phone = phone;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		PBPhone that = (PBPhone) o;
		return new EqualsBuilder().
		// if deriving: appendSuper(super.equals(obj)).
				append(this.countryCallingCode, that.countryCallingCode)
				.append(this.nationalNumber, that.nationalNumber).append(this.ext, that.ext).isEquals();
	}

	@Override
	public String toString() {
		return StringUtils.join(Constants.BLANK, countryCallingCode, nationalNumber, ext);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 31).append(countryCallingCode).append(nationalNumber).append(ext).toHashCode();
	}

	@Override
	public int compareTo(PBPhone o) {
		if (o == null) {
			return 1;
		}
		return this.toString().compareTo(o.toString());
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	@Override
	public String uuid() {
		return this.uuid;
	}

	@Override
	public String uuid(String uuid) {
		if (ArgUtil.not(this.uuid)) {
			this.uuid = uuid;
		}
		return this.uuid;
	}

	@Override
	public PBPhone update(PBPhone fromObject) {
		this.phone = fromObject.getPhone();
		this.type = fromObject.getType();
		this.label = fromObject.getLabel();

		this.country = fromObject.getCountry();
		this.countryCallingCode = fromObject.getCountryCallingCode();
		this.nationalNumber = fromObject.getNationalNumber();
		this.ext = fromObject.getExt();
		return this;
	}
}
