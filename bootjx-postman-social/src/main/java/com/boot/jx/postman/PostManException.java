package com.boot.jx.postman;

import com.boot.jx.exception.AmxApiError;
import com.boot.jx.exception.AmxApiException;
import com.boot.jx.exception.IExceptionEnum;

public class PostManException extends AmxApiException {

	private static final long serialVersionUID = -4630097170366238398L;

	public enum ErrorCode implements IExceptionEnum {
		CONTACT_NOTFOUND, CONTACT_INVALID, NO_RECIPIENT_DEFINED, TOO_MANY_RECIPIENT, NO_TENANT_DEFINED,
		NO_CHANNEL_DEFINED, NO_TOPIC_DEFINED, NO_MESSAGE_DEFINED, TOO_MANY_CONDITIONS, MESSAGE_LIMIT_EXCEEDED;

		@Override
		public String getStatusKey() {
			return this.toString();
		}

		@Override
		public int getStatusCode() {
			return this.ordinal();
		}
	}

	public PostManException(Exception e) {
		super(e);
	}

	public PostManException(String msg) {
		super(msg);
	}

	public PostManException(ErrorCode error) {
		super(error);
	}

	public PostManException(AmxApiError apiError) {
		super(apiError);
	}

	@Override
	public AmxApiException getInstance(AmxApiError apiError) {
		return new PostManException(apiError);
	}

	@Override
	public IExceptionEnum getErrorIdEnum(String errorId) {
		return ErrorCode.valueOf(errorId);
	}

	@Override
	public boolean isReportable() {
		return false;
	}

}
