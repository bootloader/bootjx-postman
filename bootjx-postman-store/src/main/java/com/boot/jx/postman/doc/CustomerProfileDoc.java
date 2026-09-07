package com.boot.jx.postman.doc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.mongo.CommonDocInterfaces.SimpleDocument;
import com.boot.jx.mongo.CommonDocInterfaces.TimeStampIndex.TimeStampDoc;
import com.boot.jx.postman.model.ContactMeta;
import com.boot.jx.postman.pbook.PBAddress;
import com.boot.jx.postman.pbook.PBEmail;
import com.boot.jx.postman.pbook.PBName;
import com.boot.jx.postman.pbook.PBPhone;
import com.boot.jx.postman.pbook.PBWebsite;
import com.boot.jx.postman.pbook.PBWork;
import com.boot.model.UtilityModels.JsonIgnoreNull;
import com.boot.model.UtilityModels.JsonIgnoreUnknown;

@Document(collection = "CUSTOMER_PROFILE")
public class CustomerProfileDoc extends TimeStampDoc
		implements Serializable, SimpleDocument, JsonIgnoreUnknown, JsonIgnoreNull {
	private static final long serialVersionUID = 1281605084248923642L;

	@Id
	public String id;

	@Indexed(sparse = true, unique = true)
	public String code;

	public PBName name;

	public Set<PBPhone> phones;
	public Set<PBEmail> emails;
	public Set<PBAddress> addresses;
	public Set<PBWebsite> urls;
	public Set<PBWork> works;
	public Map<String, Object> additionalInfo = new HashMap<>();
	public String source;
	

	public Set<ContactMeta> linked;

	public String rmCode;
	
	public List<Map<String, Object>> devices;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Set<PBWebsite> urls() {
		if (urls == null) {
			this.urls = new TreeSet<PBWebsite>();
		}
		return this.urls;
	}

	public Set<PBAddress> addresses() {
		if (addresses == null) {
			this.addresses = new TreeSet<PBAddress>();
		}
		return this.addresses;
	}

	public Set<PBPhone> phones() {
		if (phones == null) {
			this.phones = new TreeSet<PBPhone>();
		}
		return this.phones;
	}

	public Set<ContactMeta> linked() {
		if (linked == null) {
			this.linked = new TreeSet<ContactMeta>();
		}
		return this.linked;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public PBName getName() {
		return name;
	}

	public void setName(PBName name) {
		this.name = name;
	}

	public Set<PBPhone> getPhones() {
		return phones;
	}

	public void setPhones(Set<PBPhone> phones) {
		this.phones = phones;
	}

	public Set<PBEmail> getEmails() {
		return emails;
	}

	public void setEmails(Set<PBEmail> emails) {
		this.emails = emails;
	}

	public Set<PBAddress> getAddresses() {
		return addresses;
	}

	public void setAddresses(Set<PBAddress> addresses) {
		this.addresses = addresses;
	}

	public Set<PBWebsite> getUrls() {
		return urls;
	}

	public void setUrls(Set<PBWebsite> urls) {
		this.urls = urls;
	}

	public String getRmCode() {
		return rmCode;
	}

	public void setRmCode(String rmCode) {
		this.rmCode = rmCode;
	}

	public Map<String, Object> getAdditionalInfo() {
		return additionalInfo;
	}

	public void setAdditionalInfo(Map<String, Object> additionalInfo) {
		this.additionalInfo = additionalInfo;
	}

	public Set<PBWork> getWorks() {
		return works;
	}

	public void setWorks(Set<PBWork> works) {
		this.works = works;
	}

	public Set<PBWork> works() {
		if (works == null) {
			this.works = new TreeSet<PBWork>();
		}
		return this.works;
	}

	public Set<PBEmail> emails() {
		if (emails == null) {
			this.emails = new TreeSet<PBEmail>();
		}
		return this.emails;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public Set<ContactMeta> getLinked() {
		return linked;
	}

	public void setLinked(Set<ContactMeta> linked) {
		this.linked = linked;
	}
	
	public List<Map<String, Object>> getDevices() {
		return devices;
	}

	public void setDevices(List<Map<String, Object>> devices) {
		this.devices = devices;
	}

	public List<Map<String, Object>> devices() {
		if (devices == null) {
			this.devices = new ArrayList<>();
		}
		return this.devices;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}


}