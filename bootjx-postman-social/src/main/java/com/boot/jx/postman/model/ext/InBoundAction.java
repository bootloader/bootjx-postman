package com.boot.jx.postman.model.ext;

import com.boot.jx.swagger.ApiMockModelProperty;

public class InBoundAction {
	@ApiMockModelProperty(example = "SEND_INVOICE", value = "Action Triggered by Agent/Service")
	public String actionCode;
}