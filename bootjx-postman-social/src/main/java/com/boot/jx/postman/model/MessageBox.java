package com.boot.jx.postman.model;

import java.util.ArrayList;
import java.util.List;

import com.boot.jx.dict.ContactType;

public class MessageBox {

	private List<Email> emailBucket;
	private List<SMS> smsBucket;
	private List<WAMessage> waBucket;
	private List<TGMessage> tgBucket;
	private List<PushMessage> pushBucket;

	public int priorityOrder;
	public String priorityType;

	public MessageBox() {
		this.emailBucket = new ArrayList<Email>();
		this.smsBucket = new ArrayList<SMS>();
		this.waBucket = new ArrayList<WAMessage>();
		this.tgBucket = new ArrayList<TGMessage>();
		this.pushBucket = new ArrayList<PushMessage>();
		this.priorityOrder = 99;
	}

	public List<Email> getEmailBucket() {
		return emailBucket;
	}

	public void setEmailBucket(List<Email> emailBucket) {
		this.emailBucket = emailBucket;
	}

	public List<SMS> getSmsBucket() {
		return smsBucket;
	}

	public void setSmsBucket(List<SMS> smsBucket) {
		this.smsBucket = smsBucket;
	}

	public List<WAMessage> getWaBucket() {
		return waBucket;
	}

	public void setWaBucket(List<WAMessage> waBucket) {
		this.waBucket = waBucket;
	}

	public List<PushMessage> getPushBucket() {
		return pushBucket;
	}

	public void setPushBucket(List<PushMessage> pushBucket) {
		this.pushBucket = pushBucket;
	}

	public MessageBox push(Email m) {
		this.emailBucket.add(m);
		this.priority(m);
		return this;
	}

	public MessageBox push(SMS m) {
		this.smsBucket.add(m);
		this.priority(m);
		return this;
	}

	public MessageBox push(WAMessage m) {
		this.waBucket.add(m);
		this.priority(m);
		return this;
	}

	public MessageBox push(TGMessage m) {
		this.tgBucket.add(m);
		this.priority(m);
		return this;
	}

	public MessageBox push(PushMessage m) {
		this.pushBucket.add(m);
		this.priority(m);
		return this;
	}

	public List<TGMessage> getTgBucket() {
		return tgBucket;
	}

	public void setTgBucket(List<TGMessage> tgBucket) {
		this.tgBucket = tgBucket;
	}

	public int size() {
		return waBucket.size() + tgBucket.size() + smsBucket.size() + emailBucket.size()
				+ pushBucket.size();
	}

	public MessageBox push(Message<?> m) {
		if (m instanceof WAMessage || ContactType.WHATSAPP.equals(m.contact().type())) {
			this.waBucket.add((WAMessage) m);
		} else if (m instanceof TGMessage || ContactType.TELEGRAM.equals(m.contact().type())) {
			this.tgBucket.add((TGMessage) m);
		} else if (m instanceof SMS || ContactType.SMS.equals(m.contact().type())) {
			this.smsBucket.add((SMS) m);
		} else if (m instanceof Email  || ContactType.EMAIL.equals(m.contact().type())) {
			this.emailBucket.add((Email) m);
		} else if (m instanceof PushMessage  || ContactType.PUSH.equals(m.contact().type())) {
			this.pushBucket.add((PushMessage) m);
		}
		this.priority(m);
		return this;
	}

	public MessageBox priority(Message<?> m) {
		if ((m.getPriority() > 0) && (m.getPriority() < this.priorityOrder)) {
			this.priorityOrder = m.getPriority();
			if (m instanceof WAMessage) {
				this.priorityType = ContactType.WHATSAPP.getShortCode();
			} else if (m instanceof TGMessage) {
				this.priorityType = ContactType.WHATSAPP.getShortCode();
			} else if (m instanceof SMS) {
				this.priorityType = ContactType.SMS.getShortCode();
			} else if (m instanceof Email) {
				this.priorityType = ContactType.EMAIL.getShortCode();
			} else if (m instanceof PushMessage) {
				this.priorityType = ContactType.PUSH.getShortCode();
			}
		}
		return this;
	}

	public int getPriorityOrder() {
		return priorityOrder;
	}

	public void setPriorityOrder(int priorityOrder) {
		this.priorityOrder = priorityOrder;
	}

	public String getPriorityType() {
		return priorityType;
	}

	public void setPriorityType(String priorityType) {
		this.priorityType = priorityType;
	}

}
