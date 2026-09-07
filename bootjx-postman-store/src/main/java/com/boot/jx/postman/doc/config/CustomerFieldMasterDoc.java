package com.boot.jx.postman.doc.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.mongo.CommonDocInterfaces.TimeStampIndex.TimeStampDoc;

@Document(collection = "MASTER_CUSTOMER_FIELD")
@TypeAlias("CustomerFieldMaster")
public class CustomerFieldMasterDoc extends TimeStampDoc {

	@Id
	private String id;

	private String label;

	@Indexed(unique = true)
	private String code;

	private String desc;

	private String type;

	private boolean active;

	private boolean required;
	private boolean predefined;
	List<Object> possibleOptions=new ArrayList<>();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public boolean isPredefined() {
		return predefined;
	}

	public void setPredefined(boolean predefined) {
		this.predefined = predefined;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
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

	public List<Object> getPossibleOptions() {
		return possibleOptions;
	}

	public void setPossibleOptions(List<Object> possibleOptions) {
		this.possibleOptions = possibleOptions;
	}

}
