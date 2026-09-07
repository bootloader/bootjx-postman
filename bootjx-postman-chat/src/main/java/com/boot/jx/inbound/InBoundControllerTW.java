package com.boot.jx.inbound;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boot.jx.chat.ConnectorHandlerFactory;
import com.boot.jx.connectors.TwitterConnector;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.tw.TwitterClient;
import com.boot.jx.postman.tw.TwitterClientContext;
import com.boot.jx.postman.tw.WebhookInfo;
import com.boot.jx.scope.vendor.VendorContext.ApiVendorHeaders;
import com.boot.utils.ArgUtil;

import twitter4j.TwitterException;

@RestController
public class InBoundControllerTW {

	@Autowired
	private InBoundService inBoundService;

	@Autowired
	private TwitterClient twitterClient;

	@Autowired
	private TwitterConnector twitterConnector;

	private String[] pollingLanes;

	@Autowired
	private PMEnvironment pmEnvironment;

	@Autowired
	ConnectorHandlerFactory connectorHandlerFactory;

	@ApiVendorHeaders
	@RequestMapping(
			value = { "/ext/inbound/v2/tw/callback/{accountKey}/{channelId}/{channelKey}",
					"/ext/inbound/v2/tw/callback/{accountKey}/{channelId}/{channelKey}/" },
			method = { RequestMethod.GET })
	public Map<String, String> onReceiveMessageGet(@RequestParam String crc_token,
			// V2Params
			@PathVariable String accountKey, @PathVariable String channelId, @PathVariable String channelKey) {
		ChannelConfig channelConfig = pmEnvironment.local().channel(channelId);
		return twitterClient.verifyCRC(channelConfig, crc_token);
	}

	@RequestMapping(value = "/ext/crc/v2/tw/callback/{accountKey}/{channelId}/{channelKey}", method = RequestMethod.GET)
	public WebhookInfo triggerCRC(@PathVariable String accountKey, @PathVariable(required = false) String channelId,
			@PathVariable String channelKey) throws InterruptedException, TwitterException {
		ChannelConfig channelConfig = pmEnvironment.local().channel(channelId);
		TwitterClientContext ctx = twitterClient.getContext(channelConfig);
		ctx.getWebhookManager().triggerCRC();
		WebhookInfo x = ctx.getWebhookManager().getWebhookInfo();
		return x;
	}

	@Deprecated
	@ApiVendorHeaders
	@RequestMapping(value = "/ext/inbound/tw/callback/{channelId}", method = { RequestMethod.POST, })
	public List<InboxMessage> onReceiveMessagePost(@PathVariable String channelId,
			@RequestBody Map<String, Object> update) throws InterruptedException, TwitterException {
		ChannelConfig channelConfig = pmEnvironment.local().channel(channelId);
		List<InboxMessage> tmr = twitterConnector.process(channelConfig, update);
		if (tmr != null && !tmr.isEmpty()) {
			for (InboxMessage event : tmr) {
				inBoundService.invokeMethodsAsync(event);
				twitterClient.getContext(channelConfig).getTwitter()
						.destroyDirectMessage(Long.parseLong(event.getMessageIdExt()));
			}
		}
		return tmr;
	}

	@ApiVendorHeaders
	@RequestMapping(value = "/ext/inbound/tw/get", method = RequestMethod.GET)
	public List<InboxMessage> pollDirectMessages(@RequestParam(required = false) String channelId)
			throws InterruptedException, TwitterException {
		ChannelConfig channelConfig = pmEnvironment.local().channel(channelId);
		TwitterClientContext ctx = twitterClient.getContext(channelConfig);
		List<InboxMessage> tmr = twitterConnector.fetch(channelConfig);
		if (tmr != null && !tmr.isEmpty()) {
			for (InboxMessage event : tmr) {
				inBoundService.invokeMethodsAsync(event);
				ctx.getTwitter().destroyDirectMessage(Long.parseLong(event.getMessageIdExt()));
			}
		}
		return tmr;
	}

	@RequestMapping(value = "/ext/inbound/tw/registerwebhook", method = { RequestMethod.POST, RequestMethod.GET })
	public WebhookInfo registerwebhook(@RequestParam(required = false) String channelId)
			throws InterruptedException, TwitterException {
		ChannelConfig channelConfig = pmEnvironment.local().channel(channelId);
		TwitterClientContext ctx = twitterClient.getContext(channelConfig);
		connectorHandlerFactory.onChannelUpdate(channelConfig);
		WebhookInfo x = ctx.getWebhookManager().getWebhookInfo();
		return x;
	}

	// @Scheduled(fixedDelay = 5000)
	public void registerService() {
		if (!ArgUtil.is(pollingLanes)) {
			return;
		}
		for (String channelId : pollingLanes) {
			try {
				if (ArgUtil.is(channelId)) {
					pollDirectMessages(channelId);
				}
			} catch (InterruptedException | TwitterException e) {
				e.printStackTrace();
			}
		}
	}

}
