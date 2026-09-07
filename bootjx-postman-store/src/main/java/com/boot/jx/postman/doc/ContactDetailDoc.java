package com.boot.jx.postman.doc;

import java.io.Serializable;
import java.util.Map;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.postman.model.ContactMeta;
import com.boot.jx.postman.model.MessageDefinitions.Contactable;
import com.boot.utils.ArgUtil;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(as = ContactDetailDoc.class)
@Document
@CompoundIndexes({
		// route indexs
		@CompoundIndex(name = "email", def = "{ 'email': 1 }"), //
		@CompoundIndex(name = "phone", def = "{ 'phone': 1 }"), //
		@CompoundIndex(name = "name", def = "{ 'name': 1 }"), //
})
public class ContactDetailDoc extends ContactMeta implements Serializable, Contactable {

	private static final long serialVersionUID = -6046846959629225232L;

	public ContactDetailDoc() {
	}

	private String mobile;

	private Map<String, Object> filter;

	@Override
	public String phone() {
		return ArgUtil.nonEmpty(this.phone, this.mobile);
	}

	@Override
	public void phone(String phone) {
		this.phone = phone;
		this.mobile = phone;
	}

	public Map<String, Object> getFilter() {
		return filter;
	}

	public void setFilter(Map<String, Object> filter) {
		this.filter = filter;
	}

	@Deprecated
	public String getMobile() {
		return this.mobile;
	}

	@Deprecated
	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

}
