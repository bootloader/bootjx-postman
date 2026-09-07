package com.boot.jx.inbound;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.boot.jx.AppConfig;
import com.boot.jx.AppContextUtil;
import com.boot.jx.api.ApiResponse;
import com.boot.jx.api.ApiResponseUtil;
import com.boot.jx.chat.ChatSessionFactory;
import com.boot.jx.chat.ChatStatusProcessor;
import com.boot.jx.connectors.WebConnector;
import com.boot.jx.dict.ContactType;
import com.boot.jx.http.ApiRequest;
import com.boot.jx.http.CommonHttpRequest;
import com.boot.jx.http.Kooky;
import com.boot.jx.http.RequestType;
import com.boot.jx.logger.AuditService;
import com.boot.jx.postman.PMAuditEvent;
import com.boot.jx.postman.PMConfiguration;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.PMEnvironment.PMCommonConfig;
import com.boot.jx.postman.doc.ChatContactDoc;
import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.doc.MessageDoc;
import com.boot.jx.postman.doc.VisitorActivityDoc;
import com.boot.jx.postman.dto.ChatMessageDTO;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.MessageBoxEvent;
import com.boot.jx.postman.model.MessageDefinitions.Contactable;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.query.ChatContactQuery;
import com.boot.jx.postman.service.ChatDTOUtil;
import com.boot.jx.postman.store.MessageContext;
import com.boot.jx.postman.store.MessageStore;
import com.boot.jx.postman.store.SessionStore;
import com.boot.jx.postman.store.VisitorActivityStore;
import com.boot.jx.stomp.StompConfig.StompSession;
import com.boot.jx.stomp.StompTunnelSessionManager;
import com.boot.jx.utils.PostManUtil;
import com.boot.model.MapModel;
import com.boot.utils.ArgUtil;
import com.boot.utils.StringUtils;
import com.boot.utils.UniqueID;

@Controller
public class InBoundControllerWeb {

	private static final Logger LOGGER = LoggerFactory.getLogger(InBoundControllerWeb.class);
	private static final String WEB_SESSION_ID = "web-session-id";

	@Autowired
	private InBoundService inBoundService;

	@Autowired(required = false)
	private WebConnector dummyConnector;

	@Autowired
	private CommonHttpRequest commonHttpRequest;

	@Autowired
	private SessionStore sessionStore;

	@Autowired
	private ChatSessionFactory chatSessionFactory;

	@Autowired
	private MessageStore messageStore;

	@Autowired
	VisitorActivityStore visitorActivityStore;

	@Autowired
	private MessageContext messageContext;

	@Autowired
	private WebConnector connector;

	@Autowired
	private PMEnvironment pmEnvironment;

	@Autowired(required = false)
	private PMCommonConfig pmCommonConfig;

	@Autowired
	private AuditService auditService;

	@Autowired
	private ChatStatusProcessor inBoundStatusService;

	@Autowired
	private AppConfig appConfig;

	@Autowired
	private StompTunnelSessionManager stompTunnelSessionManager;

	@Value("${app.stomp}")
	boolean stompEnabled;

	@Autowired
	private InboundBottler inboundBottler;

	@ApiRequest(session = true)
	@RequestMapping(value = "/plugin/customer/**", method = RequestMethod.GET)
	public String pluginCustomer(Model model, @RequestParam(required = false) String contacyType,
			@RequestParam(required = false, defaultValue = "/plugin/customer") String path, HttpServletRequest request)
			throws InterruptedException {
		commonHttpRequest.setCookie("contactType", ArgUtil.parseAsString(contacyType, ContactType.WEBSITE.toString()));
		model.addAttribute("APP_CONTEXT", appConfig.getAppPrefix());
		model.addAttribute("POSTMAN_CONTEXT", appConfig.getAppPrefix());
		model.addAttribute("WEBAPP_BASE", appConfig.getAppPrefix() + path);
		model.addAttribute("POSTMAN_AGENT_SCHEME_COLOR",
				pmEnvironment.config().prefsEntry("postman.agent.scheme.color").asString());

		if (pmCommonConfig != null) {
			model.addAllAttributes(pmCommonConfig.appAttributes());
		}

		String nounce = UniqueID.generateString62();
		model.addAttribute("NOUNCE", nounce);
		commonHttpRequest.setCookie("NOUNCE", nounce);
		model.addAttribute("STOMP_ENABLED", stompEnabled);

		model.addAttribute("VISITOR_ID", ArgUtil.parseAsString(commonHttpRequest.get("visitorId"), "NONE"));
		model.addAttribute("VISIT_ID", ArgUtil.parseAsString(commonHttpRequest.get("visitId"), "NONE"));

		String channelId = commonHttpRequest.get("channelId");
		Contactable channel = PostManUtil.parseChannelId(channelId);

		if (ArgUtil.is(channel)) {
			model.addAttribute("WEB_CHANNEL_ID", channelId);
			ChannelConfig channelConfig = pmEnvironment.config().channel(channelId);
			if (ArgUtil.is(channelConfig) && ArgUtil.is(channelConfig.getWeb())) {
				if (ArgUtil.is(channelConfig.getWeb().getStylesheet())) {
					model.addAttribute("WEB_CHANNEL_STYLESHEET", channelConfig.getWeb().getStylesheet());
				}
				if (ArgUtil.is(channelConfig.getWeb().getTitle())) {
					model.addAttribute("WEB_CHANNEL_TITLE", channelConfig.getWeb().getTitle());
				}
			}
		}
		visitorActivityStore.save(new VisitorActivityDoc().activity("WEBCHAT_PAGE").channelId(channelId));
		return "app-customer";
	}

	@ApiRequest(session = true)
	@RequestMapping(value = "/ext/plugin/customer/**", method = RequestMethod.GET)
	public String pluginCustomerPub(Model model, @RequestParam(required = false) String contacyType,
			HttpServletRequest request) throws InterruptedException {
		return pluginCustomer(model, contacyType, "/ext/plugin/customer", request);
	}

	@Deprecated
	@ApiRequest(type = RequestType.POLL, session = true)
	@ResponseBody
	@RequestMapping(value = { "/ext/outbound/web/callback", "/ext/plugin/outbound/web/callback" },
			method = RequestMethod.GET)
	public OutboxMessage onReceiveMessage(@RequestParam(required = false) String number,
			@RequestParam(required = false) String csid, @RequestParam(required = false) String channelId,
			@RequestParam(required = false) String channelKey) throws InterruptedException {
		ChannelConfig channelConfig = pmEnvironment.config().channel(channelId);
		return dummyConnector
				.pollUnreadMessage(AppContextUtil.getTenant() + "/" + PostManUtil.CONTACT_ID(channelConfig, csid));
	}

	@ApiRequest(type = RequestType.POLL, session = true)
	@ResponseBody
	@RequestMapping(value = { "/ext/outbound/web/callback/v2", "/ext/plugin/outbound/web/callback/v2" },
			method = RequestMethod.GET)
	public ApiResponse<Object, Object> onReceiveMessage2(@RequestParam(required = false) String number,
			@RequestParam(required = false) String csid, @RequestParam(required = false) String channelId,
			@RequestParam(required = false) String channelKey, @RequestParam(required = false) String sessionId)
			throws InterruptedException {
		ChannelConfig channelConfig = pmEnvironment.config().channel(channelId);
		return ApiResponse.buildResults(dummyConnector.pollAllUnreadMessage(
				AppContextUtil.getTenant() + "/" + PostManUtil.CONTACT_ID(channelConfig, csid), sessionId));
	}

	@ApiRequest(session = true)
	@RequestMapping(value = "/ext/plugin/mobile/auth", method = RequestMethod.GET)
	public String directAuth(@RequestParam(required = false) String csid,
			@RequestParam(required = false) String deviceId, @RequestParam(required = false) String channelId,
			@RequestParam(required = false) String channelKey) {
		PMConfiguration config = pmEnvironment.config();
		ChannelConfig channelConfig = config.channel(channelId);
		if (!ArgUtil.is(channelConfig) || !ArgUtil.areEqual(channelConfig.getChannelKey(), channelKey)) {
			ApiResponseUtil.throwAccessDeniedException("Invalid Channel");
		}
		// need to cpmplte for mobile auth
		return "app-customer";
	}

	@ApiRequest(session = true)
	@ResponseBody
	@RequestMapping(value = "/ext/plugin/outbound/web/auth/v2", method = { RequestMethod.GET, RequestMethod.POST })
	public ApiResponse<ChatMessageDTO, Object> onAuthV2(@RequestParam(required = false) String user,
			@RequestParam(required = false) String number, @RequestParam(required = false) String browserfp,
			@RequestParam(required = false) String channelId, @RequestParam(required = false) String channelKey,
			@RequestParam(required = false) String userCode, // Id Set by DomainService
			@RequestParam(required = false) String userToken, // phone Set by DomainService
			@RequestParam(required = false) String userName, // phone Set by DomainService
			@RequestParam(required = false) String userEmail, // email Set by DomainService
			@RequestParam(required = false) String userPhone, // phone Set by DomainService,
			@RequestParam(required = false) String csid // ForBackward
	) throws InterruptedException {

		String webSessionId = null;
		String webSessionIdValid = null;
		String contactId = null;
		ChatSessionDoc session = null;
		ChannelConfig channelConfig = pmEnvironment.config().channel(channelId);
		String webSessionIdKey = StringUtils.sanitize(WEB_SESSION_ID + "_" + channelId);
		InboxMessage msg = connector.createInboxMessage(channelConfig);

		if (ArgUtil.is(userCode)) {
			csid = "u" + userCode;
			msg.contact().setCsid(csid);
			contactId = PostManUtil.CONTACT_ID(channelConfig, csid);
			session = chatSessionFactory.getChatSessionByContactId(contactId, null);
		} else {
			webSessionId = commonHttpRequest.get(webSessionIdKey);
			csid = "g" + ArgUtil.nonEmpty(browserfp, number, csid, UniqueID.generate());
			msg.contact().setCsid(csid);
			contactId = PostManUtil.CONTACT_ID(channelConfig, csid);
			if (ArgUtil.is(webSessionId)) {
				session = sessionStore.getValidSession(webSessionId);
			}
		}

		commonHttpRequest.setCookie(new Kooky().name("contactId").value(contactId));

		Contactable contact = PostManUtil.getContactMeta(msg.contact());
		contact.setName(userName);
		contact.setEmail(userEmail);
		contact.phone(userPhone);
		ChatContactQuery chatContactQuery = new ChatContactQuery(contact.getContactId());
		chatContactQuery.update(contact);
		chatContactQuery.updateCreatedStamp();
		if (ArgUtil.is(userCode)) {
			chatContactQuery.setUserCode(userCode);
		}
		chatContactQuery.setUserToken(userToken);
		sessionStore.upsert(chatContactQuery);

		String contactIdWeb = AppContextUtil.getTenant() + "/" + contactId;

		List<ChatMessageDTO> msgs = new ArrayList<ChatMessageDTO>();
		if (ArgUtil.is(session)) {
			webSessionIdValid = session.getSessionId();
			List<MessageDoc> messages = messageStore.findBySessionId(session.getSessionId(),
					ContactType.WEBSITE.toString());
			for (MessageDoc messageDoc : messages) {
				ChatMessageDTO outboxMessage = ChatDTOUtil.getChatMessageDTO(messageDoc);
				if (ArgUtil.isEqual(messageDoc.getType(), "I", "O")) {
					msgs.add(outboxMessage);
				}
			}
		}

		MapModel meta = MapModel.createInstance();
		meta.put("webSessionIdKey", webSessionIdKey);
		meta.put("webSessionId", webSessionIdValid);
		meta.put("csid", csid);

		StompSession stomp = null;
		if (ArgUtil.is(channelConfig)) {
			stomp = stompTunnelSessionManager.registerUser(ArgUtil.nonEmpty(user, csid), contactIdWeb, csid);
		}

		if (msgs.size() == 0) {
			ChatContactDoc chatContactDoc = sessionStore.getContact(contactId);
			OutboxMessage icebrakerMsg = dummyConnector.onIceBreak(channelConfig, chatContactDoc);
			if (ArgUtil.is(icebrakerMsg)) {
				ChatMessageDTO icebrakerMsgDto = ChatDTOUtil
						.getChatMessageDTO(messageStore.createMessageDoc(icebrakerMsg));
				icebrakerMsgDto.setMessageId("icebrakerMsg");
				msgs.add(icebrakerMsgDto);
			}
		}

		VisitorActivityDoc visit = new VisitorActivityDoc().activity("WEBCHAT_AUTH").channelId(channelId)
				.contactId(contactId);
		if (stomp != null) {
			visit.meta().put("jsessionId", stomp.getJsessionId());
			visit.meta().put("xsessionId", stomp.getXsessionId());
		}
		visitorActivityStore.save(visit);

		// In-Cognito Window does not support Cookies that is why it So important to
		// send these values to UI in advance for mapping,
		// because if cookies cant be set, JSESSION cannot be created and be relied upon
		// to store these values
		meta.put("stomp", stomp);
		return ApiResponse.buildResults(msgs, meta.toMap());
	}

	private ApiResponse<InboxMessage, Object> inboundMessageBoxEventMethod(String channelId, String channelKey,
			MapModel map, MultipartFile file) {
		PMConfiguration config = pmEnvironment.config();
		ChannelConfig channelConfig = config.channel(channelId);
		MapModel meta = MapModel.createInstance();
		// ConnectorHandler connector = connectorHandlerFactory.get(channelConfig);
		try {

			// if (!ArgUtil.areEqual(nounce, commonHttpRequest.get("NOUNCE"))) {
			// ApiResponseUtil.throwUnAuthorizedException("Invalid API");
			// }

			if (!ArgUtil.is(channelConfig) || !ArgUtil.areEqual(channelConfig.getChannelKey(), channelKey)) {
				ApiResponseUtil.throwAccessDeniedException("Invalid Channel");
			}

			String webSessionIdKey = StringUtils.sanitize(WEB_SESSION_ID + "_" + channelId);

			MessageBoxEvent messageBoxEvent = connector.inboundMessageBoxEvent(channelConfig, map,
					new MessageBoxEvent(), file);
			if (ArgUtil.is(messageBoxEvent.getInboxMessages())) {
				InboxMessage sessionMessage = new InboxMessage();

				messageBoxEvent.getInboxMessages().forEach(inboxMessage -> {
					// inBoundService.invokeMethodsAsync(inboxMessage);
					inboundBottler.push(inboxMessage);
					sessionMessage.setSessionId(inboxMessage.getSessionId());
					sessionMessage.setContact(sessionMessage.getContact());
				});
				connector.onReceiveInboxMessage(messageBoxEvent.getInboxMessages());
				String webSessionId = commonHttpRequest.get(webSessionIdKey);
				if (!ArgUtil.is(webSessionId) || !webSessionId.equalsIgnoreCase(sessionMessage.getSessionId())) {
					webSessionId = sessionMessage.getSessionId();
					commonHttpRequest.setCookie(new Kooky().name(webSessionIdKey).value(webSessionId));
				}
				meta.put("webSessionIdKey", webSessionIdKey);
				meta.put("webSessionId", webSessionId);

				return ApiResponse.buildResults(messageBoxEvent.getInboxMessages(), meta.toMap());
			} else if (ArgUtil.is(messageBoxEvent.getMessageReports())) {
				connector.onMessageReports(messageBoxEvent.getMessageReports());
				inBoundStatusService.publish(messageBoxEvent.getMessageReports());
			}
		} catch (Exception e) {
			auditService.excep(new PMAuditEvent(PMAuditEvent.Type.INBOUND_ERROR).data(map.toMap()), LOGGER, e);
		}

		return ApiResponse.buildResult(null, meta.toMap());
	}

	@ApiRequest(session = true)
	@ResponseBody
	@RequestMapping(value = "/ext/plugin/inbound/v2/web/callback/{nounce}/{channelId}/{channelKey}",
			method = { RequestMethod.POST })
	public ApiResponse<InboxMessage, Object> inboundMessageBoxEvent(@PathVariable(required = false) String nounce,
			@PathVariable(required = false) String channelId, @PathVariable(required = false) String channelKey,
			@RequestBody Map<String, Object> data) {
		MapModel map = MapModel.from(data);
		return inboundMessageBoxEventMethod(channelId, channelKey, map, null);
	}

	@ApiRequest(session = true)
	@ResponseBody
	@RequestMapping(value = "/ext/plugin/inbound/v2/web/callback/{nounce}/{channelId}/{channelKey}",
			method = { RequestMethod.PUT })
	public ApiResponse<InboxMessage, Object> inboundMediaBoxEvent(@PathVariable(required = false) String nounce,
			@PathVariable(required = false) String channelId, @PathVariable(required = false) String channelKey,
			@RequestParam String from, @RequestParam(name = "file") MultipartFile file) throws InterruptedException {
		MapModel data = MapModel.createInstance().put("from", from);
		return inboundMessageBoxEventMethod(channelId, channelKey, data, file);
	}

}
