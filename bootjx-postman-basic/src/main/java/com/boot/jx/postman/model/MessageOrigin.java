package com.boot.jx.postman.model;

import java.io.Serializable;

import com.boot.utils.ArgUtil;

public class MessageOrigin implements Serializable {

	private static final long serialVersionUID = -1835604233948927045L;
	protected String appType;
	protected String appVenv;

	public String getAppType() {
		return appType;
	}

	public void setAppType(String appType) {
		this.appType = appType;
	}

	public String getAppVenv() {
		return appVenv;
	}

	public void setAppVenv(String appVenv) {
		this.appVenv = appVenv;
	}

	public void from(MessageOrigin origin) {
		if (origin == null) {
			return;
		}
		this.appType = ArgUtil.nonEmpty(origin.appType, this.appType);
		this.appVenv = ArgUtil.nonEmpty(origin.appVenv, this.appVenv);
	}
}
