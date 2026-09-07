package com.boot.jx.inbound;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.boot.jx.AppContextUtil;
import com.boot.jx.api.ApiResponse;
import com.boot.jx.chat.ChatClient;
import com.boot.jx.chat.ChatProxyManager;
import com.boot.jx.chat.ChatSessionService;
import com.boot.jx.chat.ChatStatusProcessor;
import com.boot.jx.connectors.ClickPostConnector;
import com.boot.jx.model.CommonFile;
import com.boot.jx.postman.PMConstants.CHANNEL_TYPE;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.doc.config.ChannelConfigDoc;
import com.boot.jx.postman.doc.config.ChannelConfigDupsDoc;
import com.boot.jx.postman.fb.FacebookConstants;
import com.boot.jx.postman.fb.FacebookHookRequest;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.Message;
import com.boot.jx.postman.model.MessageDefinitions.Contactable;
import com.boot.jx.postman.model.MessageReport;
import com.boot.jx.postman.model.PMArgs;
import com.boot.jx.postman.model.ext.InBoundEvent;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.store.ConfigMaster;
import com.boot.jx.postman.store.ConfigStore;
import com.boot.jx.scope.vendor.VendorContext.ApiVendorHeaders;
import com.boot.jx.utils.PostManUtil;
import com.boot.model.MapModel;
import com.boot.utils.ArgUtil;
import com.boot.utils.JsonUtil;
import com.boot.utils.Random;

@RestController
public class InBoundController {

	private static final Logger LOGGER = LoggerFactory.getLogger(InBoundController.class);

	@Autowired
	private InBoundService inBoundService;

	@Autowired
	private ChatSessionService chatSessionService;

	@Autowired
	private InboundBottler inboundBottler;

	@Autowired
	private InBoundRouter inBoundRouter;

	@Autowired
	private PMEnvironment pmEnvironment;

	@Autowired
	ConfigMaster configMaster;

	@ApiVendorHeaders
	@RequestMapping(value = "/int/inbound/callback", method = RequestMethod.POST)
	public InboxMessage onInboundCallback(@RequestBody InboxMessage inboxMessage,
			@RequestParam(required = false, defaultValue = "false") boolean routed) throws InterruptedException {
		// botService.arhive(inbound);
		if (PostManUtil.hasValidCheckSum(inboxMessage)) {
//	    PMConfiguration config = pmEnvironment.config();
//	    String channelId = PostManUtil.CHANNEL_ID(inboxMessage.contact());
//	    ChannelConfig channelConfig = config.channel(channelId);
//	    ConnectorHandler connector = connectorHandlerFactory.get(channelConfig);
//	    connector.prompt(inboxMessage);
			inboundBottler.push(inboxMessage);
			// inBoundService.invokeMethodsAsync(inboxMessage);
		}
		return inboxMessage;
	}

	static AtomicInteger counter = new AtomicInteger(1);

	@Autowired
	private ChatStatusProcessor inBoundStatusService;

	@Autowired
	private ChatProxyManager proxyManager;

	@ApiVendorHeaders
	@RequestMapping(value = "/int/status/callback", method = RequestMethod.POST)
	public List<MessageReport> onStatusCallback() throws InterruptedException {
		List<MessageReport> list = new LinkedList<MessageReport>();
		String ser = ArgUtil.parseAsString(counter.getAndIncrement());
		for (int i = 0; i < 5; i++) {
			MessageReport report = new MessageReport();
			report.setMessageIdRef(ArgUtil.parseAsString(ser));
			report.setMessageIdExt(ArgUtil.parseAsString(i));
			int statusint = Random.getInt(0, 5);
			report.setStatus(Message.Status.values()[statusint]);
			report.setChangeStamp(Random.getInt(100, 999));
			list.add(report);
		}
		inBoundStatusService.publish(list);
		return list;
	}

	@ApiVendorHeaders
	@RequestMapping(value = ChatClient.PATH.ASSIGN_TO_AGENT_V2, method = RequestMethod.POST)
	public InBoundEvent assignToAgentV2(@RequestBody PMArgs params) {
		return chatSessionService.assignSessionToAgent(params).value();
	}

	@ApiVendorHeaders
	@RequestMapping(value = ChatClient.PATH.SESSION_EVENT, method = RequestMethod.POST)
	public InBoundEvent inboundEvent(@RequestBody MapModel map) {
		PMArgs pmArgs = map.keyEntry("pmArgs").as(PMArgs.class);
		InBoundEvent event = map.keyEntry("event").as(InBoundEvent.class);
		return inboundBottler.sessionEvent(event, pmArgs);
	}

	@RequestMapping(value = "/ext/release/v2/", method = { RequestMethod.POST })
	public ApiResponse<Contactable, Object> inboundMessageBoxRelease(@RequestBody Contactable contact) {
		String contactId = PostManUtil.CONTACT_ID(contact);
		inBoundService.invokeMethodsRelease(contactId);
		return ApiResponse.buildResult(contact).meta(contactId);
	}

	@RequestMapping(value = "/ext/messagse/media/reload", method = { RequestMethod.POST })
	public ApiResponse<Object, Object> messageMediaReload(@RequestParam String sessionId,
			@RequestParam String messageId) throws FileNotFoundException, IOException {
		inBoundRouter.reloadMedia(sessionId, messageId);
		return ApiResponse.build();
	}

	@RequestMapping(value = "/ext/messagse/media/reload", method = { RequestMethod.GET })
	@ResponseBody
	public ResponseEntity<byte[]> messageMediaReloadGet(@RequestParam String sessionId, @RequestParam String messageId,
			@RequestParam(required = false, defaultValue = "0") Integer index)
			throws FileNotFoundException, IOException {
		CommonFile file = inBoundRouter.reloadMedia(sessionId, messageId, index);
		return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(file.getBody());
	}

	@RequestMapping(value = "/ext/inbound/v2/{channelType}/callback/{accountKey}/{channelId}/{channelKey}",
			method = { RequestMethod.POST })
	public ApiResponse<Object, Object> inboundMessageBoxEvent(@PathVariable(required = false) String channelType,
			@PathVariable(required = false) String accountKey, @PathVariable(required = false) String channelId,
			@PathVariable(required = false) String channelKey,
			@RequestHeader(required = false, value = "X-Api-Key") String xApiKey,
			@RequestBody Map<String, Object> data) {

		if (CHANNEL_TYPE.CLICKPOST.equals(channelType)) {
			if (ArgUtil.is(channelKey)) {
				data.put(ClickPostConnector.PATH_CHANNEL_KEY, channelKey);
			}
			if (ArgUtil.is(xApiKey)) {
				data.put(ClickPostConnector.HDR_X_API_KEY, xApiKey);
			}
		}
		
		inBoundRouter.inboundMessageEvent(channelType, PostManUtil.CHANNEL_ID_DECODED(channelId), data);
		return ApiResponse.build();
	}

	@Autowired
	private ConfigStore configStore;

	@RequestMapping(value = "/ext/inbound/v2/{channelType}/callback/{accountKey}", method = { RequestMethod.POST })
	public ApiResponse<Object, Object> inboundMessageBoxEventAll(@PathVariable(required = false) String channelType,
			@PathVariable(required = false) String accountKey, @RequestBody Map<String, Object> data) {
		if (ArgUtil.is(channelType, CHANNEL_TYPE.FACEBOOK, CHANNEL_TYPE.INSTAGRAM, CHANNEL_TYPE.WACFB)) {
			MapModel requestMap = MapModel.from(data);
			FacebookHookRequest request = requestMap.as(FacebookHookRequest.class);
			requestMap.toJson();
			request.getEntry().forEach(pageEntry -> {

				MapModel newData = MapModel.createInstance();
				newData.put("object", request.getObject());
				ArrayList<Object> entry = new ArrayList<Object>();
				entry.add(JsonUtil.toJsonMap(pageEntry));
				newData.put("entry", entry);

				String pageId = pageEntry.getId();
				if (ArgUtil.is(channelType, CHANNEL_TYPE.WACFB)) {
					pageId = MapModel.from(pageEntry.getChanges().get(0))
							.path(FacebookConstants.WABAPaths.DISPLAY_PHONE_NUMBER).asString();
				}

				List<ChannelConfigDupsDoc> channels = configMaster.getChannelMeta(channelType, pageId, null);
				if (ArgUtil.is(channels)) {
					for (ChannelConfigDupsDoc channel : channels) {
						try {
							AppContextUtil.clear();
							AppContextUtil.setTenant(channel.getDomain());
							AppContextUtil.init();
							inBoundRouter.inboundMessageEventAsync(channelType, channel.getChannelId(), newData.map());
							AppContextUtil.clear();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				} else {
					String channelId = PostManUtil.CHANNEL_ID(channelType, pageId);
					inBoundRouter.inboundMessageEventAsync(channelType, channelId, newData.map());
				}
			});
		}
		return ApiResponse.build();
	}

	@RequestMapping(value = "/ext/inbound/v3/{channelType}/callback/{accountKey}/{channelId}/{channelKey}",
			method = { RequestMethod.POST })
	public ApiResponse<Object, Object> inboundMessageBoxEventV3(@PathVariable(required = false) String channelType,
			@PathVariable(required = false) String accountKey, @PathVariable(required = false) String channelId,
			@PathVariable(required = false) String channelKey, @RequestBody Map<String, Object> data) {
		if (ArgUtil.is(channelType, CHANNEL_TYPE.FACEBOOK, CHANNEL_TYPE.INSTAGRAM, CHANNEL_TYPE.WACFB)) {
			MapModel requestMap = MapModel.from(data);
			FacebookHookRequest request = requestMap.as(FacebookHookRequest.class);
			requestMap.toJson();
			request.getEntry().forEach(pageEntry -> {

				MapModel newData = MapModel.createInstance();
				newData.put("object", request.getObject());
				ArrayList<Object> entry = new ArrayList<Object>();
				entry.add(JsonUtil.toJsonMap(pageEntry));
				newData.put("entry", entry);
				
				String pageId = pageEntry.getId();
				String pageSubId = null;
				// NEW: Extract wabaId for WACFB routing fallback
				String wabaId = null;
				if (ArgUtil.is(channelType, CHANNEL_TYPE.WACFB)) {
					wabaId = pageEntry.getId();
				}
				
				// Existing logic - unchanged
				if (ArgUtil.is(channelType, CHANNEL_TYPE.WACFB)) {
					pageId = MapModel.from(pageEntry.getChanges().get(0))
							.path(FacebookConstants.WABAPaths.DISPLAY_PHONE_NUMBER).asString();
					pageSubId = MapModel.from(pageEntry.getChanges().get(0))
							.path(FacebookConstants.WABAPaths.PHONE_NUMBER_ID).asString();
				}

				// NEW: Use overloaded method with wabaId fallback (adds 4th parameter)
				List<ChannelConfigDupsDoc> channels = configMaster.getChannelMeta(channelType, pageId, pageSubId, wabaId);
				
				if (ArgUtil.is(channels)) {
					for (ChannelConfigDupsDoc channel : channels) {
						try {
						// Update laneParam2 if not set (lazy update for existing channels)
						if (CHANNEL_TYPE.WACFB.equalsIgnoreCase(channelType) 
							&& !ArgUtil.is(channel.getLaneParam2()) 
							&& ArgUtil.is(wabaId)) {
							
							// Switch to tenant context to get full config
							AppContextUtil.setTenant(channel.getDomain());
							AppContextUtil.init();
							
							// Get full config from tenant DB (no intermediate variable)
							ChannelConfig fullConfig = pmEnvironment.config().channel(channel.getChannelId());
							
							if (fullConfig != null) {
								// saveMaster will extract wabaId and set laneParam2
								configStore.saveMaster((ChannelConfigDoc) fullConfig);
							}
							
							AppContextUtil.clear();
						}
							
							// Continue with normal routing
							String routedDomain = ArgUtil.nonEmpty(channel.getDomainProxy(), channel.getDomain());
							inBoundRouter.inboundMessageEventAsync(routedDomain, channelType, channel.getChannelId(),
									newData.map());
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				} else if (ArgUtil.is(pageId)) {
					String channelIdForDomain = PostManUtil.CHANNEL_ID(channelType, pageId);
					inBoundRouter.inboundMessageEventAsync(channelType, channelIdForDomain, newData.map());
				} else {
					inBoundRouter.inboundMessageEventAsync(channelType, channelId, newData.map());
				}

			});
		} else {
			inBoundRouter.inboundMessageEvent(channelType, PostManUtil.CHANNEL_ID_DECODED(channelId), data);
		}
		return ApiResponse.build();
	}

}
