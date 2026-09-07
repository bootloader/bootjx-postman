package com.boot.jx.connectors;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.boot.jx.dict.ContactType;
import com.boot.jx.dict.FileFormat;
import com.boot.jx.dict.FileType;
import com.boot.jx.exception.AmxApiException;
import com.boot.jx.model.CommonFile;
import com.boot.jx.model.CommonFileStream;
import com.boot.jx.mongo.CommonMongoTemplate;
import com.boot.jx.postman.PMConstants.CHANNEL_TYPE;
import com.boot.jx.postman.PMConstants.MESSAGE_COMPOSE_TYPE;
import com.boot.jx.postman.PMConstants.MESSAGE_FORMAT_TYPE;
import com.boot.jx.postman.PMEnvironment.PMClientConfig;
import com.boot.jx.postman.client.PMFileStoreClient;
import com.boot.jx.postman.doc.ChatContactDoc;
import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.doc.CustomerProfileDoc;
import com.boot.jx.postman.doc.MessageDoc;
import com.boot.jx.postman.doc.config.ChannelConfigLogger;
import com.boot.jx.postman.model.Attachment;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.Message;
import com.boot.jx.postman.model.Message.Status;
import com.boot.jx.postman.model.MessageBoxEvent;
import com.boot.jx.postman.model.MessageReport;
import com.boot.jx.postman.model.MessageReport.MessageReportError;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.model.outbound.OutBoundMsgBasic.ApiOutBoundMsg;
import com.boot.jx.postman.pbook.PBAddress;
import com.boot.jx.postman.pbook.PBDate;
import com.boot.jx.postman.pbook.PBEmail;
import com.boot.jx.postman.pbook.PBLocation;
import com.boot.jx.postman.pbook.PBName;
import com.boot.jx.postman.pbook.PBPhone;
import com.boot.jx.postman.pbook.PBSocial;
import com.boot.jx.postman.pbook.PBVCard;
import com.boot.jx.postman.pbook.PBWebsite;
import com.boot.jx.postman.pbook.PBWork;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.plugin.ChannelPluginProvider.ConnectorMapping;
import com.boot.jx.postman.plugin.WA360Plugin;
import com.boot.jx.postman.plugin.WA360Plugin.WA360ConfigDetails;
import com.boot.jx.postman.query.ChatContactQuery;
import com.boot.jx.postman.query.WABAConversationQuery;
import com.boot.jx.postman.wa360.WA360Client;
import com.boot.jx.postman.wa360.WA360Constants;
import com.boot.jx.postman.wa360.WA360Constants.InBoundWrapperPaths;
import com.boot.jx.postman.wa360.WA360InboundMedia;
import com.boot.jx.rest.RestService;
import com.boot.jx.utils.PostManUtil;
import com.boot.model.MapModel;
import com.boot.model.MapModel.MapPathEntry;
import com.boot.utils.ArgUtil;
import com.boot.utils.Constants;
import com.boot.utils.JsonPath;
import com.boot.utils.JsonUtil;
import com.boot.utils.Urly;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

@Component
@ConnectorMapping(contactType = ContactType.WHATSAPP, channel = CHANNEL_TYPE.WA_360D)
public class WA360Connector extends AbstractConnector<WA360ConfigDetails, WA360Plugin> {

	public static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();

	private static final Logger LOGGER = LoggerFactory.getLogger(WA360Connector.class);
	@Autowired
	private RestService restService;

	@Autowired
	private PMFileStoreClient pmFileStoreClient;

	@Autowired
	private WA360Client wa360Client;

	@Autowired
	private PMClientConfig pmClientConfig;

	@Autowired
	private CommonMongoTemplate commonMongoTemplate;

	@Override
	public void onChannelUpdate(ChannelConfig channelConfig, ChannelConfigLogger channelConfigLogger) {
		String webhookUrl = pmClientConfig.getWebhookUrl(channelConfig, null, null);
		restService.ajax(WA360Constants.BASE_URL).path("v1/configs/webhook")
				.header(WA360Constants.D360_API_KEY, channelConfig.getWa360d().getApiKey())
				.post(MapModel.createInstance().put("url", webhookUrl).toMap()).asMap();
	}

	public OutboxMessage initSession(ChatSessionDoc session, InboxMessage inboxMessage) {
		ChatContactQuery contactQuery = messageContext.contact();
		ChatContactDoc chatContactDoc = contactQuery.getDoc();
		if (chatContactDoc.phone() == null) {
			contactQuery.setPhone(inboxMessage.contact().phone());
		}
		if (!chatContactDoc.getPhoneVerified()) {
			contactQuery.setPhoneVerified(true);
		}
		String user_input_type = this.context().session().getEntry("session_init_user_input_type").asString();
		if (ArgUtil.is(user_input_type)) {
			if (user_input_type.equals("name")) {
				contactQuery.setInfoName(inboxMessage.getMessage());
			}
			if (user_input_type.equals("email")) {
				String email = inboxMessage.getMessage();
				if (isValidEmail(email)) {
					contactQuery.setInfoEmail(email);
				} else {
					return (OutboxMessage) inboxMessage.replyMessage("Please enter a valid email address.");
				}
			}

			if (user_input_type.equals("phone")) {
				contactQuery.setInfoPhone(inboxMessage.getMessage());
			}

		}
		ChannelConfig channel = getChannelConfig(inboxMessage);

		if (ArgUtil.is(channel.getWa360d())) {
			if (channel.getWa360d().isPromptName()) {
				if (ArgUtil.isEmpty(chatContactDoc.info().getName())) {
					this.context().session().put("session_init_user_input_type", "name");
					return (OutboxMessage) inboxMessage.replyMessage("Please enter your name");
				}

			}

			if (channel.getWa360d().isPromptEmail()) {
				if (ArgUtil.isEmpty(chatContactDoc.info().getEmail())) {
					this.context().session().put("session_init_user_input_type", "email");
					return (OutboxMessage) inboxMessage.replyMessage("Please enter your email");
				}

			}

			if (channel.getWa360d().isPromptPhone()) {
				if (ArgUtil.isEmpty(chatContactDoc.info().getPhone())) {
					this.context().session().put("session_init_user_input_type", "phone");
					return (OutboxMessage) inboxMessage.replyMessage("Please enter your phone");
				}

			}
		}

		return null;
	}

	private boolean isValidEmail(String email) {
		String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
		Pattern pattern = Pattern.compile(emailRegex);
		return pattern.matcher(email).matches();
	}

	@Override
	protected CustomerProfileDoc findProfile(ChatContactDoc chatContactDoc) {
		return contactStore.findProfileByPhone(chatContactDoc.phone());
	}

	public InboxMessage toInboxMessage(ChannelConfig channelConfig, MapModel map) {

		// Create Default Message from Channel
		InboxMessage inboxMessage = this.createInboxMessage(channelConfig);

		// Set Contact info
		String contactNumber = map.entry(InBoundWrapperPaths.CONTACT_NUMBER).asString();
		String contactName = map.entry(InBoundWrapperPaths.CONTACT_NAME).asString();

		inboxMessage.contact().setCsid(contactNumber);
		inboxMessage.contact().setName(contactName);
		inboxMessage.contact().phone(contactNumber);

		// Set Additional info
		inboxMessage.setFrom(contactNumber);
		inboxMessage.setFromName(contactName);
		inboxMessage.to().add(channelConfig.getLane());

		// Extract Message Details
		inboxMessage.setMessageIdExt(map.entry(InBoundWrapperPaths.MESSAGE_ID).asString());

		String messageType = map.entry(InBoundWrapperPaths.MESSAGE_TYPE).asString();

		if ("text".equals(messageType)) {
			inboxMessage.setFormatType(MESSAGE_FORMAT_TYPE.TEXT);
			inboxMessage.setMessage(map.entry(InBoundWrapperPaths.MESSAGE_TEXT).asString());
		} else if ("interactive".equals(messageType)) {
			inboxMessage.setFormatType(MESSAGE_FORMAT_TYPE.TEXT);
			String interactiveType = map.entry(InBoundWrapperPaths.INTERACTIVE_TYPE).asString();
			String replyId = null;
			if ("button_reply".equals(interactiveType)) {
				replyId = map.entry(InBoundWrapperPaths.INTERACTIVE_BUTTON_ID).asString();
				inboxMessage.form().put("reply_id", replyId);
				inboxMessage.form().put("reply_title",
						map.entry(InBoundWrapperPaths.INTERACTIVE_BUTTON_REPLY).asString());
				inboxMessage.setMessage(ArgUtil.parseAsString(inboxMessage.form().get("reply_title"), Constants.BLANK));
			} else if ("list_reply".equals(interactiveType)) {
				replyId = map.entry(InBoundWrapperPaths.INTERACTIVE_LIST_ID).asString();
				inboxMessage.form().put("reply_id", replyId);
				inboxMessage.form().put("reply_title",
						map.entry(InBoundWrapperPaths.INTERACTIVE_LIST_REPLY).asString());
				inboxMessage.form().put("reply_desc", map.entry(InBoundWrapperPaths.INTERACTIVE_LIST_DESC).asString());
			} else if ("nfm_reply".equals(interactiveType)) {
				String responseJsonString = map.entry(InBoundWrapperPaths.INTERACTIVE_NFM_REPLY_RESPONSE_JSON)
						.asString();
				// replyJsonMap.put("response_json", responseJsonString);
				Map<String, Object> replyJsonMap = JsonUtil.fromJsonToMap(responseJsonString);
				inboxMessage.form().put("reply_json", replyJsonMap);
				inboxMessage.form().put("reply_title",
						map.entry(InBoundWrapperPaths.INTERACTIVE_NFM_REPLY_BODY).asString());
			}

			inboxMessage.setMessage(ArgUtil.parseAsString(inboxMessage.form().get("reply_title"), Constants.BLANK));
		} else if ("button".equals(messageType)) {
			inboxMessage.form().put("reply_title", map.entry(InBoundWrapperPaths.SIMPLE_BUTTON_REPLY).asString());
			String reply_payload = map.entry(InBoundWrapperPaths.SIMPLE_BUTTON_PAYLOAD).asString();
			inboxMessage.form().put("reply_payload", reply_payload);
			if (ArgUtil.is(reply_payload) && reply_payload.startsWith("reply_id:")) {
				String reply_id = reply_payload.replaceFirst("reply_id:", "");
				inboxMessage.form().put("reply_id", reply_id);
			}
			inboxMessage.setMessage(ArgUtil.parseAsString(inboxMessage.form().get("reply_title"), Constants.BLANK));
		} else if ("image".equals(messageType)) {
			inboxMessage.setFormatType(MESSAGE_FORMAT_TYPE.IMAGE);
			formatMedia(inboxMessage, map, channelConfig, InBoundWrapperPaths.IMAGE, FileType.IMAGE);
		} else if ("document".equals(messageType)) {
			inboxMessage.setFormatType(MESSAGE_FORMAT_TYPE.DOCUMENT);
			formatMedia(inboxMessage, map, channelConfig, InBoundWrapperPaths.DOCUMENT, FileType.DOCUMENT);
		} else if ("audio".equals(messageType)) {
			inboxMessage.setFormatType(MESSAGE_FORMAT_TYPE.AUDIO);
			formatMedia(inboxMessage, map, channelConfig, InBoundWrapperPaths.AUDIO, FileType.AUDIO);
		} else if ("voice".equals(messageType)) {
			inboxMessage.setFormatType(MESSAGE_FORMAT_TYPE.VOICE);
			formatMedia(inboxMessage, map, channelConfig, InBoundWrapperPaths.VOICE, FileType.AUDIO);
		} else if ("video".equals(messageType)) {
			inboxMessage.setFormatType(MESSAGE_FORMAT_TYPE.VIDEO);
			formatMedia(inboxMessage, map, channelConfig, InBoundWrapperPaths.VIDEO, FileType.VIDEO);
		} else if ("sticker".equals(messageType)) {
			inboxMessage.setFormatType(MESSAGE_FORMAT_TYPE.STICKER);
			formatMedia(inboxMessage, map, channelConfig, InBoundWrapperPaths.STICKER, FileType.IMAGE);
		} else if ("contacts".equals(messageType)) {
			inboxMessage.setFormatType(MESSAGE_FORMAT_TYPE.CONTACTS);
			extractContacts(map, inboxMessage);
		} else if ("location".equals(messageType)) {
			inboxMessage.setFormatType(MESSAGE_FORMAT_TYPE.LOCATION);
			PBLocation pbLocation = new PBLocation();
			pbLocation.setName(map.pathEntry("messages/[0]/location/name").asString());
			pbLocation.setAddress(map.pathEntry("messages/[0]/location/address").asString());
			pbLocation.setLatitude(map.pathEntry("messages/[0]/location/latitude").asString());
			pbLocation.setLongitude(map.pathEntry("messages/[0]/location/longitude").asString());
			pbLocation.setUrl(map.pathEntry("messages/[0]/location/url").asString());
			inboxMessage.vccards().add(new PBVCard().locations(pbLocation));
		}

		inboxMessage.setFormatSubType(messageType);

		String replyIdExt = map.entry(InBoundWrapperPaths.CONTEXT_ID).asString();
		if (ArgUtil.is(replyIdExt)) {
			inboxMessage.setReplyIdExt(replyIdExt);
		}

		inboxMessage.setOriginalMessage(map.map());

		return inboxMessage;
	}

	private void extractContacts(MapModel map, InboxMessage inboxMessage) {
		List<Map<String, Object>> cards = map.entry(InBoundWrapperPaths.VCARDS).asListOfMap();
		for (Map<String, Object> card : cards) {
			MapModel cardMap = MapModel.from(card);

			PBVCard pbContact = new PBVCard();
			PBName pbName = new PBName();
			pbName.setFirstName(cardMap.pathEntry("/name/first_name").asString());
			pbName.setLastName(cardMap.pathEntry("/name/last_name").asString());
			pbName.setFormattedName(cardMap.pathEntry("/name/formatted_name").asString());
			pbContact.setName(pbName);

			List<Map<String, String>> phones = cardMap.keyEntry("phones").asListOfMapT();
			for (Map<String, String> phone : phones) {
				PBPhone pbPhone = new PBPhone();
				pbPhone.setType(phone.get("type"));
				pbPhone.setWhatsAppId(phone.get("wa_id"));
				pbPhone.setPhone(phone.get("phone"));
				pbContact.phones().add(pbPhone);
			}

			List<Map<String, String>> addresses = cardMap.keyEntry("addresses").asListOfMapT();
			for (Map<String, String> address : addresses) {
				PBAddress pbAddress = new PBAddress();
				pbAddress.setType(address.get("type"));
				pbAddress.setCity(address.get("city"));
				pbAddress.setCountry(address.get("country"));
				pbAddress.setCountryCode(address.get("country_code"));
				pbAddress.setState(address.get("state"));
				pbAddress.setStreet(address.get("street"));
				pbAddress.setZip(address.get("zip"));
				pbContact.addresses().add(pbAddress);
			}

			List<Map<String, String>> emails = cardMap.keyEntry("emails").asListOfMapT();
			for (Map<String, String> email : emails) {
				PBEmail pbEmail = new PBEmail();
				pbEmail.setType(email.get("type"));
				pbEmail.setEmail(email.get("email"));
				pbContact.emails().add(pbEmail);
			}

			List<Map<String, String>> ims = cardMap.keyEntry("ims").asListOfMapT();
			for (Map<String, String> im : ims) {
				PBSocial pbSocial = new PBSocial();
				pbSocial.setService(im.get("service"));
				pbSocial.setUserid(im.get("user_id"));
				pbContact.ims().add(pbSocial);
			}

			List<Map<String, String>> urls = cardMap.keyEntry("urls").asListOfMapT();
			for (Map<String, String> url : urls) {
				PBWebsite pnWebsite = new PBWebsite();
				pnWebsite.setUrl(url.get("url"));
				pnWebsite.setType(url.get("type"));
				pbContact.urls().add(pnWebsite);
			}

			MapPathEntry workCompany = cardMap.pathEntry("/org/company");
			MapPathEntry workDepartment = cardMap.pathEntry("/org/department");
			MapPathEntry workTitle = cardMap.pathEntry("/org/title");

			if (workCompany.exists() || workDepartment.exists() || workTitle.exists()) {
				PBWork pbWork = new PBWork();
				pbWork.setCompany(workCompany.asString());
				pbWork.setDepartment(workDepartment.asString());
				pbWork.setTitle(workTitle.asString());
				pbContact.work().add(pbWork);
			}

			MapPathEntry birthday = cardMap.keyEntry("birthday");
			if (birthday.exists()) {
				PBDate pbDate = new PBDate();
				pbDate.setType("birthday");
				pbDate.setDate(birthday.asString());
				pbContact.dates().add(pbDate);
			}

			inboxMessage.vccards().add(pbContact);
		}
	}

	private void formatMedia(InboxMessage inboxMessage, MapModel map, ChannelConfig channelConfig, JsonPath path,
			FileType fileType) {
		try {
			WA360InboundMedia media = map.entry(path).as(WA360InboundMedia.class);
			CommonFileStream srcFile = new CommonFileStream().url(WA360Constants.MEDIA_URL(media.getId()))
					.fileType(fileType).format(FileFormat.from(media.getMimeType()))
					.header(WA360Constants.D360_API_KEY, channelConfig.getWa360d().getApiKey())
					.name(ArgUtil.nonEmpty(media.getFilename(), media.getCaption()));

			CommonFile dstFile = pmFileStoreClient.uploadSessionFileAsync(srcFile,
					PostManUtil.createContactId(inboxMessage), inboxMessage.getMessageIdExt());
			inboxMessage.attachment(new Attachment().mediaURL(dstFile.getUrl()).mediaType(dstFile.getFileType())
					.mediaSrc(srcFile.getUrl()).mediaCaption(media.getCaption()).mediaName(media.getFilename())
					.mediaMimeType(media.getMimeType()));
		} catch (IOException e) {
			logManager.error(inboxMessage, e);
		}
	}

	@Override
	public CommonFile reloadMedia(ChannelConfig channelConfig, MessageDoc msg, Attachment attachment)
			throws FileNotFoundException, IOException {
		CommonFileStream srcFile = new CommonFileStream().url(attachment.getMediaSrc())
				// .fileType(attachment.getMediaType())
				.format(FileFormat.from(attachment.getMediaMimeType()))
				.header(WA360Constants.D360_API_KEY, channelConfig.getWa360d().getApiKey())
				.name(ArgUtil.nonEmpty(attachment.getMediaName(), attachment.getMediaCaption()));

		File fileb = Urly.parse(attachment.getMediaURL()).toFile();

		CommonFile dstFile = new CommonFile().url(attachment.getMediaURL()).path(fileb.getParent())
				.fileType(ArgUtil.parseAsEnumT(attachment.getMediaType(), FileType.class));
		return pmFileStoreClient.commitSessionFileSync(srcFile, dstFile);
	}

	@Override
	public Status onSend(ChannelConfig channelConfig, ChatContactDoc chatContactDoc, OutboxMessage outboxMessage,
			ApiOutBoundMsg outBoundMsg) {
		try {

			boolean isValidContact = true;
			if (outboxMessage.messageMetaWrapper().composeTypeIs(MESSAGE_COMPOSE_TYPE.SEND_CODE)) {
				isValidContact = optin(channelConfig, chatContactDoc);
			}
			if (isValidContact) {
				wa360Client.send(channelConfig, outboxMessage);
				outboxMessage.updateStatus(OutboxMessage.Status.SENT);
				return Message.Status.SENT;
			} else {
				outboxMessage.logs().add(String.format("Invalid Contact for %s", chatContactDoc));
				outboxMessage.updateStatus(OutboxMessage.Status.SENT_ERR);
				return Message.Status.SENT_ERR;
			}

		} catch (AmxApiException e) {
			outboxMessage.updateStatus(OutboxMessage.Status.SENT_ERR);
			outboxMessage.logs().add(((AmxApiException) e).getErrorKey());
			outboxMessage.logs().add(e.getMessage());
			return Message.Status.SENT_ERR;
		}
	}

	private MessageReport toMessageReport(ChannelConfig channelConfig, MapModel requestMap) {
		MessageReport report = this.createMessageReport(channelConfig);
		String csid = requestMap.path(WA360Constants.InBoundWrapperPaths.STATUS_RECIPIENT).asString();
		if (!ArgUtil.is(csid)) {
			csid = requestMap.getString("recipient_id");
		}
		report.contact().setCsid(csid);
		report.setChangeStamp(requestMap.getLong("timestamp", 0L) * 1000);
		report.setMessageIdExt(requestMap.getString("id"));

		String status = requestMap.getString("status");

		if ("sent".equals(status)) {
			report.setStatus(Status.SENTX);
		} else if ("delivered".equals(status)) {
			report.setStatus(Status.DLVRD);
		} else if ("read".equals(status)) {
			report.setStatus(Status.READ);
		} else if ("deleted".equals(status)) {
			report.setStatus(Status.DELTD);
		} else if ("failed".equals(status)) {
			report.setStatus(Status.FAILD);

			String errorCode = requestMap.pathEntry("errors/[0]/code").asString();
			if (ArgUtil.areEqual(errorCode, "470")) {
				report.setStatus(Status.CCWIN);
			} else if (ArgUtil.areEqual(errorCode, "471")) {
				report.setStatus(Status.LIMIT);
			}
			report.setReason("Code:" + errorCode);

			List<MessageReportError> errors = requestMap.keyEntry("errors")
					.asList(MessageReport.MessageReportError.class);
			if (ArgUtil.is(errors)) {
				report.setErrors(errors);
			}
		}

		return report;
	}

	@Override
	public MessageBoxEvent inboundMessageBoxEvent(ChannelConfig channelConfig, MapModel requestMap,
			MessageBoxEvent messageBoxEvent) {
		LOGGER.info("IN message {DR}" + requestMap);
		if (requestMap.containsKey("messages")) {
			messageBoxEvent.addInboxMessage(toInboxMessage(channelConfig, requestMap));
		} else if (requestMap.containsKey("statuses")) {
			List<Map<String, Object>> statusMaps = requestMap.keyEntry("statuses").asListOfMap();
			for (Map<String, Object> statusMap : statusMaps) {
				MapModel statusModel = MapModel.from(statusMap);
				MessageReport reprt = toMessageReport(channelConfig, statusModel);
				messageBoxEvent.addMessageReport(reprt);
				if (Status.SENTX.equals(reprt.getStatus())) {
					Map<String, Object> conversation = statusModel.keyEntry("conversation").asMap();
					if (ArgUtil.is(conversation)) {
						String id = String.format("%s_%s", channelConfig.getChannelId(), conversation.get("id"));
						WABAConversationQuery query = new WABAConversationQuery(id);
						query.setContact(reprt.contact());
						query.setConversation(conversation);
						query.setPricing(statusModel.keyEntry("pricing").asMap());
						query.set("meta.to_country", getCountryCode(reprt.contact().getCsid()));
						commonMongoTemplate.upsert(query);
						reprt.setTpMeta(
								MapModel.createInstance().put("ccwExpiry", conversation.get("expiration_timestamp"))
										.put("wabaConvesationId", id).toMap());

					}
				}
			}
		}

		return messageBoxEvent;
	}

	public String getCountryCode(String phone) {
		String defaultRegion = environment.config().prefsEntry("postman.phonebook.region").asString("IN");
		PhoneNumber phoneNumber;
		try {
			phoneNumber = PHONE_NUMBER_UTIL.parse("+" + phone, defaultRegion);
			return PHONE_NUMBER_UTIL.getRegionCodeForCountryCode(phoneNumber.getCountryCode());
		} catch (NumberParseException e) {
			return defaultRegion;
		}
	}

	@Override
	public boolean optin(ChannelConfig channelConfig, ChatContactDoc chatContactDoc) {

		if (ArgUtil.isEmptyValue(chatContactDoc.getLastOptInStamp())) {
			String defaultRegion = environment.config().prefsEntry("postman.phonebook.region").asString("IN");
			String phone = chatContactDoc.phone();
			try {
				phone = phone.replace(" ", "").replaceAll("^[\\+0\\s]+(?!$)", "").trim();
				PhoneNumber phoneNumber = PHONE_NUMBER_UTIL.parse("+" + phone, defaultRegion);
				phone = String.format("+%s%s", phoneNumber.getCountryCode(), phoneNumber.getNationalNumber());
			} catch (NumberParseException e) {
				phone = String.format("+%s", phone);
			}

			MapModel resp = wa360Client.fetchContact(phone, channelConfig);
			String waId = resp.getString("wa_id");

			String input = resp.getString("input");
			String status = resp.getString("status");

			if ("valid".equals(status)) {
				ChatContactQuery chatContactQuery = new ChatContactQuery(chatContactDoc);
				chatContactQuery.updateLastOptInStamp();
				commonMongoTemplate.updateFirst(chatContactQuery);
				return true;
			}
		}
		return !ArgUtil.isEmptyValue(chatContactDoc.getLastOptInStamp());
	}

}
