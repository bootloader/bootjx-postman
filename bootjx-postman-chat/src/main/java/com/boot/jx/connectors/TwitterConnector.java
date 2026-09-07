package com.boot.jx.connectors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.boot.jx.dict.ContactType;
import com.boot.jx.postman.doc.ChatContactDoc;
import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.manager.ChatLogger;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.Message;
import com.boot.jx.postman.model.Message.Status;
import com.boot.jx.postman.model.MessageBoxEvent;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.model.outbound.OutBoundMsgBasic.ApiOutBoundMsg;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.plugin.ChannelPluginProvider.ConnectorMapping;
import com.boot.jx.postman.plugin.TwitterPlugin;
import com.boot.jx.postman.plugin.TwitterPlugin.TwitterConfigDetails;
import com.boot.jx.postman.query.ChatContactQuery;
import com.boot.jx.postman.tw.StatusCode;
import com.boot.jx.postman.tw.TwitterClient;
import com.boot.jx.postman.tw.TwitterClientContext;
import com.boot.jx.postman.tw.TwitterClientExt;
import com.boot.model.MapModel;
import com.boot.utils.ArgUtil;
import com.boot.utils.JsonUtil;

import twitter4j.DirectMessage;
import twitter4j.DirectMessageList;
import twitter4j.DirectMessageLocalImpl;
import twitter4j.ResponseList;
import twitter4j.TwitterException;

@Component
@ConnectorMapping(contactType = ContactType.TWITTER)
public class TwitterConnector extends AbstractConnector<TwitterConfigDetails, TwitterPlugin> {

	private static final Logger LOGGER = LoggerFactory.getLogger(TwitterConnector.class);

	@Autowired
	private TwitterClient twitterClient;

	@Autowired
	private TwitterClientExt twitterClientExt;

	@Autowired
	ChatLogger chatLogger;

	@Override
	public void registerWebhook(ChannelConfig channelConfig, String webhookUrl) {
		StatusCode status = twitterClient.registerWebhook(channelConfig, webhookUrl);
		chatLogger.debug("registerWebhook", String.format("isError:%s httpCode:%s", status.isError, status.httpCode),
				status.message);
	}

	@Override
	public Status onSend(ChannelConfig channelConfig, ChatContactDoc chatContactDoc, OutboxMessage outboxMessage, ApiOutBoundMsg outBoundMsg) {
		twitterClient.send(channelConfig, outboxMessage);
		outboxMessage.updateStatus(OutboxMessage.Status.SENT);
		return Message.Status.SENT;
	}

	@Override
	public InboxMessage assignToAgent(InboxMessage inboxMessage) {
		return inboxMessage;
	}

	public InboxMessage toInboxMessage(DirectMessage dm, ChannelConfig channelConfig) {
		InboxMessage ibm = this.createInboxMessage(channelConfig);
		ibm.setMessageIdExt(String.valueOf(dm.getId()));
		ibm.setMessage(dm.getText());
		ibm.setFrom(String.valueOf(dm.getSenderId()));
		ibm.to().add(String.valueOf(dm.getRecipientId()));
		ibm.contact().setCsid(String.valueOf(dm.getSenderId()));

		/**
		 * NOTE:- Do not user original DirectMessageJsonImpl as it can throw
		 * serialization error
		 */
		if (dm instanceof DirectMessageLocalImpl) {
			ibm.setOriginalMessage(dm);
		}

		return ibm;
	}

	@Override
	public OutboxMessage initSession(ChatSessionDoc session, InboxMessage inboxMessage) {
		if (ArgUtil.is(inboxMessage.getOriginalMessage())) {
			try {
				DirectMessageLocalImpl dm = JsonUtil.parse(inboxMessage.getOriginalMessage(),
						DirectMessageLocalImpl.class);
				ChatContactQuery contactQuery = messageContext.contact();
				contactQuery.setProfilePic(dm.getSender().getProfileImageURLHttps());
				contactQuery.setName(dm.getSender().getName());
//		ChannelConfig config = getChannelConfig(inboxMessage);
//		MapModel x = twitterClientExt.askInput(config, dm.getSenderId() + "");
//		System.out.println(x.toJson());
			} catch (Exception e) {
				LOGGER.error("Twitter Init Session Data Parse Errror", e);
			}
		}
		return null;
	}

	public List<InboxMessage> messageConverter(ResponseList<DirectMessage> dml, ChannelConfig channelConfig) {
		List<InboxMessage> inboxMsg = new ArrayList<InboxMessage>();
		if (ArgUtil.is(dml)) {
			for (DirectMessage dm : dml) {
				InboxMessage ibm = toInboxMessage(dm, channelConfig);
				inboxMsg.add(ibm);
			}
		}
		return inboxMsg;
	}

	public List<InboxMessage> fetch(ChannelConfig channelConfig) throws TwitterException {
		DirectMessageList dml = twitterClient.pollDirectMessagesReceived(channelConfig);
		return messageConverter(dml, channelConfig);
	}

	public List<InboxMessage> process(ChannelConfig channelConfig, Map<String, Object> update) throws TwitterException {
		TwitterClientContext ctx = twitterClient.getContext(channelConfig);
		DirectMessageList dml = DirectMessageLocalImpl.createDirectMessageList(update,
				ctx.getTwitter().getConfiguration());
		dml = ctx.removeDMsNotSentToMe(dml);
		return messageConverter(dml, channelConfig);
	}

	@Override
	public MessageBoxEvent inboundMessageBoxEvent(ChannelConfig channelConfig, MapModel requestMap,
			MessageBoxEvent messageBoxEvent) {
		try {
			messageBoxEvent.addInboxMessage(process(channelConfig, requestMap.toMap()));
		} catch (TwitterException e) {
			LOGGER.error("Exception while converting inbox message from twitter webhook", e);
		}
		return messageBoxEvent;
	}

	@Override
	public List<InboxMessage> onReceiveInboxMessage(List<InboxMessage> inboxMessages) {
		if (inboxMessages != null && !inboxMessages.isEmpty()) {
			for (InboxMessage event : inboxMessages) {
				try {
					ChannelConfig channelConfig = getChannelConfig(event);
					twitterClient.getContext(channelConfig).getTwitter()
							.destroyDirectMessage(Long.parseLong(event.getMessageIdExt()));
				} catch (NumberFormatException | TwitterException e) {
					LOGGER.error("Exception while processing after reading inbox message from twitter webhook", e);
				}
			}
		}
		return inboxMessages;
	}

}
