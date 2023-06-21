package com.boot.jx.postman.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.boot.model.MapModel;
import com.boot.utils.ArgUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public interface MessageOptions {

	@JsonIgnore
	public Map<String, Object> options();

	@JsonIgnore
	default public MessageOptions option(String key, Object value) {
		this.options().put(key, value);
		return this;
	}

	@JsonIgnore
	default public List<TmplElement> optionActionButtons() {
		if (this.options().containsKey("buttons")) {
			return new MapModel(this.options()).entry("buttons").asList(TmplElement.class);
		}
		return new ArrayList<TmplElement>();
	}

	@JsonIgnore
	default public MapModel optionsAsModel() {
		return MapModel.from(this.options());
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static interface WAMessageOptions extends MessageOptions {

		@JsonIgnore
		default public String getMsgType() {
			return ArgUtil.parseAsString(this.options().get("msg_type"));
		}

		@JsonIgnore
		default public boolean isTemplateMsg() {
			return ArgUtil.parseAsBoolean(this.options().get("isTemplate"), false);
		}

		@JsonIgnore
		default public boolean isQRButtons() {
			return ArgUtil.parseAsBoolean(this.options().get("isQRButtons"), false);
		}

		@JsonIgnore
		default public boolean isViaAgent() {
			return ArgUtil.parseAsBoolean(this.options().get("isViaAgent"), false);
		}
	}
}