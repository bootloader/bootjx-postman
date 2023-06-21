package com.boot.jx.postman.pbook;

import java.io.Serializable;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.boot.model.UtilityModels.JsonIgnoreUnknown;
import com.boot.model.UtilityModels.UniqueIndex;
import com.boot.utils.ArgUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PBEmail implements Serializable, Comparable<PBEmail>, JsonIgnoreUnknown, UniqueIndex<PBEmail> {

	private static final long serialVersionUID = 13406808264190167L;
	public String uuid;
	public String email;
	public String type;
	public String label;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
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

	public PBEmail email(String email) {
		this.email = email;
		return this;
	}

	public PBEmail type(String type) {
		this.type = type;
		return this;
	}

	public PBEmail label(String label) {
		this.label = label;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		PBEmail that = (PBEmail) o;
		return ArgUtil.equalsIgnoreCase(this.email, that.email);
	}

	@Override
	public String toString() {
		return email;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 31).append(email).toHashCode();
	}

	@Override
	public int compareTo(PBEmail o) {
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
	public PBEmail update(PBEmail fromObject) {
		this.email = fromObject.getEmail();
		this.type = fromObject.getType();
		this.label = fromObject.getLabel();
		return this;
	}

}
