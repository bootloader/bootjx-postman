package com.boot.jx.postman.client;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.boot.jx.AppConfig;
import com.boot.jx.api.ApiResponse;
import com.boot.jx.dict.ContactType;
import com.boot.jx.postman.PMConstants.PostManUrls;
import com.boot.jx.postman.PostManException;
import com.boot.jx.postman.model.WAMessage;
import com.boot.jx.rest.RestService;
import com.boot.utils.CollectionUtil;

@Component
public class WhatsAppClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(WhatsAppClient.class);

	@Autowired
	private RestService restService;

	@Autowired
	private AppConfig appConfig;

	/**
	 * This is convenient method for {@link WhatsAppClient#send(List)}
	 * 
	 * @param msg
	 * @return
	 * @throws PostManException
	 */
	public ApiResponse<WAMessage, Object> send(WAMessage msg) throws PostManException {
		return this.send(Arrays.asList(msg));
	}

	public ApiResponse<WAMessage, Object> send(List<WAMessage> msgs) throws PostManException {
		LOGGER.info("Sending WAMessage Notifications");
		try {
			WAMessage msg = CollectionUtil.getOne(msgs);
			return restService.ajax(appConfig.getPostmapURL()).path(PostManUrls.WHATS_APP_SEND_BULK)
					.priority(ContactType.WHATSAPP.getShortCode(), msg.getPriority())
					.post(msgs)
					.asApiResponse(WAMessage.class);
		} catch (Exception e) {
			throw new PostManException(e);
		}
	}

}
