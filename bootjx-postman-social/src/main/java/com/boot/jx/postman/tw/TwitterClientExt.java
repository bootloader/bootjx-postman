package com.boot.jx.postman.tw;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.rest.RestService;
import com.boot.model.MapModel;
import com.boot.utils.JsonPath;

/**
 * This is a twitter client written from scratch
 * 
 * @author lalittanwar
 *
 */

@Component
public class TwitterClientExt {
	private static final long serialVersionUID = 1L;

	@Autowired
	RestService restService;

	public MapModel getUserDetails(ChannelConfig config, String handler) {
		TwitterOauthHeaderGenerator generator = new TwitterOauthHeaderGenerator(config.getTwitter().getConsumerKey(),
				config.getTwitter().getConsumerSecret(), config.getTwitter().getAccessToken(),
				config.getTwitter().getAccessTokenSecret());
		Map<String, String> requestParams = new HashMap<>();
		requestParams.put("screen_name", handler);
		String header = generator.generateHeader("GET", "https://api.twitter.com/1.1/users/lookup.json", requestParams);
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", header);
		return restService.ajax("https://api.twitter.com/1.1/users/lookup.json").header(headers)
				.queryParam("screen_name", handler).get().asListModel().first().asMapModel();
	}

	public MapModel askInput(ChannelConfig config, String recipientId) {
		TwitterOauthHeaderGenerator generator = new TwitterOauthHeaderGenerator(config.getTwitter().getConsumerKey(),
				config.getTwitter().getConsumerSecret(), config.getTwitter().getAccessToken(),
				config.getTwitter().getAccessTokenSecret());
		Map<String, String> requestParams = new HashMap<>();
		// requestParams.put("screen_name", handler);

		MapModel map = MapModel.createInstance().put(new JsonPath("event/type"), "message_create")
				.put(new JsonPath("event/message_create/target/recipient_id"), recipientId)
				.put(new JsonPath("event/message_create/message_data/text"), "Hello");;

		String header = generator.generateHeader("POST", "https://api.twitter.com/1.1/direct_messages/events/new.json",
				requestParams);
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", header);

		return restService.ajax("https://api.twitter.com/1.1/direct_messages/events/new.json").header(headers)
				.post(map.toMap()).asMapModel();
	}

}
