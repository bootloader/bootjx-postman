package com.boot.jx.inbound;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boot.jx.connectors.FacebookConnector;
import com.boot.jx.connectors.InstagramConnector;
import com.boot.jx.http.CommonHttpRequest;
import com.boot.jx.mongo.CommonMongoTemplate;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.fb.FacebooClient;
import com.boot.jx.postman.fb.FacebookHookRequest;
import com.boot.jx.postman.fb.InstagramClient;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.wacfb.WacfbClient;
import com.boot.jx.scope.vendor.VendorContext.ApiVendorHeaders;
import com.boot.utils.ArgUtil;

/**
 * Special Handling for FB and INstagram
 * 
 * @author lalittanwar
 *
 */
@RestController
public class InBoundControllerFB {

	private static final Logger LOGGER = LoggerFactory.getLogger(InBoundControllerFB.class);

	@Autowired
	private InBoundService inBoundService;

	@Autowired
	private FacebooClient facebooClient;

	@Autowired
	private InstagramClient instaClient;

	@Autowired
	private WacfbClient wacfbClient;

	@Autowired
	private FacebookConnector facebookConnector;

	@Autowired
	private InstagramConnector instaConnector;

	@Autowired
	private PMEnvironment pmEnvironment;

	@Autowired
	private CommonHttpRequest commonHttpRequest;

	@Autowired
	private CommonMongoTemplate commonMongoTemplate;

	@RequestMapping(value = { "/ext/inbound/v2/fb/callback/{accountKey}/{channelId}/{channelKey}",
			"/ext/inbound/v2/fb/callback/{accountKey}",
			"/ext/inbound/v3/fb/callback/{accountKey}/{channelId}/{channelKey}"}, method = RequestMethod.GET)
	public Object get(@RequestParam(name = "hub.verify_token") String token,
			@RequestParam(name = "hub.challenge") String challenge,
			@RequestHeader(required = false, value = "X-Hub-Signature") String signature,
			@PathVariable(required = false) String accountKey, @PathVariable(required = false) String channelId,
			@PathVariable(required = false) String channelKey) {
		if (!ArgUtil.is(channelId)) {
			channelId = commonHttpRequest.getRequestParam("channelId");
		}
		ChannelConfig channelConfig = pmEnvironment.local().channel(channelId);
		return facebooClient.registerWebhook(channelConfig, token, challenge);
	}

	@RequestMapping(
			value = { "/ext/inbound/ig/callback", "/ext/inbound/v2/ig/callback/{accountKey}/{channelId}/{channelKey}",
					"/ext/inbound/v2/ig/callback/{accountKey}",
					"/ext/inbound/v3/ig/callback/{accountKey}/{channelId}/{channelKey}"},
			method = RequestMethod.GET)
	public Object get(@RequestParam(name = "hub.verify_token") String token,
			@RequestParam(name = "hub.challenge") String challenge, @RequestParam(required = false) String lane,
			@RequestHeader(required = false, value = "X-Hub-Signature") String signature,
			// V2Params
			@PathVariable(required = false) String channelType, @PathVariable(required = false) String accountKey,
			@PathVariable(required = false) String channelId, @PathVariable(required = false) String channelKey) {
		if (!ArgUtil.is(channelId)) {
			channelId = commonHttpRequest.getRequestParam("channelId");
		}
		ChannelConfig channelConfig = pmEnvironment.local().channel(channelId);
		return instaClient.registerWebhook(channelConfig, token, challenge);
	}

	@RequestMapping(
			value = { "/ext/inbound/wacfb/callback",
					"/ext/inbound/v2/wacfb/callback/{accountKey}/{channelId}/{channelKey}",
					"/ext/inbound/v2/wacfb/callback/{accountKey}",
					"/ext/inbound/v3/wacfb/callback/{accountKey}/{channelId}/{channelKey}", },
			method = RequestMethod.GET)
	public Object getWA(@RequestParam(name = "hub.verify_token") String token,
			@RequestParam(name = "hub.challenge") String challenge, @RequestParam(required = false) String lane,
			@RequestHeader(required = false, value = "X-Hub-Signature") String signature,
			// V2Params
			@PathVariable(required = false) String channelType, @PathVariable(required = false) String accountKey,
			@PathVariable(required = false) String channelId, @PathVariable(required = false) String channelKey) {
		if (!ArgUtil.is(channelId)) {
			channelId = commonHttpRequest.getRequestParam("channelId");
		}
		ChannelConfig channelConfig = pmEnvironment.local().channel(channelId);
		return wacfbClient.registerWebhook(channelConfig, token, challenge);
	}

	@Deprecated
	// @ApiRequest(feature = "WA_GUPSHUP_INBOUND")
	@ApiVendorHeaders
	@RequestMapping(value = "/ext/inbound/fb/callback", method = RequestMethod.POST)
	public FacebookHookRequest onReceiveMessage(@RequestBody FacebookHookRequest request,
			@RequestParam(required = false) String lane,
			@RequestHeader(required = false, value = "X-Hub-Signature") String signature) throws InterruptedException {
		request.getEntry().forEach(pageEntry -> {
			pageEntry.getMessaging().forEach(m -> {
				InboxMessage event = facebookConnector.toInboxMessage(m, pageEntry.getId());
				inBoundService.invokeMethodsAsync(event);
				// facebooClient.sendReply(event.getContactId(), "Helo", pageEntry.getId());
			});
		});
		return request;
	}

	@Deprecated
	@ApiVendorHeaders
	@RequestMapping(value = "/ext/inbound/fb/callback/{lane}", method = RequestMethod.POST)
	public FacebookHookRequest onReceiveMessageLane(@RequestBody FacebookHookRequest request, @PathVariable String lane)
			throws InterruptedException {
		request.getEntry().forEach(pageEntry -> {
			pageEntry.getMessaging().forEach(m -> {
				InboxMessage event = facebookConnector.toInboxMessage(m, pageEntry.getId());
				inBoundService.invokeMethodsAsync(event);
			});
		});
		return request;
	}

	@Deprecated
	@ApiVendorHeaders
	@RequestMapping(value = "/ext/inbound/ig/callback/{lane}", method = RequestMethod.POST)
	public FacebookHookRequest onReceiveMessageLaneIG(@RequestBody FacebookHookRequest request,
			@PathVariable String lane) throws InterruptedException {
		request.getEntry().forEach(pageEntry -> {
			pageEntry.getMessaging().forEach(m -> {
				InboxMessage event = instaConnector.toInboxMessage(m, pageEntry.getId());
				inBoundService.invokeMethodsAsync(event);
			});
		});
		return request;
	}

	@Deprecated
	// @ApiRequest(feature = "WA_GUPSHUP_INBOUND")
	@ApiVendorHeaders
	@RequestMapping(value = "/ext/inbound/ig/callback", method = RequestMethod.POST)
	public FacebookHookRequest onReceiveMessageIG(@RequestBody FacebookHookRequest request,
			@RequestParam(required = false) String lane,
			@RequestHeader(required = false, value = "X-Hub-Signature") String signature) throws InterruptedException {
		request.getEntry().forEach(pageEntry -> {
			pageEntry.getMessaging().forEach(m -> {
				InboxMessage event = instaConnector.toInboxMessage(m, pageEntry.getId());
				inBoundService.invokeMethodsAsync(event);
				// facebooClient.sendReply(event.getContactId(), "Helo", pageEntry.getId());
			});
		});
		return request;
	}

}
