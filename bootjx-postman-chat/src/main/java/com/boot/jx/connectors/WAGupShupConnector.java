package com.boot.jx.connectors;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.boot.jx.dict.ContactType;
import com.boot.jx.dict.FileFormat;
import com.boot.jx.dict.FileType;
import com.boot.jx.model.CommonFile;
import com.boot.jx.model.CommonFileStream;
import com.boot.jx.mongo.CommonMongoTemplate;
import com.boot.jx.postman.PMConstants.CHANNEL_TYPE;
import com.boot.jx.postman.client.PMFileStoreClient;
import com.boot.jx.postman.client.TmplClient;
import com.boot.jx.postman.doc.ChatContactDoc;
import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.doc.CustomerProfileDoc;
import com.boot.jx.postman.gupshup.GupShupClientChat;
import com.boot.jx.postman.gupshup.GupShupClientNotify;
import com.boot.jx.postman.gupshup.GupShupDeliveryResp;
import com.boot.jx.postman.gupshup.GupShupDeliveryResp.GupShupDeliveryDto;
import com.boot.jx.postman.gupshup.GupShupInbound;
import com.boot.jx.postman.gupshup.GupShupInbound.MediaObject;
import com.boot.jx.postman.model.Attachment;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.Message;
import com.boot.jx.postman.model.Message.Status;
import com.boot.jx.postman.model.MessageBoxEvent;
import com.boot.jx.postman.model.MessageDefinitions.IMessageExtended;
import com.boot.jx.postman.model.MessageReport;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.model.outbound.OutBoundMsgBasic.ApiOutBoundMsg;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.plugin.ChannelPluginProvider.ConnectorMapping;
import com.boot.jx.postman.plugin.WAGupShupPlugin;
import com.boot.jx.postman.plugin.WAGupShupPlugin.GupShupConfigDetails;
import com.boot.jx.postman.query.ChatContactQuery;
import com.boot.jx.postman.store.MessageContext;
import com.boot.jx.utils.PostManUtil;
import com.boot.model.MapModel;
import com.boot.utils.ArgUtil;
import com.boot.utils.JsonUtil;
import com.boot.utils.TimeUtils;

@Component
@ConnectorMapping(contactType = ContactType.WHATSAPP, channel = CHANNEL_TYPE.WA_GUPSHUP)
public class WAGupShupConnector extends AbstractConnector<GupShupConfigDetails, WAGupShupPlugin> {

	private static final Logger LOGGER = LoggerFactory.getLogger(WAGupShupConnector.class);

	@Autowired
	private GupShupClientChat gupShupChatClient;

	@Autowired
	private GupShupClientNotify gupShupNotifyClient;

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private MessageContext messageContext;

	@Autowired
	private CommonMongoTemplate commonMongoTemplate;

	@Autowired
	private TmplClient tmplClient;

	@Autowired
	private PMFileStoreClient pmFileStoreClient;

	public Status sendInternal(ChannelConfig channelConfig, OutboxMessage outboxMessage, boolean isPushMessage) {
		LOGGER.debug("sendInternal(OutboxMessage {}, boolean {})", outboxMessage, isPushMessage);
		try {
			if (isPushMessage) {
				gupShupNotifyClient.send(null, outboxMessage);
			} else {
				gupShupChatClient.send(null, outboxMessage);
			}
			outboxMessage.updateStatus(Message.Status.SENT);
			return Message.Status.SENT;
		} catch (Exception e) {
			outboxMessage.updateStatus(OutboxMessage.Status.SENT_ERR);
			outboxMessage.logs().add(e.getMessage());
			LOGGER.error("SEND ERROR", e);
			return Message.Status.SENT_ERR;
		}
	}

	@Override
	public Status send(ChannelConfig channelConfig, ChatContactDoc chatContactDoc, OutboxMessage outboxMessage) {
		outboxMessage.contact().setChannelType(chatContactDoc.getChannelType());
		outboxMessage.contact().setLane(chatContactDoc.getLane());
		template(channelConfig, chatContactDoc, outboxMessage);

		if (TimeUtils.isExpired(chatContactDoc.getLastInBoundStamp(), DEFAULT_SESISON_PERIOD)
				&& outboxMessage.optionsAsModel().entry("wa-template-id").exists()) {
			if (ArgUtil.isEmptyValue(chatContactDoc.getLastOptInStamp())) {
				gupShupNotifyClient.optIn(outboxMessage);
				commonMongoTemplate.updateFirst(
						new ChatContactQuery(chatContactDoc).setLastOptInStamp(System.currentTimeMillis()));
			}
			outboxMessage.messageMetaWrapper().sendType("PM"); // Push Message
			return this.sendInternal(channelConfig, outboxMessage, true);
		} else {
			outboxMessage.messageMetaWrapper().sendType("SM"); // Session Message
			return this.sendInternal(channelConfig, outboxMessage, false);
		}
	}

	@Override
	public Status reply(ChannelConfig channelConfig, ChatContactDoc chatContactDoc, OutboxMessage outboxMessage,
			IMessageExtended inboxMessage) {
		template(channelConfig, chatContactDoc, outboxMessage);
		return this.sendInternal(channelConfig, outboxMessage, false);
	}

	@Override
	public InboxMessage assignToAgent(InboxMessage inboxMessage) {
		return inboxMessage;
	}

	@Override
	public CustomerProfileDoc findProfile(ChatContactDoc chatContactDoc) {
		return contactStore.findProfileByPhone(chatContactDoc.phone());
	}

	@Override
	public OutboxMessage initSession(ChatSessionDoc session, InboxMessage inboxMessage) {
		if (ArgUtil.is(inboxMessage.getOriginalMessage())) {
			GupShupInbound dm = JsonUtil.parse(inboxMessage.getOriginalMessage(), GupShupInbound.class);
			ChatContactQuery contactQuery = messageContext.contact();
			contactQuery.setName(dm.getName());
			contactQuery.setPhone(dm.getMobile());
		}
		return null;
	}

	public InboxMessage toInboxMessage(GupShupInbound inbound) {

		ChannelConfig channelConfig = null;
		InboxMessage inboxMessage = this.createInboxMessage(channelConfig);

		inboxMessage.contact().setContactType(ContactType.WHATSAPP.toString());
		inboxMessage.contact().setChannelType(CHANNEL_TYPE.WA_GUPSHUP);
		inboxMessage.contact().setLane(inbound.getWaNumber());
		inboxMessage.contact().setCsid(inbound.getMobile());
		inboxMessage.contact().setName(inbound.getName());
		inboxMessage.contact().phone(inbound.getMobile());

		inboxMessage.setFrom(inbound.getMobile());
		inboxMessage.setFromName(inbound.getName());
		inboxMessage.setMessage(inbound.getText());
		inboxMessage.to().add(inbound.getWaNumber());
		inboxMessage.setMessageIdExt(inbound.getReplyId());

		if (ArgUtil.is(inbound.getImage())) {
			formatMedia(inboxMessage, inbound.getDocument(), channelConfig, FileType.IMAGE);
		} else if (ArgUtil.is(inbound.getDocument())) {
			formatMedia(inboxMessage, inbound.getDocument(), channelConfig, FileType.DOCUMENT);
		} else if (ArgUtil.is(inbound.getAudio())) {
			formatMedia(inboxMessage, inbound.getDocument(), channelConfig, FileType.AUDIO);
		} else if (ArgUtil.is(inbound.getVoice())) {
			formatMedia(inboxMessage, inbound.getDocument(), channelConfig, FileType.AUDIO);
		} else if (ArgUtil.is(inbound.getVideo())) {
			formatMedia(inboxMessage, inbound.getDocument(), channelConfig, FileType.VIDEO);
		}

		return inboxMessage;
	}

	private void formatMedia(InboxMessage inboxMessage, MediaObject mediaObject, ChannelConfig channelConfig,
			FileType fileType) {

		try {
			CommonFileStream srcFile = new CommonFileStream().url(mediaObject.getUrl() + mediaObject.getSignature())
					.fileType(fileType).format(FileFormat.from(mediaObject.getMimeType()));
			CommonFile dstFile = pmFileStoreClient.uploadSessionFileAsync(srcFile,
					PostManUtil.createContactId(inboxMessage), inboxMessage.getMessageIdExt());
			inboxMessage.attachment(new Attachment().mediaURL(dstFile.getUrl()).mediaType(dstFile.getFileType())
					.mediaSrc(srcFile.getUrl()).mediaCaption(mediaObject.getCaption()));
		} catch (IOException e) {
			logManager.error(inboxMessage, e);
		}
	}

	@Override
	public Status onSend(ChannelConfig channelConfig, ChatContactDoc chatContactDoc, OutboxMessage outboxMessage,
			ApiOutBoundMsg outBoundMsg) {
		// TODO Auto-generated method stub
		return Message.Status.SENT;
	}

	public List<MessageReport> updateDeliveryStatus(GupShupDeliveryResp status) {
		List<MessageReport> batch = new LinkedList<MessageReport>();
		for (GupShupDeliveryDto gupShupDelivery : status.getResponse()) {
			MessageReport report = new MessageReport();
			report.contact().type(ContactType.WHATSAPP);
			report.setChangeStamp(gupShupDelivery.getEventTs());

			report.setMessageIdExt(gupShupDelivery.getExternalId());
			String[] x = gupShupDelivery.getExternalId().split("-");
			if (x.length == 2) {
				report.setMessageId(x[1]);
			}
			if ("SENT".equals(gupShupDelivery.getEventType())) {
				report.setStatus(Status.SENTX);
			} else if ("DELIVERED".equals(gupShupDelivery.getEventType())) {
				report.setStatus(Status.DLVRD);
			} else if ("READ".equals(gupShupDelivery.getEventType())) {
				report.setStatus(Status.READ);
			} else if ("FAILED".equals(gupShupDelivery.getEventType())) {
				report.setStatus(Status.FAILD);
				if ("BLOCKED_FOR_USER".equalsIgnoreCase(gupShupDelivery.getCause())) {
					report.setStatus(Status.BLCKD);
				}
				report.setReason(gupShupDelivery.getCause());
			}
			batch.add(report);
		}
		return batch;
	}

	@Override
	public MessageBoxEvent inboundMessageBoxEvent(ChannelConfig channelConfig, MapModel requestMap,
			MessageBoxEvent messageBoxEvent) {
		return messageBoxEvent.addInboxMessage(toInboxMessage(requestMap.as(GupShupInbound.class)));
	}

}
