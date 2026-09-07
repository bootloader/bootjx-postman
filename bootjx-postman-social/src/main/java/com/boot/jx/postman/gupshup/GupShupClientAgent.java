package com.boot.jx.postman.gupshup;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.boot.jx.postman.PMConfiguration;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.gupshup.GupShupConstants.SessionType;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.MessageDefinitions.IMessageExtended;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.plugin.WAGupShupPlugin.GupShupConfigDetails;
import com.boot.jx.rest.RestService.Ajax;
import com.boot.jx.utils.PostManUtil;
import com.boot.utils.ArgUtil;

@Component
public class GupShupClientAgent extends GupShupClientAbstract {

    @Autowired
    private PMEnvironment environment;

    @Override
    public SessionType getSessionType() {
	return SessionType.AGENT;
    }

    public Map<String, Object> sendViaAgent(IMessageExtended inboxMessage, String message) {

	PMConfiguration config = environment.local();
	String channelId = PostManUtil.CHANNEL_ID(inboxMessage.contact());
	ChannelConfig channelConfig = config.channel(channelId);

	GupShupAgentReq gupShupAgentReq = new GupShupAgentReq();
	gupShupAgentReq.setMobile(inboxMessage.getFrom());
	gupShupAgentReq.setWaNumber(inboxMessage.to().get(0));
	gupShupAgentReq.setName(inboxMessage.getFromName());
	gupShupAgentReq.setType(GupShupConstants.MessageType.text);
	gupShupAgentReq.setMsg(message);
	return this.restService.ajax(channelConfig.getGupshup().getAgentUrl()).path("/WhatsAppConnector/api")
		.header("type", "BotRequest").post(gupShupAgentReq).asMap();
    }

    public Map<String, Object> getToken(GupShupConfigDetails configDetails, String waNumber, String mobile) {
	return this.restService.ajax(configDetails.getAgentUrl()).path("/WhatsAppConnector/api")
		.header("type", "getToken").field("userId", configDetails.getChatId())
		.field("password", configDetails.getChatPass()).field("phoneNo", mobile).field("waNumber", waNumber)
		.postForm().asMap();
    }

    public Map<String, Object> assignToAgent(InboxMessage inboxMessage) {

	PMConfiguration config = environment.local();
	String channelId = PostManUtil.CHANNEL_ID(inboxMessage.contact());
	ChannelConfig channelConfig = config.channel(channelId);

	String waNumber = inboxMessage.getTo().get(0);
	String mobile = inboxMessage.getFrom();
	String deptName = inboxMessage.session().getDept();

	String token = ArgUtil.parseAsString(this.getToken(channelConfig.getGupshup(), waNumber, mobile).get("token"));
	Ajax x = this.restService.ajax(channelConfig.getGupshup().getAgentUrl()).path("/WhatsAppConnector/api")
		.header("type", "TransferRequestToAgent").field("token", token).field("phoneNo", mobile)
		.field("waNumber", waNumber);
	if (ArgUtil.is(deptName)) {
	    x.field("deptName", deptName);
	}
	return x.postForm().asMap();
    }

}
