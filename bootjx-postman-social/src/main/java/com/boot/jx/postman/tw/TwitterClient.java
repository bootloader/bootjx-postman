package com.boot.jx.postman.tw;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import com.boot.jx.dict.FileType;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.PostManException;
import com.boot.jx.postman.PostmanPackages.MessageClient;
import com.boot.jx.postman.client.ExtUtilService;
import com.boot.jx.postman.model.Attachment;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.model.TmplElement;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.plugin.TwitterPlugin.TwitterConfigDetails;
import com.boot.jx.postman.tw.TwitterConstants.OutBoundPaths;
import com.boot.jx.rest.RestService;
import com.boot.model.MapModel;
import com.boot.utils.ArgUtil;
import com.boot.utils.CollectionUtil;
import com.boot.utils.CryptoUtil.HashBuilder;
import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;

import twitter4j.DirectMessage;
import twitter4j.DirectMessageList;
import twitter4j.DirectMessageLocalImpl;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.UploadedMedia;
import twitter4j.conf.ConfigurationBuilder;

@Component
@EnableEncryptableProperties
public class TwitterClient implements MessageClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(TwitterClient.class);
	private static Map<String, TwitterClientContext> CLIENTS = Collections
			.synchronizedMap(new HashMap<String, TwitterClientContext>());

	@Autowired
	private PMEnvironment environment;

	@Autowired
	private ExtUtilService extUtilService;

	@Autowired
	RestService restService;

	public TwitterClientContext getContext(ChannelConfig channelConfig) {
		// lane = ArgUtil.nonEmpty(lane, defaultLane);

		if (ArgUtil.isEmpty(channelConfig) || ArgUtil.isEmpty(channelConfig.getChannelId())) {
			throw new PostManException("No channel " + channelConfig);
		}

		TwitterClientContext ctx = CLIENTS.get(channelConfig.getChannelId());

		if (ArgUtil.isEmpty(ctx)) {
			TwitterConfigDetails config = channelConfig.getTwitter();
			if (!ArgUtil.is(config)) {
				throw new PostManException("No Config for channel " + channelConfig.getChannelId());
			}

			ConfigurationBuilder cb = new ConfigurationBuilder();
			cb.setDebugEnabled(true).setOAuthConsumerKey(config.getConsumerKey())
					.setOAuthConsumerSecret(config.getConsumerSecret()).setOAuthAccessToken(config.getAccessToken())
					.setOAuthAccessTokenSecret(config.getAccessTokenSecret());
			TwitterFactory tf = new TwitterFactory(cb.build());
			Twitter tw = tf.getInstance();
			ctx = new TwitterClientContext(tw);
			if (ArgUtil.is(config.getEnvName())) {
				WebhookManager manager = new WebhookManager(tw.getConfiguration(), config.getEnvName());
				ctx.setWebhookManager(manager);
			}
			CLIENTS.put(channelConfig.getChannelId(), ctx);
		}
		return ctx;
	}

	private DirectMessage sendReply(Twitter twitter, String id, String text)
			throws NumberFormatException, TwitterException {
		DirectMessageLocalImpl x = new DirectMessageLocalImpl(twitter.sendDirectMessage(Long.parseLong(id), text));
		LOGGER.debug("Message result to {} : {}", id, x.getId());
		return x;
	}

	private DirectMessage sendReply(Twitter twitter, String id, String text, String mediaId)
			throws NumberFormatException, TwitterException {
		DirectMessageLocalImpl x = new DirectMessageLocalImpl(
				twitter.sendDirectMessage(Long.parseLong(id), text, Long.parseLong(mediaId)));
		LOGGER.debug("Message result to {} : {}", id, x.getId());
		return x;
	}

	public String uploadImage(Twitter twitter, String url, String title)
			throws IOException, MalformedURLException, TwitterException {
		InputStream media = new java.net.URL(url).openStream();
		UploadedMedia uploadedMedia = twitter.uploadMedia(ArgUtil.parseAsString(title, "File"), media);
		return ArgUtil.parseAsString(uploadedMedia.getMediaId());
	}

	public String uploadMedia(Twitter twitter, String url, String title)
			throws IOException, MalformedURLException, TwitterException {
		InputStream media = new java.net.URL(url).openStream();
		UploadedMedia uploadedMedia = twitter.uploadMediaChunked(ArgUtil.parseAsString(title, "File"), media);
		return ArgUtil.parseAsString(uploadedMedia.getMediaId());
	}

	public OutboxMessage send(ChannelConfig channelConfig, OutboxMessage message) {
		String to = CollectionUtil.getOne(message.getTo());
		StringJoiner msgIds = new StringJoiner(",");
		StringJoiner sj = new StringJoiner("\n");
		sj.add(message.getMessage());

		Twitter twitter = getContext(channelConfig).getTwitter();

		MapModel reqMessage = MapModel.createInstance().put(OutBoundPaths.EVENT_TYPE, "message_create")
				.put(OutBoundPaths.DM_RECIPIENT, to);
		try {

			if (message.options().containsKey("buttons")) {
				List<TmplElement> buttons = new MapModel(message.options()).entry("buttons").asList(TmplElement.class);

				MapModel options = MapModel.createInstance();
				MapModel ctas = MapModel.createInstance();
				for (TmplElement button : buttons) {
					if (ArgUtil.is(button.getUrl())) {
						ctas.add(MapModel.createInstance().put("label", button.getLabel()).put("type", "web_url")
								.put("url", button.getUrl()).toMap());
					} else {
						options.add(MapModel.createInstance().put("label", button.getLabel())
								.put("description", ArgUtil.nonEmpty(button.getDesc(), button.getLabel()))
								.put("metadata", button.getCode()).toMap());
					}
				}

				if (options.list().size() > 0) {
					reqMessage.put(OutBoundPaths.QUICK_REPLY_TYPE, "options");
					reqMessage.put(OutBoundPaths.QUICK_REPLY_OPTIONS, options.list());
				}

				if (ctas.list().size() > 0) {
					reqMessage.put(OutBoundPaths.MESSAGE_CTA, ctas.list());
				}

			}

			if (ArgUtil.is(message.getAttachments())) {
				for (Attachment attachment : message.getAttachments()) {
					if (ArgUtil.is(attachment.getMediaURL())) {
						if (ArgUtil.areEqual(attachment.getMediaType(), FileType.IMAGE.toString())) {
							String mediaId = uploadImage(twitter, attachment.getMediaURL(),
									attachment.getMediaCaption());
							attachment.mediaId(mediaId); // TODO To Validate if it is OK
							attachment.mediaIdExt(mediaId); // Latest
							attachment.mediaIdInline(mediaId); // Latest

//			    DirectMessage resp = sendReply(twitter, to,
//				    ArgUtil.nonEmpty(attachment.getMediaCaption(), message.getSubject()), mediaId);
//			    if (ArgUtil.is(resp.getId()))
//				msgIds.add(ArgUtil.parseAsString(resp.getId()));
							reqMessage.put(OutBoundPaths.ATTACHMENT_TYPE, "media");
							reqMessage.put(OutBoundPaths.ATTACHMENT_MEDIA, mediaId);

						} else if (ArgUtil.areEqual(attachment.getMediaType(), FileType.VIDEO.toString())) {
							String mediaId = uploadMedia(twitter, attachment.getMediaURL(),
									attachment.getMediaCaption());
							attachment.mediaId(mediaId);
//			    DirectMessage resp = sendReply(twitter, to,
//				    ArgUtil.nonEmpty(attachment.getMediaCaption(), message.getSubject()), mediaId);
//			    if (ArgUtil.is(resp.getId()))
//				msgIds.add(ArgUtil.parseAsString(resp.getId()));
							reqMessage.put(OutBoundPaths.ATTACHMENT_TYPE, "media");
							reqMessage.put(OutBoundPaths.ATTACHMENT_MEDIA, mediaId);
						} else {
							sj.add(extUtilService.tinyUrl(attachment.getMediaURL()));
						}
					}
				}
			}

			if (sj.length() > 0) {
				reqMessage.put(TwitterConstants.OutBoundPaths.DM_TEXT, sj.toString());
				MapModel resp = sendAdvanced(channelConfig, reqMessage);
				String messageExtId = resp.path(OutBoundPaths.DM_MESSAGE_ID).asString();
				if (ArgUtil.is(messageExtId))
					msgIds.add(ArgUtil.parseAsString(messageExtId));
			}
			message.setMessageIdExt(msgIds.toString());
		} catch (IOException | TwitterException e) {
			throw new PostManException(e.getMessage());
		}
		return message;
	}

	public DirectMessageList pollDirectMessagesReceived(ChannelConfig channelConfig) throws TwitterException {
		TwitterClientContext ctx = getContext(channelConfig);
		return ctx.getDirectMessagesReceived(5);
	}

	public StatusCode registerWebhook(ChannelConfig channelConfig, String callbackURL) {
		CLIENTS.remove(channelConfig.getChannelId());
		TwitterClientContext ctx = getContext(channelConfig);
		LOGGER.info("RegisterWebHook " + callbackURL);
		StatusCode status = ctx.registerWebhook(callbackURL);
		if (ArgUtil.is(status) && status.isError) {
			LOGGER.error("RegisterWebHook:Error " + status.message);
		}
		return status;
	}

	public Map<String, String> verifyCRC(ChannelConfig channelConfig, String crcToken) {
		TwitterClientContext ctx = getContext(channelConfig);
		Map<String, String> map = new HashMap<String, String>();
		map.put("response_token",
				"sha256=" + new HashBuilder().secret(ctx.getTwitter().getConfiguration().getOAuthConsumerSecret())
						.message(crcToken).toHmac("HmacSHA256").hash());
		return map;

	}

	public MapModel sendAdvanced(ChannelConfig config, MapModel map) {
		TwitterOauthHeaderGenerator generator = new TwitterOauthHeaderGenerator(config.getTwitter().getConsumerKey(),
				config.getTwitter().getConsumerSecret(), config.getTwitter().getAccessToken(),
				config.getTwitter().getAccessTokenSecret());

		String url = TwitterConstants.API_V1("/direct_messages/events/new.json");

		Map<String, String> requestParams = new HashMap<>();
		String header = generator.generateHeader("POST", url, requestParams);
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", header);
		return restService.ajax(url).header(headers).post(map.toMap()).asMapModel();
	}

}