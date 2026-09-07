package com.boot.jx.tasks;

import java.io.Serializable;

public class MediaUploadEvent implements Serializable {
	private static final long serialVersionUID = 1L;

	private String tenant;
	private String key;
	private String drive;
	private String name;
	private String caption;
	private int attempt = 1;

	public String getTenant() {
		return tenant;
	}

	public void setTenant(String tenant) {
		this.tenant = tenant;
	}

	public MediaUploadEvent tenant(String tenant) {
		this.tenant = tenant;
		return this;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public MediaUploadEvent key(String key) {
		this.key = key;
		return this;
	}

	public String getDrive() {
		return drive;
	}

	public void setDrive(String drive) {
		this.drive = drive;
	}

	public MediaUploadEvent drive(String drive) {
		this.drive = drive;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public MediaUploadEvent name(String name) {
		this.name = name;
		return this;
	}

	public String getCaption() {
		return caption;
	}

	public void setCaption(String caption) {
		this.caption = caption;
	}

	public MediaUploadEvent caption(String caption) {
		this.caption = caption;
		return this;
	}

	public int getAttempt() {
		return attempt;
	}

	public void setAttempt(int attempt) {
		this.attempt = attempt;
	}

	public MediaUploadEvent attempt(int attempt) {
		this.attempt = attempt;
		return this;
	}
}
