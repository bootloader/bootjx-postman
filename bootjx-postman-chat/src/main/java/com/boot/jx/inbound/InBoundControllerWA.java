package com.boot.jx.inbound;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boot.jx.api.ApiResponse;
import com.boot.jx.chat.ChatService;
import com.boot.jx.chat.ChatStatusProcessor;
import com.boot.jx.chat.ConnectorHandlerFactory;
import com.boot.jx.connectors.WA360CloudConnector;
import com.boot.jx.connectors.WA360Connector;
import com.boot.jx.connectors.WAGupShupAgentConnector;
import com.boot.jx.connectors.WAGupShupConnector;
import com.boot.jx.connectors.WARapiwhaConnector;
import com.boot.jx.http.CommonHttpRequest;
import com.boot.jx.logger.AuditService;
import com.boot.jx.postman.PMAuditEvent;
import com.boot.jx.postman.PMConfiguration;
import com.boot.jx.postman.PMConstants.CHANNEL_TYPE;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.gupshup.GupShupDeliveryResp;
import com.boot.jx.postman.gupshup.GupShupDeliveryResp.GupShupDeliveryDto;
import com.boot.jx.postman.gupshup.GupShupInbound;
import com.boot.jx.postman.gupshup.GupShupInboundV2;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.MessageBoxEvent;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.scope.vendor.VendorContext.ApiVendorHeaders;
import com.boot.model.MapModel;
import com.boot.utils.ArgUtil;
import com.boot.utils.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;

@RestController
public class InBoundControllerWA {

	private static final Logger LOGGER = LoggerFactory.getLogger(InBoundControllerWA.class);

	@Autowired
	private InBoundMessageProcessor inBoundService;

	@Autowired
	private ChatStatusProcessor chatStatusService;

	@Autowired
	private WARapiwhaConnector waRapiwhaConnector;

	@Autowired
	private WAGupShupConnector waGupShupConnector;

	@Autowired
	private WAGupShupAgentConnector waGupShupAgentConnector;

	@Autowired
	private CommonHttpRequest commonHttpRequest;

	@Autowired
	private ChatService chatService;

	@Autowired
	private ConnectorHandlerFactory connectorHandlerFactory;

	@Autowired
	private PMEnvironment pmEnvironment;

	// @ApiRequest(feature = "WA_GUPSHUP_INBOUND")
	@ApiVendorHeaders
	@RequestMapping(
			value = { "/ext/inbound/gupshup/callback",
					"/ext/inbound/v2/wags/callback/{accountKey}/{channelId}/{channelKey}" },
			method = { RequestMethod.POST, RequestMethod.GET, RequestMethod.PUT })
	public InboxMessage onReceiveMessage(
			@RequestBody(required = false) Optional<Map<String, Object>> inboundMapOptional,
			@RequestParam(required = false, defaultValue = "false") boolean routed,
			@PathVariable(required = false) String accountKey, @PathVariable(required = false) String channelId,
			@PathVariable(required = false) String channelKey) throws InterruptedException {
		if (inboundMapOptional.isPresent()) {
			return extracted(inboundMapOptional.get(), channelId);
		}
		return null;
	}

	@ApiVendorHeaders
	@RequestMapping(
			value = { "/ext/inbound/gupshup/callback",
					"/ext/inbound/v2/wags/callback/{accountKey}/{channelId}/{channelKey}" },
			method = { RequestMethod.POST }, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public InboxMessage onReceiveMessage(@PathVariable(required = false) String accountKey,
			@PathVariable(required = false) String channelId, @PathVariable(required = false) String channelKey)
			throws InterruptedException {
		Map<String, Object> inboundMap = new HashMap<String, Object>();
		inboundMap.put("waNumber", commonHttpRequest.get("waNumber"));
		inboundMap.put("mobile", commonHttpRequest.get("mobile"));
		inboundMap.put("type", commonHttpRequest.get("type"));
		inboundMap.put("text", commonHttpRequest.get("text"));
		inboundMap.put("timestamp", commonHttpRequest.get("timestamp"));
		inboundMap.put("name", commonHttpRequest.get("name"));

		String image = commonHttpRequest.get("image");
		if (ArgUtil.is(image)) {
			inboundMap.put("image", JsonUtil.fromJsonToMap(image));
		}

		String document = commonHttpRequest.get("document");
		if (ArgUtil.is(document)) {
			inboundMap.put("document", JsonUtil.fromJsonToMap(document));
		}

		String voice = commonHttpRequest.get("document");
		if (ArgUtil.is(voice)) {
			inboundMap.put("voice", JsonUtil.fromJsonToMap(voice));
		}

		String audio = commonHttpRequest.get("audio");
		if (ArgUtil.is(audio)) {
			inboundMap.put("audio", JsonUtil.fromJsonToMap(audio));
		}

		String video = commonHttpRequest.get("video");
		if (ArgUtil.is(video)) {
			inboundMap.put("video", JsonUtil.fromJsonToMap(video));
		}

		String location = commonHttpRequest.get("location");
		if (ArgUtil.is(location)) {
			inboundMap.put("location", JsonUtil.fromJsonToMap(location));
		}
		String contacts = commonHttpRequest.get("contacts");
		if (ArgUtil.is(contacts)) {
			inboundMap.put("contacts", JsonUtil.fromJsonToMap(contacts));
		}
		return extracted(inboundMap, channelId);
	}

	private InboxMessage extracted(Map<String, Object> inboundMap, String channelId) {
		try {
			InboxMessage event = null;
			if (inboundMap.containsKey("waNumber")) {
				event = waGupShupConnector.toInboxMessage(JsonUtil.toObject(inboundMap, GupShupInbound.class));
			} else {
				event = waGupShupAgentConnector.toInboxMessage(JsonUtil.toObject(inboundMap, GupShupInboundV2.class));
			}
			event.setOriginalMessage(inboundMap);
			inBoundService.invokeMethodsAsync(event);
			return event;
		} catch (Exception e) {
			LOGGER.error("INBOUND", e);
		}
		return null;
	}

	@RequestMapping(value = "/ext/status/gupshup/callback.json", method = { RequestMethod.POST, RequestMethod.GET })
	public GupShupDeliveryResp onStatusMessage(@RequestBody GupShupDeliveryResp status) throws InterruptedException {
		chatStatusService.publish(waGupShupConnector.updateDeliveryStatus(status));
		return status;
	}

	@ApiVendorHeaders
	@RequestMapping(value = "/ext/status/gupshup/callback", method = { RequestMethod.POST },
			consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public GupShupDeliveryResp onStatusMessage() throws InterruptedException, IOException {
		GupShupDeliveryResp status = new GupShupDeliveryResp();
		String response = commonHttpRequest.get("response");
		if (ArgUtil.is(response)) {
			status.setResponse(JsonUtil.parse(response, new TypeReference<List<GupShupDeliveryDto>>() {
			}));
		}
		return onStatusMessage(status);
	}

	@RequestMapping(value = "/ext/inbound/rapiwha/callback/{secret}", method = { RequestMethod.POST })
	public ApiResponse<Object, Object> onAPIWHAMessage(@RequestParam(required = false) String secret,
			@RequestParam String data) {
		try {
			Map<String, Object> dataMap = JsonUtil.getMapFromJsonString(data);
			waRapiwhaConnector.toInboxMessage(dataMap, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ApiResponse.build();
	}

	@Autowired
	private WA360Connector w360Connector;

	@RequestMapping(value = "/ext/inbound/wa360/registerwebhook", method = RequestMethod.GET)
	public ApiResponse<Object, Object> registerWebHook(@RequestParam(required = false) String lane)
			throws InterruptedException {
		connectorHandlerFactory.onChannelUpdate(CHANNEL_TYPE.WA_360D, lane);
		return ApiResponse.build();
	}

	@Autowired
	private WA360CloudConnector w360CloudConnector;

	@RequestMapping(value = "/ext/inbound/wac360/registerwebhook", method = RequestMethod.GET)
	public ApiResponse<Object, Object> registerCloudWebHook(@RequestParam(required = false) String lane)
			throws InterruptedException {
		connectorHandlerFactory.onChannelUpdate(CHANNEL_TYPE.WA_360DC, lane);
		return ApiResponse.build();
	}

	@Autowired
	private AuditService auditService;

	@Deprecated
	@RequestMapping(value = "/ext/inbound/wa360/callback/{accountKey}/{channelId}/{channelKey}",
			method = { RequestMethod.POST })
	public ApiResponse<Object, Object> onWA360Message(@PathVariable(required = false) String accountKey,
			@PathVariable(required = false) String channelId, @PathVariable(required = false) String channelKey,
			@RequestBody Map<String, Object> data) {
		MapModel map = MapModel.from(data);
		PMConfiguration config = pmEnvironment.local();
		ChannelConfig channelConfig = config.channel(channelId);

		try {
			MessageBoxEvent messageBoxEvent = w360Connector.inboundMessageBoxEvent(channelConfig, map,
					new MessageBoxEvent());
			if (ArgUtil.is(messageBoxEvent.getInboxMessages())) {
				messageBoxEvent.getInboxMessages().forEach(inboxMessage -> {
					inBoundService.publishAsync(inboxMessage);
				});
				w360Connector.onReceiveInboxMessage(messageBoxEvent.getInboxMessages());
			} else if (ArgUtil.is(messageBoxEvent.getMessageReports())) {
				w360Connector.onMessageReports(messageBoxEvent.getMessageReports());
				chatStatusService.publish(messageBoxEvent.getMessageReports());
			}
		} catch (Exception e) {
			auditService.excep(new PMAuditEvent(PMAuditEvent.Type.INBOUND_ERROR).data(data), LOGGER, e);
		}

		return ApiResponse.build();
	}
}
