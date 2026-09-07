package com.boot.jx.postman.gupshup;

public class GupShupConstants {

	public static enum SessionType {
		CHAT, NOTIFICATION, AGENT
	}

	public static class Format {
		public static final String TEXT = "text";
		public static final String JSON = "json";
		public static final String XML = "xml";
	}

	public static class DataEncoding {
		public static final String TEXT = "text";
		public static final String UNICODE = "Unicode_text";
	}

	public static class MessageType {
		public static final String text = "text";
		public static final String TEXT = "TEXT";
		public static final String DATA_TEXT = "DATA_TEXT";
		public static final String HSM = "HSM";
		public static final String IMAGE = "IMAGE";
		public static final String DOCUMENT = "DOCUMENT";
		public static final String AUDIO = "AUDIO";
		public static final String VIDEO = "VIDEO";
	}

	public static enum Method {
		OPT_IN, OPT_OUT, SendMessage, SendMediaMessage, UploadMedia;
	}

}
