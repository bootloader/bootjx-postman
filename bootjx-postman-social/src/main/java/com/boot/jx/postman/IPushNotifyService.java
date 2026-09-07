package com.boot.jx.postman;

import java.util.List;

import com.boot.jx.api.ApiResponse;
import com.boot.jx.postman.model.PushMessage;

public interface IPushNotifyService {

	public static final String PARAM_TOKEN = "token";
	
	public static final String PARAM_RELATIVE_URL = "relativeUrl";

	public static final String PARAM_TOPIC = "topic";

	public ApiResponse<PushMessage, Object> sendDirect(PushMessage msg) throws PostManException;

	public ApiResponse<PushMessage, Object> send(List<PushMessage> msgs) throws PostManException;

	public ApiResponse<String, Object> subscribe(String token, String topic) throws PostManException;
	
	public String shortLink(String relativeUrl) throws PostManException;

}
