package com.boot.jx.postman.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.boot.jx.exception.ApiHttpExceptions.ApiHttpException;
import com.boot.jx.postman.ClientApp;
import com.boot.jx.postman.PMEnvironment.PMDomainConfig;
import com.boot.jx.postman.model.MessageDefinitions.Contactable;
import com.boot.jx.postman.rest.RestNoSSLService;
import com.boot.jx.rest.RestService.Ajax;
import com.boot.model.MapModel;
import com.boot.utils.ArgUtil;
import com.boot.utils.TimeUtils;

@Component
public class MitelClient {

	public static final long TOKEN_EXPIRY = TimeUtils.toMillis("10min");

	@Autowired
	PMDomainConfig pmDomainConfig;

	@Autowired
	private RestNoSSLService restService;

	public String getToken(ClientApp defaultClient) {

		String accessToken = ArgUtil.parseAsString(defaultClient.secret().get("accessToken"));
		Long accessTokenStamp = ArgUtil.parseAsLong(defaultClient.secret().get("accessTokenStamp"), 0L);

		if (ArgUtil.is(accessToken) && !TimeUtils.isExpired(accessTokenStamp, TOKEN_EXPIRY)) {
			return accessToken;
		}

		defaultClient.secret().put("accessTokenStamp", System.currentTimeMillis());

		String endPoint = ArgUtil.parseAsString(defaultClient.props().get("end_point"));
		String grantType = ArgUtil.parseAsString(defaultClient.props().get("grant_type"), "client_credentials");

		Ajax ajax = restService.ajax(endPoint).path("/AuthorizationServer/Token").field("grant_type", grantType);

		if (ArgUtil.areEqual(grantType, "password")) {
			String username = ArgUtil.parseAsString(defaultClient.props().get("client_id"));
			String password = ArgUtil.parseAsString(defaultClient.secret().get("client_secret"));
			ajax.field("username", username).field("password", password);
		} else {
			String clientId = ArgUtil.parseAsString(defaultClient.props().get("client_id"));
			String clientSecret = ArgUtil.parseAsString(defaultClient.secret().get("client_secret"));
			ajax.field("client_id", clientId).field("client_secret", clientSecret);
		}

		accessToken = ajax.postForm().asMapModel().keyEntry("access_token").asString();

		defaultClient.secret().put("accessToken", accessToken);
		defaultClient.secret().put("accessTokenStamp", System.currentTimeMillis());
		return accessToken;

	}

	public MapModel openMediaGetActive(ClientApp defaultClient, Contactable contactable, String sessionId,
			String openmediaId) {
		String accessToken = getToken(defaultClient);
		String endPoint = ArgUtil.parseAsString(defaultClient.props().get("end_point"));
		if (ArgUtil.is(openmediaId)) {
			try {
				MapModel resp = restService.ajax(endPoint).path("/MiccSdk/api/v1/openmedia/{id}")
						.pathParam("id", openmediaId).header("Authorization", "Bearer " + accessToken).get()
						.asMapModel();
				if (!resp.keyEntry("conversationState").in("Ended", "Abandoned")) {
					return resp;
				}
			} catch (ResourceAccessException e) {
				return null;
			} catch (HttpClientErrorException e) {
				if (HttpStatus.NOT_FOUND == e.getStatusCode()) {
					return null;
				} else {
					throw e;
				}
			}
		}
		return null;
	}

	public MapModel resend(ClientApp defaultClient, Contactable contactable, String sessionId, String openmediaId) {
		MapModel resp = openMediaGetActive(defaultClient, contactable, sessionId, openmediaId);
		if (ArgUtil.is(resp)) {
			return resp;
		}
		return this.send(defaultClient, contactable, sessionId);
	}

	public MapModel send(ClientApp defaultClient, Contactable contactable, String sessionId) {
		String accessToken = getToken(defaultClient);
		String endPoint = ArgUtil.parseAsString(defaultClient.props().get("end_point"));
		String queue = ArgUtil.parseAsString(defaultClient.props().get("queue"));
		String from = ArgUtil.parseAsString(defaultClient.props().get("from"), contactable.getName());
		String to = ArgUtil.parseAsString(defaultClient.props().get("to"));

		String url = String.format("%s/agent/plug/chat/%s/%s/%s/hide/CHATBOX", pmDomainConfig.getDomainUrl(),
				contactable.getContactId(), sessionId, contactable.getContactId());
		return restService.ajax(endPoint).path("/MiccSdk/api/v1/openmedia")
				.header("Authorization", "Bearer " + accessToken)
				.postJson(MapModel.createInstance().put("targetUri", url).put("targetUriEmbedded", true)
						.put("previewUrl", url).put("historyUrl", url).put("queue", queue).put("from", from)
						.put("to", to).put("subject", contactable.getName() + " " + contactable.getCsid()).toMap())
				.asMapModel();
	}

	public MapModel openMediaAction(ClientApp defaultClient, String openmediaId, String action) {
		try {
			String accessToken = getToken(defaultClient);
			String endPoint = ArgUtil.parseAsString(defaultClient.props().get("end_point"));
			return restService.ajax(endPoint).path("/MiccSdk/api/v1/openmedia/{id}").pathParam("id", openmediaId)
					.header("Authorization", "Bearer " + accessToken)
					.putJson(MapModel.createInstance().put("action", action).toMap()).asMapModel();
		} catch (ApiHttpException e) {
			return MapModel.from(e.getResponse().getBody());
		}

	}

}
