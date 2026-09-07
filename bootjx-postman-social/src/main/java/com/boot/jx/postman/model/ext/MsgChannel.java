package com.boot.jx.postman.model.ext;

import java.io.Serializable;

import com.boot.jx.dict.ContactType;
import com.boot.jx.swagger.ApiMockModelProperty;
import com.boot.model.UtilityModels.JsonIgnoreUnknown;

public class MsgChannel implements Serializable, JsonIgnoreUnknown {

    private static final long serialVersionUID = -476861137283561876L;

    @ApiMockModelProperty(example = "WHATSAPP", value = "Contact Type")
    public ContactType contactType;

    @ApiMockModelProperty(example = "wa360:918828218374", value = "Channel used")
    private String channelId;

    @ApiMockModelProperty(example = "919999998888",
	    value = "Contact Used by User while sending message" + "eg your business number or email address")
    public String lane;

    @ApiMockModelProperty(example = "Customer Support", value = "Channel name set in configuration")
    public String name;

    public ContactType getContactType() {
	return contactType;
    }

    public void setContactType(ContactType contactType) {
	this.contactType = contactType;
    }

    public String getChannelId() {
	return channelId;
    }

    public void setChannelId(String channelId) {
	this.channelId = channelId;
    }

    public String getLane() {
	return lane;
    }

    public void setLane(String lane) {
	this.lane = lane;
    }

    public String getName() {
	return name;
    }

    public void setName(String name) {
	this.name = name;
    }

}
