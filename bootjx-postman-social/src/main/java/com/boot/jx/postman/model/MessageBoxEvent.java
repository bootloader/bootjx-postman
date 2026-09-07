package com.boot.jx.postman.model;

import java.util.List;

import com.boot.utils.CollectionUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageBoxEvent {
	private List<InboxMessage> inboxMessages;

	public List<InboxMessage> getInboxMessages() {
		return inboxMessages;
	}

	public void setInboxMessages(List<InboxMessage> inboxMessages) {
		this.inboxMessages = inboxMessages;
	}

	public List<InboxMessage> inboxMessages() {
		if (inboxMessages == null) {
			inboxMessages = CollectionUtil.getList(InboxMessage.class);
		}
		return this.inboxMessages;
	}

	public MessageBoxEvent addInboxMessage(InboxMessage inboxMessage) {
		this.inboxMessages().add(inboxMessage);
		return this;
	}

	public MessageBoxEvent addInboxMessage(List<InboxMessage> inboxMessages) {
		this.inboxMessages().addAll(inboxMessages);
		return this;
	}

	private List<MessageReport> messageReports;

	public List<MessageReport> getMessageReports() {
		return messageReports;
	}

	public void setMessageReports(List<MessageReport> messageReports) {
		this.messageReports = messageReports;
	}

	public List<MessageReport> messageReports() {
		if (messageReports == null) {
			messageReports = CollectionUtil.getList(MessageReport.class);
		}
		return this.messageReports;
	}

	public MessageBoxEvent addMessageReport(MessageReport messageReport) {
		this.messageReports().add(messageReport);
		return this;
	}

	public MessageBoxEvent addMessageReport(List<MessageReport> messageReports) {
		this.messageReports().addAll(messageReports);
		return this;
	}
	
	// ===== TEMPLATE STATUS EVENTS =====
	
	private List<TemplateStatusEvent> templateStatusEvents;

	public List<TemplateStatusEvent> getTemplateStatusEvents() {
		return templateStatusEvents;
	}

	public void setTemplateStatusEvents(List<TemplateStatusEvent> templateStatusEvents) {
		this.templateStatusEvents = templateStatusEvents;
	}

	public List<TemplateStatusEvent> templateStatusEvents() {
		if (templateStatusEvents == null) {
			templateStatusEvents = CollectionUtil.getList(TemplateStatusEvent.class);
		}
		return this.templateStatusEvents;
	}

	public MessageBoxEvent addTemplateStatusEvent(TemplateStatusEvent event) {
		this.templateStatusEvents().add(event);
		return this;
	}

	public MessageBoxEvent addTemplateStatusEvent(List<TemplateStatusEvent> events) {
		this.templateStatusEvents().addAll(events);
		return this;
	}
}
