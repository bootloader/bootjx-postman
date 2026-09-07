package com.boot.jx.postman.fb;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FacebookMessageResp implements Serializable {

	private static final long serialVersionUID = -8675624188026661184L;

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class ErrorMessage implements Serializable {

		private static final long serialVersionUID = 1090283990468843663L;
		private String message;
		private String type;
		private Object code;
		@JsonProperty("error_subcode")
		private Object errorSubcode;

		@JsonProperty("fbtrace_id")
		private Object fbtraceId;

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public Object getCode() {
			return code;
		}

		public void setCode(Object code) {
			this.code = code;
		}

		public Object getErrorSubcode() {
			return errorSubcode;
		}

		public void setErrorSubcode(Object errorSubcode) {
			this.errorSubcode = errorSubcode;
		}

		public Object getFbtraceId() {
			return fbtraceId;
		}

		public void setFbtraceId(Object fbtraceId) {
			this.fbtraceId = fbtraceId;
		}
	}

	@JsonProperty("recipient_id")
	private String recipientId;

	@JsonProperty("message_id")
	private String messageId;

	private ErrorMessage error;

	public String getRecipientId() {
		return recipientId;
	}

	public void setRecipientId(String recipientId) {
		this.recipientId = recipientId;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public ErrorMessage getError() {
		return error;
	}

	public void setError(ErrorMessage error) {
		this.error = error;
	}

}
