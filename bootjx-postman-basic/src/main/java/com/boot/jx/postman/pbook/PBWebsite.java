package com.boot.jx.postman.pbook;

import java.io.Serializable;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.boot.model.UtilityModels.JsonIgnoreUnknown;
import com.boot.model.UtilityModels.UniqueIndex;
import com.boot.utils.ArgUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PBWebsite implements Serializable, Comparable<PBWebsite>, JsonIgnoreUnknown, UniqueIndex<PBWebsite> {

	private static final long serialVersionUID = -7496133827194014822L;
	public String uuid;
	public String url;
	public String type;
	public String label;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
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

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 31).append(this.toString()).toHashCode();
	}

	@Override
	public String toString() {
		return url;
	}

	@Override
	public int compareTo(PBWebsite o) {
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
		PBWebsite that = (PBWebsite) o;
		if (ArgUtil.equalsIgnoreCase(this.url, that.url)) {
			return false;
		}
		return true;
	}

	@Override
	public String uuid() {
		return this.uuid;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	@Override
	public String uuid(String uuid) {
		if (ArgUtil.not(this.uuid)) {
			this.uuid = uuid;
		}
		return this.uuid;
	}

	@Override
	public PBWebsite update(PBWebsite fromObject) {
		this.url = fromObject.getUrl();
		this.type = fromObject.getType();
		this.label = fromObject.getLabel();
		return this;
	}
}
