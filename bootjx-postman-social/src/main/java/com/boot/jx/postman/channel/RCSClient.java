package com.boot.jx.postman.channel;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.boot.jx.dict.ContactType;
import com.boot.jx.postman.PMConstants.CHANNEL_TYPE;
import com.boot.jx.postman.PostManException;
import com.boot.jx.postman.channel.ChannelClientFactory.ChannelClient;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.plugin.ChannelPluginProvider.ConnectorMapping;
import com.boot.jx.postman.plugin.RCSPlugin;
import com.boot.jx.postman.plugin.RCSPlugin.RCSConfigDetails;
import com.boot.jx.rest.RestService;
import com.boot.model.MapModel;
import com.boot.utils.ArgUtil;

/**
 * Telinfy hub RCS client —
 * {@code POST {baseUrl}/api/v1/rcs/messages/:contactID?messageId=...}
 * {@code GET  {baseUrl}/api/v1/rcs/templates}
 */
@Component
@ConnectorMapping(contactType = ContactType.RCS, channel = CHANNEL_TYPE.RCS_TELINFY)
public class RCSClient implements ChannelClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(RCSClient.class);

	@Autowired
	private RestService restService;

	public OutboxMessage sendRCS(ChannelConfig channelConfig, OutboxMessage outboxMessage) {
		RCSConfigDetails rcs = channelConfig.getRcs();
		if (!ArgUtil.is(rcs) || !ArgUtil.is(rcs.getApiKey())) {
			throw new PostManException("RCS API key is not configured");
		}

		String contactId = ArgUtil.nonEmpty(outboxMessage.contact().getCsid(), outboxMessage.contact().phone());
		if (!ArgUtil.is(contactId)) {
			throw new PostManException("RCS recipient phone / csid is required");
		}
		contactId = contactId.replace("+", "").replaceAll("\\s+", "");

		String templateName = resolveTemplateName(outboxMessage);
		if (!ArgUtil.is(templateName)) {
			throw new PostManException("RCS templateName is required (set hsm.code / template)");
		}

		Map<String, Object> body = new HashMap<String, Object>();
		body.put("templateName", templateName);
		Map<String, Object> customParams = resolveCustomParams(outboxMessage);
		if (ArgUtil.is(customParams) && !customParams.isEmpty()) {
			// Telinfy hub docs use "lcustomParam" for placeholder replacement
			body.put("lcustomParam", customParams);
		}

		String messageId = ArgUtil.nonEmpty(outboxMessage.getMessageId(), null);

		try {
			MapModel resp = restService.ajax(RCSPlugin.resolveBaseUrl(rcs)).path("/api/v1/rcs/messages/{contactId}")
					.pathParam("contactId", contactId).queryParam("messageId", messageId)
					.header("x-api-key", rcs.getApiKey()).header("Content-Type", "application/json").postJson(body)
					.asMapModel();

			LOGGER.debug("RCS send response: {}", resp.toJson());

			String jobId = resp.entry("jobId").asString();
			if (ArgUtil.is(jobId)) {
				outboxMessage.setMessageIdExt(jobId);
			} else if (ArgUtil.is(resp.entry("messageId").asString())) {
				outboxMessage.setMessageIdExt(resp.entry("messageId").asString());
			}

			String message = resp.entry("message").asString();
			if (ArgUtil.is(message) && message.toLowerCase().contains("fail")) {
				outboxMessage.logs().add("RCS API: " + message);
				outboxMessage.updateStatus(OutboxMessage.Status.SENT_ERR);
				throw new PostManException(message);
			}

			return outboxMessage;
		} catch (PostManException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.error("RCS send failed: {}", e.getMessage());
			outboxMessage.logs().add("RCS send failed: " + e.getMessage());
			outboxMessage.updateStatus(OutboxMessage.Status.SENT_ERR);
			throw new PostManException(e);
		}
	}

	private String resolveTemplateName(OutboxMessage outboxMessage) {
		if (ArgUtil.is(outboxMessage.templateCode())) {
			return outboxMessage.templateCode();
		}
		if (ArgUtil.is(outboxMessage.getHsm()) && ArgUtil.is(outboxMessage.getHsm().getCode())) {
			return outboxMessage.getHsm().getCode();
		}
		if (ArgUtil.is(outboxMessage.getTemplateExt()) && ArgUtil.is(outboxMessage.getTemplateExt().getCode())) {
			return outboxMessage.getTemplateExt().getCode();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> resolveCustomParams(OutboxMessage outboxMessage) {
		Map<String, Object> params = new HashMap<String, Object>();

		if (ArgUtil.is(outboxMessage.getHsm()) && ArgUtil.is(outboxMessage.getHsm().getData())) {
			putStringEntries(params, outboxMessage.getHsm().getData());
		}

		if (ArgUtil.is(outboxMessage.getModel())) {
			putStringEntries(params, outboxMessage.getModel());
		}

		if (ArgUtil.is(outboxMessage.getTemplateExt()) && ArgUtil.is(outboxMessage.getTemplateExt().getVarMap())) {
			Object body = outboxMessage.getTemplateExt().getVarMap().get("body");
			if (body instanceof Map) {
				putStringEntries(params, (Map<String, Object>) body);
			} else {
				putStringEntries(params, outboxMessage.getTemplateExt().getVarMap());
			}
		}

		return params;
	}

	private void putStringEntries(Map<String, Object> target, Map<String, Object> source) {
		if (source == null) {
			return;
		}
		for (Entry<String, Object> e : source.entrySet()) {
			if (ArgUtil.is(e.getKey()) && ArgUtil.is(e.getValue()) && !(e.getValue() instanceof Map)
					&& !(e.getValue() instanceof Iterable)) {
				target.put(e.getKey(), ArgUtil.parseAsString(e.getValue()));
			}
		}
	}

	@Override
	public MapModel fetchTemplates(ChannelConfig channelConfig) {
		RCSConfigDetails rcs = channelConfig.getRcs();
		if (!ArgUtil.is(rcs) || !ArgUtil.is(rcs.getApiKey())) {
			throw new PostManException("RCS API key is not configured");
		}

		MapModel resp = restService.ajax(RCSPlugin.resolveBaseUrl(rcs)).path("/api/v1/rcs/templates")
				.header("x-api-key", rcs.getApiKey()).get().asMapModel();

		LOGGER.debug("RCS fetchTemplates response: {}", resp.toJson());
		return resp;
	}

	@Override
	public MapModel createTemplates(ChannelConfig channelConfig, MapModel from) {
		return null;
	}

	@Override
	public MapModel updateTemplates(ChannelConfig channelConfig, MapModel from) {
		return null;
	}

	@Override
	public MapModel listOfFlows(ChannelConfig channelConfig) {
		return null;
	}

	@Override
	public MapModel flowsAssets(String flowId, ChannelConfig channelConfig) {
		return null;
	}

}
