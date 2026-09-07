package com.boot.jx.connectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.boot.jx.dict.ContactType;
import com.boot.jx.postman.PMConfiguration;
import com.boot.jx.postman.client.PostManClient;
import com.boot.jx.postman.doc.ChatContactDoc;
import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.doc.CustomerProfileDoc;
import com.boot.jx.postman.gupshup.GupShupClientAgent;
import com.boot.jx.postman.gupshup.GupShupClientChat;
import com.boot.jx.postman.gupshup.GupShupClientNotify;
import com.boot.jx.postman.gupshup.GupShupInboundV2;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.Message;
import com.boot.jx.postman.model.Message.Status;
import com.boot.jx.postman.model.MessageBox;
import com.boot.jx.postman.model.MessageBoxEvent;
import com.boot.jx.postman.model.MessageDefinitions.IMessageExtended;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.model.WAMessage.Channel;
import com.boot.jx.postman.model.outbound.OutBoundMsgBasic.ApiOutBoundMsg;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.plugin.ChannelPluginProvider.ConnectorMapping;
import com.boot.jx.postman.plugin.WAGupShupPlugin;
import com.boot.jx.postman.plugin.WAGupShupPlugin.GupShupConfigDetails;
import com.boot.jx.utils.PostManUtil;
import com.boot.model.MapModel;
import com.boot.utils.ArgUtil;

@Component
@ConnectorMapping(contactType = ContactType.WHATSAPP, channel = "GUPSHUPAGENT")
public class WAGupShupAgentConnector extends AbstractConnector<GupShupConfigDetails, WAGupShupPlugin> {

	@Autowired
	private GupShupClientChat gupShupChatClient;

	@Autowired
	private GupShupClientNotify gupShupNotifyClient;

	@Autowired
	private GupShupClientAgent gupShupAgentClient;

	@Autowired
	private PostManClient postManClient;

	@Override
	public Status send(ChannelConfig channelConfig, ChatContactDoc chatContactDoc, OutboxMessage outboxMessage) {
		outboxMessage.contact().setChannelType(chatContactDoc.getChannelType());
		outboxMessage.contact().setLane(chatContactDoc.getLane());
		if (ArgUtil.isEqual(outboxMessage.contact().getChannelType(), Channel.GUPSHUPAGENT.toString())) {
			if (outboxMessage.isViaAgent() && ArgUtil.isEmpty(outboxMessage.getFiles())) {
				gupShupChatClient.sendMessage(chatContactDoc.getCsid(), outboxMessage.getMessage());
			} else if (outboxMessage.isTemplateMsg() || outboxMessage.isQRButtons()) {
				gupShupNotifyClient.send(channelConfig, outboxMessage);
			} else {
				gupShupChatClient.send(channelConfig, outboxMessage);
			}
		} else if (ArgUtil.isEqual(outboxMessage.contact().getChannelType(), Channel.DEFAULT.toString())) {
			MessageBox mb = new MessageBox();
			mb.push(outboxMessage);
			postManClient.send(mb);
		}
		return Message.Status.SENT;
	}

	@Override
	public Status reply(ChannelConfig channelConfig, ChatContactDoc chatContactDoc, OutboxMessage outboxMessage,
			IMessageExtended inboxMessage) {
		outboxMessage.contact().setChannelType(inboxMessage.contact().getChannelType());
		outboxMessage.contact().setLane(inboxMessage.contact().getLane());
		if (ArgUtil.isEqual(inboxMessage.contact().getChannelType(), Channel.GUPSHUPAGENT.toString())) {
			if (outboxMessage.isViaAgent() && ArgUtil.isEmpty(outboxMessage.getFiles())) {
				gupShupAgentClient.sendViaAgent(inboxMessage, outboxMessage.getMessage());
			} else if (outboxMessage.isTemplateMsg() || outboxMessage.isQRButtons()) {
				// gupShupNotifyClient.optIn(inboxMessage.getFrom());
				gupShupNotifyClient.send(channelConfig, outboxMessage);
			} else {
				gupShupChatClient.send(channelConfig, outboxMessage);
			}
		} else if (ArgUtil.isEqual(inboxMessage.contact().getChannelType(), Channel.DEFAULT.toString())) {
			Message<?> reply = inboxMessage.replyMessage(outboxMessage.getMessage());
			MessageBox mb = new MessageBox();
			mb.push(reply);
			postManClient.send(mb);
		}
		return Message.Status.SENT;
	}

	@Override
	public InboxMessage assignToAgent(InboxMessage inboxMessage) {

		PMConfiguration config = environment.local();
		String channelId = PostManUtil.CHANNEL_ID(inboxMessage.contact());
		ChannelConfig channelConfig = config.channel(channelId);

		if (ArgUtil.isEqual(inboxMessage.contact().getChannelType(), Channel.GUPSHUPAGENT.toString())) {
			gupShupAgentClient.assignToAgent(inboxMessage);
		} else if (ArgUtil.isEqual(inboxMessage.contact().getChannelType(), Channel.DEFAULT.toString())) {
			Message<?> reply = inboxMessage.replyMessage("Call us @ " + channelConfig.getGupshup().getNumber());
			MessageBox mb = new MessageBox();
			mb.push(reply);
			postManClient.send(mb);
		}
		return inboxMessage;
	}

	@Override
	public CustomerProfileDoc findProfile(ChatContactDoc chatContactDoc) {
		return contactStore.findProfileByPhone(chatContactDoc.phone());
	}

	@Override
	public OutboxMessage initSession(ChatSessionDoc session, InboxMessage inboxMessage) {
		return null;
	}

	public InboxMessage toInboxMessage(GupShupInboundV2 inboundV2) {
		InboxMessage inboxMessage = new InboxMessage();
		inboxMessage.contact().setContactType(ContactType.WHATSAPP.toString());
		inboxMessage.contact().setChannelType(Channel.GUPSHUPAGENT.toString());

		if (ArgUtil.is(inboundV2.getMessages())) {
			inboxMessage.from(inboundV2.getMessages().get(0).getFrom());
			inboxMessage.setFromName(inboundV2.getContacts().get(0).getProfile().getName());

			if (ArgUtil.areEqual(inboundV2.getMessages().get(0).getType(), "button")) {
				inboxMessage.setMessage(inboundV2.getMessages().get(0).getButton().getText());
			} else {
				inboxMessage.setMessage(inboundV2.getMessages().get(0).getText().getBody());
			}

			inboxMessage.to().add(inboundV2.getContacts().get(0).getWaId());
			inboxMessage.setMessageIdExt(inboundV2.getMessages().get(0).getId());
		}

		return inboxMessage;
	}

	@Override
	public Status onSend(ChannelConfig channelConfig, ChatContactDoc chatContactDoc, OutboxMessage outboxMessage,
			ApiOutBoundMsg outBoundMsg) {
		// TODO Auto-generated method stub
		return Message.Status.SENT;
	}

	@Override
	public MessageBoxEvent inboundMessageBoxEvent(ChannelConfig channelConfig, MapModel requestMap,
			MessageBoxEvent messageBoxEvent) {
		return messageBoxEvent.addInboxMessage(toInboxMessage(requestMap.as(GupShupInboundV2.class)));
	}
}
