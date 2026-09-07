package com.boot.jx.postman.doc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.mongo.CommonDocInterfaces.SimpleDocument;
import com.boot.jx.mongo.CommonDocInterfaces.TimeStampIndex.TimeStampDoc;
import com.boot.model.UtilityModels.JsonIgnoreNull;
import com.boot.model.UtilityModels.JsonIgnoreUnknown;

@Deprecated
@Document(collection = "CUSTOMER_PROFILE_NEW")
public class CustomerContactProfileDoc extends TimeStampDoc
		implements Serializable, SimpleDocument, JsonIgnoreUnknown, JsonIgnoreNull {
	private static final long serialVersionUID = 1281605084248923642L;

	@Id
	private String id;
	List<Map<String, Object>> contactmap = new ArrayList<>();
	private String contactIdRef;

	private String isactive;
	private Date createdDate;
	private Date modifiedDate;
	private Long modifiedStamp;
	private String modifiedBy;

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setId(String id) {
		// TODO Auto-generated method stub

	}

	public List<Map<String, Object>> getContactmap() {
		return contactmap;
	}

	public void setContactmap(List<Map<String, Object>> contactmap) {
		this.contactmap = contactmap;
	}

	public String getContactIdRef() {
		return contactIdRef;
	}

	public void setContactIdRef(String contactIdRef) {
		this.contactIdRef = contactIdRef;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public String getIsactive() {
		return isactive;
	}

	public void setIsactive(String isactive) {
		this.isactive = isactive;
	}

	public Date getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	public Date getModifiedDate() {
		return modifiedDate;
	}

	public void setModifiedDate(Date modifiedDate) {
		this.modifiedDate = modifiedDate;
	}

	public Long getModifiedStamp() {
		return modifiedStamp;
	}

	public void setModifiedStamp(Long modifiedStamp) {
		this.modifiedStamp = modifiedStamp;
	}

	public String getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(String modifiedBy) {
		this.modifiedBy = modifiedBy;
	}
}
