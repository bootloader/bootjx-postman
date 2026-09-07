package com.boot.jx.postman.model;

public class ExceptionReport extends Exception {

	private static final long serialVersionUID = -3506343577395440166L;
	private String title = null;
	private String exception = null;
	private Email email = null;

	public ExceptionReport() {
		super();
	}

	public ExceptionReport(String message) {
		super(message);
	}

	public ExceptionReport(Exception e) {
		this(e.getMessage());
		this.exception = e.getClass().getName();
		this.setStackTrace(e.getStackTrace());
	}

	public ExceptionReport(String title, Exception e) {
		this(e);
		this.title = title;
	}

	public ExceptionReport(Exception e, Email email) {
		this(email.getSubject(), e);
		this.email = email;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getException() {
		return exception;
	}

	public void setException(String exception) {
		this.exception = exception;
	}

	public Email getEmail() {
		return email;
	}

	public void setEmail(Email email) {
		this.email = email;
	}

}
