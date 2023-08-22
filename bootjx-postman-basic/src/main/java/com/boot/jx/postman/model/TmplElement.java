package com.boot.jx.postman.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.boot.jx.swagger.ApiMockModelProperty;

public class TmplElement implements Serializable {

	private static final long serialVersionUID = 8844236002971255681L;

	public static class TYPES {
		public static final String QUICK_REPLY = "QUICK_REPLY";
		public static final String URL = "URL";
		public static final String PHONE_NUMBER = "PHONE_NUMBER";
	}

	@ApiMockModelProperty(example = "occupation", value = "key to be used programmatically", required = false)
	private String code;

	@ApiMockModelProperty(example = "Occupation", value = "Display Text to be used programmatically", required = false)
	private String label;

	@ApiMockModelProperty(example = "QUICK_REPLY", value = "Display Text", allowableValues = "QUICK_REPLY,URL,PHONE_NUMBER", required = false)
	private String type;

	@ApiMockModelProperty(example = "What occupation you have", value = "Description of of element", required = false)
	private String desc;

	@ApiMockModelProperty(example = "http://url", value = "If type is set to URL", required = false)
	private String url;

	@ApiMockModelProperty(example = "+91 9988776655", value = "If type is set to PHONE_NUMBER", required = false)
	private String phone;

	public String getCode() {
		return code;
	}

	public String getName() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public TmplElement code(String code) {
		this.code = code;
		return this;
	}

	public TmplElement label(String label) {
		this.label = label;
		return this;
	}

	public TmplElement type(String type) {
		this.type = type;
		return this;
	}

	public TmplElement url(String url) {
		this.url = url;
		return this;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public TmplElement desc(String desc) {
		this.desc = desc;
		return this;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public TmplElement phone(String phone) {
		this.phone = phone;
		return this;
	}

	public TmplElement name(String string) {
		return this.code(string);
	}

	public static List<TmplElement> list() {
		return new ArrayList<TmplElement>();
	}

}
