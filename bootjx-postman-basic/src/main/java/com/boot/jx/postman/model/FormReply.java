package com.boot.jx.postman.model;

import java.io.Serializable;
import java.util.Map;

import com.boot.jx.swagger.ApiMockModelProperty;
import com.boot.model.UtilityModels.JsonIgnoreNull;
import com.boot.model.UtilityModels.JsonIgnoreUnknown;

public class FormReply implements Serializable, JsonIgnoreNull, JsonIgnoreUnknown {

	private static final long serialVersionUID = -2276133193201979913L;

	@ApiMockModelProperty(example = "ACCEPT", value = "Button Code")
	public String reply_id;

	@ApiMockModelProperty(example = "I Accept", value = "Button Title")
	public String reply_title;

	@ApiMockModelProperty(value = "Button Description")
	public String reply_desc;

	@ApiMockModelProperty(value = "JSON Collection of all input selections by customer in form")
	public Map<String, Object> reply_json;
}
