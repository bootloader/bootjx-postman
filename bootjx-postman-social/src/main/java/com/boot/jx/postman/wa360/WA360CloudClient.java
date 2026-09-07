package com.boot.jx.postman.wa360;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;

import com.boot.jx.api.ApiFieldError;
import com.boot.jx.api.ApiResponseUtil;
import com.boot.jx.dict.ContactType;
import com.boot.jx.dict.FileType;
import com.boot.jx.exception.ApiHttpExceptions.ApiHttpException;
import com.boot.jx.exception.ApiHttpExceptions.ApiHttpServerException;
import com.boot.jx.postman.PMConstants.CHANNEL_TYPE;
import com.boot.jx.postman.PostManException;
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
import com.boot.jx.postman.wa360.WA360Constants.OutBoundWrapperPaths;
import com.boot.jx.postman.wa360.WA360Constants.TmplComponent;
import com.boot.jx.rest.RestService;
import com.boot.model.MapModel;
import com.boot.model.MapModel.MapPathEntry;
import com.boot.utils.ArgUtil;
import com.boot.utils.CollectionUtil;
import com.boot.utils.Constants;
import com.boot.utils.JsonPath;
import com.boot.utils.StringUtils;

@Component
@ConnectorMapping(contactType = ContactType.WHATSAPP, channel = CHANNEL_TYPE.WA_360DC)
public class WA360CloudClient implements ChannelClient {

	@Autowired
	private RestService restService;

	@Retryable(value = ApiHttpServerException.class, maxAttempts = 3, backoff = @Backoff(delay = 3000))
	public OutboxMessage send(ChannelConfig channelConfig, OutboxMessage outboxMessage) {
		StringJoiner msgIds = new StringJoiner(",");

		if (ArgUtil.is(outboxMessage.getRawMessageFormat())) { /** for Moengage **/
			MapModel resp = sendTemplateRaw(channelConfig, outboxMessage);
			msgIds.add(getMessageId(resp));
		} else if (ArgUtil.is(outboxMessage.getTemplateExt())) {
			MapModel resp = sendTemplate(channelConfig, outboxMessage);
			msgIds.add(getMessageId(resp));
		} else {
			boolean isList = false;
			boolean isButton = false;
			boolean isCtaUrl = false;
			boolean isLocationRequest = false;
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

				List<TmplElement> allbuttons = options.entry("buttons").asList(TmplElement.class);

				for (TmplElement b : allbuttons) {
					if (ArgUtil.areEqual(b.getType(), TmplElement.TYPES.URL)) {
						bodyUrlAppend = bodyUrlAppend
								+ StringUtils.wrap("\n" + WA360Constants.componentButtonSubTypesIconLink + " *",
										StringUtils.trim(b.getLabel()), "*")
								+ "\n" + b.getUrl() + "\n" + StringUtils.wrap(" _", b.getDesc(), "_\n");
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
					MapModel resp = sendList(channelConfig, outboxMessage, buttons);
					msgIds.add(getMessageId(resp));
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
					MapModel resp = sendList(channelConfig, outboxMessage, newButtons);
					msgIds.add(getMessageId(resp));
				}
			} else if (isButton) {
				MapModel resp = sendButton(channelConfig, outboxMessage, buttons, "button");
				msgIds.add(getMessageId(resp));
			} else if (isCtaUrl) {
				MapModel resp = sendButton(channelConfig, outboxMessage, noButtons, "cta_url");
				msgIds.add(getMessageId(resp));
			} else if (isLocationRequest) {
				MapModel resp = sendButton(channelConfig, outboxMessage, noButtons, "location_request_message");
				msgIds.add(getMessageId(resp));
			} else if (isFlow) {
				MapModel resp = sendButton(channelConfig, outboxMessage, noButtons, "flow");
				msgIds.add(getMessageId(resp));
			} else {
				String textMessage = outboxMessage.getMessage();
				textMessage = checkAndSendMedia(channelConfig, outboxMessage, msgIds, textMessage);

				if (ArgUtil.is(textMessage)) {
					MapModel resp = sendText(channelConfig, outboxMessage);
					msgIds.add(getMessageId(resp));
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

				MapModel resp = sendMedia(channelConfig, outboxMessage, attachment);
				msgIds.add(getMessageId(resp));
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
					MapModel resp = send(req, channelConfig);
					msgIds.add(getMessageId(resp));
				}
			}

			if (contacts.size() > 0) {
				MapModel req = MapModel.createInstance()
						.put("messaging_product", outboxMessage.getContact().getContactType())
						.put("recipient_type", "individual").put("to", outboxMessage.contact().getCsid());
				req.put(OutBoundWrapperPaths.MESSAGE_TYPE, "contacts");
				req.put("contacts", contacts.list());
				textMessage = null;
				MapModel resp = send(req, channelConfig);
				msgIds.add(getMessageId(resp));
			}
		}
		return textMessage;
	}

	private MapModel sendTemplate(ChannelConfig channelConfig, OutboxMessage outboxMessage) {
		MapModel req = MapModel.createInstance().put("messaging_product", outboxMessage.getContact().getContactType())
				.put("recipient_type", "individual").put("to", outboxMessage.contact().getCsid());

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
					WA360CloudOutBoundMedia media = createMedia(lowerFormat, outboxMessage.getAttachments().get(0));
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
						String originalText = model.pathEntry(path).asString(Constants.BLANK);
						originalText = originalText.replaceAll("\n", "\\\\n");
						bodyComponent.parameter("text", originalText);

					}
					components.add(bodyComponent.build().map());
				}
			} else if ("BUTTONS".equals(extTemplateComponentType)) {
				List<Map<String, Object>> extTemplateComponentButtons = MapModel.from(extTemplateComponent)
						.keyEntry("buttons").asListOfMap();
				List<List<Map<String, Object>>> buttonsParametersVars = varMap.entry("buttons").asListListOfMap();

				for (int i = 0; i < extTemplateComponentButtons.size(); i++) {
					Map<String, Object> extTemplateComponentButton = extTemplateComponentButtons.get(i);
					List<Map<String, Object>> buttonParameterVar = CollectionUtil.getArray(buttonsParametersVars, i);
					if (ArgUtil.is(buttonParameterVar)) {
						String buttonType = (String) extTemplateComponentButton.get("type");
						if ("URL".equals(buttonType)) {
							for (Map<String, Object> buttonParameter : buttonParameterVar) {
								if (buttonParameter.containsKey("path")) {
									String path = (String) buttonParameter.get("path");
									TmplComponent buttonComponent = TmplComponent.createInstance().button("url", i);
									buttonComponent.parameter("text", model.pathEntry(path).asString());
									components.add(buttonComponent.build().map());
								}
							}
						} else if ("QUICK_REPLY".equals(buttonType)) {
							for (Map<String, Object> buttonParameter : buttonParameterVar) {
								if (buttonParameter.containsKey("path")) {
									String path = (String) buttonParameter.get("path");
									String code = model.pathEntry(path).asString();
									TmplComponent buttonComponent = TmplComponent.createInstance().button("quick_reply",
											i);
									buttonComponent.parameter("payload", code);
									components.add(buttonComponent.build().map());
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
						String buttonType = (String) extTemplateComponentButton.get("type");
						List<TmplElement> buttons = outboxMessage.optionActionButtons();
						TmplElement button = CollectionUtil.getArray(buttons, i);
						if (ArgUtil.is(button) && "QUICK_REPLY".equals(button.getType())
								&& "QUICK_REPLY".equals(buttonType)) {
							TmplComponent buttonComponent = TmplComponent.createInstance().button("quick_reply", i);
							buttonComponent.parameter("payload", "reply_id:" + button.getCode());
							components.add(buttonComponent.build().map());
						} else if ("FLOW".equals(buttonType)) {
							TmplComponent buttonComponent = TmplComponent.createInstance().button("flow", i);
							buttonComponent.parameter("action",
									MapModel.createInstance().put("flow_token", outboxMessage.getMessageId()));
							components.add(buttonComponent.build().map());
						}
					}
				}

			}

		}

		req.put(OutBoundWrapperPaths.TEMPLATE_COMPONENTS, components.list());
		return send(req, channelConfig);
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

	private WA360CloudOutBoundMedia createMedia(String mediaType, Attachment attachment) {
		WA360CloudOutBoundMedia wa360OutBoundMedia = new WA360CloudOutBoundMedia();
		wa360OutBoundMedia.setFilename(ArgUtil.nonEmpty(attachment.getMediaCaption(), attachment.getMediaName()));
		wa360OutBoundMedia.setCaption(attachment.getMediaCaption());
		wa360OutBoundMedia.setLink(attachment.getMediaURL());
		if (mediaType.equalsIgnoreCase("image") || mediaType.equalsIgnoreCase("video")) {
			wa360OutBoundMedia.setFilename(null);
		}
		return wa360OutBoundMedia;
	}

	private MapModel sendText(ChannelConfig channelConfig, OutboxMessage outboxMessage) {
		MapModel req = MapModel.createInstance().put("messaging_product", outboxMessage.getContact().getContactType())
				.put("recipient_type", "individual").put("to", outboxMessage.contact().getCsid());
		req.put(OutBoundWrapperPaths.MESSAGE_TYPE, "text");
		String originalText = outboxMessage.getMessage();
		outboxMessage.setMessage(originalText);
		req.put(OutBoundWrapperPaths.MESSAGE_TEXT_BODY,
				StringUtils.wrap("*", outboxMessage.getSubject(), "*\n") + outboxMessage.getMessage());
		return send(req, channelConfig);
	}

	private MapModel sendMedia(ChannelConfig channelConfig, OutboxMessage outboxMessage, Attachment attachment) {
		MapModel req = MapModel.createInstance().put("messaging_product", outboxMessage.getContact().getContactType())
				.put("recipient_type", "individual").put("to", outboxMessage.contact().getCsid());

		WA360CloudOutBoundMedia wa360OutBoundMedia = new WA360CloudOutBoundMedia();
		wa360OutBoundMedia.setCaption(ArgUtil.nonEmpty(attachment.getMediaCaption(), outboxMessage.getSubject()));
		wa360OutBoundMedia.setLink(attachment.getMediaURL());
		wa360OutBoundMedia.setFilename(attachment.getMediaName());

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

		return send(req, channelConfig);
	}

	private MapModel sendList(ChannelConfig channelConfig, OutboxMessage outboxMessage, List<TmplElement> buttons) {
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
		return send(req, channelConfig);
	}

	private MapModel sendButton(ChannelConfig channelConfig, OutboxMessage outboxMessage, List<TmplElement> buttons,
			String type) {
		MapModel req = MapModel.createInstance().put("messaging_product", outboxMessage.getContact().getContactType())
				.put("recipient_type", "individual").put("to", outboxMessage.contact().getCsid());

		req.put(OutBoundWrapperPaths.MESSAGE_TYPE, "interactive");
		req.put(new JsonPath("/interactive/type"), type);

		if (ArgUtil.is(outboxMessage.getAttachments())) {
			MapModel intr = MapModel.createInstance();
			Attachment attachment = outboxMessage.getAttachments().get(0);
			WA360CloudOutBoundMedia wa360OutBoundMedia = new WA360CloudOutBoundMedia();
			// wa360OutBoundMedia.setCaption(ArgUtil.nonEmpty(attachment.getMediaCaption(),
			// outboxMessage.getSubject()));
			wa360OutBoundMedia.setLink(attachment.getMediaURL());
			wa360OutBoundMedia.setFilename(attachment.getMediaName());
			if (ArgUtil.areEqual(attachment.getMediaType(), FileType.IMAGE.toString())) {
				intr.put(OutBoundWrapperPaths.MESSAGE_TYPE, "image");
				wa360OutBoundMedia.setFilename(null);
				intr.put("image", wa360OutBoundMedia);
			} else if (ArgUtil.areEqual(attachment.getMediaType(), FileType.VIDEO.toString())) {
				intr.put(OutBoundWrapperPaths.MESSAGE_TYPE, "video");
				wa360OutBoundMedia.setFilename(null);
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

		if (ArgUtil.is(outboxMessage.getFooter())) {
			req.put(OutBoundWrapperPaths.INTERACTIVE_FOOTER_TEXT,
					ArgUtil.parseAsString(outboxMessage.getFooter(), Constants.BLANK));
		}

		req.put(OutBoundWrapperPaths.INTERACTIVE_ACTION_BUTTON, "menu");

		if ("button".equalsIgnoreCase(type)) {
			List<Object> rows = new ArrayList<Object>();
			for (TmplElement button : buttons) {
				if (ArgUtil.is(button.getCode())) {
					rows.add(MapModel.createInstance().put("type", "reply")
							.put(OutBoundWrapperPaths.INTERACTIVE_ACTION_REPLY_ID,
									StringUtils.substring(button.getCode(), 256))
							.put(OutBoundWrapperPaths.INTERACTIVE_ACTION_REPLY_TITLE,
									StringUtils.substring(button.getLabel(), 20))
							.toMap());
				} else {
					rows.add(MapModel.createInstance().put("type", "reply")
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
							.put("url", button.getUrl()).toMap());
		} else if ("location_request_message".equalsIgnoreCase(type)) {
			req.put(OutBoundWrapperPaths.INTERACTIVE_ACTION_NAME, "send_location");
		} else if ("flow".equalsIgnoreCase(type)) {
			TmplElement button = buttons.get(0);
			req.put(OutBoundWrapperPaths.INTERACTIVE_ACTION_NAME, "flow");

			req.put(OutBoundWrapperPaths.INTERACTIVE_ACTION_PARAMATERS, MapModel.createInstance()
					.put("flow_message_version", "3").put("flow_token", outboxMessage.getMessageId())
					.put("flow_id", button.getUid()).put("flow_cta", button.getLabel())
					.put("flow_action", StringUtils.toLowerCase(button.getAction().toLowerCase()))
					.put("flow_action_payload", MapModel.createInstance().put("screen", button.getCode()).toMap())
					.toMap());
		}

		return send(req, channelConfig);
	}

	public MapModel send(MapModel req, ChannelConfig channelConfig) {

		try {
			MapModel resp = restService.ajax(WA360Constants.BASE_CLOUD_URL).path("messages")
					.header(WA360Constants.D360_CLOUD_API_KEY, channelConfig.getWa360dc().getApiKey()).post(req.toMap())
					.asMapModel();

			return resp;
		} catch (ApiHttpServerException e) {
			throw e;
			// return
			// MapModel.from(e.getResponse().getBody()).put(OutBoundWrapperPaths.RESPONSE_ERROR_CODE,
			// e.getHttpStatus().value());
		} catch (ApiHttpException e) {
			return MapModel.from(e.getResponse().getBody());
		}
	}

	private String getMessageId(MapModel resp) {
		String id = resp.entry(OutBoundWrapperPaths.RESPONSE_MSG_ID).asString();
		String errorCode = resp.entry(OutBoundWrapperPaths.RESPONSE_ERROR_CODE).asString();
		String errorCodeLegacy = resp.entry(OutBoundWrapperPaths.RESPONSE_ERRORS_CODE).asString();
		if (ArgUtil.is(errorCode) || ArgUtil.is(errorCodeLegacy) || !ArgUtil.is(id)) {
			if (ArgUtil.is(errorCodeLegacy)) {
				return getMessageIdLegacy(resp, id, errorCodeLegacy);
			} else {
				return getMessageIdCloud(resp, id, errorCode);
			}
		}
		return id;
	}

	private String getMessageIdCloud(MapModel resp, String messageId, String errorCode) {
		String errorTitle = resp.entry(OutBoundWrapperPaths.RESPONSE_ERROR_MSG).orKeyEntry("error").asString();
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
			MapModel resp = restService.ajax(WA360Constants.BASE_CLOUD_URL).path("v1/contacts")
					.header(WA360Constants.D360_CLOUD_API_KEY, channelConfig.getWa360dc().getApiKey())
					.post(MapModel.createInstance().put("blocking", "wait")
							.put(OutBoundWrapperPaths.FETCH_CONTACTS_DETAILS, contact).toMap())
					.asMapModel();
			return resp.path(OutBoundWrapperPaths.FETCH_CONTACTS_DETAILS).asMapModel();
		} catch (ApiHttpServerException e) {
			return MapModel.from(e.getResponse().getBody()).put(OutBoundWrapperPaths.RESPONSE_ERRORS_CODE,
					e.getHttpStatus().value());
		} catch (ApiHttpException e) {
			return MapModel.from(e.getResponse().getBody());
		}
	}

	public MapModel fetchTemplates(ChannelConfig channelConfig) {
		MapModel resp = restService.ajax(WA360Constants.BASE_CLOUD_URL).path("v1/configs/templates")
				.header(WA360Constants.D360_CLOUD_API_KEY, channelConfig.getWa360dc().getApiKey()).get().asMapModel();
		return resp;
	}

	public MapModel deleteTemplates(ChannelConfig channelConfig, String templateName) {
		MapModel resp = restService.ajax(WA360Constants.BASE_CLOUD_URL).path("v1/configs/templates/{templateName}")
				.header(WA360Constants.D360_CLOUD_API_KEY, channelConfig.getWa360dc().getApiKey())
				.pathParam("templateName", templateName).delete().asMapModel();
		return resp;
	}

	public MapModel updateTemplates(ChannelConfig channelConfig, MapModel req) {
		try {
			String templateName = req.getString("name");
			MapModel resp = restService.ajax(WA360Constants.BASE_CLOUD_URL).path("v1/configs/templates/{templateName}")
					.header(WA360Constants.D360_CLOUD_API_KEY, channelConfig.getWa360dc().getApiKey())
					.pathParam("templateName", templateName).post(req.toMap()).asMapModel();

			return resp;
		} catch (HttpStatusCodeException | ApiHttpException e) {
			if (e instanceof HttpStatusCodeException)
				ApiResponseUtil.addError(((HttpStatusCodeException) e).getResponseBodyAsString());
			else
				ApiResponseUtil.addError(((ApiHttpException) e));
			throw e;
		}
	}

	public MapModel createTemplates(ChannelConfig channelConfig, MapModel req) {
		try {
			MapModel resp = restService.ajax(WA360Constants.BASE_CLOUD_URL).path("v1/configs/templates")
					.header(WA360Constants.D360_CLOUD_API_KEY, channelConfig.getWa360dc().getApiKey()).post(req.toMap())
					.asMapModel();

			return resp;
		} catch (HttpStatusCodeException | ApiHttpException e) {
			if (e instanceof HttpStatusCodeException)
				ApiResponseUtil.addError(((HttpStatusCodeException) e).getResponseBodyAsString());
			else
				ApiResponseUtil.addError(((ApiHttpException) e));
			throw e;
		}
	}

	public String getMediaUrl(ChannelConfig channelConfig, String url) {

		if (url.contains("mid=")) {
			String mid = url.split("mid=")[1].split("&")[0];
			url = WA360Constants.MEDIA_CLOUD_URL(mid);
		}

		MapModel resp = restService.ajax(url.replace("/v1/media/", "/"))
				.header(WA360Constants.D360_CLOUD_API_KEY, channelConfig.getWa360dc().getApiKey()).acceptJson().get()
				.asMapModel();
		return resp.getString("url").replace("https://lookaside.fbsbx.com", WA360Constants.BASE_CLOUD_URL);
	}

	/** Call new metod to post msg directly to waba API **/
	public MapModel sendTemplateRaw(ChannelConfig channelConfig, OutboxMessage outboxMessage) {
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
		return send(req, channelConfig);
	}

	@Override
	public MapModel listOfFlows(ChannelConfig channelConfig) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MapModel flowsAssets(String flowId, ChannelConfig channelConfig) {
		// TODO Auto-generated method stub
		return null;
	}

}
