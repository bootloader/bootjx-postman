package com.boot.jx.connectors;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.boot.jx.dict.ContactType;
import com.boot.jx.exception.AmxApiException;
import com.boot.jx.logger.LoggerService;
import com.boot.jx.postman.PMConstants.CHANNEL_TYPE;
import com.boot.jx.postman.channel.RCSClient;
import com.boot.jx.postman.doc.ChatContactDoc;
import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.doc.CustomerProfileDoc;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.Message;
import com.boot.jx.postman.model.Message.Status;
import com.boot.jx.postman.model.MessageBoxEvent;
import com.boot.jx.postman.model.MessageReport;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.model.outbound.OutBoundMsgBasic.ApiOutBoundMsg;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.plugin.ChannelPluginProvider.ConnectorMapping;
import com.boot.jx.postman.plugin.RCSPlugin;
import com.boot.jx.postman.plugin.RCSPlugin.RCSConfigDetails;
import com.boot.model.MapModel;
import com.boot.utils.ArgUtil;

/**
 * Telinfy hub RCS connector — template send + inbound webhooks (user message,
 * button click, delivery status).
 */
@Component
@ConnectorMapping(contactType = ContactType.RCS, channel = CHANNEL_TYPE.RCS_TELINFY)
public class RCSConnector extends AbstractConnector<RCSConfigDetails, RCSPlugin> {

	private static Logger LOGGER = LoggerService.getLogger(RCSConnector.class);

	@Autowired
	private RCSClient rcsClient;

	@Override
	public Status onSend(ChannelConfig channelConfig, ChatContactDoc chatContactDoc, OutboxMessage outboxMessage,
			ApiOutBoundMsg outBoundMsg) {
		try {
			rcsClient.sendRCS(channelConfig, outboxMessage);
			outboxMessage.updateStatus(OutboxMessage.Status.SENT);
			return Message.Status.SENT;
		} catch (AmxApiException e) {
			outboxMessage.updateStatus(OutboxMessage.Status.SENT_ERR);
			outboxMessage.logs().add(e.getErrorKey());
			return Message.Status.SENT_ERR;
		}
	}

	@Override
	public CustomerProfileDoc findProfile(ChatContactDoc chatContactDoc) {
		return contactStore.findProfileByPhone(chatContactDoc.phone());
	}

	@Override
	public OutboxMessage initSession(ChatSessionDoc session, InboxMessage inboxMessage) {
		return null;
	}

	@Override
	public MessageBoxEvent inboundMessageBoxEvent(ChannelConfig channelConfig, MapModel requestMap,
			MessageBoxEvent messageBoxEvent) {

		// Delivery / read / undelivered status webhooks
		String eventDLR = requestMap.entry("eventDLR").asString();
		if (ArgUtil.is(eventDLR)) {
			MessageReport report = toMessageReport(channelConfig, requestMap, eventDLR);
			return messageBoxEvent.addMessageReport(report);
		}

		// User text message or button click
		if (ArgUtil.is(requestMap.entry("user_Messaged").asString())
				|| ArgUtil.is(requestMap.entry("user_action_clicked").asString())
				|| ArgUtil.is(requestMap.entry("senderPhoneNumber").asString())) {
			InboxMessage inboxMessage = toInboxMessage(channelConfig, requestMap);
			if (ArgUtil.is(inboxMessage.getMessage()) || ArgUtil.is(inboxMessage.contact().getCsid())) {
				return messageBoxEvent.addInboxMessage(inboxMessage);
			}
		}

		LOGGER.debug("Unhandled RCS inbound payload: {}", requestMap.toJson());
		return messageBoxEvent;
	}

	public InboxMessage toInboxMessage(ChannelConfig channelConfig, MapModel requestMap) {
		InboxMessage inboxMessage = this.createInboxMessage(channelConfig);

		String phone = normalizePhone(requestMap.entry("senderPhoneNumber").asString());
		inboxMessage.contact().setCsid(phone);
		inboxMessage.contact().phone(phone);
		inboxMessage.setFrom(phone);
		inboxMessage.to().add(channelConfig.getLane());

		String messageId = ArgUtil.nonEmpty(requestMap.entry("messageId").asString(),
				requestMap.entry("recordID").asString());
		inboxMessage.setMessageIdExt(messageId);

		String userMessage = requestMap.entry("user_Messaged").asString();
		String buttonClick = requestMap.entry("user_action_clicked").asString();

		if (ArgUtil.is(buttonClick)) {
			inboxMessage.form().put("reply_title", buttonClick);
			inboxMessage.form().put("reply_id", buttonClick);
			inboxMessage.setMessage(buttonClick);
		} else if (ArgUtil.is(userMessage)) {
			inboxMessage.setMessage(userMessage);
		}

		return inboxMessage;
	}

	private MessageReport toMessageReport(ChannelConfig channelConfig, MapModel requestMap, String eventDLR) {
		MessageReport report = this.createMessageReport(channelConfig);

		String messageId = ArgUtil.nonEmpty(requestMap.entry("messageId").asString(), null);
		String recordId = requestMap.entry("recordID").asString();
		report.setMessageIdExt(ArgUtil.nonEmpty(messageId, recordId));
		if (ArgUtil.is(messageId)) {
			report.setMessageId(messageId);
		}

		String phone = normalizePhone(requestMap.entry("senderPhoneNumber").asString());
		if (ArgUtil.is(phone)) {
			report.contact().setCsid(phone);
			report.contact().phone(phone);
		}

		String dlr = eventDLR.toUpperCase().replace("-", "_").replace(" ", "_");
		if (dlr.contains("READ")) {
			report.setStatus(Status.READ);
		} else if (dlr.contains("DELIVERED") && !dlr.contains("UN")) {
			report.setStatus(Status.DLVRD);
		} else if (dlr.contains("UNDELIVER") || dlr.contains("FAILED") || dlr.contains("FAIL")) {
			report.setStatus(Status.FAILD);
			report.setReason(eventDLR);
		} else {
			report.setStatus(Status.SENTX);
		}

		report.setChangeStamp(System.currentTimeMillis());
		return report;
	}

	private String normalizePhone(String phone) {
		if (!ArgUtil.is(phone)) {
			return phone;
		}
		return phone.replace("+", "").replaceAll("\\s+", "");
	}

}
