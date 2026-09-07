package com.boot.jx.postman.gupshup;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import com.boot.jx.dict.FileType;
import com.boot.jx.postman.PMConstants.CHANNEL_TYPE;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.PostManException;
import com.boot.jx.postman.gupshup.GupShupConstants.SessionType;
import com.boot.jx.postman.model.Attachment;
import com.boot.jx.postman.model.MessageOptions;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.rest.RestService;
import com.boot.jx.rest.RestService.Ajax;
import com.boot.jx.utils.PostManUtil;
import com.boot.utils.ArgUtil;
import com.boot.utils.CollectionUtil;
import com.boot.utils.CryptoUtil;
import com.boot.utils.JsonUtil;

public abstract class GupShupClientAbstract {

    public static final String API_URL = "https://media.smsgupshup.com";

    @Autowired
    protected RestService restService;

    public abstract SessionType getSessionType();

    public boolean getIsHSM() {
	return false;
    }

    @Autowired
    private PMEnvironment environment;

    private Ajax ajax(GupShupReq req, boolean encrypt) {
	Ajax ajax = restService.ajax(API_URL).path("/GatewayAPI/rest");

	if (ArgUtil.isEmpty(req.getWaNumber())) {
	    throw new PostManException("No lane " + req.getWaNumber());
	}

	String channelId = PostManUtil.CHANNEL_ID(CHANNEL_TYPE.WA_GUPSHUP, req.getWaNumber());

	ChannelConfig config = environment.config().channel(channelId);

	if (ArgUtil.isEmpty(config)) {
	    throw new PostManException("No Config for lane " + req.getWaNumber());
	}

	if (getSessionType() == SessionType.NOTIFICATION) {
	    if (ArgUtil.isEmpty(config.getGupshup().getNotifyId())
		    || ArgUtil.isEmpty(config.getGupshup().getNotifyPass())) {
		throw new PostManException("Notification Not Configured for this lane " + req.getWaNumber());
	    }
	    ajax.field("userid", config.getGupshup().getNotifyId());
	    req.password(config.getGupshup().getNotifyPass());
	} else {
	    ajax.field("userid", config.getGupshup().getChatId());
	    req.password(config.getGupshup().getChatPass());
	}
	if (encrypt) {
	    ajax.field("encrdata", CryptoUtil.getEncoder().obzect(req.password(config.getGupshup().getChatPass()))
		    .encodeBase64().toString());
	} else {
	    Map<String, Object> reqMap = JsonUtil.toMap(req);
	    for (Entry<String, Object> entrySet : reqMap.entrySet()) {
		ajax.field(entrySet.getKey(), entrySet.getValue());
	    }
	}
	return ajax;
    }

    private GupShupResp post(GupShupReq req, boolean encrypt) {
	Ajax ajax = ajax(req, encrypt);
	GupShupResp x = ajax.postForm().as(GupShupResp.class);
	// System.out.println("=============" + x.getResponse().getDetails());
	return x;
    }

    public GupShupResp uploadDocument(String phoneNumber, MultipartFile file) throws IOException {
	return ajax(new GupShupReq(GupShupConstants.Method.UploadMedia).sendTo(phoneNumber), false)
		.field("media_type", GupShupConstants.MessageType.DOCUMENT).field("media_file", file).postForm()
		.as(GupShupResp.class);
    }

    protected GupShupResp post(GupShupReq req) {
	return post(req, false);
    }

    public GupShupResp optIn(String phoneNumber) {
	GupShupReq gupShupReq = new GupShupReq(GupShupConstants.Method.OPT_IN).phoneNumber(phoneNumber);
	gupShupReq.setChannel("WHATSAPP");
	return post(gupShupReq);
    }

    public GupShupResp optOut(String phoneNumber) {
	return post(new GupShupReq(GupShupConstants.Method.OPT_OUT).phoneNumber(phoneNumber));
    }

    public GupShupResp sendMessage(String phoneNumber, String message) {
	return post(new GupShupReq(GupShupConstants.Method.SendMessage).sendTo(phoneNumber)
		.messageType(GupShupConstants.MessageType.TEXT)
		.message(CryptoUtil.getEncoder().message(message).toString()));
    }

    public GupShupResp sendImageURL(String phoneNumber, String media_url, String caption) {
	return post(new GupShupReq(GupShupConstants.Method.SendMediaMessage).sendTo(phoneNumber)
		.messageType(GupShupConstants.MessageType.IMAGE).hsm(getIsHSM())
		.dataEncoding(GupShupConstants.DataEncoding.TEXT).mediaURL(media_url)
		.caption(CryptoUtil.getEncoder().message(caption).encodeURL().toString()));
    }

    public GupShupResp sendDocumentURL(String phoneNumber, String media_url, String caption) {
	return post(new GupShupReq(GupShupConstants.Method.SendMediaMessage).sendTo(phoneNumber)
		.messageType(GupShupConstants.MessageType.DOCUMENT).hsm(getIsHSM()).mediaURL(media_url)
		.caption(CryptoUtil.getEncoder().message(caption).encodeURL().toString()));
    }

    public GupShupResp sendDocument(String phoneNumber, MultipartFile file, String caption) throws IOException {
	GupShupResp media = uploadDocument(phoneNumber, file);
	return post(new GupShupReq(GupShupConstants.Method.SendMediaMessage).sendTo(phoneNumber)
		.messageType(GupShupConstants.MessageType.DOCUMENT).hsm(getIsHSM())
		.dataEncoding(GupShupConstants.DataEncoding.TEXT).mediaId(media.getResponse().getId())
		.caption(CryptoUtil.getEncoder().message(caption).encodeURL().toString()));
    }

    public OutboxMessage send(ChannelConfig channelConfig, OutboxMessage message) {

	String to = null;
	if (ArgUtil.is(message.getCsid())) {
	    to = message.getCsid();
	} else {
	    to = CollectionUtil.getOne(message.getTo());
	}

	GupShupReq gupShupReq = new GupShupReq();
	gupShupReq.setSendTo(to);
	gupShupReq.setPhoneNumber(to);
	gupShupReq.setMessageId(message.getMessageId());
	gupShupReq.setWaNumber(message.contact().getLane());

	GupShupResp resp = null;
	String id = null;

	StringJoiner msgIds = new StringJoiner(",");

	if (ArgUtil.is(message.getAttachments())) {
	    for (Attachment attachment : message.getAttachments()) {
		gupShupReq.setCaption(ArgUtil.nonEmpty(attachment.getMediaCaption(), message.getSubject()));
		gupShupReq.setMessage(message.getMessage());
		if (ArgUtil.is(attachment.getMediaURL())) {
		    if (ArgUtil.areEqual(attachment.getMediaType(), FileType.IMAGE.toString())) {
			gupShupReq.setMediaURL(attachment.getMediaURL());
			resp = sendImageURL(gupShupReq);
			if (ArgUtil.is(id = getMessageId(resp)))
			    msgIds.add(id);
		    } else {
			gupShupReq.setMediaURL(attachment.getMediaURL());
			resp = sendDocumentURL(gupShupReq);
			if (ArgUtil.is(id = getMessageId(resp)))
			    msgIds.add(id);
		    }
		}
	    }
	}

	if (ArgUtil.is(message.getMessage())) {
	    gupShupReq.setMessage(message.getMessage());
	    resp = sendMessage(gupShupReq, message);
	    if (ArgUtil.is(id = getMessageId(resp)))
		msgIds.add(id);
	}
	message.setMessageIdExt(msgIds.toString());

	return message;
    }

    private String getMessageId(GupShupResp resp) {
	if (!ArgUtil.is(resp) || !ArgUtil.is(resp.getResponse())) {
	    throw new PostManException("No Response Object");
	} else if (ArgUtil.isEqual(resp.getResponse().getStatus(), "error")) {
	    throw new PostManException(
		    String.format("%s : %s", resp.getResponse().getId(), resp.getResponse().getDetails()));
	}
	return resp.getResponse().getId();
    }

    public GupShupResp optIn(OutboxMessage outboxMessage) {
	GupShupReq gupShupReq = new GupShupReq(GupShupConstants.Method.OPT_IN).phoneNumber(outboxMessage.getCsid());
	gupShupReq.setChannel("WHATSAPP");
	gupShupReq.setWaNumber(outboxMessage.contact().getLane());
	return post(gupShupReq);
    }

    public GupShupResp sendDocumentURL(GupShupReq gupShupReq) {
	return post(gupShupReq);
    }

    public GupShupResp sendMessage(GupShupReq gupShupReq, MessageOptions options) {
	gupShupReq.method(GupShupConstants.Method.SendMessage);
	return post(gupShupReq);
    }

    public GupShupResp sendImageURL(GupShupReq gupShupReq) {
	return post(gupShupReq);
    }

    public GupShupResp sendAudioURL(GupShupReq gupShupReq) {
	return post(gupShupReq);
    }

    public GupShupResp sendVideoURL(GupShupReq gupShupReq) {
	return post(gupShupReq);
    }

}
