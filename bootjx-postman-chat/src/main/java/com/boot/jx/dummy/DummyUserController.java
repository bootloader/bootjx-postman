package com.boot.jx.dummy;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boot.jx.AppConfig;
import com.boot.jx.connectors.WebConnector;
import com.boot.jx.dict.ContactType;
import com.boot.jx.http.CommonHttpRequest;
import com.boot.jx.inbound.InBoundService;
import com.boot.jx.postman.PMConstants;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.PMEnvironment.PMCommonConfig;
import com.boot.jx.postman.PMEnvironment.PMConfigurationObject;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.utils.ArgUtil;
import com.boot.utils.Constants;

import io.swagger.annotations.ApiOperation;

@Controller
public class DummyUserController {

	@Autowired
	private InBoundService inBoundEngine;

	@Autowired(required = false)
	private WebConnector dummyConnector;

	@Autowired
	AppConfig appConfig;

	@Autowired
	CommonHttpRequest commonHttpRequest;

	@Autowired(required = false)
	private PMCommonConfig pmCommonConfig;

	@Autowired
	private PMEnvironment pmEnvironment;

	@Autowired
	private InBoundService inBoundService;

	@RequestMapping(value = { "/dummy/user", "/pub/customer" }, method = RequestMethod.GET)
	public String dummyUser(@RequestParam(required = false) String number, Model model) throws InterruptedException {

		model.addAttribute("LOCAL_PATH", appConfig.getAppPrefix());
		model.addAttribute("POSTMAN_CONTEXT", appConfig.getAppPrefix());

		if (pmCommonConfig != null) {
			if (!pmCommonConfig.isValidDomain()) {
				return pmCommonConfig.mainDomainRedirect();
			}
			model.addAllAttributes(pmCommonConfig.appAttributes());
		}
		model.addAttribute("APP", "CUSTOMER");

		model.addAttribute("POSTMAN_AGENT_SCHEME_COLOR",
				pmEnvironment.config().prefsEntry("postman.agent.scheme.color").asString());

		if (pmCommonConfig != null) {
			model.addAttribute("CDN_URL", pmCommonConfig.getCdnServerDebug());
		}

		PMConfigurationObject defaultWebChannel = pmEnvironment.config()
				.prefsEntry(PMConstants.PROPERTIES.POSTMAN_CHAT_WEB_CHANNEL);

		ChannelConfig channelConfig = null;
		if (defaultWebChannel.exists()) {
			channelConfig = pmEnvironment.config().channel(defaultWebChannel.asString());
		}

		if (!ArgUtil.is(channelConfig)) {
			channelConfig = pmEnvironment.config().channel("web:" + pmCommonConfig.getServiceServer());
		}

		if (!ArgUtil.is(channelConfig)) {
			channelConfig = pmEnvironment.config().channel("web:page");
		}

		if (ArgUtil.is(channelConfig)) {
			model.addAttribute("CHANNEL_ID", channelConfig.getChannelId());
			model.addAttribute("CHANNEL_KEY", channelConfig.getChannelKey());
		}

		model.addAttribute("USER_CODE", ArgUtil.parseAsString(commonHttpRequest.get("CODE"), Constants.BLANK));
		model.addAttribute("USER_NAME", ArgUtil.parseAsString(commonHttpRequest.get("NAME"), Constants.BLANK));
		model.addAttribute("USER_EMAIL", ArgUtil.parseAsString(commonHttpRequest.get("EMAIL"), Constants.BLANK));
		model.addAttribute("USER_PHONE", ArgUtil.parseAsString(commonHttpRequest.get("PHONE"), Constants.BLANK));
		model.addAttribute("USER_TOKEN", ArgUtil.parseAsString(commonHttpRequest.get("TOKEN"), Constants.BLANK));
		return "dummyuser";
	}

	@ResponseBody
	@RequestMapping(value = "/dummy/messages", method = RequestMethod.GET)
	public OutboxMessage onReceiveMessage(@RequestParam String number) throws InterruptedException {
		return dummyConnector.pollUnreadMessage(number);
	}

	@ResponseBody
	@RequestMapping(value = "/dummy/messages", method = RequestMethod.POST)
	public InboxMessage onReceiveMessage(@RequestParam String message, @RequestParam String number)
			throws InterruptedException {
		InboxMessage event = new InboxMessage();
		Cookie cookie = commonHttpRequest.getCookie("contactType");

		ContactType contactType = ContactType.WEBSITE;
		if (ArgUtil.is(cookie)) {
			contactType = ArgUtil.parseAsEnumT(cookie.getValue(), contactType, ContactType.class);
		}

		event.contact().setContactType(contactType.toString());
		event.contact().setLane("DUMMY");
		event.from(number);
		event.setMessage(message);
		inBoundEngine.invokeMethodsAsync(event);
		return event;
	}

	@ResponseBody
	@RequestMapping(value = "/ext/inbound/web/callback", method = RequestMethod.POST)
	public InboxMessage onReceiveMessage(@RequestBody InboxMessage event) throws InterruptedException {
		event.contact().setContactType(ContactType.WEBSITE.toString());
		event.contact().setLane("MainSite");

		// event.contact().setContactType(ContactType.TELEGRAM.toString());
		// event.contact().setLane("MeheryDemoBot");
		// event.setLane("919082854885");
		// event.setChannel("GUPSHUPW");
		// event.setFrom("919930104050");
		// event.setFromName("Lalit Tanwar");

		// Cleaning
		// event.setSessionId("600edc822743742e916202b9");
		event.setSessionId(null);
		event.setMessageId(null);
		event.contact().setCsid(event.getFrom());
		event.session().setAgent(null);
		event.session().setDept(null);
		inBoundService.invokeMethodsAsync(event);

		String webSessionId = commonHttpRequest.get("web-session-id");
		if (!ArgUtil.is(webSessionId) || !webSessionId.equalsIgnoreCase(event.getSessionId())) {
			commonHttpRequest.setCookie("web-session-id", event.getSessionId());
		}
		return event;
	}

	@RequestMapping(value = "/dummy/customer", method = RequestMethod.GET)
	public String dummyCustomer(Model model, @RequestParam(required = false) String contacyType)
			throws InterruptedException {
		commonHttpRequest.setCookie("contactType", ArgUtil.parseAsString(contacyType, ContactType.WEBSITE.toString()));
		model.addAttribute("APP_CONTEXT", appConfig.getAppPrefix());
		model.addAttribute("POSTMAN_CONTEXT", appConfig.getAppPrefix());
		model.addAttribute("POSTMAN_AGENT_SCHEME_COLOR",
				pmEnvironment.config().prefsEntry("postman.agent.scheme.color").asString());
		return "customer.plugin.bubble";
	}

	@ApiOperation(value = "Try docs", hidden = true)
	@RequestMapping(value = { "/docs" }, method = { RequestMethod.GET, RequestMethod.POST })
	public String docs(Model model, @RequestParam(required = false) String path) {
		return "redirect:" + pmEnvironment.config().prefsEntry("mry.prop.service.docs.link").asString()
				+ ArgUtil.nonEmpty(path, Constants.BLANK);
	}

	@ApiOperation(value = "Try docs", hidden = true)
	@RequestMapping(value = { "/server-{xms}/**" }, method = { RequestMethod.GET, RequestMethod.POST })
	public String serverXmsDocs(Model model, @PathVariable String xms, HttpServletRequest request) {

		String[] paths = request.getRequestURI().split(request.getContextPath()
		// + "/server-"+ xms
		);
		String path = paths.length > 1 ? paths[1] : Constants.BLANK;
		return "redirect:" + pmEnvironment.config().prefsEntry("mry.prop.service.docs.link").asString()
				+ ArgUtil.nonEmpty(path, Constants.BLANK);
	}
}
