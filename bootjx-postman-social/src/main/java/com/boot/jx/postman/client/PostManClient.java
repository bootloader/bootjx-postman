package com.boot.jx.postman.client;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.boot.jx.AppConfig;
import com.boot.jx.api.ApiResponse;
import com.boot.jx.dict.ContactType;
import com.boot.jx.postman.PMConstants.PostManUrls;
import com.boot.jx.postman.PostManException;
import com.boot.jx.postman.PostManService;
import com.boot.jx.postman.model.Email;
import com.boot.jx.postman.model.ExceptionReport;
import com.boot.jx.postman.model.MessageBox;
import com.boot.jx.postman.model.Notipy;
import com.boot.jx.postman.model.SMS;
import com.boot.jx.postman.model.SupportEmail;
import com.boot.jx.rest.RestService;
import com.boot.utils.ArgUtil;
import com.boot.utils.CollectionUtil;
import com.boot.utils.ContextUtil;

@Component
@PropertySource("classpath:application-postman.properties")
public class PostManClient implements PostManService {

	private static final Logger LOGGER = LoggerFactory.getLogger(PostManClient.class);

	@Autowired
	private RestService restService;

	@Autowired
	private AppConfig appConfig;

	@Value("${postman.service.url}")
	private String serviceUrl;

	public String getPostmapURL() {
		if (ArgUtil.is(serviceUrl)) {
			return serviceUrl;
		}
		return appConfig.getPostmapURL();
	}

	public void setLang(String lang) {
		ContextUtil.map().put(PARAM_LANG, lang);
	}

	public String getLang() {
		return ArgUtil.parseAsString(ContextUtil.map().get(PARAM_LANG));
	}

	public ApiResponse<SMS, Object> sendSMS(SMS sms, Boolean async) throws PostManException {
		try {
			return restService.ajax(getPostmapURL()).path(PostManUrls.SEND_SMS).queryParam(PARAM_LANG, getLang())
					.queryParam(PARAM_ASYNC, async).priority(ContactType.SMS.getShortCode(), sms.getPriority())
					.post(sms).asApiResponse(SMS.class);
		} catch (Exception e) {
			throw new PostManException(e);
		}
	}

	@Override
	public ApiResponse<SMS, Object> sendSMS(SMS sms) throws PostManException {
		return sendSMS(sms, Boolean.FALSE);
	}

	@Override
	public ApiResponse<SMS, Object> sendSMSAsync(SMS sms) throws PostManException {
		return sendSMS(sms, Boolean.TRUE);
	}

	public ApiResponse<Email, Object> sendEmail(Email email, Boolean async) throws PostManException {
		try {
			return restService.ajax(getPostmapURL()).path(PostManUrls.SEND_EMAIL).queryParam(PARAM_LANG, getLang())
					.queryParam(PARAM_ASYNC, async).priority(ContactType.EMAIL.getShortCode(), email.getPriority())
					.post(email).asApiResponse(Email.class);
		} catch (Exception e) {
			throw new PostManException(e);
		}
	}

	@Override
	public ApiResponse<Email, Object> sendEmail(Email email) throws PostManException {
		return sendEmail(email, Boolean.FALSE);
	}

	@Override
	public ApiResponse<Email, Object> sendEmailAsync(Email email) throws PostManException {
		return sendEmail(email, Boolean.TRUE);
	}

	/*
	 * Sends Bulk email Async. Template is being picked up from the first email in
	 * the list (non-Javadoc)
	 * 
	 * @see
	 * com.amx.jax.postman.PostManService#sendEmailBulkForTemplate(java.util.List)
	 */
	public ApiResponse<Email, Object> sendEmailBulk(List<Email> emailList) {
		LOGGER.info("Sending bulk Email for Notification Service ");
		Email email = CollectionUtil.getOne(emailList);
		try {
			return restService.ajax(getPostmapURL()).path(PostManUrls.SEND_EMAIL_BULK)
					.priority(ContactType.EMAIL.getShortCode(), email.getPriority()).post(emailList)
					.asApiResponse(Email.class);
		} catch (Exception e) {
			throw new PostManException(e);
		}
	}

	@Override
	public ApiResponse<MessageBox, Object> send(MessageBox messageBox) {
		LOGGER.info("Sending bulk messages for Notification Service ");
		try {
			return restService.ajax(getPostmapURL()).path(PostManUrls.SEND_MESSAGE_BOX)
					.priority(messageBox.getPriorityType(), messageBox.getPriorityOrder()).post(messageBox)
					.as(new ParameterizedTypeReference<ApiResponse<MessageBox, Object>>() {
					});
		} catch (Exception e) {
			throw new PostManException(e);
		}
	}

	@Override
	public ApiResponse<Email, Object> sendEmailToSupprt(SupportEmail email) throws PostManException {
		LOGGER.info("Sending support email from {}", email.getVisitorName());
		try {
			return restService.ajax(getPostmapURL()).path(PostManUrls.SEND_EMAIL_SUPPORT)
					.queryParam(PARAM_LANG, getLang()).priority(ContactType.EMAIL.getShortCode(), email.getPriority())
					.post(email).asApiResponse(Email.class);
		} catch (Exception e) {
			throw new PostManException(e);
		}
	}

	@Override
	@Async
	public ApiResponse<Notipy, Object> notifySlack(Notipy msg) throws PostManException {
		try {
			return restService.ajax(getPostmapURL()).path(PostManUrls.NOTIFY_SLACK).queryParam(PARAM_LANG, getLang())
					.post(msg).asApiResponse(Notipy.class);
		} catch (Exception e) {
			throw new PostManException(e);
		}
	}

	@Override
	@Async
	public ApiResponse<ExceptionReport, Object> notifyException(ExceptionReport e) {
		LOGGER.info("Sending exception = {} : {}", e.getTitle(), e.getClass().getName());
		try {
			return restService.ajax(getPostmapURL()).path(PostManUrls.NOTIFY_SLACK_EXCEP_REPORT).contentTypeJson()
					.queryParam("appname", appConfig.getAppName()).queryParam("title", e.getTitle())
					.queryParam("exception", e.getException()).post(e).asApiResponse(ExceptionReport.class);
		} catch (Exception e1) {
			LOGGER.error("Exception while sending title={}", e.getTitle(), e1);
		}
		return null;
	}

	@Override
	@Async
	public ApiResponse<ExceptionReport, Object> notifyException(String title, Exception exc) {
		return this.notifyException(new ExceptionReport(title, exc));
	}

}
