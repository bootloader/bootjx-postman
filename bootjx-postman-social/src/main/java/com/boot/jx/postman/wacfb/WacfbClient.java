package com.boot.jx.postman.wacfb;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.boot.jx.api.ApiFieldError;
import com.boot.jx.api.ApiResponseUtil;
import com.boot.jx.dict.ContactType;
import com.boot.jx.dict.FileFormat;
import com.boot.jx.dict.FileType;
import com.boot.jx.exception.ApiHttpExceptions.ApiHttpException;
import com.boot.jx.exception.ApiHttpExceptions.ApiHttpServerException;
import com.boot.jx.exception.ApiHttpExceptions.ApiStatusCodes;
import com.boot.jx.postman.PMConstants;
import com.boot.jx.postman.PMConstants.CHANNEL_TYPE;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.PostManException;
import com.boot.jx.postman.PostmanPackages.MockMessenger;
import com.boot.jx.postman.PostmanPackages.MessageFailureGuard;
import com.boot.jx.postman.channel.ChannelClientFactory.ChannelClient;
import com.boot.jx.postman.model.Attachment;
import com.boot.jx.postman.model.MessagePrompt;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.model.TmplElement;
import com.boot.jx.postman.pbook.PBAddress;
import com.boot.jx.postman.pbook.PBDate;
import com.boot.jx.postman.pbook.PBEmail;
import com.boot.jx.postman.pbook.PBLocation;
import com.boot.jx.postman.pbook.PBPhone;
import com.boot.jx.postman.pbook.PBVCard;
import com.boot.jx.postman.pbook.PBWebsite;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.plugin.ChannelPluginProvider.ConnectorMapping;
import com.boot.jx.postman.plugin.WacfbPlugin.WACFBConfigDetails;
import com.boot.jx.postman.wa360.WA360CloudOutBoundMedia;
import com.boot.jx.postman.wa360.WA360Constants;
import com.boot.jx.postman.wa360.WA360Constants.OutBoundWrapperPaths;
import com.boot.jx.postman.wa360.WA360Constants.TmplComponent;
import com.boot.jx.postman.wa360.WA360OutBoundMedia;
import com.boot.jx.postman.wa360.WA360Template;
import com.boot.jx.rest.RestObjectAbstract.RestMapModel;
import com.boot.jx.rest.RestService;
import com.boot.model.MapModel;
import com.boot.model.MapModel.MapPathEntry;
import com.boot.utils.ArgUtil;
import com.boot.utils.CollectionUtil;
import com.boot.utils.Constants;
import com.boot.utils.CryptoUtil;
import com.boot.utils.JsonPath;
import com.boot.utils.JsonUtil;
import com.boot.utils.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@ConnectorMapping(contactType = ContactType.WHATSAPP, channel = CHANNEL_TYPE.WACFB)
public class WacfbClient implements ChannelClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(WacfbClient.class);

	public static final Pattern VARIABLES = Pattern.compile("\\{\\{([a-zA-Z0-9_]+)\\}\\}");

	@Autowired
	private RestService restService;

	@Autowired(required = false)
	private MockMessenger mockMessenger;
	
	@Autowired(required = false)
	private MessageFailureGuard messageFailureGuard;

	@Autowired
	private PMEnvironment pmEnvironment;

	public String registerWebhook(ChannelConfig channelConfig, String token, String challenge) {
		WACFBConfigDetails config = channelConfig.getWacfb();
		String verifyToken = ArgUtil.nonEmpty(config.getMasterAppVerifyToken(), config.getVerifyToken());
		if (token != null && !token.isEmpty() && token.equals(verifyToken)) {
			return challenge;
		} else {
			return "Wrong Token";
		}
	}

	@Retryable(value = ApiHttpServerException.class, maxAttempts = 3, backoff = @Backoff(delay = 3000))
	public OutboxMessage send(ChannelConfig channelConfig, OutboxMessage outboxMessage) {
		
		if (messageFailureGuard != null) {
			MapModel enforceBody = messageFailureGuard.enforce(outboxMessage);
			if (enforceBody != null) {
				RestMapModel rest = new RestMapModel();
				rest.response(enforceBody);
				getMessageId(rest, outboxMessage);
			}
		}
		
		StringJoiner msgIds = new StringJoiner(",");
		if (ArgUtil.is(outboxMessage.getRawMessageFormat())) { /** for Moengage **/
			RestMapModel resp = sendTemplateRaw(channelConfig, outboxMessage);
			msgIds.add(getMessageId(resp, outboxMessage));
		} else if (ArgUtil.is(outboxMessage.getTemplateExt())) {
			RestMapModel resp = sendTemplate(channelConfig, outboxMessage);
			msgIds.add(getMessageId(resp, outboxMessage));
		} else {
			boolean isList = false;
			boolean isButton = false;
			boolean isCtaUrl = false;
			boolean isLocationRequest = false;
			boolean isAddressRequest = false;
			boolean isFlow = false;
			int buttonsCount = 0;
			int urlCount = 0;
			String bodyUrlAppend = Constants.BLANK;
			String bodyPhoneAppend = Constants.BLANK;

			List<TmplElement> buttons = new ArrayList<TmplElement>();
			List<TmplElement> noButtons = new ArrayList<TmplElement>();
			MapModel options = MapModel.from(outboxMessage.options());

			checkFlowButton(options);

			if (options.containsKey("buttons")) {

				List<TmplElement> allbuttons = options.entry("buttons").asList(TmplElement.class);// null

				for (TmplElement b : allbuttons) {

					if (ArgUtil.areEqual(b.getType(), TmplElement.TYPES.URL)
							|| ArgUtil.areEqual(b.getType(), TmplElement.TYPES.COPY)) {
						if (ArgUtil.areEqual(b.getType(), TmplElement.TYPES.COPY)) {
							b.setUrl("https://www.whatsapp.com/otp/code/?otp_type=COPY_CODE&code=otp" + b.getCode());
						}
						bodyUrlAppend = bodyUrlAppend
								+ StringUtils.wrap("\n" + WA360Constants.componentButtonSubTypesIconLink + " *",
										StringUtils.trim(b.getLabel()), "*")
								+ "\n" + ArgUtil.anyOf(b.getShorturl(), b.getUrl()) + "\n"
								+ StringUtils.wrap(" _", b.getDesc(), "_\n");
						urlCount++;
						noButtons.add(b);
					} else if (ArgUtil.areEqual(b.getType(), TmplElement.TYPES.PHONE_NUMBER)) {
						bodyPhoneAppend = bodyPhoneAppend
								+ StringUtils.wrap("\n" + WA360Constants.componentButtonSubTypesIconPhone + " *",
										StringUtils.trim(b.getLabel()), "*")
								+ "\n" + b.getPhone() + "\n" + StringUtils.wrap(" _", b.getDesc(), "_\n");
					} else if (ArgUtil.areEqual(b.getType(), TmplElement.TYPES.LOCATION_REQUEST)) {
						isLocationRequest = true;
						noButtons.add(b);
					} else if (ArgUtil.areEqual(b.getType(), TmplElement.TYPES.ADDRESS_REQUEST)) {
						isAddressRequest = true;
						noButtons.add(b);
					} else if (ArgUtil.areEqual(b.getType(), TmplElement.TYPES.FLOW)) {
						isFlow = true;
						noButtons.add(b);
					} else {
						buttonsCount++;
						buttons.add(b);
					}
				}
				isList = (buttonsCount > 0) && (buttonsCount > 3);
				isButton = (buttonsCount > 0) && (buttonsCount < 4);
			}

			isList = options.entry("is_list").asBoolean(isList);
			isCtaUrl = !isList && !isButton && (urlCount == 1);

			if (ArgUtil.is(bodyPhoneAppend)) {
				outboxMessage.setMessage(outboxMessage.getMessage() + "\n" + bodyPhoneAppend);
			}

			if (!isCtaUrl && ArgUtil.is(bodyUrlAppend)) {
				outboxMessage.setMessage(outboxMessage.getMessage() + "\n" + bodyUrlAppend);
			}

			if (isList) {
				if (buttons.size() <= 10) {
					checkAndSendMedia(channelConfig, new OutboxMessage().contact(outboxMessage.contact())
							.attachment(outboxMessage.attachments()), msgIds, Constants.BLANK);
					RestMapModel resp = sendList(channelConfig, outboxMessage, buttons);
					msgIds.add(getMessageId(resp, outboxMessage));
				} else {
					MessagePrompt prompt = new MessagePrompt();
					if (ArgUtil.is(outboxMessage.getPrompt())
							&& MessagePrompt.TYPE.MOREOPTIONS.equals(outboxMessage.getPrompt().type)) {
						prompt = outboxMessage.getPrompt();
						prompt.pageIndex++;
					}
					prompt.type = MessagePrompt.TYPE.MOREOPTIONS;
					prompt.messageId = outboxMessage.getMessageId();

					int start = prompt.pageIndex * 9;
					int pending = buttons.size() - start;
					int end = Math.min((start + 9), buttons.size());
					List<TmplElement> newButtons;

					if (pending < 10) {
						newButtons = buttons.subList(start, end);
					} else if (pending == 10) {
						newButtons = buttons.subList(start, end + 1);
					} else {
						newButtons = buttons.subList(start, end);

						if (options.containsKey("more_option_title")) {
							newButtons.add(new TmplElement().label(options.getString("more_option_title"))
									.code(prompt.toString()));
						} else {
							newButtons.add(new TmplElement().label("More Options").code(prompt.toString()));
						}
					}
					if (options.containsKey("list_option_title")) {
						options.put("list_option_title",
								options.getString("list_option_title") + (prompt.pageIndex + 1));
					} else {
						options.put("list_option_title", "List " + (prompt.pageIndex + 1));
					}

					if (prompt.pageIndex == 0) {
						checkAndSendMedia(channelConfig, new OutboxMessage().contact(outboxMessage.contact())
								.attachment(outboxMessage.attachments()), msgIds, Constants.BLANK);
					}
					RestMapModel resp = sendList(channelConfig, outboxMessage, newButtons);
					msgIds.add(getMessageId(resp, outboxMessage));
				}
			} else if (isButton) {
				RestMapModel resp = sendButton(channelConfig, outboxMessage, buttons, "button");
				msgIds.add(getMessageId(resp, outboxMessage));
			} else if (isCtaUrl) {
				RestMapModel resp = sendButton(channelConfig, outboxMessage, noButtons, "cta_url");
				msgIds.add(getMessageId(resp, outboxMessage));
			} else if (isLocationRequest) {
				RestMapModel resp = sendButton(channelConfig, outboxMessage, noButtons, "location_request_message");
				msgIds.add(getMessageId(resp, outboxMessage));
			} else if (isAddressRequest) {
				RestMapModel resp = sendButton(channelConfig, outboxMessage, noButtons, "address_message");
				msgIds.add(getMessageId(resp, outboxMessage));
			} else if (isFlow) {
				RestMapModel resp = sendButton(channelConfig, outboxMessage, noButtons, "flow");
				msgIds.add(getMessageId(resp, outboxMessage));
			} else {
				String textMessage = outboxMessage.getMessage();
				textMessage = checkAndSendMedia(channelConfig, outboxMessage, msgIds, textMessage);

				if (ArgUtil.is(textMessage)) {
					RestMapModel resp = sendText(channelConfig, outboxMessage);
					msgIds.add(getMessageId(resp, outboxMessage));
				}
			}
		}

		outboxMessage.setMessageIdExt(msgIds.toString());
		outboxMessage.setHsm(outboxMessage.getHsm());
		return outboxMessage;
	}

	private String checkAndSendMedia(ChannelConfig channelConfig, OutboxMessage outboxMessage, StringJoiner msgIds,
			String textMessage) {
		if (ArgUtil.is(outboxMessage.getAttachments())) {
			for (Attachment attachment : outboxMessage.getAttachments()) {
				if (ArgUtil.is(textMessage) && ArgUtil.isEqual(attachment.getMediaType(), FileType.IMAGE.toString(),
						FileType.VIDEO.toString(), FileType.DOCUMENT.toString())) {
					attachment.setMediaCaption(textMessage);
					textMessage = null;
				}
				RestMapModel resp = sendMedia(channelConfig, outboxMessage, attachment);
				msgIds.add(getMessageId(resp, outboxMessage));
			}
		} else if (ArgUtil.is(outboxMessage.getVccards()) && outboxMessage.getVccards().size() > 0) {
			MapModel locations = MapModel.createInstance();
			MapModel contacts = MapModel.createInstance();

			for (PBVCard card : outboxMessage.getVccards()) {
				if (ArgUtil.is(card.getLocations())) {
					for (PBLocation location : card.getLocations()) {
						locations.put("longitude", location.getLongitude());
						locations.put("latitude", location.getLatitude());
						locations.put("name", location.getName());
						locations.put("address", location.getAddress());
						locations.map2list();
					}
				}

				if (ArgUtil.is(card.getName())) {
					contacts.put(WA360Constants.OutBoundWrapperPaths.NAME_FIRST_NAME, card.getName().getFirstName());
					contacts.put(WA360Constants.OutBoundWrapperPaths.NAME_LAST_NAME, card.getName().getLastName());
					contacts.put(WA360Constants.OutBoundWrapperPaths.NAME_FORMATTED_NAME,
							card.getName().getFormattedName());
				}

				if (ArgUtil.is(card.getWork()) && card.getWork().size() > 0) {
					contacts.put(WA360Constants.OutBoundWrapperPaths.ORG_COMPANY, card.getWork().get(0).getCompany());
					contacts.put(WA360Constants.OutBoundWrapperPaths.ORG_DEPARTMENT,
							card.getWork().get(0).getDepartment());
					contacts.put(WA360Constants.OutBoundWrapperPaths.ORG_TITLE, card.getWork().get(0).getTitle());
				}

				if (ArgUtil.is(card.getDates()) && card.getDates().size() > 0) {
					for (PBDate date : card.getDates()) {
						if ("birthday".equalsIgnoreCase(date.getType())) {
							contacts.put("birthday", date.getDate());
						}
					}
				}

				if (ArgUtil.is(card.getEmails())) {
					MapModel emails = MapModel.createInstance();
					for (PBEmail email : card.getEmails()) {
						emails.put("type", email.getType()).put("email", email.getEmail()).map2list();
					}
					contacts.put("emails", emails.list());
				}

				if (ArgUtil.is(card.getPhones())) {
					MapModel phones = MapModel.createInstance();
					for (PBPhone phone : card.getPhones()) {
						phones.put("type", phone.getType()).put("phone", phone.getPhone()).map2list();
					}
					contacts.put("phones", phones.list());
				}

				if (ArgUtil.is(card.getUrls())) {
					MapModel urls = MapModel.createInstance();
					for (PBWebsite url : card.getUrls()) {
						urls.put("type", url.getType()).put("url", url.getUrl()).map2list();
					}
					contacts.put("urls", urls.list());
				}

				if (ArgUtil.is(card.getAddresses())) {
					MapModel addresses = MapModel.createInstance();
					for (PBAddress address : card.getAddresses()) {
						addresses.put("type", address.getType()).put("city", address.getCity())
								.put("country", address.getCountry()).put("country_code", address.getCountryCode())
								.put("state", address.getState()).put("street", address.getStreet())
								.put("zip", address.getZip()).map2list();
					}
					contacts.put("addresses", addresses.list());
				}
				if (contacts.size() > 0)
					contacts.map2list();
			}

			if (locations.size() > 0) {
				for (Object location : locations.list()) {
					MapModel req = MapModel.createInstance()
							.put("messaging_product", outboxMessage.getContact().getContactType())
							.put("recipient_type", "individual").put("to", outboxMessage.contact().getCsid());
					req.put(OutBoundWrapperPaths.MESSAGE_TYPE, "location");
					req.put("location", location);
					textMessage = null;
					/** Contextual replies **/
					if (ArgUtil.is(outboxMessage.getMessageIdRef())) {
						MapModel context = MapModel.createInstance().put("message_id", outboxMessage.getMessageIdRef());
						req.put(OutBoundWrapperPaths.MESSAGE_CONTEXT, context.toMap());
					}
					RestMapModel resp = send(req, channelConfig, outboxMessage);
					msgIds.add(getMessageId(resp, outboxMessage));
				}
			}

			if (contacts.size() > 0) {
				MapModel req = MapModel.createInstance()
						.put("messaging_product", outboxMessage.getContact().getContactType())
						.put("recipient_type", "individual").put("to", outboxMessage.contact().getCsid());
				req.put(OutBoundWrapperPaths.MESSAGE_TYPE, "contacts");
				req.put("contacts", contacts.list());
				textMessage = null;
				RestMapModel resp = send(req, channelConfig, outboxMessage);
				msgIds.add(getMessageId(resp, outboxMessage));
			}
		}
		return textMessage;
	}

	private RestMapModel sendTemplate(ChannelConfig channelConfig, OutboxMessage outboxMessage) {
		MapModel req = MapModel.createInstance().put("messaging_product", outboxMessage.getContact().getContactType())
				.put("recipient_type", "individual").put("to", outboxMessage.contact().getCsid());

		// Adds bulkSessionId if available
		if (outboxMessage.getBulkSessionId() != null) {
			req.put("bulkSessionId", outboxMessage.getBulkSessionId());
		}

		// Add template category from templateExt (WABA template)
		String templateCategory = null;
		if (outboxMessage.getTemplateExt() != null && outboxMessage.getTemplateExt().getTemplate() != null) {
			templateCategory = ArgUtil.parseAsString(outboxMessage.getTemplateExt().getTemplate().get("category"));
		}

		if (ArgUtil.is(templateCategory)) {
			req.put("categoryType", templateCategory);
			LOGGER.debug("Added template category from templateExt: {}", templateCategory);
		}

		MapModel extTemplate = MapModel.from(outboxMessage.getTemplateExt().getTemplate());
		MapModel model = MapModel.from(outboxMessage.getModel());
		MapModel varMap = MapModel.from(outboxMessage.getTemplateExt().getVarMap());

		req.put(OutBoundWrapperPaths.MESSAGE_TYPE, "template");
		req.put(OutBoundWrapperPaths.TEMPLATE_NAMESPACE, extTemplate.get("namespace"));
		req.put(OutBoundWrapperPaths.TEMPLATE_NAME, extTemplate.get("name"));
		req.put(OutBoundWrapperPaths.TEMPLATE_LANGUAGE_CODE, extTemplate.get("language"));
		req.put(OutBoundWrapperPaths.TEMPLATE_LANGUAGE_POLICY, "deterministic");

		MapModel components = MapModel.createInstance();
		List<Map<String, Object>> extTemplateComponents = extTemplate.keyEntry("components").asListOfMap();

		for (Map<String, Object> extTemplateComponent : extTemplateComponents) {
			String extTemplateComponentType = (String) extTemplateComponent.get("type");
			if ("HEADER".equals(extTemplateComponentType)) {
				TmplComponent headerComponentReq = TmplComponent.createInstance().header();
				String extTemplateComponentFormat = (String) extTemplateComponent.get("format");
				if ("TEXT".equals(extTemplateComponentFormat)) {
					if (varMap.containsKey("header")) {
						List<Map<String, Object>> headerParametersTemp = varMap.entry("header").asListOfMap();
						for (Map<String, Object> headerParameter : headerParametersTemp) {
							String path = (String) headerParameter.get("path");
							headerComponentReq.parameter("text", model.pathEntry(path).asString());
						}
						if (headerComponentReq.parameters().size() > 0) {
							components.add(headerComponentReq.build().map());
						}
					}
				} else if (ArgUtil.is(outboxMessage.getAttachments())) {
					String lowerFormat = extTemplateComponentFormat.toLowerCase();
					WA360CloudOutBoundMedia media = createMedia(lowerFormat, outboxMessage.getAttachments().get(0),
							channelConfig);
					media.setCaption(null);
					headerComponentReq.parameter(lowerFormat, media);
					if (headerComponentReq.parameters().size() > 0) {
						components.add(headerComponentReq.build().map());

					}
				}

			} else if ("BODY".equals(extTemplateComponentType)) {
				if (varMap.containsKey("body")) {
					List<Map<String, Object>> bodyParametersTemp = varMap.entry("body").asListOfMap();
					TmplComponent bodyComponent = TmplComponent.createInstance().body();
					for (Map<String, Object> bodyParameter : bodyParametersTemp) {
						String path = (String) bodyParameter.get("path");
						String modelKey = "data.";

						if (path.startsWith("data.")) {
							// Already starts with "data."
							modelKey = "data.";
						} else if (path.startsWith("profile.")) {
							modelKey = "profile.";
						}

						String[] pathParts = path.split("\\.");
						String variable = "";
						if (ArgUtil.is(pathParts) && pathParts.length > 1) {
							variable = String.join(".", Arrays.copyOfRange(pathParts, 1, pathParts.length));
						}

						if (modelKey.contains("data")) {
							bodyComponent.parameter("text", model.pathEntry(modelKey + variable).asString());
						} else if (modelKey.contains("profile")) {
							// String value =getProfileInfo(variable, modelKey, model);
							// bodyComponent.parameter("text",ArgUtil.parseAsString(value,
							// PMConstants.NOT_AVALIABE));
							String value = ArgUtil.parseAsString(model.pathEntry(modelKey + variable),
									PMConstants.NOT_AVALIABE);
							bodyComponent.parameter("text", value);
						}

					}
					components.add(bodyComponent.build().map());
				} else {
					String extTemplateComponentText = (String) extTemplateComponent.get("text");
					// Extract placeholders
					if (ArgUtil.is(extTemplateComponentText)) {
						List<String> placeholders = getVariablePlaceHolders(extTemplateComponentText);
						if (placeholders.size() > 0) {
							TmplComponent bodyComponent = TmplComponent.createInstance().body();
							for (String variable : placeholders) {
								bodyComponent.parameter("text", model.pathEntry("data." + variable).asString());
							}
							components.add(bodyComponent.build().map());
						}
					}

				}
			} else if ("BUTTONS".equals(extTemplateComponentType)) {
				List<Map<String, Object>> extTemplateComponentButtons = MapModel.from(extTemplateComponent)
						.keyEntry("buttons").asListOfMap();
				List<List<Map<String, Object>>> buttonsParametersVars = varMap.entry("buttons").asListListOfMap();

				// Map button vars by Meta type: URL by placeholder in url; never bind URL vars to QR
				// (QR payload uses options.buttons fallback in emit)
				Map<Map<String, Object>, List<Map<String, Object>>> buttonVarMap = new HashMap<>();
				Set<String> usedVariables = new HashSet<>();
				Set<Map<String, Object>> mappedButtons = new HashSet<>();

				for (List<Map<String, Object>> varList : buttonsParametersVars) {
					if (!ArgUtil.is(varList)) {
						continue;
					}

					// 1) Bind URL first (Meta URL button expects text suffix for {{n}})
					boolean mappedToUrl = false;
					for (Map<String, Object> button : extTemplateComponentButtons) {
						if (mappedButtons.contains(button) || !"URL".equals(button.get("type"))
								|| !ArgUtil.is(button.get("url"))) {
							continue;
						}
						for (Map<String, Object> var : varList) {
							String numVar = ArgUtil.parseAsString(var.get("numVar"));
							String variable = ArgUtil.parseAsString(var.get("variable"));
							String varKey = numVar + ":" + variable;
							if (ArgUtil.is(numVar) && ArgUtil.is(variable) && !usedVariables.contains(varKey)
									&& button.get("url").toString().contains(numVar)) {
								buttonVarMap.put(button, varList);
								usedVariables.add(varKey);
								mappedButtons.add(button);
								mappedToUrl = true;
								break;
							}
						}
						if (mappedToUrl) {
							break;
						}
					}

					// URL placeholder vars must not go to QUICK_REPLY/FLOW
					if (mappedToUrl || hasUrlVar(varList, extTemplateComponentButtons)) {
						continue;
					}

					// 2) Leftover vars -> FLOW
					for (Map<String, Object> button : extTemplateComponentButtons) {
						if (mappedButtons.contains(button) || !"FLOW".equals(button.get("type"))) {
							continue;
						}
						for (Map<String, Object> var : varList) {
							String numVar = ArgUtil.parseAsString(var.get("numVar"));
							String variable = ArgUtil.parseAsString(var.get("variable"));
							String varKey = numVar + ":" + variable;
							if (ArgUtil.is(numVar) && ArgUtil.is(variable) && !usedVariables.contains(varKey)) {
								buttonVarMap.put(button, varList);
								usedVariables.add(varKey);
								mappedButtons.add(button);
								break;
							}
						}
					}
				}

				for (int i = 0; i < extTemplateComponentButtons.size(); i++) {
					Map<String, Object> extTemplateComponentButton = extTemplateComponentButtons.get(i);
					String buttonType = (String) extTemplateComponentButton.get("type");
					List<Map<String, Object>> buttonParameterVar = buttonVarMap.get(extTemplateComponentButton);
//					List<Map<String, Object>> buttonParameterVar = CollectionUtil.getArray(buttonsParametersVars, i);)
					if (ArgUtil.is(buttonParameterVar)) {
//						String buttonType = (String) extTemplateComponentButton.get("type");
						if ("URL".equals(buttonType)) {
							String extTemplateComponentUrl = (String) extTemplateComponentButton.get("url");

							// Check if this is a tracking URL that needs special handling
							if (isTrackingUrl(extTemplateComponentUrl)) {
								// Handle tracking URL with msgHash generation
								List<TmplElement> buttons = outboxMessage.optionActionButtons();
								TmplElement button = CollectionUtil.getArray(buttons, i);

								if (ArgUtil.is(button)) {
									String msgHash = generateTrackingMsgHash(button.getUrl(),
											outboxMessage.getMessageId());
									LOGGER.debug("Processing tracking URL: {}, generated msgHash: {}",
											extTemplateComponentUrl, msgHash);

									TmplComponent buttonComponent = TmplComponent.createInstance().button("url", i);
									buttonComponent.parameter("text", msgHash);
									components.add(buttonComponent.build().map());
								} else {
									LOGGER.warn("Button not found for tracking URL processing at index: {}", i);
								}
							} else {
								// Handle regular URL with variable substitution (existing logic)
								for (Map<String, Object> buttonParameter : buttonParameterVar) {
									if (buttonParameter.containsKey("path")) {
										String path = (String) buttonParameter.get("path");
										TmplComponent buttonComponent = TmplComponent.createInstance().button("url", i);
										buttonComponent.parameter("text", model.pathEntry(path).asString());
										components.add(buttonComponent.build().map());
									}
								}
							}
						} else if ("QUICK_REPLY".equals(buttonType)) {
							for (Map<String, Object> buttonParameter : buttonParameterVar) {
								if (buttonParameter.containsKey("path")) {
									String path = (String) buttonParameter.get("path");
									String code = model.pathEntry(path).asString();
									if (validateButton(code, path)) {
										TmplComponent buttonComponent = TmplComponent.createInstance()
												.button("quick_reply", i);
										buttonComponent.parameter("payload", "reply_id:" + code);
										components.add(buttonComponent.build().map());
									}
								}
							}
						} else if ("FLOW".equals(buttonType)) {
							for (Map<String, Object> buttonParameter : buttonParameterVar) {
								if (buttonParameter.containsKey("path")) {
									String path = (String) buttonParameter.get("path");
									TmplComponent buttonComponent = TmplComponent.createInstance().button("flow", i);
									buttonComponent.parameter("action", MapModel.createInstance());
									components.add(buttonComponent.build().map());
								}
							}
						}
					} else {
//						String buttonType = (String) extTemplateComponentButton.get("type");
						List<TmplElement> buttons = outboxMessage.optionActionButtons();
						TmplElement button = CollectionUtil.getArray(buttons, i);
						if (ArgUtil.is(button) && "QUICK_REPLY".equals(button.getType())
								&& "QUICK_REPLY".equals(buttonType)) {
							TmplComponent buttonComponent = TmplComponent.createInstance().button("quick_reply", i);
							if (validateButton(button.getCode(), button.getVariable())) {
								buttonComponent.parameter("payload", "reply_id:" + button.getCode());
								components.add(buttonComponent.build().map());
							}
						} else if ("URL".equalsIgnoreCase(buttonType)) {
							String extTemplateComponentUrl = (String) extTemplateComponentButton.get("url");

							// Process the extTemplate URL directly (original flow)

							if (ArgUtil.is(extTemplateComponentUrl)) {
								// Check if this is a tracking URL that needs special handling
								if (isTrackingUrl(extTemplateComponentUrl)) {
									// Handle tracking URL with msgHash generation
									String originalUrl = button.getUrl();
									String msgHash = generateTrackingMsgHash(originalUrl, outboxMessage.getMessageId());
									LOGGER.debug("Processing tracking URL in else block: {}, generated msgHash: {}",
											extTemplateComponentUrl, msgHash);

									TmplComponent buttonComponent = TmplComponent.createInstance().button("url", i);
									buttonComponent.parameter("text", msgHash);
									components.add(buttonComponent.build().map());
								} else {
									// Handle URLs with {{variable}} placeholders (non-tracking URLs)
									List<String> placeholders = getVariablePlaceHolders(extTemplateComponentUrl);
									if (placeholders.size() > 0) {
										TmplComponent buttonComponent = TmplComponent.createInstance().button("url", i);
										for (String variable : placeholders) {
											// Regular variable placeholder
											buttonComponent.parameter("text",
													model.pathEntry("data." + variable).asString());
										}
										components.add(buttonComponent.build().map());
									}
								}
							}
						} else if ("FLOW".equals(buttonType)) {
							if (ArgUtil.is(buttonParameterVar)) {
								for (Map<String, Object> buttonParameter : buttonParameterVar) {
									if (buttonParameter.containsKey("path")) {

										String path = (String) buttonParameter.get("path");

										TmplComponent buttonComponent = TmplComponent.createInstance().button("flow",
												i);
										buttonComponent.parameter("action", MapModel.createInstance().put("flow_token",
												outboxMessage.getMessageId()));
										components.add(buttonComponent.build().map());
									}
								}
							} else {
								Object flowIdObj = extTemplateComponentButton.get("flow_id");
								String flow_id = flowIdObj != null ? String.valueOf(flowIdObj) : null;
								TmplComponent buttonComponent = TmplComponent.createInstance().button("flow", i);
								buttonComponent.parameter("action", MapModel.createInstance().put("flow_token",
										outboxMessage.getMessageId() + "/" + flow_id));
								components.add(buttonComponent.build().map());
							}
						}
					}
				}

			}
		}

		req.put(OutBoundWrapperPaths.TEMPLATE_COMPONENTS, components.list());

		/** Contextual replies **/
		if (ArgUtil.is(outboxMessage.getMessageIdRef())) {
			MapModel context = MapModel.createInstance().put("message_id", outboxMessage.getMessageIdRef());
			req.put(OutBoundWrapperPaths.MESSAGE_CONTEXT, context.toMap());
		}
		return send(req, channelConfig, outboxMessage);
	}

	private List<String> getVariablePlaceHolders(String extTemplateComponentText) {
		Matcher matcher = VARIABLES.matcher(extTemplateComponentText);
		List<String> placeholders = new ArrayList<>();
		while (matcher.find()) {
			placeholders.add(matcher.group(1)); // Group 1 contains the placeholder value
		}
		return placeholders;
	}

	/**
	 * Generates msgHash for tracking URLs
	 * 
	 * @param originalUrl The original URL to hash
	 * @param messageId   The message ID
	 * @return Generated msgHash or "tracking_error" if generation fails
	 */
	private String generateTrackingMsgHash(String originalUrl, String messageId) {
		if (!ArgUtil.is(originalUrl)) {
			return "tracking_error";
		}

		try {
			String urlHash = CryptoUtil.getMD5Hash(originalUrl);
			return messageId + "_" + urlHash;
		} catch (NoSuchAlgorithmException e) {
			LOGGER.warn("Failed to generate tracking msgHash for URL: {}", originalUrl, e);
			return "tracking_error";
		}
	}

	/**
	 * Detects if a URL is a tracking URL that requires msgHash generation
	 * 
	 * @param url The URL to check
	 * @return true if this is a tracking URL, false otherwise
	 */
	private boolean isTrackingUrl(String url) {
		return ArgUtil.is(url) && url.contains("/link/short/template/") && url.contains("msgHash={{1}}");
	}
	
	/** True if var.numVar is a placeholder in any WABA URL button. */
	private boolean isUrlVar(Map<String, Object> var, List<Map<String, Object>> wabaButtons) {
		String numVar = ArgUtil.parseAsString(var == null ? null : var.get("numVar"));
		if (!ArgUtil.is(numVar) || !ArgUtil.is(wabaButtons)) {
			return false;
		}
		for (Map<String, Object> button : wabaButtons) {
			if ("URL".equals(button.get("type")) && ArgUtil.is(button.get("url"))
					&& button.get("url").toString().contains(numVar)) {
				return true;
			}
		}
		return false;
	}

	/** True if any var in the list is a URL button placeholder. */
	private boolean hasUrlVar(List<Map<String, Object>> varList, List<Map<String, Object>> wabaButtons) {
		for (Map<String, Object> var : varList) {
			if (isUrlVar(var, wabaButtons)) {
				return true;
			}
		}
		return false;
	}

	private MapModel checkFlowButton(MapModel options) {
		MapPathEntry extTemplateComponents = options.pathEntry("waba/components");
		if (extTemplateComponents.exists()) {
			List<TmplElement> allbuttons = options.entry("buttons").asList(TmplElement.class);
			for (Map<String, Object> extTemplateComponent : extTemplateComponents.asListOfMap()) {
				String extTemplateComponentType = (String) extTemplateComponent.get("type");
				if ("BUTTONS".equals(extTemplateComponentType)) {
					List<Map<String, Object>> extTemplateComponentButtons = MapModel.from(extTemplateComponent)
							.keyEntry("buttons").asListOfMap();
					for (Map<String, Object> extTemplateComponentButton : extTemplateComponentButtons) {
						String buttonType = (String) extTemplateComponentButton.get("type");
						if ("FLOW".equals(buttonType)) {
							TmplElement flowButton = new TmplElement();
							flowButton.setType(TmplElement.TYPES.FLOW);
							flowButton.setUid(ArgUtil.parseAsString(extTemplateComponentButton.get("flow_id")));
							flowButton.setAction((String) extTemplateComponentButton.get("flow_action"));
							flowButton.setCode((String) extTemplateComponentButton.get("navigate_screen"));
							flowButton.setLabel((String) extTemplateComponentButton.get("text"));
							allbuttons.add(flowButton);
						}
					}

				}
			}
			options.put("buttons", allbuttons);
		}
		return options;
	}

	private WA360CloudOutBoundMedia createMedia(String mediaType, Attachment attachment, ChannelConfig channelConfig) {
		WA360CloudOutBoundMedia wa360OutBoundMedia = new WA360CloudOutBoundMedia();
		attachMetaMedia(wa360OutBoundMedia, attachment, channelConfig);
		wa360OutBoundMedia.setFilename(ArgUtil.nonEmpty(attachment.getMediaCaption(), attachment.getMediaName()));
		wa360OutBoundMedia.setCaption(attachment.getMediaName());
		//wa360OutBoundMedia.setLink(attachment.getMediaURL());

		if (mediaType.equalsIgnoreCase("image") || mediaType.equalsIgnoreCase("video")) {

			wa360OutBoundMedia.setFilename(null);
		}
		return wa360OutBoundMedia;
	}

	private RestMapModel sendText(ChannelConfig channelConfig, OutboxMessage outboxMessage) {
		MapModel req = MapModel.createInstance().put("messaging_product", outboxMessage.getContact().getContactType())
				.put("recipient_type", "individual").put("to", outboxMessage.contact().getCsid());
		req.put(OutBoundWrapperPaths.MESSAGE_TYPE, "text");
		req.put(OutBoundWrapperPaths.MESSAGE_TEXT_BODY,
				StringUtils.wrap("*", outboxMessage.getSubject(), "*\n") + outboxMessage.getMessage());
		/** Contextual replies **/
		if (ArgUtil.is(outboxMessage.getMessageIdRef())) {
			MapModel context = MapModel.createInstance().put("message_id", outboxMessage.getMessageIdRef());
			req.put(OutBoundWrapperPaths.MESSAGE_CONTEXT, context.toMap());
		}
		return send(req, channelConfig, outboxMessage);
	}

	private RestMapModel sendMedia(ChannelConfig channelConfig, OutboxMessage outboxMessage, Attachment attachment) {
		MapModel req = MapModel.createInstance().put("messaging_product", outboxMessage.getContact().getContactType())
				.put("recipient_type", "individual").put("to", outboxMessage.contact().getCsid());

		WA360CloudOutBoundMedia wa360OutBoundMedia = new WA360CloudOutBoundMedia();
		wa360OutBoundMedia.setCaption(ArgUtil.nonEmpty(attachment.getMediaCaption(), outboxMessage.getSubject()));
		wa360OutBoundMedia.setFilename(attachment.getMediaName());
		attachMetaMedia(wa360OutBoundMedia, attachment, channelConfig);

		if (ArgUtil.areEqual(attachment.getMediaType(), FileType.IMAGE.toString())) {
			req.put(OutBoundWrapperPaths.MESSAGE_TYPE, "image");
			wa360OutBoundMedia.setFilename(null);
			req.put("image", wa360OutBoundMedia);

		} else if (ArgUtil.areEqual(attachment.getMediaType(), FileType.VIDEO.toString())) {
			req.put(OutBoundWrapperPaths.MESSAGE_TYPE, "video");
			wa360OutBoundMedia.setFilename(null);
			req.put("video", wa360OutBoundMedia);
		} else if (ArgUtil.areEqual(attachment.getMediaType(), FileType.AUDIO.toString())) {
			req.put(OutBoundWrapperPaths.MESSAGE_TYPE, "audio");

			wa360OutBoundMedia.setCaption(null);
			wa360OutBoundMedia.setFilename(null);
			req.put("audio", wa360OutBoundMedia);
		} else {
			req.put(OutBoundWrapperPaths.MESSAGE_TYPE, "document");

			req.put("document", wa360OutBoundMedia);
		}
		/** Contextual replies **/
		if (ArgUtil.is(outboxMessage.getMessageIdRef())) {
			MapModel context = MapModel.createInstance().put("message_id", outboxMessage.getMessageIdRef());
			req.put(OutBoundWrapperPaths.MESSAGE_CONTEXT, context.toMap());
		}

		return send(req, channelConfig, outboxMessage);
	}

	private RestMapModel sendList(ChannelConfig channelConfig, OutboxMessage outboxMessage, List<TmplElement> buttons) {
		MapModel req = MapModel.createInstance().put("messaging_product", outboxMessage.getContact().getContactType())
				.put("recipient_type", "individual").put("to", outboxMessage.contact().getCsid());

		MapModel options = outboxMessage.optionsAsModel();

		req.put(OutBoundWrapperPaths.MESSAGE_TYPE, "interactive");

		req.put(new JsonPath("/interactive/type"), "list");
		req.put(OutBoundWrapperPaths.INTERACTIVE_HEADER_TYPE, "text");
		req.put(OutBoundWrapperPaths.INTERACTIVE_HEADER_TEXT,
				ArgUtil.parseAsString(outboxMessage.getSubject(), Constants.BLANK));
		req.put(OutBoundWrapperPaths.INTERACTIVE_BODY_TEXT, ArgUtil.nonEmpty(outboxMessage.getMessage(), "---"));
		req.put(OutBoundWrapperPaths.INTERACTIVE_FOOTER_TEXT,
				ArgUtil.parseAsString(outboxMessage.getFooter(), Constants.BLANK));
		req.put(OutBoundWrapperPaths.INTERACTIVE_ACTION_BUTTON, options.getString("list_option_title", "Menu"));

		List<Object> sections = new ArrayList<Object>();
		Map<String, Object> section = null;
		List<Object> rows = null;

		for (TmplElement button : buttons) {
			if (section == null) {
				section = new HashMap<String, Object>();
				// section.put("title", "Menu " + (sections.size() + 1));
				sections.add(section);
			}
			if (rows == null) {
				rows = new ArrayList<Object>();
				// section.put("title", "Section Title");
				section.put("rows", rows);
			}

			Map<String, Object> row = new HashMap<String, Object>();
			row.put("id", StringUtils.substring(button.getCode(), 200));
			row.put("title", StringUtils.substring(button.getLabel(), 24));
			// row.put("description", button.getType());
			if (ArgUtil.is(button.getDesc())) {
				row.put("description", StringUtils.substring(button.getDesc(), 72));
			}
			rows.add(row);

			if (rows.size() > 9) {
				section = null;
				rows = null;
			}
		}
		req.put(OutBoundWrapperPaths.INTERACTIVE_ACTION_SECTIONS, sections);
		return send(req, channelConfig, outboxMessage);
	}

	private RestMapModel sendButton(ChannelConfig channelConfig, OutboxMessage outboxMessage, List<TmplElement> buttons,
			String type) {
		MapModel req = MapModel.createInstance().put("messaging_product", outboxMessage.getContact().getContactType())
				.put("recipient_type", "individual").put("to", outboxMessage.contact().getCsid());

		req.put(OutBoundWrapperPaths.MESSAGE_TYPE, "interactive");
		req.put(new JsonPath("/interactive/type"), type);

		if (ArgUtil.is(outboxMessage.getAttachments())) {
			MapModel intr = MapModel.createInstance();
			Attachment attachment = outboxMessage.getAttachments().get(0);
			WA360OutBoundMedia wa360OutBoundMedia = new WA360OutBoundMedia();

			// wa360OutBoundMedia.setCaption(ArgUtil.nonEmpty(attachment.getMediaCaption(),
			// outboxMessage.getSubject()));
			attachMetaMedia(wa360OutBoundMedia, attachment, channelConfig);
			wa360OutBoundMedia.setFilename(attachment.getMediaName());
			if (ArgUtil.areEqual(attachment.getMediaType(), FileType.IMAGE.toString())) {
				intr.put(OutBoundWrapperPaths.MESSAGE_TYPE, "image");
				wa360OutBoundMedia.setFilename(null);
				intr.put("image", wa360OutBoundMedia);
			} else if (ArgUtil.areEqual(attachment.getMediaType(), FileType.VIDEO.toString())) {
				intr.put(OutBoundWrapperPaths.MESSAGE_TYPE, "video");
				wa360OutBoundMedia.setFilename(null);// filename not supported in meta-cloud for video/image. for
														// refernce-https://developers.facebook.com/docs/messenger-platform/send-messages/template/media/
				intr.put("video", wa360OutBoundMedia);
			} else if (ArgUtil.areEqual(attachment.getMediaType(), FileType.AUDIO.toString())) {
				intr.put(OutBoundWrapperPaths.MESSAGE_TYPE, "audio");
				wa360OutBoundMedia.setCaption(null);
				wa360OutBoundMedia.setFilename(null);
				intr.put("audio", wa360OutBoundMedia);
			} else {
				intr.put(OutBoundWrapperPaths.MESSAGE_TYPE, "document");
				intr.put("document", wa360OutBoundMedia);
			}
			req.put(new JsonPath("interactive/header"), intr.toMap());
		} else if (ArgUtil.is(outboxMessage.getSubject())) {
			MapModel intr = MapModel.createInstance();
			intr.put(OutBoundWrapperPaths.MESSAGE_TYPE, "text");
			intr.put(OutBoundWrapperPaths.MESSAGE_TEXT, ArgUtil.nonEmpty(outboxMessage.getSubject(), Constants.BLANK));
		}

		req.put(OutBoundWrapperPaths.INTERACTIVE_BODY_TEXT, ArgUtil.nonEmpty(outboxMessage.getMessage(), "---"));

		String footer = ArgUtil.parseAsString(outboxMessage.getFooter(), Constants.BLANK);
		if (ArgUtil.is(footer)) {
			req.put(OutBoundWrapperPaths.INTERACTIVE_FOOTER_TEXT, footer);
		}

		req.put(OutBoundWrapperPaths.INTERACTIVE_ACTION_BUTTON, "menu");

		if ("button".equalsIgnoreCase(type)) {
			List<Object> rows = new ArrayList<Object>();
			for (TmplElement button : buttons) {
				if (validateButton(button.getCode(), button.getVariable())) {
					rows.add(MapModel.createInstance().put("type", "reply")
							.put(OutBoundWrapperPaths.INTERACTIVE_ACTION_REPLY_ID,
									StringUtils.substring(button.getCode(), 256))
							.put(OutBoundWrapperPaths.INTERACTIVE_ACTION_REPLY_TITLE,
									StringUtils.substring(button.getLabel(), 20))
							.toMap());
				}
			}
			req.put(OutBoundWrapperPaths.INTERACTIVE_ACTION_BUTTONS, rows);
		} else if ("cta_url".equalsIgnoreCase(type)) {
			TmplElement button = buttons.get(0);
			req.put(OutBoundWrapperPaths.INTERACTIVE_ACTION_NAME, "cta_url");
			req.put(OutBoundWrapperPaths.INTERACTIVE_ACTION_PARAMATERS,
					MapModel.createInstance().put("display_text", ArgUtil.nonEmpty(button.getLabel(), "Visit"))
							.put("url", ArgUtil.anyOf(button.getShorturl(), button.getUrl())).toMap());
		} else if ("location_request_message".equalsIgnoreCase(type)) {
			req.put(OutBoundWrapperPaths.INTERACTIVE_ACTION_NAME, "send_location");
		} else if ("address_message".equalsIgnoreCase(type)) {
			TmplElement button = buttons.get(0);
			req.put(OutBoundWrapperPaths.INTERACTIVE_ACTION_NAME, "address_message");
			req.put(OutBoundWrapperPaths.INTERACTIVE_ACTION_PARAMATERS,
					MapModel.createInstance().put("country", button.params().countryCode));
		} else if ("flow".equalsIgnoreCase(type)) {
			TmplElement button = buttons.get(0);
			if (button.getAction() == (null)) {
				button = buttons.get(1);
			}

			req.put(OutBoundWrapperPaths.INTERACTIVE_ACTION_NAME, "flow");

			req.put(OutBoundWrapperPaths.INTERACTIVE_ACTION_PARAMATERS, MapModel.createInstance()
					.put("flow_message_version", "3").put("flow_token", button.getUid()).put("flow_id", button.getUid())
					.put("flow_cta", button.getLabel())
					.put("flow_action", StringUtils.toLowerCase(button.getAction().toLowerCase()))
					.put("flow_action_payload", MapModel.createInstance().put("screen", button.getCode()).toMap())
					.toMap());
		}

		/** Contextual replies **/
		if (ArgUtil.is(outboxMessage.getMessageIdRef())) {
			MapModel context = MapModel.createInstance().put("message_id", outboxMessage.getMessageIdRef());
			req.put(OutBoundWrapperPaths.MESSAGE_CONTEXT, context.toMap());
		}

		return send(req, channelConfig, outboxMessage);
	}

	private boolean validateButton(String code, String variable) {
		if (!ArgUtil.is(code)) {
			ApiFieldError error = new ApiFieldError();
			error.code("100");
			// error.codeKey(errorTitle);
			error.setDescription(ArgUtil.nonEmpty(variable, "button.code") + " is missing");
			error.field("button.code").code(ApiStatusCodes.PARAM_MISSING);
			// error.setBody(resp.get("errors"));
			ApiResponseUtil.throwException(error);
		}
		return true;
	}

	public RestMapModel send(MapModel req, ChannelConfig channelConfig, OutboxMessage outboxMessage) {

		RestMapModel rest = new RestMapModel();
		try {
			String endpoint = "/messages";
			// mmlite is enabled and category is marketing

			if (channelConfig.getWacfb().isMmlite()) {
				String categoryType = ArgUtil.parseAsString(req.get("categoryType"));
				if ("MARKETING".equalsIgnoreCase(categoryType)) {
					endpoint = "/marketing_messages";
				}
			}

			rest.request(req);

			MapModel resp = null;

			if (mockMessenger != null && (channelConfig.isMockEnabled()
					&& mockMessenger.isMockNumber(outboxMessage.contact().getCsid()))) {
				resp = mockMessenger.send(req, channelConfig);
			} else {
				resp = restService.ajax(WA360Constants.META_WA_CLOUD_URL)
						.path(channelConfig.getWacfb().getPhoneNumberId() + endpoint)
						.authBearer(channelConfig.getWacfb().getAccessToken()).post(req.toMap()).asMapModel();
			}
			rest.response(resp);
			dumpMeta(rest, channelConfig);
			return rest;

		} catch (ApiHttpServerException e) {
			throw e;
		} catch (ApiHttpException e) {
			rest.response(MapModel.from(e.getResponse().getBody()));
			dumpMeta(rest, channelConfig);
			return rest;
		}
	}

	private void dumpMeta(RestMapModel rest, ChannelConfig channelConfig) {
		if (rest == null || channelConfig == null || channelConfig.getWacfb() == null) {
			return;
		}
		rest.meta().put("phoneNumberId", channelConfig.getWacfb().getPhoneNumberId());
		String token = channelConfig.getWacfb().getAccessToken();
		if (!ArgUtil.is(token)) {
			rest.meta().put("token", null);
		} else {
			rest.meta().put("token", token.substring(token.length() - 6));
		}
	}

	private void attachMetaMedia(WA360CloudOutBoundMedia media, Attachment attachment, ChannelConfig channelConfig) {
		String id = tryUploadMediaToMeta(attachment.getMediaURL(), channelConfig);
		if (ArgUtil.is(id)) {
			media.setId(id);
			media.setLink(null);
		} else {
			media.setLink(attachment.getMediaURL());
		}
	}

	private void attachMetaMedia(WA360OutBoundMedia media, Attachment attachment, ChannelConfig channelConfig) {
		String id = tryUploadMediaToMeta(attachment.getMediaURL(), channelConfig);
		if (ArgUtil.is(id)) {
			media.setId(id);
			media.setLink(null);
		} else {
			media.setLink(attachment.getMediaURL());
		}
	}

	private String tryUploadMediaToMeta(String mediaUrl, ChannelConfig channelConfig) {
		try {
			return uploadMediaToMeta(mediaUrl, channelConfig);
		} catch (Exception e) {
			LOGGER.warn("Meta media upload failed for {}, falling back to link: {}", mediaUrl, e.getMessage());
			return null;
		}
	}

	private String normalizeMimeType(String contentType) {
		if (!ArgUtil.is(contentType)) {
			return contentType;
		}
		String mime = contentType.split(";")[0].trim().toLowerCase();
		if ("image/jpg".equals(mime)) {
			return "image/jpeg";
		}
		FileFormat format = FileFormat.from(mime);
		if (format != FileFormat.UNKNOWN) {
			return format.getContentType();
		}
		return mime;
	}

	private String uploadMediaToMeta(String mediaUrl, ChannelConfig channelConfig) throws IOException {
		String boundary = "----MetaBoundary" + System.currentTimeMillis();
		String LINE_FEED = "\r\n";

		byte[][] result = downloadMedia(mediaUrl);
		byte[] mediaBytes = result[0];
		String mimeType = normalizeMimeType(new String(result[1]));
		String fileName = new String(result[2]);

		String uploadUrl = WA360Constants.META_WA_CLOUD_URL + channelConfig.getWacfb().getPhoneNumberId() + "/media";

		HttpURLConnection conn = (HttpURLConnection) new URL(uploadUrl).openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Authorization", "Bearer " + channelConfig.getWacfb().getAccessToken());
		conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

		try (OutputStream output = conn.getOutputStream();
				PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, "UTF-8"), true)) {

			writer.append("--").append(boundary).append(LINE_FEED);
			writer.append("Content-Disposition: form-data; name=\"messaging_product\"").append(LINE_FEED);
			writer.append(LINE_FEED).append("whatsapp").append(LINE_FEED).flush();

			writer.append("--").append(boundary).append(LINE_FEED);
			writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName).append("\"")
					.append(LINE_FEED);
			writer.append("Content-Type: ").append(mimeType).append(LINE_FEED);
			writer.append(LINE_FEED).flush();

			output.write(mediaBytes);
			output.flush();

			writer.append(LINE_FEED).flush();
			writer.append("--").append(boundary).append("--").append(LINE_FEED).flush();
		}

		InputStream responseStream = (conn.getResponseCode() >= 400) ? conn.getErrorStream() : conn.getInputStream();
		String response = new BufferedReader(new InputStreamReader(responseStream)).lines()
				.collect(Collectors.joining("\n"));

		MapModel uploadResp = MapModel.from(response);
		return uploadResp.getString("id");
	}

	private byte[][] downloadMedia(String mediaUrl) throws IOException {
		int maxRetries = 3;
		int attempt = 0;

	    while (attempt < maxRetries) {
	        attempt++;
	        HttpURLConnection conn = null;
	        try {
	            conn = (HttpURLConnection) new URL(mediaUrl).openConnection();
	            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
	            conn.setConnectTimeout(10000);
	            conn.setReadTimeout(15000);

				int status = conn.getResponseCode();

				if (status == HttpURLConnection.HTTP_OK) {
					String contentType = normalizeMimeType(conn.getContentType());
					if (contentType == null || contentType.trim().isEmpty()
							|| "application/octet-stream".equals(contentType)
							|| "binary/octet-stream".equals(contentType)) {
						contentType = guessMimeTypeFromUrl(mediaUrl);
					}

					String fileName = guessFileNameFromUrl(mediaUrl, contentType);

					try (InputStream is = conn.getInputStream();
							ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
						byte[] buffer = new byte[4096];
						int bytesRead;
						while ((bytesRead = is.read(buffer)) != -1) {
							baos.write(buffer, 0, bytesRead);
						}
						return new byte[][] { baos.toByteArray(), contentType.getBytes(StandardCharsets.UTF_8),
								fileName.getBytes(StandardCharsets.UTF_8) };
					}
				} else {
					try (InputStream errorStream = conn.getErrorStream();
							ByteArrayOutputStream errorBaos = new ByteArrayOutputStream()) {
						if (errorStream != null) {
							byte[] buffer = new byte[1024];
							int bytesRead;
							while ((bytesRead = errorStream.read(buffer)) != -1) {
								errorBaos.write(buffer, 0, bytesRead);
							}
							System.err.println("Download error: " + new String(errorBaos.toByteArray(), "UTF-8"));
						}
					}
					throw new IOException("HTTP " + status + " for URL: " + mediaUrl);
				}
			} catch (IOException e) {
				if (attempt >= maxRetries) {
					throw new IOException("Failed to download media after " + maxRetries + " attempts: " + mediaUrl, e);
				}
			} finally {
				if (conn != null)
					conn.disconnect();
			}
		}
		throw new IOException("Unexpected error occurred while downloading media: " + mediaUrl);
	}

	private String guessMimeTypeFromUrl(String url) {
		String lower = url.toLowerCase();
		if (lower.endsWith(".jpg") || lower.endsWith(".jpeg"))
			return "image/jpeg";
		if (lower.endsWith(".png"))
			return "image/png";
		if (lower.endsWith(".webp"))
			return "image/webp";
		if (lower.endsWith(".mp4"))
			return "video/mp4";
		if (lower.endsWith(".3gp"))
			return "video/3gpp";
		if (lower.endsWith(".pdf"))
			return "application/pdf";
		if (lower.endsWith(".doc"))
			return "application/msword";
		if (lower.endsWith(".docx"))
			return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
		if (lower.endsWith(".xls"))
			return "application/vnd.ms-excel";
		if (lower.endsWith(".xlsx"))
			return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
		if (lower.endsWith(".ppt"))
			return "application/vnd.ms-powerpoint";
		if (lower.endsWith(".pptx"))
			return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
		if (lower.endsWith(".txt"))
			return "text/plain";
		if (lower.endsWith(".aac"))
			return "audio/aac";
		if (lower.endsWith(".mpeg") || lower.endsWith(".mp3"))
			return "audio/mpeg";
		if (lower.endsWith(".ogg"))
			return "audio/ogg";
		if (lower.endsWith(".opus"))
			return "audio/opus";
		if (lower.endsWith(".amr"))
			return "audio/amr";
		return "application/octet-stream";
	}

	private String guessFileNameFromUrl(String url, String mimeType) {
		String fileName = url.substring(url.lastIndexOf("/") + 1);
		if (!fileName.contains(".")) {
			if (mimeType.startsWith("image/"))
				return fileName + ".jpg";
			if (mimeType.startsWith("video/"))
				return fileName + ".mp4";
			if (mimeType.startsWith("audio/"))
				return fileName + ".mp3";
			if (mimeType.equals("application/pdf"))
				return fileName + ".pdf";
		}
		return fileName;
	}

	private String getMessageId(RestMapModel rest, OutboxMessage outboxMessage) {
		MapModel resp = rest.getResponse().getBody();
		String id = resp.entry(OutBoundWrapperPaths.RESPONSE_MSG_ID).asString();
		String errorCode = resp.entry(OutBoundWrapperPaths.RESPONSE_ERROR_CODE).asString();
		String errorCodeLegacy = resp.entry(OutBoundWrapperPaths.RESPONSE_ERRORS_CODE).asString();
		if (ArgUtil.is(errorCode) || ArgUtil.is(errorCodeLegacy) || !ArgUtil.is(id)) {
			
			String code = ArgUtil.nonEmpty(errorCode, errorCodeLegacy);
			if (messageFailureGuard != null) {
				messageFailureGuard.escalate(outboxMessage, code);
			}
			
			boolean messageDumpEnabled = pmEnvironment.domainConfig().getMessageOutboundDump();
			if (messageDumpEnabled) {
				outboxMessage.setDump(rest);
			}
			if (ArgUtil.is(errorCodeLegacy)) {
				return getMessageIdLegacy(resp, id, errorCodeLegacy);
			} else {
				String errorTraceId = resp.entry(OutBoundWrapperPaths.RESPONSE_ERROR_TRACE_ID).asString();
				outboxMessage.dump().meta().put("fbtrace_id", errorTraceId);
				return getMessageIdCloud(resp, id, errorCode);
			}
		}
		return id;
	}

	private String getMessageIdCloud(MapModel resp, String messageId, String errorCode) {
		String errorTitle = resp.entry(OutBoundWrapperPaths.RESPONSE_ERROR_MSG).asString();
		String errorDetails = resp.entry(OutBoundWrapperPaths.RESPONSE_ERROR_DETAILS).asString();

		ApiFieldError error = new ApiFieldError();
		error.code(errorCode);
		error.codeKey(errorTitle);

		if (!ArgUtil.is(errorDetails)) {
			error.setDescriptionKey(resp.entry(OutBoundWrapperPaths.RESPONSE_ERROR_USER_TITLE).asString());
			errorDetails = resp.entry(OutBoundWrapperPaths.RESPONSE_ERROR_USER_MSG).asString();
		}

		error.setDescription(String.format("%s : %s / %s / %s ", messageId, errorCode, errorTitle, errorDetails));
		if ("1006".equals(errorCode) || "131026".equals(errorCode)) {
			error.setDescriptionKey("File or resource not found");
			if ("unknown contact".equals(errorDetails)) {
				error.field("to").code(PostManException.ErrorCode.CONTACT_NOTFOUND);
			}
		} else if ("471".equals(errorCode)) {
			error.setDescriptionKey("File or resource not found");
			error.code(PostManException.ErrorCode.MESSAGE_LIMIT_EXCEEDED);
		}
		error.setBody(resp.get("errors"));
		ApiResponseUtil.throwException(error);
		return messageId;
	}

	private String getMessageIdLegacy(MapModel resp, String messageId, String errorCode) {
		String errorTitle = resp.entry(OutBoundWrapperPaths.RESPONSE_ERRORS_TITLE).asString();
		String errorDetails = resp.entry(OutBoundWrapperPaths.RESPONSE_ERRORS_DETAILS).asString();

		ApiFieldError error = new ApiFieldError();
		error.code(errorCode);
		error.codeKey(errorTitle);
		error.setDescription(String.format("%s : %s / %s / %s ", messageId, errorCode, errorTitle, errorDetails));
		if ("1006".equals(errorCode)) {
			error.setDescriptionKey("File or resource not found");
			if ("unknown contact".equals(errorDetails)) {
				error.field("to").code(PostManException.ErrorCode.CONTACT_NOTFOUND);
			}
		} else if ("471".equals(errorCode)) {
			error.setDescriptionKey("File or resource not found");
			error.code(PostManException.ErrorCode.MESSAGE_LIMIT_EXCEEDED);
		}
		error.setBody(resp.get("errors"));
		ApiResponseUtil.throwException(error);
		return messageId;
	}

	public MapModel fetchContact(String contact, ChannelConfig channelConfig) {
		try {
			MapModel resp = restService.ajax(WA360Constants.META_WA_CLOUD_URL)
					.path(channelConfig.getWacfb().getWabaId() + "/contacts")
					.authBearer(channelConfig.getWacfb().getAccessToken()).post(MapModel.createInstance()
							.put("blocking", "wait").put(OutBoundWrapperPaths.FETCH_CONTACTS_DETAILS, contact).toMap())
					.asMapModel();
			return resp.path(OutBoundWrapperPaths.FETCH_CONTACTS_DETAILS).asMapModel();
		} catch (ApiHttpServerException e) {
			return MapModel.from(e.getResponse().getBody()).put(OutBoundWrapperPaths.RESPONSE_ERROR_CODE,
					e.getHttpStatus().value());
		} catch (ApiHttpException e) {
			return MapModel.from(e.getResponse().getBody());
		}
	}

	public MapModel fetchTemplates(ChannelConfig channelConfig) {
		MapModel resp = restService.ajax(WA360Constants.META_WA_CLOUD_URL)
				.path(channelConfig.getWacfb().getWabaId() + "/message_templates")
				.authBearer(channelConfig.getWacfb().getAccessToken()).queryParam("limit", 200).get().asMapModel();

		LOGGER.info("WABA template fetch finished for channelId={}: {} templates from Meta (limit=200)",
				channelConfig.getChannelId(), resp.keyEntry("data").asList().size());
		return resp;
	}
	
	public String resolveTemplateId(ChannelConfig channelConfig, String name, String language, String status) {
		if (!ArgUtil.is(name) || !ArgUtil.is(language)) {
			return Constants.BLANK;
		}
		try {
			MapModel resp = restService.ajax(WA360Constants.META_WA_CLOUD_URL)
					.path(channelConfig.getWacfb().getWabaId() + "/message_templates")
					.authBearer(channelConfig.getWacfb().getAccessToken())
					.queryParam("name", name)
					.queryParam("language", language)
					.queryParam("status", status)
					.queryParam("limit", 1)
					.get().asMapModel();

			List<WA360Template> data = resp.keyEntry("data").asList(WA360Template.class);
			if (!ArgUtil.is(data)) {
				LOGGER.warn("No Meta template found for name={} language={} status={}", name, language, status);
				return Constants.BLANK;
			}
			return ArgUtil.parseAsString(data.get(0).getId(), Constants.BLANK);
		} catch (Exception e) {
			LOGGER.warn("Failed to resolve Meta template id for name={} language={}: {}", name, language,
					e.getMessage());
			return Constants.BLANK;
		}
	}
	
	public MapModel deleteTemplates(ChannelConfig channelConfig, String templateName) {
		MapModel resp = restService.ajax(WA360Constants.META_WA_CLOUD_URL)
				.path(channelConfig.getWacfb().getWabaId() + "/message_templates")
				.authBearer(channelConfig.getWacfb().getAccessToken()).queryParam("name", templateName).delete()
				.asMapModel();
		return resp;
	}
	
	public MapModel unarchiveTemplates(ChannelConfig channelConfig, List<String> hsmIds) {
		try {
			Map<String, Object> body = new HashMap<>();
			body.put("hsm_ids", hsmIds);
			return restService.ajax(WA360Constants.META_WA_API_URL)
					.path(channelConfig.getWacfb().getWabaId() + "/message_templates/unarchive")
					.authBearer(channelConfig.getWacfb().getAccessToken())
					.postJson(body)
					.asMapModel();
		} catch (HttpStatusCodeException | ApiHttpException e) {
			if (e instanceof HttpStatusCodeException)
				ApiResponseUtil.addError(((HttpStatusCodeException) e).getResponseBodyAsString());
			else
				ApiResponseUtil.addError(((ApiHttpException) e));
			throw e;
		}
	}

	/**
	 * Fetch a single template from Meta API by template ID
	 * 
	 * @param channelConfig Channel configuration
	 * @param templateId    Meta template ID (e.g., "123456789")
	 * @return MapModel containing template data, or null if not found/error
	 */
	public MapModel fetchTemplateById(ChannelConfig channelConfig, String templateId) {
		if (!ArgUtil.is(templateId)) {
			LOGGER.warn("Template ID is null or empty");
			return null;
		}

		try {
			MapModel resp = restService.ajax(WA360Constants.META_WA_CLOUD_URL).path(templateId)
					.authBearer(channelConfig.getWacfb().getAccessToken()).get().asMapModel();

			return resp;
		} catch (HttpStatusCodeException e) {
			if (e.getStatusCode().value() == 404) {
				LOGGER.debug("Template not found in Meta API: {}", templateId);
			} else {
				LOGGER.warn("Failed to fetch template from Meta API: {} - {}", templateId, e.getStatusCode().value());
			}
			return null;
		} catch (Exception e) {
			LOGGER.error("Unexpected error fetching template {}: {}", templateId, e.getMessage(), e);
			return null;
		}

	}

	public MapModel updateTemplates(ChannelConfig channelConfig, MapModel req) {
		try {
			String templateId = req.getString("id");
			if (ArgUtil.isEmpty(templateId)) {
				templateId = channelConfig.getWacfb().getPhoneNumberId();
			}
			MapModel resp = restService.ajax(WA360Constants.META_WA_CLOUD_URL).path("/" + templateId)
					.authBearer(channelConfig.getWacfb().getAccessToken()).post(req.toMap()).asMapModel();

			return resp;
		} catch (HttpStatusCodeException | ApiHttpException e) {
			if (e instanceof HttpStatusCodeException)
				ApiResponseUtil.addError(((HttpStatusCodeException) e).getResponseBodyAsString());
			else
				ApiResponseUtil.addError(((ApiHttpException) e));
			throw e;
		}
	}

	public MapModel listOfFlows(ChannelConfig channelConfig) {
		try {
			MapModel resp = restService.ajax(WA360Constants.META_WA_CLOUD_URL)
					.path(channelConfig.getWacfb().getWabaId() + "/flows")
					.authBearer(channelConfig.getWacfb().getAccessToken()).header("Content-Type", "application/json")
					.get().asMapModel();

			return resp;
		} catch (Exception e) {
			System.err.println("Unexpected error: " + e.getMessage());
			throw e;
		}
	}

	public MapModel flowsAssets(String flowId, ChannelConfig channelConfig) {
		try {
			MapModel resp = restService.ajax(WA360Constants.META_WA_CLOUD_URL).path(flowId + "/assets")
					.authBearer(channelConfig.getWacfb().getAccessToken()).header("Content-Type", "application/json")
					.get().asMapModel();
			return resp;
		} catch (Exception e) {
			System.err.println("Unexpected error: " + e.getMessage());
			throw e;
		}
	}

	public MapModel createTemplates(ChannelConfig channelConfig, MapModel req) {

		try {

			List<Map<String, Object>> components = req.keyEntry("components").asListOfMap();
			for (Map<String, Object> component : components) {
				if ("HEADER".equals(component.get("type"))) {
					MapModel componentMap = MapModel.from(component);
					MapPathEntry format = componentMap.keyEntry("format");
					if (format.is("IMAGE") || format.is("DOCUMENT") || format.is("VIDEO")) {
						String fileUrl = componentMap.pathEntry("/example/header_handle/[0]").asString();
						if (ArgUtil.is(fileUrl)) {
							String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
							byte[] fileData = downloadFile(fileUrl);
							int fileLength = fileData.length;
							String fileType = determineFileType(fileUrl);

							String UPLOAD_URL = WA360Constants.META_WA_CLOUD_URL
									+ channelConfig.getWacfb().getMasterAppId() + "/uploads";
							String uploadResponse = firstApiCall(fileData, fileName, fileType, fileLength, UPLOAD_URL,
									channelConfig);
							String id = extractIdFromResponse(uploadResponse);
							String finalResponse = secondApiCall(id, fileData, fileName, fileType, channelConfig);

							/// Save
							componentMap.put(JsonPath.at("/example/header_handle"), finalResponse);
						}
					}

				}
			}

			MapModel resp = restService.ajax(WA360Constants.META_WA_CLOUD_URL)
					.path(channelConfig.getWacfb().getWabaId() + "/message_templates")
					.authBearer(channelConfig.getWacfb().getAccessToken()).post(req.toMap()).asMapModel();

			return resp;

		} catch (HttpStatusCodeException | ApiHttpException e) {
			if (e instanceof HttpStatusCodeException)
				ApiResponseUtil.addError(((HttpStatusCodeException) e).getResponseBodyAsString());
			else
				ApiResponseUtil.addError(((ApiHttpException) e));
			throw e;
		} catch (IOException e) {
			e.printStackTrace();
			// Stop the NullPointerException and return the actual error!
			MapModel errorResp = MapModel.createInstance();
			errorResp.put("status", "FAILED");
			errorResp.put("error_details", "S3 Download or File Error: " + e.getMessage());
			return errorResp;
		}
	}

	private static byte[] downloadFile(String fileUrl) throws IOException {
		try (InputStream in = new URL(fileUrl).openStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			byte[] buffer = new byte[1024];
			int bytesRead;
			while ((bytesRead = in.read(buffer)) != -1) {
				baos.write(buffer, 0, bytesRead);
			}
			return baos.toByteArray();
		}
	}

	private static String determineFileType(String fileUrl) throws IOException {
		try (InputStream is = new URL(fileUrl).openStream()) {
			String mime = URLConnection.guessContentTypeFromStream(is);
			if (mime != null && !mime.equalsIgnoreCase("application/octet-stream")) {
				return mime;
			}
		}

		String lowerUrl = fileUrl.toLowerCase();
		if (lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg"))
			return "image/jpeg";
		if (lowerUrl.endsWith(".png"))
			return "image/png";
		if (lowerUrl.endsWith(".gif"))
			return "image/gif";
		if (lowerUrl.endsWith(".pdf"))
			return "application/pdf";
		if (lowerUrl.endsWith(".mp4"))
			return "video/mp4";
		if (lowerUrl.endsWith(".doc"))
			return "application/msword";
		if (lowerUrl.endsWith(".docx"))
			return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
		if (lowerUrl.endsWith(".xls"))
			return "application/vnd.ms-excel";
		if (lowerUrl.endsWith(".xlsx"))
			return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
		if (lowerUrl.endsWith(".txt"))
			return "text/plain";

		return "application/octet-stream";
	}

	private static String firstApiCall(byte[] fileData, String fileName, String fileType, int fileLength,
			String uploadUrl, ChannelConfig channelConfig) throws IOException {

		URL url = new URL(uploadUrl + "?file_type=" + fileType + "&file_length=" + fileLength);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setDoOutput(true);
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/octet-stream");
		connection.setRequestProperty("Authorization", "Bearer " + channelConfig.getWacfb().getAccessToken());
		connection.setRequestProperty("Cookie", "ps_l=1; ps_n=1");

		// Check for Meta API errors
		int responseCode = connection.getResponseCode();
		if (responseCode >= 400) {
			try (InputStream es = connection.getErrorStream();
					ByteArrayOutputStream errorBaos = new ByteArrayOutputStream()) {
				byte[] buffer = new byte[1024];
				int bytesRead;
				if (es != null) {
					while ((bytesRead = es.read(buffer)) != -1) {
						errorBaos.write(buffer, 0, bytesRead);
					}
				}
				String metaError = errorBaos.toString();

				// Throw the exception so the upstream caller can handle and log the real error
				throw new IOException("Meta API Error " + responseCode + ": " + metaError);
			}
		}

		// Read and return the success response (Session ID)
		try (InputStream is = connection.getInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			byte[] buffer = new byte[1024];
			int bytesRead;
			while ((bytesRead = is.read(buffer)) != -1) {
				baos.write(buffer, 0, bytesRead);
			}
			return baos.toString();
		}
	}

	private static String extractIdFromResponse(String response) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode jsonNode = objectMapper.readTree(response);
		return jsonNode.get("id").asText();
	}

	private static String secondApiCall(String id, byte[] fileData, String fileName, String fileType,
			ChannelConfig channelConfig) throws IOException {
		URL url = new URL(WA360Constants.META_WA_CLOUD_URL + id);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setDoOutput(true);
		connection.setRequestMethod("POST");
		connection.setRequestProperty("file_offset", "0");
		connection.setRequestProperty("Content-Type", fileType);
		connection.setRequestProperty("Authorization", "OAuth " + channelConfig.getWacfb().getAccessToken());
		connection.setRequestProperty("Cookie", "ps_l=1; ps_n=1");

		try (OutputStream os = connection.getOutputStream()) {
			os.write(fileData);
		}

		try (InputStream is = connection.getInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

			byte[] buffer = new byte[1024];
			int bytesRead;
			while ((bytesRead = is.read(buffer)) != -1) {
				baos.write(buffer, 0, bytesRead);
			}

			String rawResponse = baos.toString().trim();

			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode jsonNode = objectMapper.readTree(rawResponse);

			String hValue = jsonNode.get("h").asText();

			int secondHandleIndex = hValue.indexOf("4::", 3);
			if (secondHandleIndex > 0) {
				hValue = hValue.substring(0, secondHandleIndex);
			}

			return hValue;
		}
	}

	public String getMediaUrl(ChannelConfig channelConfig, String url) {

		if (url.contains("mid=")) {
			String mid = url.split("mid=")[1].split("&")[0];
			url = WA360Constants.META_WA_CLOUD_URL(mid);
		}

		MapModel resp = restService.ajax(url.replace("/v1/media/", "/"))
				.authBearer(channelConfig.getWacfb().getAccessToken())
				.queryParam("phone_number_id", channelConfig.getWacfb().getPhoneNumberId()).acceptJson().get()
				.asMapModel();
		return resp.getString("url");// .replace("https://lookaside.fbsbx.com", WA360Constants.META_WA_CLOUD_URL);
	}

	/** Call new metod to post msg directly to waba API **/
	public RestMapModel sendTemplateRaw(ChannelConfig channelConfig, OutboxMessage outboxMessage) {
		MapModel req = MapModel.createInstance().put("messaging_product", outboxMessage.getContact().getContactType())
				.put("recipient_type", "individual").put("to", outboxMessage.contact().getCsid());

		MapModel extTemplate = MapModel.from(outboxMessage.getRawMessageFormat());

		// List<Map<String, Object>> extTemplateComponents =
		// extTemplate.keyEntry("template").asListOfMap();
		// System.out.println("JSON
		// -extTemplateComponents--"+JsonUtil.toJsonPrettyPrint(extTemplateComponents));

		MapModel model = MapModel.from(outboxMessage.getModel());
		MapModel varMap = null;// MapModel.from(outboxMessage.getTemplateExt().getVarMap());

		req.put(OutBoundWrapperPaths.MESSAGE_TYPE, "template");
		req.put(OutBoundWrapperPaths.TEMPLATE_NAMESPACE, extTemplate.get("namespace"));
		req.put(OutBoundWrapperPaths.TEMPLATE_NAME, extTemplate.get("name"));
		req.put(OutBoundWrapperPaths.TEMPLATE_LANGUAGE_CODE, extTemplate.get("language"));
		req.put(OutBoundWrapperPaths.TEMPLATE_LANGUAGE_POLICY, "deterministic");

		req.putAll(extTemplate);

		MapModel components = MapModel.createInstance();
		// List<Map<String, Object>> extTemplateComponents =
		// null;//extTemplate.keyEntry("components").asListOfMap();

		/*
		 * for (Map<String, Object> extTemplateComponent : extTemplateComponents) {
		 * String extTemplateComponentType = (String) extTemplateComponent.get("type");
		 * if ("HEADER".equals(extTemplateComponentType)) { TmplComponent
		 * headerComponentReq = TmplComponent.createInstance().header(); String
		 * extTemplateComponentFormat = (String) extTemplateComponent.get("format"); if
		 * ("TEXT".equals(extTemplateComponentFormat)) { if
		 * (varMap.containsKey("header")) { List<Map<String, Object>>
		 * headerParametersTemp = varMap.entry("header").asListOfMap(); for (Map<String,
		 * Object> headerParameter : headerParametersTemp) { String path = (String)
		 * headerParameter.get("path"); String path2 = (String)
		 * headerParameter.get("path2"); String defaultValue = (String)
		 * headerParameter.get("defaultValue"); headerComponentReq.parameter("text",
		 * model.pathEntry(path).pathEntrySafe(path2).asString(defaultValue)); } if
		 * (headerComponentReq.parameters().size() > 0) {
		 * components.add(headerComponentReq.build().map()); } } } else if
		 * (ArgUtil.is(outboxMessage.getAttachments())) { String lowerFormat =
		 * extTemplateComponentFormat.toLowerCase(); WA360CloudOutBoundMedia media =
		 * createMedia(lowerFormat, outboxMessage.getAttachments().get(0));
		 * headerComponentReq.parameter(lowerFormat, media); if
		 * (headerComponentReq.parameters().size() > 0) {
		 * components.add(headerComponentReq.build().map()); } }
		 * 
		 * } else if ("BODY".equals(extTemplateComponentType)) { if
		 * (varMap.containsKey("body")) { List<Map<String, Object>> bodyParametersTemp =
		 * varMap.entry("body").asListOfMap(); TmplComponent bodyComponent =
		 * TmplComponent.createInstance().body(); for (Map<String, Object> bodyParameter
		 * : bodyParametersTemp) { String path = (String) bodyParameter.get("path");
		 * String path2 = (String) bodyParameter.get("path2"); String defaultValue =
		 * (String) bodyParameter.get("defaultValue"); bodyComponent.parameter("text",
		 * model.pathEntry(path).pathEntrySafe(path2).asString(defaultValue)); }
		 * components.add(bodyComponent.build().map()); } } else if
		 * ("BUTTONS".equals(extTemplateComponentType)) { List<Map<String, Object>>
		 * extTemplateComponentButtons = MapModel.from(extTemplateComponent)
		 * .keyEntry("buttons").asListOfMap(); List<List<Map<String, Object>>>
		 * buttonsParametersVars = varMap.entry("buttons").asListListOfMap();
		 * 
		 * for (int i = 0; i < extTemplateComponentButtons.size(); i++) { Map<String,
		 * Object> extTemplateComponentButton = extTemplateComponentButtons.get(i);
		 * List<Map<String, Object>> buttonParameterVar =
		 * CollectionUtil.getArray(buttonsParametersVars, i); if
		 * (ArgUtil.is(buttonParameterVar)) { String buttonType = (String)
		 * extTemplateComponentButton.get("type"); if ("URL".equals(buttonType)) { for
		 * (Map<String, Object> buttonParameter : buttonParameterVar) { if
		 * (buttonParameter.containsKey("path")) { String path = (String)
		 * buttonParameter.get("path"); String path2 = (String)
		 * buttonParameter.get("path2"); String defaultValue = (String)
		 * buttonParameter.get("defaultValue"); TmplComponent buttonComponent =
		 * TmplComponent.createInstance().button("url", i);
		 * buttonComponent.parameter("text",
		 * model.pathEntry(path).pathEntrySafe(path2).asString(defaultValue));
		 * components.add(buttonComponent.build().map()); } } } else if
		 * ("QUICK_REPLY".equals(buttonType)) { for (Map<String, Object> buttonParameter
		 * : buttonParameterVar) { if (buttonParameter.containsKey("path")) { String
		 * path = (String) buttonParameter.get("path"); String path2 = (String)
		 * buttonParameter.get("path2"); String defaultValue = (String)
		 * buttonParameter.get("defaultValue"); TmplComponent buttonComponent =
		 * TmplComponent.createInstance().button("quick_reply", i);
		 * buttonComponent.parameter("payLoad",
		 * model.pathEntry(path).pathEntrySafe(path2).asString(defaultValue));
		 * components.add(buttonComponent.build().map()); } } } } } }
		 * 
		 * }
		 */

		// req.put(OutBoundWrapperPaths.TEMPLATE_COMPONENTS, components.list());
		return send(req, channelConfig, outboxMessage);
	}

	public MapModel fetchMMLiteStatus(ChannelConfig channelConfig) {
		try {
			String wabaId = channelConfig.getWacfb().getWabaId();
			String accessToken = channelConfig.getWacfb().getAccessToken();
			LOGGER.debug("Fetching MM Lite status for WABA ID: {}", wabaId);
			MapModel resp = restService.ajax(WA360Constants.META_WA_CLOUD_URL).path(wabaId)
					.queryParam("fields", "marketing_messages_lite_api_status").authBearer(accessToken).get()
					.asMapModel();
			LOGGER.debug("MM Lite status response: {}", resp.toMap());
			// Transform the response to match our expected format
			MapModel result = MapModel.createInstance();
			if (resp.containsKey("marketing_messages_lite_api_status")) {
				String status = resp.getString("marketing_messages_lite_api_status");
				boolean isEligible = "ELIGIBLE".equals(status) || "ONBOARDED".equals(status);
				result.put("eligible", isEligible);
				result.put("message", "MM Lite status: " + status);
				result.put("data", resp.toMap());
			} else {
				result.put("eligible", false);
				result.put("message", "Unable to determine MM Lite status");
			}
			return result;
		} catch (ApiHttpServerException e) {
			LOGGER.error("Facebook API server error: {} - {}", e.getHttpStatus(), e.getResponse().getBody());
			return MapModel.createInstance().put("eligible", false).put("message",
					"Facebook API server error: " + e.getHttpStatus().value());
		} catch (ApiHttpException e) {
			LOGGER.error("Facebook API error: {}", e.getResponse().getBody());
			return MapModel.createInstance().put("eligible", false).put("message",
					"Facebook API error: " + e.getMessage());
		} catch (Exception e) {
			LOGGER.error("Unexpected error fetching MM Lite status: {}", e.getMessage());
			return MapModel.createInstance().put("eligible", false).put("message",
					"Unexpected error: " + e.getMessage());
		}
	}

	public MapModel onboardMMLite(ChannelConfig channelConfig, ChannelConfig setup) {
		try {
			String bmId = channelConfig.getWacfb().getBmId();
			String systemUserToken = null;
			// Get system user token from setup (master channel) - same pattern as
			// WacfbConnector
			if (ArgUtil.is(setup) && ArgUtil.is(setup.getWacfb())) {
				systemUserToken = setup.getWacfb().getMasterSUAccessToken();
				LOGGER.info("Using system user token from setup (master channel): {}",
						systemUserToken != null
								? systemUserToken.substring(0, Math.min(20, systemUserToken.length())) + "..."
								: "null");
			}
			if (!ArgUtil.is(systemUserToken)) {
				LOGGER.error("System user token not available from setup for channel: {}",
						channelConfig.getChannelId());
				return MapModel.createInstance().put("error",
						"System user token not available from master channel configuration");
			}
			if (!ArgUtil.is(bmId)) {
				LOGGER.error("Business Manager ID (bmId) is not available for channel: {}",
						channelConfig.getChannelId());
				return MapModel.createInstance().put("error",
						"Business Manager ID (bmId) is not available. Please ensure the channel was properly registered.");
			}
			LOGGER.debug("Calling MM Lite onboarding API for BM ID: {} with system user token", bmId);
			MapModel resp = restService.ajax(WA360Constants.META_WA_CLOUD_URL).path(bmId + "/")
					.path("onboard_partners_to_mm_lite").queryParam("access_token", systemUserToken).post()
					.asMapModel();
			LOGGER.debug("MM Lite onboarding API response (raw): {}", resp.toJson());
			return resp;
		} catch (ApiHttpException e) {
			String body = e.getResponse().getBody();
			LOGGER.error("MM Lite onboarding API error (raw body): {}", body);
			try {
				MapModel parsed = MapModel.from(body);
				LOGGER.debug("Parsed Facebook error as JSON: {}", parsed.toJson());
				return parsed;
			} catch (Exception parseEx) {
				LOGGER.error("Failed to parse error body as JSON: {}", parseEx.getMessage());
				return MapModel.createInstance().put("success", false)
						.put("message", "Error during MM Lite onboarding: " + parseEx.getMessage())
						.put("rawBody", body);
			}
		}
	}

	public MapModel fetchBusinessManagerInfo(ChannelConfig channelConfig) {
		try {
			String wabaId = channelConfig.getWacfb().getWabaId();
			String accessToken = channelConfig.getWacfb().getAccessToken();
			LOGGER.debug("Fetching Business Manager info for WABA ID: {}", wabaId);
			MapModel resp = restService.ajax(WA360Constants.META_WA_CLOUD_URL).path(wabaId)
					.queryParam("fields", "on_behalf_of_business_info").authBearer(accessToken).get().asMapModel();
			LOGGER.debug("Business Manager info response: {}", resp.toMap());
			return resp;
		} catch (ApiHttpServerException e) {
			LOGGER.error("Facebook API server error: {} - {}", e.getHttpStatus(), e.getResponse().getBody());
			return MapModel.from(e.getResponse().getBody()).put("error_code", e.getHttpStatus().value());
		} catch (ApiHttpException e) {
			LOGGER.error("Facebook API error: {}", e.getResponse().getBody());
			return MapModel.from(e.getResponse().getBody());
		}
	}

	/**
	 * 
	 * @param req
	 * @param channelConfig
	 * @return : Block WhatsApp user numbers
	 */
	public MapModel blockUsers(MapModel req, ChannelConfig channelConfig) {

		try {
			String endpoint = "/block_users";
			req.put("messaging_product", "whatsapp");

			MapModel resp = restService.ajax(WA360Constants.META_WA_CLOUD_URL)
					.path(channelConfig.getWacfb().getPhoneNumberId() + endpoint)
					.authBearer(channelConfig.getWacfb().getAccessToken()).post(req.toMap()).asMapModel();

			return resp;
		} catch (ApiHttpServerException e) {
			throw e;
		} catch (ApiHttpException e) {
			return MapModel.from(e.getResponse().getBody());
		}
	}

	/**
	 * 
	 * @param req
	 * @param channelConfig
	 * @return : Unblock WhatsApp user numbers
	 */

	@Autowired
	private RestTemplate restTemplate;

	public MapModel unBlockUsers(MapModel req, ChannelConfig channelConfig) {

		try {

			String endpoint = "/block_users";
			req.put("messaging_product", "whatsapp");
			String json = JsonUtil.toJson(req);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.add("Authorization", "Bearer " + channelConfig.getWacfb().getAccessToken());

			HttpEntity<String> requestEntity = new HttpEntity<>(json, headers);

			ResponseEntity<Map> resp = restTemplate.exchange(
					WA360Constants.META_WA_CLOUD_URL + channelConfig.getWacfb().getPhoneNumberId() + endpoint,
					HttpMethod.DELETE, requestEntity, Map.class);
			return MapModel.from(resp.getBody());
		} catch (ApiHttpServerException e) {
			throw e;
		} catch (ApiHttpException e) {
			return MapModel.from(e.getResponse().getBody());
		}
	}

	/**
	 * 
	 * @param req
	 * @param channelConfig
	 * @return Get list of blocked WhatsApp user numbers
	 */

	public MapModel getBlockUsers(MapModel req, ChannelConfig channelConfig) {

		try {
			String endpoint = "/block_users";
			MapModel resp = restService.ajax(WA360Constants.META_WA_CLOUD_URL)
					.path(channelConfig.getWacfb().getPhoneNumberId() + endpoint)
					.authBearer(channelConfig.getWacfb().getAccessToken()).get().asMapModel();

			return resp;
		} catch (ApiHttpServerException e) {
			throw e;
		} catch (ApiHttpException e) {
			return MapModel.from(e.getResponse().getBody());
		}
	}

	/** 
	 * 
	 */
	public String extractFormattedOrKey(Object keyValue, String fallbackKey) {
		if (keyValue == null)
			return null;

		String s = keyValue.toString().replaceAll("[{}]", "").replace(", ", "\n");

		Properties props = new Properties();
		try {
			props.load(new StringReader(s));
		} catch (Exception e) {
			return null;
		}
		// formattedName has priority
		return props.getProperty("formattedName", props.getProperty(fallbackKey));
	}

	public String getProfileInfo(String variable, String modelKey, MapModel model) {
		String value = "";
		Object keyValue = null;
		if (ArgUtil.is(variable) && variable.equalsIgnoreCase("name")) {
			keyValue = model.pathEntry(modelKey + variable).getValue();
			keyValue = extractFormattedOrKey(keyValue, "formattedName");
		} else if (ArgUtil.is(variable) && variable.contains("phone")) { // phones
			keyValue = model.pathEntry(modelKey + variable).getValue();
			keyValue = extractFormattedOrKey(keyValue, "phone");
		} else if (ArgUtil.is(variable) && variable.contains("email")) { // emails
			keyValue = model.pathEntry(modelKey + variable).getValue();
			keyValue = extractFormattedOrKey(keyValue, "email");
		} else {
			keyValue = model.pathEntry(modelKey + variable).getValue();
		}
		if (ArgUtil.isNotEmpty(keyValue)) {
			value = keyValue.toString().replaceAll("[\\[\\]]", "");
		} else {
			keyValue = model.pathEntry(modelKey + "additionalInfo." + variable).getValue();
			if (ArgUtil.is(keyValue)) {
				keyValue = checkDocument(keyValue);
				value = keyValue.toString().replaceAll("[\\[\\]]", "");
			}
		}
		return value;

	}

	private String checkDocument(Object keyValue) {
		String input = keyValue.toString().replaceAll("[{}]", "").replace(", ", "\n");
		String value = "";
		if (input.contains("fileType=DOCUMENT")) {
			// Convert string into map-like structure
			Map<String, String> map = Arrays.stream(input.replaceAll("[\\[\\]]", "").split("\\n")).map(String::trim)
					.filter(s -> s.contains("=")).map(s -> s.split("=", 2))
					.collect(Collectors.toMap(arr -> arr[0].trim(), arr -> arr[1].trim()));
			// Extract url
			value = map.get("url");
			return value;

		}
		return input;
	}

	public MapModel fetchMetaDetails(ChannelConfig channelConfig) {

		String url = WA360Constants.META_WA_CLOUD_URL + channelConfig.getWacfb().getPhoneNumberId();

		try {
			return restService.ajax(url).authBearer(channelConfig.getWacfb().getAccessToken()).get().asMapModel();
		} catch (Exception e) {
			LOGGER.error("Error fetching details for WABA {}", channelConfig.getWacfb().getWabaId(), e);
			return MapModel.createInstance().put("error", "Failed to fetch details");
		}
	}

	public MapModel getMessagingLimit(ChannelConfig channelConfig) {
		try {
			return restService.ajax(WA360Constants.META_WA_CLOUD_URL).path(channelConfig.getWacfb().getPhoneNumberId())
					.queryParam("fields", "whatsapp_business_manager_messaging_limit")
					.authBearer(channelConfig.getWacfb().getAccessToken()).get().asMapModel();
		} catch (Exception e) {
			LOGGER.error("Failed to fetch messaging limit for", channelConfig.getWacfb().getPhoneNumberId(), e);
			return null;

		}
	}

	/**
	 * Configure WhatsApp Call Settings for a phone number.
	 */
	public MapModel configureCallSettings(ChannelConfig channelConfig, Map<String, Object> callSettingsPayload) {
		try {
			String phoneNumberId = channelConfig.getWacfb().getPhoneNumberId();
			String accessToken = channelConfig.getWacfb().getAccessToken();

			LOGGER.info("Configuring call settings for Phone Number ID: {}", phoneNumberId);

			MapModel resp = restService.ajax(WA360Constants.META_WA_CLOUD_URL).path(phoneNumberId).path("/settings")
					.authBearer(accessToken).post(callSettingsPayload).asMapModel();

			LOGGER.info("Call settings response: {}", resp.toMap());
			return resp;
		} catch (ApiHttpServerException e) {
			LOGGER.error("Facebook API server error: {} - {}", e.getHttpStatus(), e.getResponse().getBody());
			return MapModel.from(e.getResponse().getBody()).put("error_code", e.getHttpStatus().value());
		} catch (ApiHttpException e) {
			LOGGER.error("Facebook API error: {}", e.getResponse().getBody());
			return MapModel.from(e.getResponse().getBody());
		} catch (Exception e) {
			LOGGER.error("Unexpected error configuring call settings: {}", e.getMessage());
			return MapModel.createInstance().put("error", e.getMessage());
		}
	}

	/**
	 * Get current webhook subscriptions for WABA
	 *
	 * @param channelConfig Channel configuration
	 * @param masterChannel Master channel with System User token
	 * @return Response with current subscriptions
	 */
	public MapModel getSubscribedApps(ChannelConfig channelConfig, ChannelConfig masterChannel) {

		try {
			String wabaId = channelConfig.getWacfb().getWabaId();
			String systemUserToken = null;

			if (ArgUtil.is(masterChannel) && ArgUtil.is(masterChannel.getWacfb())) {
				systemUserToken = masterChannel.getWacfb().getMasterSUAccessToken();
			}

			if (!ArgUtil.is(systemUserToken)) {
				LOGGER.error("System User token not available");
				return MapModel.createInstance().put("error", "System User token not available").put("success", false);
			}

			if (!ArgUtil.is(wabaId)) {
				LOGGER.error("WABA ID not available for channel: {}", channelConfig.getChannelId());
				return MapModel.createInstance().put("error", "WABA ID not configured").put("success", false);
			}

			MapModel resp = restService.ajax(WA360Constants.META_WA_CLOUD_URL).path(wabaId).path("/subscribed_apps")
					.authBearer(systemUserToken).get().asMapModel();

			return resp;

		} catch (ApiHttpServerException e) {
			LOGGER.error("Facebook API server error: {} - {}", e.getHttpStatus(), e.getResponse().getBody());
			return MapModel.from(e.getResponse().getBody()).put("error_code", e.getHttpStatus().value());

		} catch (ApiHttpException e) {
			LOGGER.error("Facebook API error: {}", e.getResponse().getBody());
			return MapModel.from(e.getResponse().getBody());

		} catch (Exception e) {
			LOGGER.error("Unexpected error getting subscribed apps: {}", e.getMessage());
			return MapModel.createInstance().put("error", e.getMessage());
		}
	}

	/**
	 * Unsubscribe app from WABA webhooks System User token identifies which app to
	 * unsubscribe
	 * 
	 * @param channelConfig - Channel configuration
	 * @param masterChannel - Master channel with System User token
	 * @return Response from Facebook API
	 */
	public MapModel deleteSubscribedApps(ChannelConfig channelConfig, ChannelConfig masterChannel) {
		try {
			String wabaId = channelConfig.getWacfb().getWabaId();
			String systemUserToken = null;

			if (ArgUtil.is(masterChannel) && ArgUtil.is(masterChannel.getWacfb())) {
				systemUserToken = masterChannel.getWacfb().getMasterSUAccessToken();
			}

			if (!ArgUtil.is(systemUserToken)) {
				LOGGER.error("System User token not available");
				return MapModel.createInstance().put("error", "System User token not available").put("success", false);
			}

			if (!ArgUtil.is(wabaId)) {
				LOGGER.error("WABA ID not available for channel: {}", channelConfig.getChannelId());
				return MapModel.createInstance().put("error", "WABA ID not configured").put("success", false);
			}

			LOGGER.debug("Deleting subscribed_apps for WABA: {}", wabaId);

			MapModel resp = restService.ajax(WA360Constants.META_WA_CLOUD_URL).path(wabaId).path("/subscribed_apps")
					.authBearer(systemUserToken).delete().asMapModel();

			return resp;

		} catch (ApiHttpServerException e) {
			LOGGER.error("Facebook API server error: {} - {}", e.getHttpStatus(), e.getResponse().getBody());
			return MapModel.from(e.getResponse().getBody()).put("error_code", e.getHttpStatus().value());
		} catch (ApiHttpException e) {
			LOGGER.error("Facebook API error: {}", e.getResponse().getBody());
			return MapModel.from(e.getResponse().getBody());
		} catch (Exception e) {
			LOGGER.error("Unexpected error deleting subscribed_apps: {}", e.getMessage());
			return MapModel.createInstance().put("error", e.getMessage());
		}
	}

	/**
	 * Subscribe app to WABA webhooks System User token identifies which app to
	 * subscribe
	 * 
	 * @param channelConfig - Channel configuration
	 * @param masterChannel - Master channel with System User token
	 * @return Response from Facebook API
	 */
	public MapModel subscribeApps(ChannelConfig channelConfig, ChannelConfig masterChannel) {
		try {
			String wabaId = channelConfig.getWacfb().getWabaId();
			String systemUserToken = null;

			if (ArgUtil.is(masterChannel) && ArgUtil.is(masterChannel.getWacfb())) {
				systemUserToken = masterChannel.getWacfb().getMasterSUAccessToken();
			}

			if (!ArgUtil.is(systemUserToken)) {
				LOGGER.error("System User token not available");
				return MapModel.createInstance().put("error", "System User token not available").put("success", false);
			}

			if (!ArgUtil.is(wabaId)) {
				LOGGER.error("WABA ID not available for channel: {}", channelConfig.getChannelId());
				return MapModel.createInstance().put("error", "WABA ID not configured").put("success", false);
			}

			LOGGER.debug("Subscribing app for WABA: {}", wabaId);

			MapModel resp = restService.ajax(WA360Constants.META_WA_CLOUD_URL).path(wabaId).path("/subscribed_apps")
					.authBearer(systemUserToken).post().asMapModel();

			LOGGER.debug("Subscribe apps response: {}", resp.toMap());
			return resp;

		} catch (ApiHttpServerException e) {
			LOGGER.error("Facebook API server error: {} - {}", e.getHttpStatus(), e.getResponse().getBody());
			return MapModel.from(e.getResponse().getBody()).put("error_code", e.getHttpStatus().value());
		} catch (ApiHttpException e) {
			LOGGER.error("Facebook API error: {}", e.getResponse().getBody());
			return MapModel.from(e.getResponse().getBody());
		} catch (Exception e) {
			LOGGER.error("Unexpected error subscribing apps: {}", e.getMessage());
			return MapModel.createInstance().put("error", e.getMessage());
		}
	}

}
