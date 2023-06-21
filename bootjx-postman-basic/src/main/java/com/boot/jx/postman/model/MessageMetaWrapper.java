package com.boot.jx.postman.model;

import java.util.Map;

import com.boot.model.MapModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageMetaWrapper extends MapModel {

	public MessageMetaWrapper(Map<String, Object> meta) {
		super(meta);
	}

	/**
	 * 
	 * @return
	 */
	public MessageMetaWrapper isPush(boolean isPush) {
		this.put("isPush", isPush);
		return this;
	}

	/**
	 * Value can differ from channel to channel
	 * 
	 * @param sendType
	 * @return
	 */
	public MessageMetaWrapper sendType(String sendType) {
		this.put("sendType", sendType);
		return this;
	}

	public MessageMetaWrapper composeType(String composeType) {
		this.put("composeType", composeType);
		return this;
	}

	public boolean composeTypeIs(String composeType) {
		return this.keyEntry("composeType").is(composeType);
	}

	public String sendType() {
		return this.getString("sendType");
	}

}
