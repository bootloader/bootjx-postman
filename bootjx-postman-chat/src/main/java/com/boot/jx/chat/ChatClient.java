package com.boot.jx.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import com.boot.jx.api.ApiResponse;
import com.boot.jx.postman.PMEnvironment.PMClientConfig;
import com.boot.jx.postman.PMEnvironment.PMCommonConfig;
import com.boot.jx.postman.PMEnvironment.UrlPath;
import com.boot.jx.postman.PostManException;
import com.boot.jx.postman.dto.ChatProfileDTO;
import com.boot.jx.postman.dto.ChatProfileDTO.ChatUserProfileRequest;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.PMArgs;
import com.boot.jx.postman.model.ext.InBoundEvent;
import com.boot.jx.rest.RestService;
import com.boot.jx.utils.PostManUtil;
import com.boot.model.MapModel;
import com.boot.utils.ArgUtil;

@Component
public class ChatClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(ChatClient.class);

	public static class PATH {
		public static final String ASSIGN_TO_AGENT = "/int/assign/agent";
		public static final String ASSIGN_TO_AGENT_V2 = "/int/assign/v2/agent/";
		public static final String INBOUND_FRWRD = "/int/inbound/callback";
		public static final String SESSION_EVENT = "/int/session/event";

		public static final UrlPath APP_SCRIPT_FRWRD = new UrlPath().v1("/bot/sendMessage")
				.v2("/scriptus/api/message/inbound");
		public static final UrlPath APP_SCRIPT_FRWRD_OUTBOUND = new UrlPath().v1("/bot/outbound/message");
		public static final UrlPath APP_SCRIPT_GET_LOGS = new UrlPath().v1("/bot/getLogs")
				.v2("/scriptus/api/console/logs");
		public static final UrlPath APP_SCRIPT_SET_BOT = new UrlPath().v1("/bot/setBot").v2("/scriptus/api/script");
		public static final UrlPath APP_SCRIPT_GET_BOT = new UrlPath().v1("/bot/getBot").v2("/scriptus/api/script");

	}

	@Autowired
	private RestService restService;

	@Autowired
	private PMClientConfig chatClientConfig;

	@Autowired
	private PMCommonConfig pmCommonConfig;

	public ApiResponse<InboxMessage, Object> forward(String inboundForwardUrl, InboxMessage inboxMessage) {
		LOGGER.debug("Forwarding InboxMessage to other Service ");
		try {
			if (ArgUtil.is(inboundForwardUrl)) {
				inboxMessage.setChecksum(PostManUtil.generateCheckSum(inboxMessage));
				return restService.ajax(inboundForwardUrl).post(inboxMessage).cookie("JSESSIONID", "JSESSIONID")
						.cookie("JXSESSIONID", "JXSESSIONID").cookie("AGENTSESSIONID", "AGENTSESSIONID")
						.as(new ParameterizedTypeReference<ApiResponse<InboxMessage, Object>>() {
						});
			}
		} catch (Exception e) {
			throw new PostManException(e);
		}
		return ApiResponse.buildResult(inboxMessage);
	}

	@Deprecated
	public ApiResponse<InboxMessage, Object> assignToAgent(InboxMessage inboxMessage) {
		LOGGER.debug("Assign InboxMessage Session to other Agent ");
		if (ArgUtil.is(pmCommonConfig.getAgentUrl())) {
			inboxMessage.setChecksum(PostManUtil.generateCheckSum(inboxMessage));
			return restService.ajax(pmCommonConfig.getAgentUrl()).path(PATH.ASSIGN_TO_AGENT)
					.cookie("JSESSIONID", "JSESSIONID").cookie("JXSESSIONID", "JXSESSIONID")
					.cookie("AGENTSESSIONID", "AGENTSESSIONID").post(inboxMessage)
					.as(new ParameterizedTypeReference<ApiResponse<InboxMessage, Object>>() {
					});
		} else {
			return null;
		}
	}

	public InBoundEvent assignToAgentV2(PMArgs params) {
		LOGGER.debug("Assign InboxMessage Session to other Agent ");
		if (ArgUtil.is(pmCommonConfig.getAgentUrl())) {
			params.setChecksum(PostManUtil.generateCheckSum(params));
			return restService.ajax(pmCommonConfig.getAgentUrl()).path(PATH.ASSIGN_TO_AGENT_V2)
					.cookie("JSESSIONID", "JSESSIONID").cookie("JXSESSIONID", "JXSESSIONID")
					.cookie("AGENTSESSIONID", "AGENTSESSIONID").post(params)
					.as(new ParameterizedTypeReference<InBoundEvent>() {
					});
		} else {
			return null;
		}
	}

	public void sessionEvent(String inboundForwardUrl, InBoundEvent event, PMArgs pmArgs) {
		LOGGER.debug("Assign InboxMessage Session to other BotCode ");
		event.setChecksum(PostManUtil.generateCheckSum(event));
		pmArgs.setChecksum(PostManUtil.generateCheckSum(pmArgs));
		restService.ajax(inboundForwardUrl).path(PATH.SESSION_EVENT).cookie("JSESSIONID", "JSESSIONID")
				.cookie("JXSESSIONID", "JXSESSIONID").cookie("AGENTSESSIONID", "AGENTSESSIONID")
				.post(MapModel.createInstance().put("event", event).put("pmArgs", pmArgs).toMap()).asNone();
	}

	public ChatProfileDTO fetchContactDetails(ChatUserProfileRequest chatUserProfileRequest) {
		if (ArgUtil.is(chatClientConfig.getContactDetailsUrl())) {
			return restService.ajax(chatClientConfig.getContactDetailsUrl()).post(chatUserProfileRequest)
					.as(new ParameterizedTypeReference<ChatProfileDTO>() {
					});
		} else {
			return null;
		}
	}

}
