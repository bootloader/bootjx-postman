package com.boot.jx.setup;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boot.jx.AppConfig;
import com.boot.jx.AppConfigPackage.AppCommonConfig;
import com.boot.jx.AppContextUtil;
import com.boot.jx.api.ApiResponse;
import com.boot.jx.auth.AuthStateManager;
import com.boot.jx.auth.AuthStateManager.AuthState;
import com.boot.jx.cdn.BootJxConfigService;
import com.boot.jx.chat.ConnectorHandlerFactory;
import com.boot.jx.chat.ConnectorHandlerFactory.ConnectorHandler;
import com.boot.jx.dict.ContactType;
import com.boot.jx.http.ApiRequest;
import com.boot.jx.http.CommonHttpRequest;
import com.boot.jx.mongo.CommonMongoQB.MongoQueryBuilder;
import com.boot.jx.mongo.CommonMongoTemplate;
import com.boot.jx.postman.PMConstants.CHANNEL_TYPE_ENUM;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.PMEnvironment.PMCommonConfig;
import com.boot.jx.postman.doc.config.ChannelConfigDoc;
import com.boot.jx.postman.doc.config.ChannelConfigLogger;
import com.boot.jx.postman.manager.ConfigManager;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.scope.tnt.Tenants;
import com.boot.model.MapModel;
import com.boot.utils.ArgUtil;
import com.boot.utils.CollectionUtil;
import com.boot.utils.Constants;
import com.boot.utils.JsonPath;
import com.boot.utils.UniqueID;

@Controller
public class ChannelSetupController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ChannelSetupController.class);

	@Autowired
	private AppConfig appConfig;

	@Autowired(required = false)
	private AppCommonConfig appCommonConfig;

	@Autowired
	private CommonMongoTemplate commonMongoTemplate;

	@Autowired(required = false)
	private BootJxConfigService bootJxConfigService;

	@Autowired
	private ConnectorHandlerFactory connectorHandlerFactory;

	@Autowired
	private PMEnvironment pmEnvironment;

	@Autowired
	private CommonHttpRequest commonHttpRequest;

	@Autowired
	private ConfigManager configManager;

	@Autowired
	private PMCommonConfig pmCommonConfig;

	@Autowired(required = false)
	private AuthStateManager authStateManager;

	@ApiRequest(tenant = "app")
	@RequestMapping(value = "/ext/setup/channel", method = { RequestMethod.GET, RequestMethod.POST })
	public String setupChannel(@RequestParam(required = false) CHANNEL_TYPE_ENUM channelType,
			@RequestParam(required = false) String postKey, @RequestParam(required = false) ContactType contactType,
			@RequestParam(required = false) String masterChannelId, Model model)
			throws FileNotFoundException, IOException, URISyntaxException {

		String domainName = ArgUtil.nonEmpty(commonHttpRequest.get("domain"), commonHttpRequest.getRequestParam("tnt"),
				commonHttpRequest.getSubDomain());

		model.addAttribute("domain", domainName);
		model.addAttribute("PROP_SERVICE_SERVER", pmCommonConfig.getServiceServerByRequest());

		model.addAttribute("APP_NAME", appConfig.getAppName());
		model.addAttribute("APP_CONTEXT", appConfig.getAppPrefix());
		model.addAttribute("CDN_URL", appConfig.getAppPrefix());
		if (ArgUtil.is(appCommonConfig)) {
			model.addAllAttributes(appCommonConfig.appAttributes());
		}
		if (ArgUtil.is(bootJxConfigService)) {
			model.addAllAttributes(bootJxConfigService.bootJxAttributesModel().cdnApp("test").cdnEntry("app-dev")
					.preventUpgradeInsecureRequest().map());
		}
		model.addAttribute("APP_USER", "");
		model.addAttribute("APP_USER_NAME", "User");
		model.addAttribute("APP_USER_ROLE", "['GUEST']");

		if (!ArgUtil.is(postKey)) {
			if (Tenants.isDefault(domainName)) {
				model.addAttribute("FORM_URL", String.format("%s/ext/setup/channel", appConfig.getAppPrefix()));
			} else {
				model.addAttribute("FORM_URL", String.format("https://app.%s%s/ext/setup/channel",
						pmCommonConfig.getServiceServerByRequest(), appConfig.getAppPrefix()));
			}

			model.addAttribute("postKey", UniqueID.generateString62());
			model.addAttribute("channelType", channelType);
			model.addAttribute("contactType", contactType);
			model.addAttribute("masterChannelId", masterChannelId);
			return "app-setup-channel-post";
		}

		// System.out.println("tnt="+AppContextUtil.getTenant());
		// System.out.println("domainName="+domainName);
		// System.out.println("header:tnt="+commonHttpRequest.get("tnt"));
		// System.out.println("tnt="+AppContextUtil.getTenant());

		List<ChannelConfigDoc> channels = CollectionUtil.asList();
		MongoQueryBuilder<ChannelConfigDoc> q = MongoQueryBuilder.collection(ChannelConfigDoc.class).page(0, 25);
		if (ArgUtil.is(masterChannelId)) {
			q = q.whereIdSafe(masterChannelId);
		}
		q.where("isMaster", true);

		if (ArgUtil.is(channelType)) {
			q.search("channelType", ArgUtil.parseAsString(channelType));
		}
		if (ArgUtil.is(contactType)) {
			q.search("contactType", ArgUtil.parseAsString(contactType));
		}

		q.search("server", ArgUtil.parseAsString(pmCommonConfig.getServiceServer()));

		channels = commonMongoTemplate.find(q);

		model.addAttribute("channels", channels);

		boolean channelSelected = (channels.size() == 1);
		model.addAttribute("channelSelected", channelSelected);
		model.addAttribute("selectedChannelConfigId", Constants.BLANK);
		model.addAttribute("AUTH_URL", Constants.BLANK);
		if (channelSelected) {
			ChannelConfigDoc channel = channels.get(0);
			model.addAttribute("selectedChannel", channel);
			model.addAttribute("selectedChannelConfigId", channel.getId());
			ConnectorHandler connector = connectorHandlerFactory.get(channel.getContactType(),
					channel.getChannelType());
			AuthState state = authStateManager.createState();
			state.setDomain(domainName);
			model.addAttribute("AUTH_URL", connector.createAuthUrl(channel, null, state));
		}
		model.addAttribute("OPTIONS_URL", String.format("https://%s.%s%s/ext/setup/channel", domainName,
				pmCommonConfig.getServiceServerByRequest(), appConfig.getAppPrefix()));

		return "app-setup-channel";

	}

	@ApiRequest(tenant = "app")
	@RequestMapping(value = "/ext/setup/channel/callback/fb", method = { RequestMethod.GET, RequestMethod.POST })
	public String setupChannelCallback(@RequestParam(required = false) String code, Model model)
			throws FileNotFoundException, IOException, URISyntaxException {
		model.addAttribute("response", MapModel.createInstance().put(JsonPath.at("authResponse.code"), code).toJson());
		return this.setupChannel(CHANNEL_TYPE_ENUM.fb, UniqueID.generateString62(), ContactType.FACEBOOK,
				Constants.BLANK, model);
	}

	@ApiRequest(tenant = "app")
	@RequestMapping(value = "/ext/setup/channel/callback/ig", method = { RequestMethod.GET, RequestMethod.POST })
	public String setupChannelCallbackIg(@RequestParam(required = false) String code, Model model)
			throws FileNotFoundException, IOException, URISyntaxException {
		model.addAttribute("response", MapModel.createInstance().put(JsonPath.at("authResponse.code"), code).toJson());
		return this.setupChannel(CHANNEL_TYPE_ENUM.ig, UniqueID.generateString62(), ContactType.INSTAGRAM,
				Constants.BLANK, model);
	}

	@ApiRequest(tenant = "app")
	@RequestMapping(value = "/ext/setup/channel/callback/outlook", method = { RequestMethod.GET, RequestMethod.POST })
	public String setupChannelCallbackOutlook(@RequestParam(required = false) String code, Model model,
			@RequestParam(required = false) String state)
			throws FileNotFoundException, IOException, URISyntaxException {
		AuthState authState = authStateManager.fromState(state);
		if (ArgUtil.is(authState) && ArgUtil.is(authState.getDomain())) {
			AppContextUtil.set("domain", authState.getDomain());
		}
		model.addAttribute("response", MapModel.createInstance().put(JsonPath.at("authResponse.code"), code).toJson());
		return this.setupChannel(CHANNEL_TYPE_ENUM.outlook, UniqueID.generateString62(), ContactType.EMAIL,
				Constants.BLANK, model);
	}

	@ApiRequest(tenant = "app")
	@RequestMapping(value = "/ext/setup/channel/callback/gmail", method = { RequestMethod.GET, RequestMethod.POST })
	public String setupChannelCallbackGmail(@RequestParam(required = false) String code, Model model,
			@RequestParam(required = false) String state, @RequestParam(required = false) String credential,
			@RequestParam(required = false) String scope)
			throws FileNotFoundException, IOException, URISyntaxException {
		AuthState authState = authStateManager.fromState(state);
		if (ArgUtil.is(authState) && ArgUtil.is(authState.getDomain())) {
			AppContextUtil.set("domain", authState.getDomain());
		}
		model.addAttribute("response", MapModel.createInstance().put(JsonPath.at("authResponse.code"), code)
				.put(JsonPath.at("authResponse.credential"), credential).put(JsonPath.at("authResponse.scope"), scope)
				.put(JsonPath.at("authResponse.state"), state).toJson());
		return this.setupChannel(CHANNEL_TYPE_ENUM.gmail, UniqueID.generateString62(), ContactType.EMAIL,
				Constants.BLANK, model);
	}

	@ResponseBody
	@ApiRequest(tenant = "app")
	@RequestMapping(value = "/ext/setup/channel/resp", method = { RequestMethod.POST })
	public ApiResponse<Object, MapModel> setupChannelSave(@RequestParam String masterChannelId,
			@RequestBody Map<String, Object> response) throws FileNotFoundException, IOException {
		String domainName = ArgUtil.nonEmpty(commonHttpRequest.get("domain"), commonHttpRequest.getRequestParam("tnt"),
				commonHttpRequest.getSubDomain());
		MapModel returnVal = MapModel.createInstance();
		ChannelConfig master = pmEnvironment.local().channel(masterChannelId);
		// ChannelConfigSetupDoc setup = commonMongoTemplate.findById(channelConfigId,
		// ChannelConfigSetupDoc.class);
		if (ArgUtil.is(master)) {
			ChannelConfigLogger respDoc = new ChannelConfigLogger();
			respDoc.setChannelConfigId(masterChannelId);
			respDoc.setResp(response);
			respDoc.setChannelType(master.getChannelType());
			respDoc.setDomain(domainName);
			commonMongoTemplate.save(respDoc);
			returnVal.put("id", respDoc.getId());
			ConnectorHandler connector = connectorHandlerFactory.get(master.getContactType(), master.getChannelType());
			if (ArgUtil.is(connector)) {
				List<ChannelConfig> channels = connector.onRegister(master, respDoc, authStateManager.getState());
				if (ArgUtil.is(channels)) {
					for (ChannelConfig channel : channels) {
						channel.setContactType(master.getContactType());
						channel.setChannelType(master.getChannelType());
						channel.setMasterChannelId(masterChannelId);
						channel.setChannelConfigTempId(respDoc.getId());
						configManager.saveForDomain(channel, domainName);
						returnVal.put(channel.getChannelId(), domainName);
					}
				}

			}
		}
		return ApiResponse.buildMeta(returnVal).redirectUrl(String.format("https://%s.%s/admin/app/setup/channels",
				domainName, pmCommonConfig.getServiceServerByRequest(), appConfig.getAppPrefix()));
	}

}
