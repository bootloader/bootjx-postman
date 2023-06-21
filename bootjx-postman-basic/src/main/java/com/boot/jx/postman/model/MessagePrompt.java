package com.boot.jx.postman.model;

import java.io.Serializable;

import com.boot.model.UtilityModels.JsonIgnoreUnknown;

public class MessagePrompt implements Serializable, JsonIgnoreUnknown {
	private static final long serialVersionUID = -562270873353679506L;

	public static class TYPE {
		public static final String MOREOPTIONS = "moreoptions";
	}

	public String type;
	public String messageId;
	public int pageIndex;

	public String toString() {
		return String.format("#%s#%d#%s", type, pageIndex, messageId);
	}

}
