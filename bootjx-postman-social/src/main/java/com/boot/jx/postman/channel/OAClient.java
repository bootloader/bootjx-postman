package com.boot.jx.postman.channel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.plugin.OAPlugin.OAConfigDetails;
import com.boot.jx.rest.RestService;
import com.boot.model.MapModel;
import com.boot.utils.JsonPath;

@Component
public class OAClient {

	public static final String TEXTLOCAL = "TEXTLOCAL";
	public static final String TEXTLOCAL_URL = "https://api.otp.in/send";
	public static final JsonPath TEXTLOCAL_URL_RESPONSE_MSG_ID = new JsonPath("messages/[0]/id");

	public static final String OA_URL = ".otpalerts.com/entoc/api/v1/e2ee/send";
	public static final JsonPath TEMPLATE_CODE = new JsonPath("template.code");
	public static final JsonPath TEMPLATE_MODEL = new JsonPath("template.model");
	public static final JsonPath TEMPLATE_DATA = new JsonPath("template.model.data");

	@Autowired
	private RestService restService;

	public OutboxMessage sendMessage(ChannelConfig channelConfig, OutboxMessage outboxMessage) {
		OAConfigDetails oa = channelConfig.getOa();
		MapModel resp = restService.ajax("https://" + oa.getClientId() + OA_URL).header("x-api-key", oa.getApiKey())
				.postJson(MapModel.createInstance()
						//
						.put("phone", outboxMessage.contact().phone())
						.put(TEMPLATE_CODE, outboxMessage.getTemplateExt().getCode())
						.put(TEMPLATE_MODEL, outboxMessage.getModel())
						//
						.toMap())
				.asMapModel();;
		return outboxMessage;
	}

}
