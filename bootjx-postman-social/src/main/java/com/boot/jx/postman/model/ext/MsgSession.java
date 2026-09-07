package com.boot.jx.postman.model.ext;

import com.boot.jx.swagger.ApiMockModelProperty;

public class MsgSession {
	@ApiMockModelProperty(example = "SUPPORT", value = "Message Assignment If Any")
	public String assignedToDept;

	@ApiMockModelProperty(example = "SUPPORT", value = "Message Assignment If Any")
	public String assignedToAgent;

	@ApiMockModelProperty(example = "xsds34434", value = "SessionId")
	public String sessionId;

	@ApiMockModelProperty(example = "xsds34434-323-232", value = "RoutingId")
	public String routingId;
}
