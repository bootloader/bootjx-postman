package com.boot.jx.connectors;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.boot.jx.AppConfig;
import com.boot.jx.auth.AuthStateManager.AuthState;
import com.boot.jx.dict.ContactType;
import com.boot.jx.email.EmailReplyParser;
import com.boot.jx.exception.ApiHttpExceptions.ApiHttpException;
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
import com.boot.jx.postman.plugin.OutlookPlugin;
import com.boot.jx.postman.plugin.OutlookPlugin.OutlookConfigDetails;
import com.boot.jx.postman.store.MessageStore;
import com.boot.jx.rest.RestService;
import com.boot.jx.utils.PostManUtil;
import com.boot.model.MapModel;
import com.boot.model.MapModel.MapPathEntry;
import com.boot.utils.ArgUtil;
import com.boot.utils.DateUtil;
import com.boot.utils.TimeUtils.TimePeriod;
import com.boot.utils.Urly;

@Component
@ConnectorMapping(contactType = ContactType.EMAIL, channel = CHANNEL_TYPE.OUTLOOK)
public class OutlookConnector extends AbstractConnector<OutlookConfigDetails, OutlookPlugin> {

	private static Logger LOGGER = LoggerService.getLogger(OutlookConnector.class);
	private static final String AUTHORITY = "https://login.microsoftonline.com";
	private static final String GRAPH_API = "https://graph.microsoft.com";
	private static final String AUTHORIZE_URL = AUTHORITY + "/common/oauth2/v2.0/authorize";
	private static final String AUTHORIZE_TOKEN = AUTHORITY + "/common/oauth2/v2.0/token";

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
	public String createAuthUrl(ChannelConfig setup, ChannelConfigLogger channelConfigLogger, AuthState state)
			throws URISyntaxException, MalformedURLException {
		String redirectUri = String.format("%s%s/ext/setup/channel/callback/outlook", commonHttpRequest.getServerHost(),
				appConfig.getAppPrefix(), environment.config().prefsEntry("mry.prop.service.server").asString());
		/// &state=fooobar&scope=r_liteprofile%20r_emailaddress%20w_member_social
		state.setRedirectUrl(redirectUri);

		return Urly.parse(AUTHORIZE_URL).queryParam("response_type", "code") //
				.queryParam("client_id", setup.getOutlook().getMasterClientId()) //
				.queryParam("redirect_uri", redirectUri).queryParam("state", state.toString()) // State
				.queryParam("nonce", state.getNonce()) //
				.queryParam("scope", "offline_access user.read mail.read mail.send Mail.ReadWrite") //
				.queryParam("response_mode", "form_post") //
				.getURL();
	}

	public List<ChannelConfig> onRegister(ChannelConfig setup, ChannelConfigLogger channelConfigLogger,
			AuthState state) {
		List<ChannelConfig> channels = new ArrayList<ChannelConfig>();
		try {

			MapModel resp = MapModel.from(channelConfigLogger.getResp());
			MapModel tokenResponse = restService.ajax(AUTHORIZE_TOKEN)//
					.field("grant_type", "authorization_code")//
					.field("code", resp.pathEntry("authResponse.code").asString())//
					.field("client_id", setup.getOutlook().getMasterClientId())//
					.field("client_secret", setup.getOutlook().getMasterClientSecret())//
					.field("redirect_uri", state.getRedirectUrl()).submit().asMapModel();

			channelConfigLogger.log("oauth2/v2.0/token", tokenResponse.toMap());

			String accessToken = tokenResponse.keyEntry("access_token").asString();
			String refreshToken = tokenResponse.keyEntry("refresh_token").asString();

			MapModel profileResponse = restService.ajax("https://graph.microsoft.com/v1.0/me")
					.header("Authorization", "Bearer " + accessToken).get().asMapModel();

			channelConfigLogger.log("/me", profileResponse.toMap());

			ChannelConfig channel = new ChannelConfig();
			channel.setApiVersion("v3");
			channel.setOutlook(new OutlookConfigDetails());
			channel.getOutlook().setAccessToken(accessToken);
			channel.getOutlook().setRefreshToken(refreshToken);
			channel.getOutlook().setEmail(profileResponse.keyEntry("mail").orKeyEntry("userPrincipalName").asString());
			channel.getOutlook().setMasterClientId(setup.getOutlook().getMasterClientId());
			channel.setName(profileResponse.keyEntry("displayName").asString());

			channels.add(channel);
		} catch (ApiHttpException e) {
			channelConfigLogger.log("exception", MapModel.from(e.getResponse().getBody()).toMap());
		}
		commonMongoTemplate.save(channelConfigLogger);
		return channels;
	}

	@Override
	public void onChannelUpdate(ChannelConfig channelConfig, ChannelConfigLogger channelConfigLogger) {
		nexusEmailClient.subscribe(channelConfig);
	}

	@Deprecated
	public void onChannelUpdateFallback(ChannelConfig channelConfig, ChannelConfigLogger channelConfigLogger) {
		Map<String, Object> meta = channelConfig.getMeta();
		if (!ArgUtil.is(channelConfig.getMeta())) {
			meta = new HashMap<String, Object>();
		}

		try {
			String webhookUrl = pmClientConfig.getWebhookUrl(channelConfig, "nexus/email/api/v1",
					MapModel.createInstance().put("folder", "inbox").toMap());
			MapModel inbox = restService.ajax(GRAPH_API).path("/v1.0/subscriptions")
					.authBearer(channelConfig.getOutlook().getAccessToken())
					.postJson(MapModel.createInstance().put("changeType", "created").put("notificationUrl", webhookUrl)
							.put("lifecycleNotificationUrl", webhookUrl)
							.put("resource", "/me/mailFolders('inbox')/messages")
							.put("expirationDateTime", DateUtil.toISOString(TimePeriod.of("1hour")))
							.put("clientState", channelConfig.getOutlook().getMasterClientId())
							.put("latestSupportedTlsVersion", "v1_2").toMap())
					.asMapModel();

			channelConfigLogger.log("/subscriptions?inbox", inbox.toMap());

			if (inbox.containsKey("data")) {
				meta.put("inbox_subscription", inbox.keyEntry("data").value());
			}

			webhookUrl = pmClientConfig.getWebhookUrl(channelConfig, "nexus/email/api/v1",
					MapModel.createInstance().put("folder", "SentItems").toMap());

			MapModel sentItems = restService.ajax(GRAPH_API).path("/v1.0/subscriptions")
					.authBearer(channelConfig.getOutlook().getAccessToken())
					.postJson(MapModel.createInstance().put("changeType", "created").put("notificationUrl", webhookUrl)
							.put("lifecycleNotificationUrl", webhookUrl)
							.put("resource", "/me/mailFolders('SentItems')/messages")
							.put("expirationDateTime", DateUtil.toISOString(TimePeriod.of("1hour")))
							.put("clientState", channelConfig.getOutlook().getMasterClientId())
							.put("latestSupportedTlsVersion", "v1_2").toMap())
					.asMapModel();

			channelConfigLogger.log("/subscriptions?SentItems", sentItems.toMap());

			if (sentItems.containsKey("data")) {
				meta.put("sent_subscription", sentItems.keyEntry("data").value());
			}
		} catch (ApiHttpException e) {
			LOGGER.error("onChannelUpdate", e);
			channelConfigLogger.log("exception", MapModel.from(e.getResponse().getBody()).toMap());
		}

		channelConfig.setMeta(meta);
	}

	@Override
	public Status onSend(ChannelConfig channelConfig, ChatContactDoc chatContactDoc, OutboxMessage outboxMessage, ApiOutBoundMsg outBoundMsg) {
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
		outboxMessage.updateStatus(Message.Status.SENT);
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
