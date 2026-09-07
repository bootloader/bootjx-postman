package com.boot.jx.postman.gupshup;

import java.io.Serializable;

import com.boot.jx.swagger.ApiMockModelProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GupShupAgentReq implements Serializable {

	private static final long serialVersionUID = -5985194546657716271L;

	@ApiMockModelProperty(example = "919560222091")
	String waNumber;

	@ApiMockModelProperty(example = "919004371797")
	String mobile;

	@ApiMockModelProperty(example = "Sherlock Holmes")
	String name;

	@ApiMockModelProperty(example = "Hi, I need help")
	String msg;

	@ApiMockModelProperty(example = "text")
	String type;

	public String getWaNumber() {
		return waNumber;
	}

	public void setWaNumber(String waNumber) {
		this.waNumber = waNumber;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

}
