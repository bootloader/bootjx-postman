package com.boot.jx.postman.channel;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.boot.jx.AppContextUtil;
import com.boot.jx.AppParam;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.plugin.FirebasePlugin.FirebaseConfigDetails;
import com.boot.jx.rest.RestService;
import com.boot.utils.ArgUtil;
import com.boot.utils.JsonUtil;

@Component
public class PushClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(PushClient.class);

	public static class To {
		public static String format(String format, Object... string) {
			return String.format(format, string).replaceAll("[^a-zA-Z0-9\\-]+", "").toLowerCase();
		}

		public static String all() {
			return format("%s", AppParam.APP_ENV.getValue());
		}

		public static String domain() {
			return format("%s-%s", AppParam.APP_ENV.getValue(), AppContextUtil.getTenant());
		}

		public static String dept(String dept) {
			return format("%s-%s-dept-%s", AppParam.APP_ENV.getValue(), AppContextUtil.getTenant(), dept);
		}

		public static String agent(String agent) {
			return format("%s-%s-agent-%s", AppParam.APP_ENV.getValue(), AppContextUtil.getTenant(), agent);
		}

	}

	public static String topics(String topic) {
		return "/topics/" + topic;
	}

	@Autowired
	private PMEnvironment pmEnvironment;

	@Autowired
	private RestService restService;

	public OutboxMessage send(ChannelConfig channelConfig, OutboxMessage outboxMessage) {
		if (!ArgUtil.is(channelConfig)) {
			return null;
		}

		FirebaseConfigDetails firebase = channelConfig.getFirebase();

		if (!ArgUtil.is(firebase)) {
			return null;
		}

		HashMap<String, Object> bodyObject = new HashMap<>();
		bodyObject.put("to", topics(outboxMessage.contact().getCsid()));
		HashMap<String, String> notification = new HashMap<>();
		notification.put("title", outboxMessage.getSubject());
		notification.put("body", outboxMessage.getMessage());
		bodyObject.put("notification", notification);
		bodyObject.put("data", outboxMessage.modelMap().entry("data").asMap());

		try {
			restService.ajax("https://fcm.googleapis.com/fcm/send")
					.header("Authorization", "key=" + firebase.getServerKey())
					.header("Content-Type", "application/json").post(JsonUtil.toJson(bodyObject)).asNone();
		} catch (Exception e) {
			LOGGER.error("Firebase push Exception", e.getMessage());
		}
		return outboxMessage;
	}

	public OutboxMessage send(OutboxMessage outboxMessage) {
		ChannelConfig channelConfig = pmEnvironment.config().channel("firebase:magent");
		return send(channelConfig, outboxMessage);

	}

}
