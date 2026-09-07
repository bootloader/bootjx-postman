package com.boot.jx.connectors;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import com.boot.jx.postman.plugin.GmailPlugin;
import com.boot.jx.postman.plugin.GmailPlugin.GmailConfigDetails;
import com.boot.jx.postman.store.MessageStore;
import com.boot.jx.rest.RestService;
import com.boot.jx.utils.PostManUtil;
import com.boot.model.MapModel;
import com.boot.model.MapModel.MapPathEntry;
import com.boot.utils.ArgUtil;
import com.boot.utils.JsonUtil;
import com.boot.utils.Urly;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

@Component
@ConnectorMapping(contactType = ContactType.EMAIL, channel = CHANNEL_TYPE.GMAIL)
public class GmailConnector extends AbstractConnector<GmailConfigDetails, GmailPlugin> {

	private static Logger LOGGER = LoggerService.getLogger(GmailConnector.class);
//	private static final String AUTHORITY = "https://login.microsoftonline.com";
//	private static final String GRAPH_API = "https://graph.microsoft.com";
//	private static final String AUTHORIZE_URL = AUTHORITY + "/common/oauth2/v2.0/authorize";
//	private static final String AUTHORIZE_TOKEN = AUTHORITY + "/common/oauth2/v2.0/token";

	private static final String GOOGLE = "https://accounts.google.com";
	private static final String GOOGLE_OAUTH_URL = GOOGLE + "/o/oauth2/v2/auth";
	private static final String FACEBOOK_GRAPHAPI = "https://oauth2.googleapis.com";
	private static final String AUTHORIZE_TOKEN = FACEBOOK_GRAPHAPI + "/token";

	private static final String[] SCOPES = { "https://www.googleapis.com/auth/gmail.modify", // Modify Gmail
			"https://www.googleapis.com/auth/pubsub", // Pub/Sub access
			"https://www.googleapis.com/auth/gmail.send", // Send emails
			"openid", "email", "profile" };

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
		String redirectUri = String.format("%s%s/ext/setup/channel/callback/gmail", commonHttpRequest.getServerHost(),
				appConfig.getAppPrefix(), environment.config().prefsEntry("mry.prop.service.server").asString());
		/// &state=fooobar&scope=r_liteprofile%20r_emailaddress%20w_member_social
		state.setRedirectUrl(redirectUri);

		return Urly.parse(GOOGLE_OAUTH_URL).queryParam("response_type", "code")
				.queryParam("client_id", setup.getGmail().getMasterClientId()) // CLIENT_ID
				.queryParam("access_type", "offline") // access_type
				.queryParam("scope", String.join(" ", SCOPES)) //
				.queryParam("redirect_uri", redirectUri) //
				.queryParam("prompt", "consent") //
				.queryParam("state", state.toString()) //
				.queryParam("nonce", state.getNonce()).getURL();
	}

	public List<ChannelConfig> onRegister(ChannelConfig setup, ChannelConfigLogger channelConfigLogger,
			AuthState state) {
		List<ChannelConfig> channels = new ArrayList<ChannelConfig>();
		try {

			MapModel resp = MapModel.from(channelConfigLogger.getResp());

			MapPathEntry token = resp.pathEntry("authResponse.credential");
			MapPathEntry stateStr = resp.pathEntry("authResponse.state");
			MapPathEntry code = resp.pathEntry("authResponse.code");
			MapPathEntry scope = resp.pathEntry("authResponse.scope");
			MapPathEntry id_token = resp.pathEntry("authResponse.id_token");

			String redirectUri = String.format("%s%s/ext/setup/channel/callback/gmail",
					commonHttpRequest.getServerHost(), appConfig.getAppPrefix(),
					environment.config().prefsEntry("mry.prop.service.server").asString());
			if (ArgUtil.is(state.getRedirectUrl())) {
				redirectUri = state.getRedirectUrl();
			} else if (ArgUtil.is(stateStr)) {
				// AuthState newstate = AuthState.fromString(stateStr.toString());
				// redirectUri = newstate.getRedirectUrl();
			}

			ChannelConfig channel = new ChannelConfig();
			channel.setApiVersion("v3");
			channel.setGmail(new GmailConfigDetails());
			channel.getGmail().setMasterClientId(setup.getGmail().getMasterClientId());

			if (code.exists()) {
				MapModel tokenResponse = restService.ajax(AUTHORIZE_TOKEN)//
						.field("code", code.asString())//
						.field("client_id", setup.getGmail().getMasterClientId())//
						.field("client_secret", setup.getGmail().getMasterClientSecret())//
						.field("grant_type", "authorization_code")//
						.field("redirect_uri", redirectUri)//
						.submit().asMapModel();
				channelConfigLogger.log("oauth2/v2.0/token", tokenResponse.toMap());
				token = tokenResponse.keyEntry("access_token");
				id_token = tokenResponse.keyEntry("id_token");

				channel.getGmail().setAccessToken(token.asString());
				channel.getGmail().setRefreshToken(tokenResponse.keyEntry("refresh_token").asString());
			}

			if (token.exists()) {
				GsonFactory jacksonFactory = new GsonFactory();
				NetHttpTransport netHttpTransport = new NetHttpTransport();

				GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(netHttpTransport, jacksonFactory)
						.setAudience(Collections.singletonList(setup.getGmail().getMasterClientId())).build();

				GoogleIdToken idToken = null;
				try {
					idToken = verifier.verify(id_token.asString());
					if (idToken != null) {
						GoogleIdToken.Payload payload = idToken.getPayload();

						channelConfigLogger.log("/me", JsonUtil.toJsonMap(payload));

						// channel.getGmail().setAccessToken(accessToken);
						// channel.getGmail().setRefreshToken(refreshToken);
						channel.getGmail().setEmail(payload.getEmail());

						channel.setName(ArgUtil.parseAsString(payload.get("name")));

						channels.add(channel);

					} else {
						LOGGER.warn("Invalid Google ID token.");
					}
				} catch (GeneralSecurityException e) {
					LOGGER.warn(e.getLocalizedMessage());
				} catch (IOException e) {
					LOGGER.warn(e.getLocalizedMessage());
				}
			}

		} catch (ApiHttpException e) {
			channelConfigLogger.log("exception", MapModel.from(e.getResponse().getBody()).toMap());
		} catch (Exception e) {
			channelConfigLogger.log("exception", e);
		}
		commonMongoTemplate.save(channelConfigLogger);
		return channels;
	}

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
