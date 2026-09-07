package com.boot.jx.postman.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.boot.jx.dict.ContactType;

public class Email extends Message<Email> implements Cloneable {

	private static final long serialVersionUID = 9210306073311369368L;
	private String from = null;
	private String replyToEmail = null;
	private List<String> cc = null;

	private boolean isHtml;

	public String getReplyToEmail() {
		return replyToEmail;
	}

	public void setReplyToEmail(String replyTo) {
		this.replyToEmail = replyTo;
	}

	public Email() {
		super(ContactType.EMAIL);
		this.to = new ArrayList<String>();
		this.cc = new ArrayList<String>();
		this.isHtml = true;
	}

	public Email(String from, String toList, String subject) {
		this();
		this.from = from;
		this.subject = subject;
		this.to.addAll(Arrays.asList(splitByComma(toList)));
	}

	public Email(String from, String toList, String subject, String message) {
		this();
		this.from = from;
		this.subject = subject;
		this.message = message;
		this.to.addAll(Arrays.asList(splitByComma(toList)));
	}

	public Email(String from, String toList, String ccList, String subject, String message) {
		this();
		this.from = from;
		this.subject = subject;
		this.message = message;
		this.to.addAll(Arrays.asList(splitByComma(toList)));
		this.cc.addAll(Arrays.asList(splitByComma(ccList)));
	}

	/**
	 * @return the from
	 */
	public String getFrom() {
		return from;
	}

	/**
	 * @param from the from to set
	 */
	public void setFrom(String from) {
		this.from = from;
	}

	/**
	 * @return the cc
	 */
	public List<String> getCc() {
		return cc;
	}

	/**
	 * @param cc the cc to set
	 */
	public void setCc(List<String> cc) {
		this.cc = cc;
	}

	/**
	 * @return the isHtml
	 */
	public boolean isHtml() {
		return isHtml;
	}

	/**
	 * @param isHtml the isHtml to set
	 */
	public void setHtml(boolean isHtml) {
		this.isHtml = isHtml;
	}

	private String[] splitByComma(String toMultiple) {
		return toMultiple.split(",");
	}

	public void addAllTo(String toMultiple) {
		this.to.addAll(Arrays.asList(splitByComma(toMultiple)));
	}

	public Email clone() throws CloneNotSupportedException {
		return (Email) super.clone();
	}

}
