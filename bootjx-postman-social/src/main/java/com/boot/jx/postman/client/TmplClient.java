package com.boot.jx.postman.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import com.boot.jx.api.ApiResponse;
import com.boot.jx.dict.ContactType;
import com.boot.jx.model.CommonFile;
import com.boot.jx.postman.PMEnvironment.PMCommonConfig;
import com.boot.jx.postman.PMEnvironment.PMGateKeeper;
import com.boot.jx.postman.PostManException;
import com.boot.jx.postman.PostmanPackages.ICommonTmplPackage;
import com.boot.jx.postman.PostmanPackages.Text2Media;
import com.boot.jx.postman.model.Attachment;
import com.boot.jx.postman.model.MessageDefinitions.Contactable;
import com.boot.jx.postman.model.MessageMetaWrapper;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.model.PostManFile;
import com.boot.jx.postman.model.TmplElement;
import com.boot.jx.rest.RestService;
import com.boot.model.MapModel;
import com.boot.utils.ArgUtil;
import com.boot.utils.CollectionUtil;
import com.boot.utils.JsonUtil;

@Component
public class TmplClient {

	public static class PATH {
		public static final String TMPL_FILE_PROCESS = "/tmpl/file/process";
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(TmplClient.class);

	@Autowired
	private RestService restService;

	@Autowired
	private PostManClient postManClient;

	@Autowired(required = false)
	private ICommonTmplPackage iCommonTmplPackage;

	@Autowired
	protected Text2Media text2Media;

	@Autowired
	protected PMGateKeeper pmGateKeeper;

	@Autowired
	protected PMCommonConfig pmCommonConfig;

	public ApiResponse<CommonFile, Object> process(CommonFile file, Contactable contactable) throws PostManException {
		if (ArgUtil.is(iCommonTmplPackage)) {
			return ApiResponse.buildResult(iCommonTmplPackage.process(file, contactable));
		}
		return restService.ajax(postManClient.getPostmapURL()).path(PATH.TMPL_FILE_PROCESS)
				.queryParam("contactType", contactable.getContactType())
				.queryParam(PostManClient.PARAM_LANG, postManClient.getLang()).contentTypeJson().acceptJson().post(file)
				.as(new ParameterizedTypeReference<ApiResponse<CommonFile, Object>>() {
				});
	}

	public String postProcessSanitize(String content) {
		if (content == null) {
			return content;

		}
		return content.replace("&#x3D;", "=");
	}

	public OutboxMessage process(OutboxMessage outboxMessage, Contactable contactable) {

		CommonFile file = new PostManFile();

		file.setModel(outboxMessage.getModel());
		file.setTemplate(outboxMessage.getHsm());
		file = this.process(file, contactable).getResult();
		outboxMessage.setModel(file.model()); // Template needs to be restored as it has been deep-copied

		String content = postProcessSanitize(file.getContent());
		outboxMessage.setMessage(content);

		MessageMetaWrapper messageMeta = MessageMetaWrapper.from(file.meta());

		if (ArgUtil.is(messageMeta.categoryType().exists())) {
			outboxMessage.messageMetaWrapper().categoryType(messageMeta.categoryType().asString());
		}

		if (ArgUtil.is(messageMeta.categorySubType().exists())) {
			outboxMessage.messageMetaWrapper().categorySubType(messageMeta.categorySubType().asString());
		}

		if (!ArgUtil.is(outboxMessage.getSubject())) {
			outboxMessage.setSubject(ArgUtil.parseAsString(file.getOptions().get("subject"), file.getTitle()));
		}

		Map<String, Object> options = new HashMap<String, Object>();
		List<TmplElement> buttons = new ArrayList<TmplElement>();
		List<TmplElement> inputs = new ArrayList<TmplElement>();

		MapModel optionsModel = MapModel.from(file.options());
		List<Map<String, Object>> buttonsModel = optionsModel.keyEntry("buttons").asListOfMap();

		for (Map<String, Object> map : buttonsModel) {
			MapModel buttonMapModel = MapModel.from(map);

			buttons.add(new TmplElement().code(buttonMapModel.getString("key")).label(buttonMapModel.getString("label"))
					.desc(buttonMapModel.getString("desc")).type(buttonMapModel.getString("type"))
					.variable(buttonMapModel.getString("variable")).url(buttonMapModel.getString("url"))
					.phone(buttonMapModel.getString("phone_number")));
		}

		for (Entry<String, Object> entry : file.getOptions().entrySet()) {
			if (entry.getKey().indexOf("form-input-") == 0) {
				String[] params = ArgUtil.parseAsString(entry.getValue()).split("\\|");
				inputs.add(new TmplElement().code(entry.getKey().replace("form-input-", ""))
						.label(CollectionUtil.get(params, 0)).type(CollectionUtil.get(params, 1)));
			} else if (entry.getKey().indexOf("actions-button-") == 0) {
				String[] params = ArgUtil.parseAsString(entry.getValue()).split("\\|");
				buttons.add(new TmplElement().code(entry.getKey().replace("actions-button-", ""))
						.label(CollectionUtil.get(params, 0)).type(CollectionUtil.get(params, 1)));
			} else {
				options.put(entry.getKey(), entry.getValue());
			}
		}
		options.put("inputs", inputs);
		options.put("buttons", buttons);

		Attachment defaultAttachment = optionsModel.keyEntry("attachment").as(Attachment.class);
		if (ArgUtil.is(defaultAttachment) && outboxMessage.attachments().size() == 0) {
			outboxMessage.attachments().add(defaultAttachment);
		}

		Attachment backgroundVoice = optionsModel.keyEntry("bg_voice").as(Attachment.class);
		// TODO:-@lalit to review
		if (ArgUtil.is(backgroundVoice) && outboxMessage.getContact().type().equals(ContactType.WEBSITE)) {
			outboxMessage.attachments().add(backgroundVoice);
		}

		if (ArgUtil.is(outboxMessage.attachments())) {
			try {
				for (Attachment attach : outboxMessage.getAttachments()) {
					if (ArgUtil.is(attach.getMediaTemplate())) {
						if (pmGateKeeper.canSendTemplateMedia(outboxMessage)) {
							attach.setMediaURL(toImage(attach, outboxMessage.getModel()));
						} else {
							return outboxMessage;
						}
					}
				}
			} catch (Exception e) {
				outboxMessage.logs().add("MediaTemplateException : " + e.getMessage());
				LOGGER.error("MediaTemplateException", e);
			}
		}
		outboxMessage.options().putAll(options);
		return outboxMessage;
	}

	public String process(String template, Object model) {
		if (ArgUtil.is(iCommonTmplPackage)) {
			return iCommonTmplPackage.process(template, model);
		}
		return template;
	}

	public String toImage(Attachment attach, Object model) throws IOException {
		String attachFileStr = process(attach.getMediaTemplate(), model);
		String version = pmCommonConfig.getMediaTemplateVersion();
		if ("v2".equals(version)) {
			return text2Media.toImageUrl(attachFileStr, attach.getMediaTemplateStyle(), attach.getAttachmentId());
		}
		return text2Media.toImage(attachFileStr, attach.getMediaTemplateStyle(), attach.getAttachmentId());
	}

}
