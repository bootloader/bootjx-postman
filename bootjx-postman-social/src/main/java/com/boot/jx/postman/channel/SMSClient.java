package com.boot.jx.postman.channel;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.boot.jx.postman.PostManException;
import com.boot.jx.postman.client.TmplClient;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.plugin.SMSPlugin.SMSConfigDetails; 
import com.boot.jx.rest.RestService;
import com.boot.jx.rest.RestService.Ajax;
import com.boot.model.MapModel;
import com.boot.utils.ArgUtil;
import com.boot.utils.Constants;
import com.boot.utils.CryptoUtil;
import com.boot.utils.JsonPath;

@Component
public class SMSClient {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SMSClient.class);

	public static final String TEXTLOCAL = "TEXTLOCAL";
	public static final String TEXTLOCAL_URL = "https://api.textlocal.in/send";
	public static final JsonPath TEXTLOCAL_URL_RESPONSE_MSG_ID = new JsonPath("messages/[0]/id");

	public static final String TWILIO = "TWILIO";
	public static final String TWILIO_URL = "https://api.twilio.com/2010-04-01/";
	public static final JsonPath TWILIO_URL_RESPONSE_MSG_ID = new JsonPath("sid");
	
	public static final String SMARTPING = "SMARTPING";
	public static final String SMARTPING_URL = "https://pgapi.smartping.ai/fe/api/v1/send";

	@Autowired
	private RestService restService;

	@Autowired
	TwilioClient twilioClient;

	@Autowired
	TmplClient tmplClient;

	public OutboxMessage sendSMS(ChannelConfig channelConfig, OutboxMessage outboxMessage) {
		SMSConfigDetails sms = channelConfig.getSms();

		MapModel pub = MapModel.from(sms.getPub());
		MapModel secret = MapModel.from(sms.getSecret());
		MapModel model = MapModel.createInstance().putAll(pub).putAll(secret).put("message", outboxMessage.getMessage())
				.put("message_id", outboxMessage.getMessageId()).put("csid", outboxMessage.contact().getCsid());
		
		if (TEXTLOCAL.equalsIgnoreCase(sms.getProvider())) {
			MapModel resp = restService.ajax(TEXTLOCAL_URL).field("apikey", secret.entry("apikey").asString())
					.field("sender", channelConfig.getSms().getNumber())
					.field("numbers", outboxMessage.contact().getCsid()).field("message", outboxMessage.getMessage())
					.submit().asMapModel();
			outboxMessage.setMessageIdExt(resp.path(TEXTLOCAL_URL_RESPONSE_MSG_ID).asString());
		} else if (TWILIO.equalsIgnoreCase(sms.getProvider())) {
			String sid = pub.entry("sid").asString();
			String apiKey = secret.entry("apikey").asString();
			String authToken = secret.entry("authToken").asString();
			
			MapModel resp = restService.ajax(TWILIO_URL).path("/Accounts/{sid}/Messages.json").pathParam("sid", sid)
					.field("From", "+" + channelConfig.getSms().getNumber())
					.field("To", "+" + outboxMessage.contact().getCsid()).field("Body", outboxMessage.getMessage())
					.header("Authorization",
							"Basic " + CryptoUtil.getEncoder().message(sid + ":" + authToken).encodeBase64().toString())
					.postForm().asMapModel();
			outboxMessage.setMessageIdExt(resp.keyEntry("sid").asString());
		}else if (SMARTPING.equalsIgnoreCase(sms.getProvider())) {
			try {
				// Get DLT Content ID from tptinfo in options
				String dltContentId = null;
				
				// Check if template has tptinfo in options
				if (outboxMessage.templateId() != null && outboxMessage.options().containsKey("tptinfo")) {
					@SuppressWarnings("unchecked")
					List<Map<String, Object>> tptinfoList = (List<Map<String, Object>>) outboxMessage.options().get("tptinfo");
					if (tptinfoList != null) {
						for (Map<String, Object> tptinfo : tptinfoList) {
							String channelId = ArgUtil.parseAsString(tptinfo.get("channelId"));
							if (channelConfig.getChannelId().equals(channelId)) {
								dltContentId = ArgUtil.parseAsString(tptinfo.get("dltContentId"));
								break;
							}
						}
					}
				}
				
				// Throw error if no DLT Content ID found
				if (dltContentId == null || dltContentId.trim().isEmpty()) {
					String errorMsg = "DLT Content ID not found";
					LOGGER.error("No DLT Content ID found in tptinfo for channel: {}", channelConfig.getChannelId());
					outboxMessage.logs().add(errorMsg);
					outboxMessage.updateStatus(OutboxMessage.Status.SENT_ERR);
					throw new PostManException(errorMsg);
				}

				MapModel resp = restService.ajax(SMARTPING_URL)
					.field("username", pub.entry("username").asString())
					.field("password", secret.entry("password").asString())
					.field("unicode", pub.entry("unicode").asString("false"))
					.field("from", pub.entry("from").asString())
					.field("to", outboxMessage.contact().getCsid())
					.field("text", outboxMessage.getMessage())
					.field("dltContentId", dltContentId)
					.field("dltPrincipalEntityId", pub.entry("dltPrincipalEntityId").asString())
					.submit()
					.asMapModel();
				
				LOGGER.debug("response: {}", resp.toJson());

				// Handle SmartPing-specific errors if any
				if (resp.containsKey("error")) {
					outboxMessage.logs().add("SMS API Error: " + resp.toJson());
					outboxMessage.updateStatus(OutboxMessage.Status.SENT_ERR);
				} else {
					// Set external message ID if available in response
					if (resp.containsKey("message_id")) {
						outboxMessage.setMessageIdExt(resp.entry("message_id").asString());
					}
					outboxMessage.updateStatus(OutboxMessage.Status.SENT);
				}
			} catch (Exception e) {
				String errorMsg = "SMS sending failed";
				LOGGER.error("Error sending SMS : {}", e.getMessage());
				outboxMessage.logs().add(errorMsg);
				outboxMessage.updateStatus(OutboxMessage.Status.SENT_ERR);
				throw new PostManException(errorMsg);
			}
		}else {
			Ajax ajax = restService.ajax(sms.getRequest().getUrl());
			if (sms.getRequest().getHeaders() != null) {
				for (String header : sms.getRequest().getHeaders()) {
					String[] hd = header.split(":");
					ajax.header(hd[0], fullfull(hd[1], model));
				}
			}
			if (sms.getRequest().getAuths() != null) {
				for (String auth : sms.getRequest().getAuths()) {
					ajax.header("Authorization", "Basic " + CryptoUtil.getEncoder()
							.message(ArgUtil.parseAsString(fullfull(auth, model))).encodeBase64().toString());
				}
			}

			if ("POST".equalsIgnoreCase(sms.getRequest().getMethod())) {
				// ajax.header("Accept", "text/html");
				if (sms.getRequest().getFields() != null) {
					for (Entry<String, String> field : sms.getRequest().getFields().entrySet()) {
						ajax.field(field.getKey(), fullfull(field.getValue(), model));
					}
					ajax.submit().asNone();
				} else if (sms.getRequest().getData() != null) {
					Map<String, Object> data = MapModel.newMap();
					for (Entry<String, Object> field : sms.getRequest().getData().entrySet()) {
						data.put(field.getKey(), fullfull(field.getValue(), model));
					}
					ajax.postJson(data).asNone();
				}
			} else if ("GET".equalsIgnoreCase(sms.getRequest().getMethod())) {
				if (sms.getRequest().getFields() != null) {
					for (Entry<String, String> field : sms.getRequest().getFields().entrySet()) {
						ajax.queryParam(field.getKey(), fullfull(field.getValue(), model));
					}
				}
				ajax.get().asNone();
			}
		}

		return outboxMessage;
	}

	private Object fullfull(Object template, MapModel model) {
		String tempString = ArgUtil.parseAsString(template, Constants.BLANK);
		if (tempString.contains("{{")) {
			return tmplClient.process(tempString, model.toMap());
		}
		return template;
	}
}
