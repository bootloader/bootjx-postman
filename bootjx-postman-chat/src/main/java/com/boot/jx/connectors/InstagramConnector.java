package com.boot.jx.connectors;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.boot.jx.api.ApiResponseUtil;
import com.boot.jx.auth.AuthStateManager.AuthState;
import com.boot.jx.dict.ContactType;
import com.boot.jx.dict.FileType;
import com.boot.jx.exception.ApiHttpExceptions.ApiHttpException;
import com.boot.jx.postman.PMConstants.CHANNEL_TYPE;
import com.boot.jx.postman.doc.ChatContactDoc;
import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.doc.config.ChannelConfigLogger;
import com.boot.jx.postman.fb.FacbookAttachment;
import com.boot.jx.postman.fb.FacebookConstants.InBoundWrapperPaths;
import com.boot.jx.postman.fb.FacebookEntry;
import com.boot.jx.postman.fb.FacebookHookRequest;
import com.boot.jx.postman.fb.FacebookMessaging;
import com.boot.jx.postman.fb.InstagramClient;
import com.boot.jx.postman.fb.InstagramUserProfile;
import com.boot.jx.postman.model.Attachment;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.Message;
import com.boot.jx.postman.model.Message.Status;
import com.boot.jx.postman.model.MessageBoxEvent;
import com.boot.jx.postman.model.MessageReport;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.model.outbound.OutBoundMsgBasic.ApiOutBoundMsg;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.plugin.ChannelPluginProvider.ConnectorMapping;
import com.boot.jx.postman.plugin.InstagramPlugin;
import com.boot.jx.postman.plugin.InstagramPlugin.InstagramConfig;
import com.boot.jx.postman.query.ChatContactQuery;
import com.boot.jx.rest.RestService;
import com.boot.model.MapModel;
import com.boot.utils.ArgUtil;

@Component
@ConnectorMapping(contactType = ContactType.INSTAGRAM)
public class InstagramConnector extends AbstractConnector<InstagramConfig, InstagramPlugin> {
	private static final Logger LOGGER = LoggerFactory.getLogger(InstagramConnector.class);

	@Autowired
	private InstagramClient instaClient;

	@Autowired
	private RestService restService;

	@Override
	public List<ChannelConfig> onRegister(ChannelConfig setup, ChannelConfigLogger channelConfigTemp, AuthState state) {
		List<ChannelConfig> channels = new ArrayList<ChannelConfig>();
		try {

			MapModel resp = MapModel.from(channelConfigTemp.getResp());

			MapModel accessToken = restService.ajax("https://graph.facebook.com/v21.0").path("/oauth/access_token")
					.field("client_id", setup.getInstagram().getMasterAppId())
					.field("client_secret", setup.getInstagram().getMasterAppSecret())
					.field("code", resp.pathEntry("authResponse.code").asString()).submit().asMapModel();
			channelConfigTemp.log("oauth/access_token", accessToken.toMap());

			MapModel me = restService.ajax("https://graph.facebook.com/v21.0").path("/me/accounts")
					.queryParam("access_token", accessToken.keyEntry("access_token").asString())
					.queryParam("fields", "id,name,access_token,instagram_business_account").get().asMapModel();

			channelConfigTemp.log("me/accounts", me.toMap());

			me.keyEntry("data").asListOfMap().forEach(page -> {
				MapModel pagemap = MapModel.from(page);
				ChannelConfig channel = new ChannelConfig();
				channel.setInstagram(new InstagramConfig());
				channel.getInstagram().setAccessToken(pagemap.keyEntry("access_token").asString());
				channel.getInstagram().setPageId(pagemap.pathEntry("instagram_business_account.id").asString());
				channel.getInstagram().setFbPageId(pagemap.keyEntry("id").asString());
				channel.getInstagram().setHandler(channel.getInstagram().getPageId());
				channel.getInstagram().setType("page");
				channel.getInstagram().setMasterAppId(setup.getInstagram().getMasterAppId());
				channel.getInstagram().setMasterAppConfigId(setup.getInstagram().getMasterAppConfigId());
				channel.setName(pagemap.keyEntry("name").asString());
				channels.add(channel);
			});
		} catch (ApiHttpException e) {
			channelConfigTemp.log("exception", MapModel.from(e.getResponse().getBody()).toMap());
		}

		commonMongoTemplate.save(channelConfigTemp);

		return channels;
	}

	@Override
	public void onChannelUpdate(ChannelConfig channelConfig, ChannelConfigLogger channelConfigLogger) {
		// MENTOR FIX: Applied /me/ alias for cleaner webhook registration.
		// Replaced explicit page ID mapping with /me/subscribed_apps
		restService.ajax("https://graph.facebook.com/v21.0/me/subscribed_apps")
				.field("access_token", channelConfig.getInstagram().getAccessToken())
				.field("subscribed_fields",
						"message_deliveries, message_echoes, message_reads, messages, messaging_optins, messaging_postbacks")
				.submit().asMap();
		ApiResponseUtil.addWarning("Set webhook URL manually from Facebook Developer Portal.");
	}

	@Override
	public Status onSend(ChannelConfig channelConfig, ChatContactDoc chatContactDoc, OutboxMessage outboxMessage,
			ApiOutBoundMsg outBoundMsg) {
		instaClient.send(channelConfig, outboxMessage);
		outboxMessage.updateStatus(Message.Status.SENT);
		return Message.Status.SENT;
	}

	@Override
	public InboxMessage assignToAgent(InboxMessage inboxMessage) {
		this.reply(null, null, new OutboxMessage().message("Our agent will get in touch with you"), inboxMessage);
		return inboxMessage;
	}

	@Override
	public OutboxMessage initSession(ChatSessionDoc session, InboxMessage inboxMessage) {
		try {
			ChannelConfig config = getChannelConfig(inboxMessage);
			InstagramUserProfile profile = instaClient.getUserProfile(config, inboxMessage.contact());
			ChatContactQuery contactQuery = messageContext.contact();
			ChatContactDoc chatContactDoc = messageContext.contact().getDoc();
			contactQuery.setProfilePic(profile.getProfilePic());
			contactQuery.setEmail(profile.getEmail());
			contactQuery.setName(profile.getName());
			String user_input_type = this.context().session().getEntry("session_init_user_input_type").asString();
			if (ArgUtil.is(user_input_type)) {
				if (user_input_type.equals("name")) {
					contactQuery.setInfoName(inboxMessage.getMessage());
				}
				if (user_input_type.equals("email")) {
					String email = inboxMessage.getMessage();
					if (isValidEmail(email)) {
						contactQuery.setInfoEmail(email);
					} else {
						return (OutboxMessage) inboxMessage.replyMessage("Please enter a valid email address.");
					}
				}
				if (user_input_type.equals("phone")) {
					contactQuery.setInfoPhone(inboxMessage.getMessage());
				}
			}
			ChannelConfig channel = getChannelConfig(inboxMessage);

			if (channel.getInstagram().isPromptName()) {
				if (ArgUtil.isEmpty(chatContactDoc.info().getName())) {
					this.context().session().put("session_init_user_input_type", "name");
					return (OutboxMessage) inboxMessage.replyMessage("Please enter your name");
				}
			}

			if (channel.getInstagram().isPromptEmail()) {
				if (ArgUtil.isEmpty(chatContactDoc.info().getEmail())) {
					this.context().session().put("session_init_user_input_type", "email");
					return (OutboxMessage) inboxMessage.replyMessage("Please enter your email id");
				}
			}

			if (channel.getInstagram().isPromptPhone()) {
				if (ArgUtil.isEmpty(chatContactDoc.info().getPhone())) {
					this.context().session().put("session_init_user_input_type", "phone");
					return (OutboxMessage) inboxMessage.replyMessage("Please enter your phone");
				}
			}
		} catch (ApiHttpException e) {
			logManager.error(inboxMessage, e);
		}

		return null;
	}

	private boolean isValidEmail(String email) {
		String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
		Pattern pattern = Pattern.compile(emailRegex);
		return pattern.matcher(email).matches();
	}

	@Deprecated
	public InboxMessage toInboxMessage(FacebookMessaging m, String lane) {
		InboxMessage event = new InboxMessage();
		String id = m.getSender().get("id");
		event.contact().setChannelType(CHANNEL_TYPE.INSTAGRAM);
		event.setFrom(id);
		event.contact().setCsid(id);
		if (ArgUtil.is(m.getPostBack()) && ArgUtil.is(m.getPostBack().getTitle())) {
			event.setMessage(m.getPostBack().getTitle());
		} else {
			event.setMessage(m.getMessage().getText());
		}

		event.to().add(m.getRecipient().get("id"));
		event.contact().type(ContactType.INSTAGRAM);
		event.contact().setLane(lane);
		return event;
	}

	public InboxMessage toInboxMessage(FacebookMessaging m, ChannelConfig channelConfig) {

		// Create Default Message from Channel
		InboxMessage inboxMessage = this.createInboxMessage(channelConfig);

		// Set Contact info
		String csid = m.getSender().get("id");
		inboxMessage.contact().setCsid(csid);

		// Set Additional info
		inboxMessage.setFrom(csid);
		inboxMessage.to().add(m.getRecipient().get("id"));

		/**
		 * https://developers.facebook.com/docs/messenger-platform/instagram/features/webhook
		 */
		if (ArgUtil.is(m.getMessage())) {
			if (ArgUtil.is(m.getMessage().getAttachments())
					&& ArgUtil.is(m.getMessage().getAttachments()[0].getPayload())) {
				if (ArgUtil.is(m.getMessage().getAttachments()[0].getPayload().getUrl())) {
					FacbookAttachment attchment = m.getMessage().getAttachments()[0];
					FileType attachmentType = ArgUtil.parseAsEnumT(attchment.getType(), FileType.class);
					if (ArgUtil.is(attachmentType)) {
						inboxMessage.setFormatType(attachmentType.toString().toLowerCase());
						inboxMessage.attachment(new Attachment().mediaURL(attchment.getPayload().getUrl())
								.mediaType(attachmentType).mediaSrc(attchment.getPayload().getUrl()));
					} else if (ArgUtil.isEqual(attchment.getType(), "story_mention", "share")) {
						// FIX: Removed the erroneous `.mediaType(m)` that was passing a
						// FacebookMessaging object
						inboxMessage.attachment(new Attachment().mediaURL(attchment.getPayload().getUrl())
								.mediaCaption(attchment.getPayload().getTitle())
								.mediaSrc(attchment.getPayload().getUrl()).mediaType(FileType.URL)
								.mediaSubType(attchment.getType()));
					}
				}
			}
			// Extract Message Details
			inboxMessage.setMessageIdExt(m.getMessage().getMid());
			inboxMessage.setMessage(m.getMessage().getText());

			MapModel qr = m.getMessage().getQuickReply();
			if (ArgUtil.is(qr)) {
				inboxMessage.form().put("reply_id", qr.getString("payload"));
				inboxMessage.form().put("reply_title", m.getMessage().getText());
			}

			MapModel rt = m.getMessage().getReplyTo();
			if (ArgUtil.is(rt)) {
				inboxMessage.setReplyIdExt(rt.getString("mid"));
				if (rt.containsKey("story")) {
					inboxMessage.replyTo().put("type", "story");
					inboxMessage.replyTo().put("id", rt.entry(InBoundWrapperPaths.STORY_ID).asString());
					inboxMessage.replyTo().put("url", rt.entry(InBoundWrapperPaths.STORY_URL).asString());

					inboxMessage.referral().setSourceCategory("social");
					inboxMessage.referral().setSourceType("story");
					inboxMessage.referral().setSourceId(rt.entry(InBoundWrapperPaths.STORY_ID).asString());
					inboxMessage.referral().setSourceUrl(rt.entry(InBoundWrapperPaths.STORY_URL).asString());
				}
			}
		} else if (ArgUtil.is(m.getPostBack()) && ArgUtil.is(m.getPostBack().getTitle())) {
			inboxMessage.setMessageIdExt(m.getPostBack().getMid());
			inboxMessage.setMessage(m.getPostBack().getTitle());
			inboxMessage.form().put("reply_id", m.getPostBack().getPayload());
			inboxMessage.form().put("reply_title", m.getPostBack().getTitle());
		}

		return inboxMessage;
	}

	private MessageReport toMessageReport(FacebookMessaging m, ChannelConfig channelConfig) {
		MessageReport report = this.createMessageReport(channelConfig);
		String csid = m.getSender().get("id");
		report.contact().setCsid(csid);
		report.setChangeStamp(m.getTimestamp());
		if (ArgUtil.is(m.getRead())) {
			report.setChangeStamp(m.getReadWatermark());
			report.setStatus(Status.READ);

		} else if (ArgUtil.is(m.getMessage()) && ArgUtil.is(m.getMessage().isIs_deleted())) {
			report.setMessageIdExt(m.getMessage().getMid());
			report.setChangeStamp(m.getTimestamp());
			report.setStatus(Status.DELTD);
		}

		return report;
	}

	@Override
	public MessageBoxEvent inboundMessageBoxEvent(ChannelConfig channelConfig, MapModel requestMap,
			MessageBoxEvent messageBoxEvent) {
		FacebookHookRequest request = requestMap.as(FacebookHookRequest.class);
		requestMap.toJson();
		request.getEntry().forEach(pageEntry -> {
			ChannelConfig channelConfigDefault = channelConfig;
			String pageId = pageEntry.getId();
			if (!ArgUtil.is(channelConfig.getLane(), pageId)) {
				channelConfigDefault = getChannelConfig(channelConfig.getChannelType(), pageId);
			}
			if (ArgUtil.is(channelConfigDefault)) {
				inboundMessageBoxEvent(messageBoxEvent, pageEntry, channelConfigDefault);
			}
		});
		return messageBoxEvent;
	}

	private void inboundMessageBoxEvent(MessageBoxEvent messageBoxEvent, FacebookEntry pageEntry,
			final ChannelConfig channelConfig) {
		pageEntry.getMessaging().forEach(m -> {
			if ((ArgUtil.is(m.getMessage()) && (m.getMessage().isIs_deleted())) // Message is deleted
					|| ArgUtil.is(m.getRead()) // or Message is Read
					|| ArgUtil.is(m.getReaction())) {
				messageBoxEvent.addMessageReport(toMessageReport(m, channelConfig));
			} else if (ArgUtil.is(m.getMessage()) || ArgUtil.is(m.getPostBack())) {
				InboxMessage inboxMessage = toInboxMessage(m, channelConfig);
				if (!ArgUtil.areEqual(channelConfig.getLane(), inboxMessage.contact().getCsid())) {
					messageBoxEvent.addInboxMessage(inboxMessage);
				}
			}
		});
	}
}