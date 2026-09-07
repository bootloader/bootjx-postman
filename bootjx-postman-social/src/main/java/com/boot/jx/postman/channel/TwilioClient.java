package com.boot.jx.postman.channel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.rest.RestService;
import com.boot.model.MapModel;
import com.boot.utils.CryptoUtil;

@Component
public class TwilioClient {

	public static final String BASE_URL = "https://api.twilio.com/2010-04-01/";

	@Autowired
	private RestService restService;

	public OutboxMessage sendSMS(ChannelConfig channelConfig, OutboxMessage outboxMessage) {
		MapModel resp = restService.ajax(BASE_URL).path("/Accounts/{sid}/Messages.json")
				.pathParam("sid", channelConfig.getTwilio().getSid())
				.field("From", "+" + channelConfig.getTwilio().getNumber())
				.field("To", "+" + outboxMessage.contact().getCsid()).field("Body", outboxMessage.getMessage())
				.header("Authorization", "Basic " + CryptoUtil.getEncoder()
						.message(channelConfig.getTwilio().getSid() + ":" + channelConfig.getTwilio().getToken())
						.encodeBase64().toString())
				.postForm().asMapModel();
		//System.out.println(resp.toJson());
		return outboxMessage;
	}
}
