package com.boot.jx.postman.gupshup;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GupShupInboundV2 implements Serializable {

	private static final long serialVersionUID = -5985194546657716271L;

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Profile implements Serializable {
		private static final long serialVersionUID = 1L;
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Contact implements Serializable {
		public Contact() {
			super();
			this.profile = new Profile();
		}

		private static final long serialVersionUID = 1L;

		private Profile profile;

		@JsonProperty("wa_id")
		private String waId;

		public Profile getProfile() {
			return profile;
		}

		public void setProfile(Profile profile) {
			this.profile = profile;
		}

		public String getWaId() {
			return waId;
		}

		public void setWaId(String waId) {
			this.waId = waId;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Text implements Serializable {

		private static final long serialVersionUID = 1L;
		private String body;

		public String getBody() {
			return body;
		}

		public void setBody(String body) {
			this.body = body;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Button implements Serializable {

		private static final long serialVersionUID = 1L;
		private String text;

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Message implements Serializable {
		public Message() {
			super();
			this.type = "text";
			this.text = new Text();
			this.button = new Button();
		}

		private static final long serialVersionUID = 1L;
		private String from;
		private String id;
		private Text text;
		private Button button;
		private String timestamp;
		private String type;

		public String getFrom() {
			return from;
		}

		public void setFrom(String from) {
			this.from = from;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public Text getText() {
			return text;
		}

		public void setText(Text text) {
			this.text = text;
		}

		public String getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(String timestamp) {
			this.timestamp = timestamp;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public Button getButton() {
			return button;
		}

		public void setButton(Button button) {
			this.button = button;
		}

	}

	private List<Contact> contacts;
	private List<Message> messages;

	public GupShupInboundV2() {
		super();
		this.contacts = new ArrayList<Contact>();
		this.messages = new ArrayList<Message>();
	}

	public List<Contact> getContacts() {
		return contacts;
	}

	public void setContacts(List<Contact> contacts) {
		this.contacts = contacts;
	}

	public List<Message> getMessages() {
		return messages;
	}

	public void setMessages(List<Message> messages) {
		this.messages = messages;
	}

}
