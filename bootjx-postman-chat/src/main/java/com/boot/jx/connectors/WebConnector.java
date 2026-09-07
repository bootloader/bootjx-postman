package com.boot.jx.connectors;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.boot.jx.AppContextUtil;
import com.boot.jx.connectors.AbstractConnector.DefaultConnector;
import com.boot.jx.def.MCQCodecDefs;
import com.boot.jx.dict.ContactType;
import com.boot.jx.model.CommonFile;
import com.boot.jx.model.CommonFileStream;
import com.boot.jx.postman.PMConstants.PROPERTIES;
import com.boot.jx.postman.client.PMFileStoreClient;
import com.boot.jx.postman.doc.ChatContactDoc;
import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.doc.MessageDoc;
import com.boot.jx.postman.dto.ChatMessageDTO;
import com.boot.jx.postman.model.Attachment;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.Message;
import com.boot.jx.postman.model.Message.Status;
import com.boot.jx.postman.model.MessageBoxEvent;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.model.TmplElement;
import com.boot.jx.postman.model.outbound.OutBoundMsgBasic.ApiOutBoundMsg;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.plugin.ChannelPluginProvider.ConnectorMapping;
import com.boot.jx.postman.plugin.WebPlugin;
import com.boot.jx.postman.plugin.WebPlugin.WebConfigDetails;
import com.boot.jx.postman.query.ChatContactQuery;
import com.boot.jx.postman.service.ChatDTOUtil;
import com.boot.jx.postman.store.MessageStore;
import com.boot.jx.stomp.StompTunnelService;
import com.boot.jx.utils.PostManUtil;
import com.boot.model.MapModel;
import com.boot.utils.ArgUtil;
import com.boot.utils.JsonUtil;

@Component
@ConnectorMapping(contactType = ContactType.WEBSITE)
public class WebConnector extends DefaultConnector<WebConfigDetails, WebPlugin> {

	private static final String WEB_USER_MESSAGE_STR = "WEB_USER_MESSAGE_STR_";
	private static final Logger LOGGER = LoggerFactory.getLogger(WebConnector.class);

	private static String queueKey(String contactId) {
		return WEB_USER_MESSAGE_STR + "C" + MCQCodecDefs.CODEC_VERSION + "_" + contactId;
	}

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

	public static class MessageQueue<T> {

		private List<T> queue = new LinkedList<T>();
		private int limit = 10;

		public MessageQueue(int limit) {
			this.limit = limit;
		}

		public synchronized void enqueue(T item) throws InterruptedException {
			while (this.queue.size() == this.limit) {
				wait();
			}
			this.queue.add(item);
			if (this.queue.size() == 1) {
				notifyAll();
			}
		}

		public synchronized T dequeue() throws InterruptedException {
			while (this.queue.size() == 0) {
				wait();
			}
			if (this.queue.size() == this.limit) {
				notifyAll();
			}

			return this.queue.remove(0);
		}
	}

	private MessageQueue<OutboxMessage> messageQueue = new MessageQueue<OutboxMessage>(100);

	public OutboxMessage pollUnreadMessage(String contactId) throws InterruptedException {
		if (redisson == null) {
			try {
				return messageQueue.dequeue();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		RBlockingQueue<String> messageQueue = redisson.getBlockingQueue(queueKey(contactId));
		String x = messageQueue.poll(5, TimeUnit.SECONDS);

		if (ArgUtil.is(x)) {
			return JsonUtil.parse(x, OutboxMessage.class);
		}
		return null;
	}

	public List<Object> pollAllUnreadMessage(String contactId, String sessionid) {
		List<Object> msgs = new ArrayList<Object>();
		try {
			if (redisson == null) {
				try {
					OutboxMessage msg = messageQueue.dequeue();
					if (ArgUtil.is(msg)) {
						msgs.add(msg);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else if (environment.config().prefsEntry(PROPERTIES.POSTMAN_CHAT_WEB_QUEUE).asBoolean()) {
				RBlockingQueue<String> messageQueue = redisson.getBlockingQueue(queueKey(contactId));
				String x = messageQueue.poll(5, TimeUnit.SECONDS);
				while (ArgUtil.is(x)) {
					msgs.add(JsonUtil.parse(x, OutboxMessage.class));
					x = messageQueue.poll();
				}
			} else if (ArgUtil.is(sessionid)) {
				List<MessageDoc> messages = messageStore.findBySessionId(sessionid, ContactType.WEBSITE.toString());
				for (MessageDoc messageDoc : messages) {
					ChatMessageDTO outboxMessage = ChatDTOUtil.getChatMessageDTO(messageDoc);
					if (ArgUtil.isEqual(messageDoc.getType(), "I", "O")) {
						msgs.add(outboxMessage);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return msgs;
	}

	@Override
	public Status onSend(ChannelConfig channelConfig, ChatContactDoc chatContactDoc, OutboxMessage outboxMessage,
			ApiOutBoundMsg outBoundMsg) {
		String contactId = outboxMessage.contact().getContactId();
		String contactIdWeb = AppContextUtil.getTenant() + "/" + contactId;
		if (redisson == null) {
			try {
				messageQueue.enqueue(outboxMessage);
				outboxMessage.updateStatus(Message.Status.SENT);
				return Message.Status.SENT;
			} catch (InterruptedException e) {
				outboxMessage.updateStatus(Message.Status.SENT_ERR);
				outboxMessage.logs().add(e.getMessage());
				e.printStackTrace();
				return Message.Status.SENT_ERR;
			}
		} else {
			if (!stompEnabled && environment.config().prefsEntry(PROPERTIES.POSTMAN_CHAT_WEB_QUEUE).asBoolean()) {
				LOGGER.debug("sendReply to " + contactIdWeb);
				RBlockingQueue<String> messageQueue = redisson.getBlockingQueue(queueKey(contactIdWeb));
				messageQueue.add(JsonUtil.toJson(outboxMessage));
			}
			stompTunnelService.sendToTag(contactIdWeb, "/message/receive/new", outboxMessage);
		}
		return Message.Status.SENT;
	}

	public OutboxMessage onIceBreak(ChannelConfig channelConfig, ChatContactDoc chatContactDoc) {
		OutboxMessage icebrakerMsg = null;
		if (ArgUtil.is(chatContactDoc)) {
			if (ArgUtil.is(channelConfig.getWeb().getIceBreaker())) {
				InboxMessage inboxMessage = this.createInboxMessage(channelConfig);
				icebrakerMsg = inboxMessage.replyMessage("Howdy").template(channelConfig.getWeb().getIceBreaker());
				template(channelConfig, chatContactDoc, icebrakerMsg);
			}
		} else {
			if (ArgUtil.is(channelConfig.getWeb().getWelcomeBack())) {
				InboxMessage inboxMessage = this.createInboxMessage(channelConfig);
				icebrakerMsg = inboxMessage.replyMessage("Welcome Back")
						.template(channelConfig.getWeb().getWelcomeBack());
				ChatContactDoc c = new ChatContactDoc();
				c.copyFrom(inboxMessage.contact());
				template(channelConfig, c, icebrakerMsg);
			}
		}
		return icebrakerMsg;
	}

	@Override
	public InboxMessage assignToAgent(InboxMessage inboxMessage) {
		this.reply(null, null, new OutboxMessage().message("Call us"), inboxMessage);
		return inboxMessage;
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

		ChannelConfig channel = getChannelConfig(inboxMessage);
		List<TmplElement> inputs = new ArrayList<TmplElement>();
		if (channel.getWeb().isPromptName()) {
			if (ArgUtil.isEmpty(chatContactDoc.getName())) {
				inputs.add(new TmplElement().code("name").type("TEXT"));

				if (!channel.getWeb().isPromptForm()) {
					return (OutboxMessage) inboxMessage.replyMessage("Please enter your name").option("inputs", inputs);
				}
			}
		}

		if (channel.getWeb().isPromptEmail()) {
			if (ArgUtil.isEmpty(chatContactDoc.getEmail())) {
				inputs.add(new TmplElement().code("email").type("EMAIL"));
				if (!channel.getWeb().isPromptForm()) {
					return (OutboxMessage) inboxMessage.replyMessage("Please enter your email").option("inputs",
							inputs);
				}

			}
		}
		if (channel.getWeb().isPromptPhone()) {
			if (ArgUtil.isEmpty(chatContactDoc.phone())) {
				inputs.add(new TmplElement().code("phone").type("PHONE"));
				if (!channel.getWeb().isPromptForm()) {
					return (OutboxMessage) inboxMessage.replyMessage("Please enter your phone").option("inputs",
							inputs);
				}
			}
		}

		if (ArgUtil.is(inputs) && (inputs.size() > 0) && channel.getWeb().isPromptForm()) {
			return (OutboxMessage) inboxMessage.replyMessage("Please enter your").option("inputs", inputs);
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

}
