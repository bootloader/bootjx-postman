package com.boot.jx.bot;

import com.boot.jx.exception.AmxException;

public class ChatException extends AmxException {

	private static final long serialVersionUID = 1L;

	private String targetHandler;

	public ChatException(String msg) {
		super(msg, null, true, false);
	}

	public String getTargetHandler() {
		return targetHandler;
	}

	public ChatException targetHandler(String targetHandler) {
		this.targetHandler = targetHandler;
		return this;
	}

}
