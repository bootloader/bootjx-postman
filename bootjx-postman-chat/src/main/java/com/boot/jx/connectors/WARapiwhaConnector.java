package com.boot.jx.connectors;

import java.util.Map;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.boot.jx.dict.ContactType;
import com.boot.jx.dict.FileType;
import com.boot.jx.logger.LoggerService;
import com.boot.jx.postman.client.TmplClient;
import com.boot.jx.postman.doc.ChatContactDoc;
import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.doc.CustomerProfileDoc;
import com.boot.jx.postman.doc.QuickMedia;
import com.boot.jx.postman.model.Attachment;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.Message;
import com.boot.jx.postman.model.Message.Status;
import com.boot.jx.postman.model.MessageBoxEvent;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.model.outbound.OutBoundMsgBasic.ApiOutBoundMsg;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.plugin.ChannelPluginProvider.ConnectorMapping;
import com.boot.jx.postman.plugin.WebPlugin;
import com.boot.jx.postman.plugin.WebPlugin.WebConfigDetails;
import com.boot.jx.postman.query.ChatContactQuery;
import com.boot.jx.rest.RestService;
import com.boot.model.MapModel;
import com.boot.utils.ArgUtil;
import com.boot.utils.CollectionUtil;
import com.boot.utils.Constants;
import com.boot.utils.JsonUtil;

@Component
@ConnectorMapping(contactType = ContactType.WHATSAPP, channel = "RAPIWHA")
public class WARapiwhaConnector extends AbstractConnector<WebConfigDetails, WebPlugin> {

	private static Logger LOGGER = LoggerService.getLogger(WARapiwhaConnector.class);

	@Autowired
	private RestService restService;

	private String apiWhaKey;

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private TmplClient tmplClient;

	@Override
	public Status onSend(ChannelConfig channelConfig, ChatContactDoc chatContactDoc, OutboxMessage outboxMessage,
			ApiOutBoundMsg outBoundMsg) {
		String to = CollectionUtil.getOne(outboxMessage.getTo());

		outboxMessage.contact().setChannelType(outboxMessage.contact().getChannelType());
		String text = outboxMessage.getMessage();
		if (ArgUtil.is(outboxMessage.templateCode())) {
			QuickMedia mediaReply = mongoTemplate.findById(outboxMessage.templateCode(), QuickMedia.class);
			if (ArgUtil.is(mediaReply)) {
				if ("image".equalsIgnoreCase(mediaReply.getType())) {
					outboxMessage.attachment(
							new Attachment().mediaURL(mediaReply.getUrl()).mediaType(FileType.IMAGE.toString()));
					text = mediaReply.getUrl();
				}
			} else {
				tmplClient.process(outboxMessage, outboxMessage.contact());
				text = outboxMessage.getMessage();
			}
		}
		String responseText = restService.ajax("http://panel.apiwha.com/send_message.php").field("apikey", apiWhaKey)
				.field("number", to).field("text", text).postForm().asString();
		LOGGER.info(responseText);
		return Message.Status.SENT;
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
		Object x = inboxMessage.getOriginalMessage();
		if (ArgUtil.is(x)) {
			Map<String, Object> map = JsonUtil.toMap(x);
			ChatContactQuery contactQuery = messageContext.contact();
			contactQuery.setProfilePic(ArgUtil.parseAsString(map.get("profilepicture"), Constants.BLANK));
			contactQuery.setName(ArgUtil.parseAsString(map.get("pushname"), Constants.BLANK));
		}
		return null;
	}

	public InboxMessage toInboxMessage(Map<String, Object> dataMap, String lane) {
		InboxMessage event = new InboxMessage();
		String eventName = ArgUtil.parseAsString(dataMap.get("event"), Constants.BLANK);
		event.contact().setContactType(ContactType.WHATSAPP.toString());
		event.contact().setChannelType("RAPIWHA");
		event.contact().setLane(lane);
		if ("INBOX".equals(eventName)) {
			event.from(ArgUtil.parseAsString(dataMap.get("from"), Constants.BLANK));
			event.to().add(ArgUtil.parseAsString(dataMap.get("to"), Constants.BLANK));
			event.setMessage(ArgUtil.parseAsString(dataMap.get("text"), Constants.BLANK));
			event.setFromName(ArgUtil.parseAsString(dataMap.get("pushname"), Constants.BLANK));
			event.setOriginalMessage(dataMap);
		}
		return event;
	}

	@Override
	public MessageBoxEvent inboundMessageBoxEvent(ChannelConfig channelConfig, MapModel requestMap,
			MessageBoxEvent messageBoxEvent) {
		return messageBoxEvent.addInboxMessage(toInboxMessage(requestMap.toMap(), channelConfig.getLane()));
	}

}
