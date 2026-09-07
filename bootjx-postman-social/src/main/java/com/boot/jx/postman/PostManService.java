package com.boot.jx.postman;

import java.util.List;

import com.boot.jx.api.ApiResponse;
import com.boot.jx.postman.model.Email;
import com.boot.jx.postman.model.ExceptionReport;
import com.boot.jx.postman.model.MessageBox;
import com.boot.jx.postman.model.Notipy;
import com.boot.jx.postman.model.SMS;
import com.boot.jx.postman.model.SupportEmail;

public interface PostManService {

	public static final String PARAM_LANG = "lang";
	public static final String PARAM_ASYNC = "async";

	public ApiResponse<Email, Object> sendEmail(Email email) throws PostManException;

	public ApiResponse<Email, Object> sendEmailToSupprt(SupportEmail email) throws PostManException;

	public ApiResponse<SMS, Object> sendSMS(SMS sms) throws PostManException;

	public ApiResponse<Notipy, Object> notifySlack(Notipy msg) throws PostManException;

	public ApiResponse<ExceptionReport, Object> notifyException(ExceptionReport e);

	public ApiResponse<ExceptionReport, Object> notifyException(String title, Exception exc);

	public ApiResponse<Email, Object> sendEmailAsync(Email email) throws PostManException;

	public ApiResponse<SMS, Object> sendSMSAsync(SMS sms) throws PostManException;

	public ApiResponse<Email, Object> sendEmailBulk(List<Email> emailList);

	public ApiResponse<MessageBox, Object> send(MessageBox messageBox);

}
