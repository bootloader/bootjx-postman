/*package com.boot.jx.connectors;

import java.util.ArrayList;
import java.util.List;

import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.boot.jx.AppContextUtil;
import com.boot.jx.chat.ConnectorHandlerFactory.ConnectorMapping;
import com.boot.jx.dict.ContactType;
import com.boot.jx.model.CommonFile;
import com.boot.jx.model.CommonFileStream;
import com.boot.jx.postman.PMConstants.PROPERTIES;
import com.boot.jx.postman.client.PMFileStoreClient;
import com.boot.jx.postman.doc.ChatContactDoc;
import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.model.Attachment;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.MessageBoxEvent;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.model.TmplElement;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.plugin.FirebasePlugin;
import com.boot.jx.postman.plugin.FirebasePlugin.FirebaseConfigDetails;
import com.boot.jx.postman.query.ChatContactQuery;
import com.boot.jx.postman.store.MessageStore;
import com.boot.jx.stomp.StompTunnelService;
import com.boot.jx.utils.PostManUtil;
import com.boot.model.MapModel;
import com.boot.utils.ArgUtil;
import com.boot.utils.JsonUtil;

@Component
@ConnectorMapping(contactType = ContactType.PUSH)
public class FirebaseConnector extends AbstractConnector<FirebaseConfigDetails, FirebasePlugin> {

	private static final String WEB_USER_MESSAGE_STR = "WEB_USER_MESSAGE_STR_";
	private static final Logger LOGGER = LoggerFactory.getLogger(FirebaseConnector.class);

	@Value("${app.stomp}")
	boolean stompEnabled;

	@Autowired
	private PMFileStoreClient pmFileStoreClient;

	@Autowired(required = false)
	RedissonClient redisson;

	@Autowired
	private StompTunnelService stompTunnelService;

	@Autowired
	private MessageStore messageStore;

	@Override
	public void onSend(ChannelConfig channelConfig, ChatContactDoc chatContactDoc, OutboxMessage outboxMessage) {
		String contactId = outboxMessage.contact().getContactId();
		template(channelConfig, chatContactDoc, outboxMessage);
		String contactIdWeb = AppContextUtil.getTenant() + "/" + contactId;
		if (redisson == null) {
			outboxMessage.updateStatus(OutboxMessage.Status.SENT);
		} else {
			if (!stompEnabled && environment.keyEntry(PROPERTIES.POSTMAN_CHAT_WEB_QUEUE).asBoolean()) {
				LOGGER.debug("sendReply to " + contactIdWeb);
				RBlockingQueue<String> messageQueue = redisson.getBlockingQueue(WEB_USER_MESSAGE_STR + contactIdWeb);
				messageQueue.add(JsonUtil.toJson(outboxMessage));
			}
			stompTunnelService.sendToTag(contactIdWeb, "/message/receive/new", outboxMessage);
		}
	}

	@Override
	public OutboxMessage initSession(ChatSessionDoc session, InboxMessage inboxMessage) {

		ChatContactQuery contactQuery = messageContext.contact();
		ChatContactDoc chatContactDoc = messageContext.contact().getDoc();

		if (ArgUtil.is(inboxMessage.getForm())) {
			if (ArgUtil.is(inboxMessage.getForm().get("name"))) {
				String name = ArgUtil.parseAsString(inboxMessage.getForm().get("name"));
				contactQuery.setName(name);
			}
			if (ArgUtil.is(inboxMessage.getForm().get("email"))) {
				String email = ArgUtil.parseAsString(inboxMessage.getForm().get("email"));
				contactQuery.setEmail(email);
				contactQuery.setEmailVerified(false);
			}
			if (ArgUtil.is(inboxMessage.getForm().get("phone"))) {
				contactQuery.setPhone(ArgUtil.parseAsString(inboxMessage.getForm().get("phone")));
				contactQuery.setPhoneVerified(false);
			}
		}

		List<TmplElement> inputs = new ArrayList<TmplElement>();
		if (ArgUtil.isEmpty(chatContactDoc.getName())) {
			inputs.add(new TmplElement().code("name").label("Name").type("TEXT"));
//			return (OutboxMessage) inboxMessage.replyMessage("Please fill below inputs to continue").option("inputs",
//					inputs);
		}

		return null;
	}

	public InboxMessage toInboxMessage(ChannelConfig channelConfig, MapModel map, MultipartFile file) {
		// Create Default Message from Channel
		InboxMessage inboxMessage = this.createInboxMessage(channelConfig, map.as(InboxMessage.class));
		inboxMessage.setSessionId(null);
		inboxMessage.setMessageId(null);

		inboxMessage.contact().setCsid(inboxMessage.getFrom());
		inboxMessage.session().setAgent(null);
		inboxMessage.session().setDept(null);

		CommonFileStream srcFile = new CommonFileStream().from(file);

		if (ArgUtil.is(file)) {
			CommonFile dstFile = pmFileStoreClient.uploadSessionFile(file, PostManUtil.createContactId(inboxMessage),
					inboxMessage.getMessageIdExt());
			inboxMessage.attachment(new Attachment().mediaURL(dstFile.getUrl()).mediaType(dstFile.getFileType())
					.mediaSrc(srcFile.getUrl()).mediaName(srcFile.getName()).mediaMimeType(srcFile.getContentType()));
		}

		return inboxMessage;
	}

	public MessageBoxEvent inboundMessageBoxEvent(ChannelConfig channelConfig, MapModel requestMap,
			MessageBoxEvent messageBoxEvent, MultipartFile file) {
		return messageBoxEvent.addInboxMessage(toInboxMessage(channelConfig, requestMap, file));
	}

	@Override
	public MessageBoxEvent inboundMessageBoxEvent(ChannelConfig channelConfig, MapModel requestMap,
			MessageBoxEvent messageBoxEvent) {
		return null;
	}

}*/
