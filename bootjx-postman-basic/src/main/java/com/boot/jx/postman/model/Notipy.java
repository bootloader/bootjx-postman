package com.boot.jx.postman.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Notipy extends Message<Notipy> {

	private static final long serialVersionUID = -8315838727087594607L;

	public static enum Workspace {
		ALMEX, MODEX
	}

	public static enum ChannelType implements IChannel {
		NOTIPY("C9AK11W2K"), ALERTY("CET4055AB"), FEED("CET8JSKFZ"), DEPLOYER("C8L3GL92A"), GENERAL("C7F823MLJ"),
		INQUIRY("CAQ4WUNAZ", Workspace.MODEX);

		String code;
		Workspace workspace;

		public static final ChannelType DEFAULT = GENERAL;

		ChannelType(String code) {
			this.code = code;
			this.workspace = Workspace.ALMEX;
		}

		ChannelType(String code, Workspace workspace) {
			this.code = code;
			this.workspace = workspace;
		}

		public String getCode() {
			return code;
		}

		public Workspace getWorkspace() {
			return workspace;
		}

	}

	protected Object author;
	protected List<Map<String, Object>> fields;
	protected String color;

	public Notipy() {
		super();
		this.fields = new ArrayList<Map<String, Object>>();
		this.contact().setChannelType(ChannelType.DEFAULT.toString());
	}

	public Object getAuthor() {
		return author;
	}

	public void setAuthor(Object author) {
		this.author = author;
	}

	public List<Map<String, Object>> getFields() {
		return fields;
	}

	public void setFields(List<Map<String, Object>> fields) {
		this.fields = fields;
	}

	public void addField(String title, String value) {
		Map<String, Object> field = new HashMap<String, Object>();
		field.put("title", title);
		field.put("value", value);
		field.put("short", false);
		this.fields.add(field);
	}

	public String getColor() {
		return this.color;
	}

	public void setColor(String color) {
		this.color = color;
	}

}
