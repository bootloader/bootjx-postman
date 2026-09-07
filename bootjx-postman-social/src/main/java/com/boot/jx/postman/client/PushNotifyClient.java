package com.boot.jx.postman.client;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import com.boot.jx.AppConfig;
import com.boot.jx.api.ApiResponse;
import com.boot.jx.dict.ContactType;
import com.boot.jx.postman.IPushNotifyService;
import com.boot.jx.postman.PMConstants.PostManUrls;
import com.boot.jx.postman.PostManException;
import com.boot.jx.postman.model.PushMessage;
import com.boot.jx.rest.RestService;
import com.boot.utils.CollectionUtil;

@Component
public class PushNotifyClient implements IPushNotifyService {

	private static final Logger LOGGER = LoggerFactory.getLogger(PushNotifyClient.class);

	@Autowired
	RestService restService;

	@Autowired
	AppConfig appConfig;

	@Override
	public ApiResponse<PushMessage, Object> sendDirect(PushMessage msg) throws PostManException {
		LOGGER.info("Sending Push Notifications");
		try {
			return restService.ajax(appConfig.getPostmapURL()).path(PostManUrls.NOTIFY_PUSH)
					.priority(ContactType.PUSH.getShortCode(), msg.getPriority())
					.post(msg)
					.as(new ParameterizedTypeReference<ApiResponse<PushMessage, Object>>() {
					});
		} catch (Exception e) {
			throw new PostManException(e);
		}
	}

	/**
	 * This is convenient method for {@link PushNotifyClient#send(List)}
	 * 
	 * @param msg
	 * @return
	 * @throws PostManException
	 */
	public ApiResponse<PushMessage, Object> send(PushMessage msg) throws PostManException {
		return this.send(Arrays.asList(msg));
	}

	@Override
	public ApiResponse<PushMessage, Object> send(List<PushMessage> msgs) throws PostManException {
		LOGGER.info("Sending Push Notifications");
		try {
			PushMessage msg = CollectionUtil.getOne(msgs);
			return restService.ajax(appConfig.getPostmapURL()).path(PostManUrls.NOTIFY_PUSH_BULK)
					.priority(ContactType.PUSH.getShortCode(), msg.getPriority())
					.post(msgs)
					.asApiResponse(PushMessage.class);
		} catch (Exception e) {
			throw new PostManException(e);
		}
	}

	@Override
	public ApiResponse<String, Object> subscribe(String token, String topic) throws PostManException {
		LOGGER.info("Subscribing for Push Notifications on web");
		try {
			return restService.ajax(appConfig.getPostmapURL()).path(PostManUrls.NOTIFY_PUSH_SUBSCRIBE)
					.queryParam(PARAM_TOKEN, token).pathParam(PARAM_TOPIC, topic)
					.post().asApiResponse(String.class);
		} catch (Exception e) {
			throw new PostManException(e);
		}

	}

	@Override
	public String shortLink(String relativeUrl) throws PostManException {
		LOGGER.info("Subscribing for Push Notifications on web" + appConfig.getPostmapURL());
		try {
			return restService.ajax(appConfig.getPostmapURL()).path(PostManUrls.SHORT_LINK)
					.queryParam(PARAM_RELATIVE_URL, relativeUrl).post().asString();
		} catch (Exception e) {
			throw new PostManException(e);
		}
	}

}
