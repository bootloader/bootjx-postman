package com.boot.jx.connectors;

import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.boot.jx.AppConfig;
import com.boot.jx.dict.ContactType;
import com.boot.jx.email.EmailReplyParser;
import com.boot.jx.http.CommonHttpRequest;
import com.boot.jx.logger.LoggerService;
import com.boot.jx.postman.PMConstants.CHANNEL_TYPE;
import com.boot.jx.postman.doc.ChatContactDoc;
import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.doc.CustomerProfileDoc;
import com.boot.jx.postman.doc.MessageDoc;
import com.boot.jx.postman.doc.MessageTempInbound;
import com.boot.jx.postman.doc.config.ChannelConfigLogger;
import com.boot.jx.postman.dto.ChatMessageDTO;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.Message;
import com.boot.jx.postman.model.Message.Status;
import com.boot.jx.postman.model.MessageBoxEvent;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.model.ext.InBoundMsg;
import com.boot.jx.postman.model.ext.InBoundMsgStatus;
import com.boot.jx.postman.model.ext.InBoundWrapper;
import com.boot.jx.postman.model.outbound.OutBoundMsgBasic.ApiOutBoundMsg;
import com.boot.jx.postman.nexus.NexusEmailClient;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.plugin.ChannelPluginProvider.ConnectorMapping;
import com.boot.jx.postman.plugin.ImapPlugin;
import com.boot.jx.postman.plugin.ImapPlugin.ImapConfigDetails;
import com.boot.jx.postman.store.MessageStore;
import com.boot.jx.rest.RestService;
import com.boot.jx.utils.PostManUtil;
import com.boot.model.MapModel;
import com.boot.model.MapModel.MapPathEntry;
import com.boot.utils.ArgUtil;

@Component
@ConnectorMapping(contactType = ContactType.EMAIL, channel = CHANNEL_TYPE.IMAP)
public class ImapConnector extends AbstractConnector<ImapConfigDetails, ImapPlugin> {

	private static Logger LOGGER = LoggerService.getLogger(ImapConnector.class);

	@Autowired
	private RestService restService;

	@Autowired
	private CommonHttpRequest commonHttpRequest;

	@Autowired
	private AppConfig appConfig;

	@Autowired
	private NexusEmailClient nexusEmailClient;

	@Autowired
	private MessageStore messageStore;

	@Override
	public void onChannelUpdate(ChannelConfig channelConfig, ChannelConfigLogger channelConfigLogger) {
		nexusEmailClient.subscribe(channelConfig);
	}

	@Override
	public Status onSend(ChannelConfig channelConfig, ChatContactDoc chatContactDoc, OutboxMessage outboxMessage,
			ApiOutBoundMsg outBoundMsg) {
		ChatSessionDoc chatSession = ArgUtil.is(context().session()) ? context().session().getDoc() : null;

		if (!ArgUtil.is(outboxMessage.getReplyIdExt())) {
			if (ArgUtil.is(chatSession)) {
				ChatMessageDTO lastMsg = chatSession.lastMsg();
				if (ArgUtil.is(lastMsg) && ArgUtil.is(lastMsg.getMessageIdExt())) {
					outboxMessage.setReplyIdExt(lastMsg.getMessageIdExt());
					outboxMessage.setReplyId(lastMsg.getMessageId());
				} else if (ArgUtil.is(lastMsg) && ArgUtil.is(lastMsg.getMessageId())) {
					MessageDoc lastMsgDoc = messageStore.findById(lastMsg.getMessageId(), ContactType.EMAIL);
					outboxMessage.setReplyIdExt(lastMsgDoc.getMessageIdExt());
					outboxMessage.setReplyId(lastMsg.getMessageId());
				}
			}

			if (!ArgUtil.is(outboxMessage.getReplyIdExt())) {
				ChatMessageDTO lastMsg = chatSession.lastInBoundMsg();
				if (ArgUtil.is(lastMsg) && ArgUtil.is(lastMsg.getMessageIdExt())) {
					outboxMessage.setReplyIdExt(lastMsg.getMessageIdExt());
					outboxMessage.setReplyId(lastMsg.getMessageId());
				}
			}

			if (!ArgUtil.is(outboxMessage.getReplyIdExt())) {
				ChatMessageDTO lastMsg = chatSession.lastOutBoundMsg();
				if (ArgUtil.is(lastMsg) && ArgUtil.is(lastMsg.getMessageIdExt())) {
					outboxMessage.setReplyIdExt(lastMsg.getMessageIdExt());
					outboxMessage.setReplyId(lastMsg.getMessageId());
				}
			}

		}

		if (!ArgUtil.is(outboxMessage.getSubject())) {
			if (ArgUtil.is(chatSession)) {
				outboxMessage.setSubject(chatSession.getSubject());
			}
		}

		if (ArgUtil.is(chatSession) && ArgUtil.is(chatSession.getTicketHash())) {
			outboxMessage.session().setTicketHash(chatSession.getTicketHash());
		}

		nexusEmailClient.send(channelConfig, outboxMessage);
		outboxMessage.updateStatus(OutboxMessage.Status.SENT);
		return Message.Status.SENT;
	}

	@Override
	public CustomerProfileDoc findProfile(ChatContactDoc chatContactDoc) {
		return contactStore.findProfileByPhone(chatContactDoc.phone());
	}

	@Override
	public OutboxMessage initSession(ChatSessionDoc session, InboxMessage inboxMessage) {
		return null;
	}

	public InboxMessage toInboxMessage(ChannelConfig channelConfig, MessageTempInbound inbound)
			throws NoSuchAlgorithmException {

		// Create Default Message from Channel
		InboxMessage inboxMessage = this.createInboxMessage(channelConfig);

		MapModel m = MapModel.from(inbound.getData());

		// Set Contact info
		MapPathEntry from = m.entry("from");
		if (!ArgUtil.is(from.exists())) {
			return null;
		}

		inboxMessage.contact().setEmail(from.pathEntry("emailAddress.address").asString());
		inboxMessage.contact().setCsid(inboxMessage.contact().getEmail());
		inboxMessage.contact().setName(from.pathEntry("emailAddress.name").asString());

		// Set Additional info
		inboxMessage.setFrom(inboxMessage.contact().getEmail());
		inboxMessage.setFromName(inboxMessage.contact().getName());
		inboxMessage.to().add(channelConfig.getLane());

		// Extract Message Details
		inboxMessage.setMessageIdExt(m.keyEntry("id").asString());
		// inboxMessage.setReplyIdExt(CollectionUtil.first(msg.getMimeMessage().getHeader("In-Reply-To")));
		inboxMessage.setSubject(m.keyEntry("subject").asString());
		inboxMessage.setMessage(EmailReplyParser.parseReply(m.pathEntry("body.content").asString()));
		inboxMessage.setMessageTrail(m.pathEntry("body.trail").asString());

		MapPathEntry conversationId = m.pathEntry("conversationId");
		if (conversationId.exists()) {
			inboxMessage.session().setTicketHash(conversationId.asString());
		} else if (ArgUtil.is(inboxMessage.getSubject())) {
			inboxMessage.session().setTicketHash(PostManUtil.createTicketHash(inboxMessage));
		}

		inboxMessage.setAttachments(inbound.getAttachments());
		inboxMessage.setReferral(inbound.getReferral());
		inboxMessage.setForm(inbound.getForm());

		return inboxMessage;
	}

	@Override
	public MessageBoxEvent inboundMessageBoxEvent(ChannelConfig channelConfig, MapModel requestMap,
			MessageBoxEvent messageBoxEvent) {

		InBoundWrapper inbound = requestMap.as(InBoundWrapper.class);

		if (ArgUtil.is(inbound.messages)) {
			for (InBoundMsg message : inbound.messages) {
				try {
					MessageTempInbound msg = commonMongoTemplate.findById(message.messageId, MessageTempInbound.class);
					if (ArgUtil.is(msg)) {
						messageBoxEvent.addInboxMessage(toInboxMessage(channelConfig, msg));
					} else {
						LOGGER.warn("no Message found for {} ", message.messageId);
					}
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				}
			}
		} else if (ArgUtil.is(inbound.statuses)) {
			for (InBoundMsgStatus status : inbound.statuses) {
				if (ArgUtil.is(status)) {
					messageBoxEvent.addMessageReport(toMessageReport(channelConfig, status));
				}
			}
		}
		return messageBoxEvent;
	}

}
