package com.boot.jx.postman.model;

import java.io.Serializable;

import com.boot.jx.swagger.ApiMockModelProperty;
import com.boot.model.UtilityModels.JsonIgnoreNull;
import com.boot.model.UtilityModels.JsonIgnoreUnknown;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MessageReplyTo implements Serializable, JsonIgnoreNull, JsonIgnoreUnknown {

	private static final long serialVersionUID = 3550234068884721183L;

	@JsonProperty("type")
	@ApiMockModelProperty(example = "feedback", value = "type", allowableValues = "story,feedback")
	public String type;

	public String bulkSessionId;

}
