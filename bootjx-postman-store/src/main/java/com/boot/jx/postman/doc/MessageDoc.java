package com.boot.jx.postman.doc;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.dict.ContactType;

@Document(collection = MessageDoc.COLLECTION_NAME)
@TypeAlias("MessageDoc")
public class MessageDoc extends MessageDocAbstract {
	private static final long serialVersionUID = -7003453286628859075L;
	public static final String COLLECTION_NAME = "MESSAGE";

	@Id
	private String messageId;

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	@Document(collection = COLLECTION_NAME + "_WHATSAPP")
	@TypeAlias("MessageDocWA")
	public static class MessageDocWA extends MessageDoc {
		private static final long serialVersionUID = -2175867087327783840L;
	}

	@Document(collection = COLLECTION_NAME + "_FACEBOOK")
	@TypeAlias("MessageDocFB")
	public static class MessageDocFB extends MessageDoc {
		private static final long serialVersionUID = -2175867087327783840L;
	}

	@Document(collection = COLLECTION_NAME + "_TELEGRAM")
	@TypeAlias("MessageDocTG")
	public static class MessageDocTG extends MessageDoc {
		private static final long serialVersionUID = -2175867087327783840L;
	}

	@Document(collection = COLLECTION_NAME + "_INSTAGRAM")
	@TypeAlias("MessageDocIG")
	public static class MessageDocIG extends MessageDoc {
		private static final long serialVersionUID = -2175867087327783840L;
	}

	@Document(collection = COLLECTION_NAME + "_" + "TWITTER")
	@TypeAlias("MessageDocTW")
	public static class MessageDocTW extends MessageDoc {
		private static final long serialVersionUID = -2175867087327783840L;
	}

	@Document(collection = COLLECTION_NAME + "_" + "WEBSITE")
	@TypeAlias("MessageDocWeb")
	public static class MessageDocWeb extends MessageDoc {
		private static final long serialVersionUID = -2175867087327783840L;
	}

	@Document(collection = COLLECTION_NAME + "_" + "EMAIL")
	@TypeAlias("MessageDocEmail")
	public static class MessageDocEmail extends MessageDoc {
		private static final long serialVersionUID = -2175867087327783840L;
	}

	@Document(collection = COLLECTION_NAME + "_" + "LOGS")
	@TypeAlias("MessageDocLogs")
	public static class MessageDocLogs extends MessageDocAbstract {
		private static final long serialVersionUID = -2175867087327783840L;

		@Id
		private String logId;

		private Object httpReq;
		private Object httpResp;
		private int httpStatusCode;

		private String messageId;

		public String getMessageId() {
			return messageId;
		}

		public void setMessageId(String messageId) {
			this.messageId = messageId;
		}

		public Object getHttpReq() {
			return httpReq;
		}

		public void setHttpReq(Object httpReq) {
			this.httpReq = httpReq;
		}

		public Object getHttpResp() {
			return httpResp;
		}

		public void setHttpResp(Object httpResp) {
			this.httpResp = httpResp;
		}

		public int getHttpStatusCode() {
			return httpStatusCode;
		}

		public void setHttpStatusCode(int httpStatusCode) {
			this.httpStatusCode = httpStatusCode;
		}
	}

	public static MessageDoc instance(ContactType contactType) {
		switch (contactType) {
		case WHATSAPP:
			return new MessageDocWA();
		case FACEBOOK:
			return new MessageDocFB();
		case TELEGRAM:
			return new MessageDocTG();
		case INSTAGRAM:
			return new MessageDocIG();
		case TWITTER:
			return new MessageDocTW();
		case WEBSITE:
			return new MessageDocWeb();
		case EMAIL:
			return new MessageDocEmail();
		default:
			return new MessageDoc();
		}

	}

}
