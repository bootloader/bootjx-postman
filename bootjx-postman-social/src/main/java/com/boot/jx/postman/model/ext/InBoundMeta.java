package com.boot.jx.postman.model.ext;

import com.boot.jx.swagger.ApiMockModelProperty;

public class InBoundMeta {
	@ApiMockModelProperty(example = "alex", value = "your domain name")
	public String domain;

	@ApiMockModelProperty(example = "server.com", value = "Server as per Enviroment")
	public String server;

	@ApiMockModelProperty(example = "61ec7d9c2ce85742b201c5ab", value = "Client App Id if webhook is set for an App ",
			required = false)
	public String appId;

	@ApiMockModelProperty(example = "my_bot", value = "Client App Queue Code if webhook is set for an App ",
			required = false)
	public String appCode;

	@ApiMockModelProperty(example = "FEEDBACK_V2", value = "Client App Type ", required = false,
			allowableValues = "FEEDBACK_V2,BOTFLOW,APP_SCRIPT,BOT,FAQ,DEFAULT")
	public String appType;

	@ApiMockModelProperty(example = "WEBHOOK", value = "Client App mode ", required = false,
			allowableValues = "AGENT,BOT,SCRIPTUS,PUSH,WEBHOOK,NONE")
	public String appMode;

	public boolean debug;

	@ApiMockModelProperty(example = "1", value = "Client App Version changes after every update", required = false)
	private Long appVersion;

	public InBoundMeta domain(String domain) {
		this.domain = domain;
		return this;
	}

	public InBoundMeta server(String server) {
		this.server = server;
		return this;
	}

	public InBoundMeta appId(String appId) {
		this.appId = appId;
		return this;
	}

	public InBoundMeta appCode(String appCode) {
		this.appCode = appCode;
		return this;
	}

	public InBoundMeta debug(boolean debug) {
		this.debug = debug;
		return this;
	}

	public InBoundMeta appType(String appType) {
		this.appType = appType;
		return this;
	}

	public InBoundMeta appMode(String appMode) {
		this.appMode = appMode;
		return this;
	}

	public InBoundMeta appVersion(Long appVersion) {
		this.appVersion = appVersion;
		return this;
	}

}