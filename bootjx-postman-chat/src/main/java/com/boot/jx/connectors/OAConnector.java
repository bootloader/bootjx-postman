package com.boot.jx.connectors;

import java.util.Map;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.boot.jx.dict.ContactType;
import com.boot.jx.exception.AmxApiException;
import com.boot.jx.logger.LoggerService;
import com.boot.jx.postman.PMConstants.CHANNEL_TYPE;
import com.boot.jx.postman.channel.SMSClient;
import com.boot.jx.postman.doc.ChatContactDoc;
import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.doc.CustomerProfileDoc;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.Message;
import com.boot.jx.postman.model.Message.Status;
import com.boot.jx.postman.model.MessageBoxEvent;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.model.outbound.OutBoundMsgBasic.ApiOutBoundMsg;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.plugin.ChannelPluginProvider.ConnectorMapping;
import com.boot.jx.postman.plugin.OAPlugin;
import com.boot.jx.postman.plugin.OAPlugin.OAConfigDetails;
import com.boot.model.MapModel;

@Component
@ConnectorMapping(contactType = ContactType.OA, channel = CHANNEL_TYPE.OA)
public class OAConnector extends AbstractConnector<OAConfigDetails, OAPlugin> {

	private static Logger LOGGER = LoggerService.getLogger(OAConnector.class);

	@Autowired
	private SMSClient clientClient;

	@Override
	public Status onSend(ChannelConfig channelConfig, ChatContactDoc chatContactDoc, OutboxMessage outboxMessage,
			ApiOutBoundMsg outBoundMsg) {
		try {
			// generic
			clientClient.sendSMS(channelConfig, outboxMessage);
			outboxMessage.updateStatus(OutboxMessage.Status.SENT);
			return Message.Status.SENT;
		} catch (AmxApiException e) {
			outboxMessage.updateStatus(OutboxMessage.Status.SENT_ERR);
			outboxMessage.logs().add(((AmxApiException) e).getErrorKey());
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

	public InboxMessage toInboxMessage(ChannelConfig channelConfig, Map<String, Object> dataMap) {
		InboxMessage inboxMessage = this.createInboxMessage(channelConfig);
		return inboxMessage;
	}

	@Override
	public MessageBoxEvent inboundMessageBoxEvent(ChannelConfig channelConfig, MapModel requestMap,
			MessageBoxEvent messageBoxEvent) {
		return messageBoxEvent.addInboxMessage(toInboxMessage(channelConfig, requestMap.toMap()));
	}

}
