package com.boot.jx.postman.model.ext;

import java.io.Serializable;
import java.util.List;

import com.boot.jx.swagger.ApiMockModelProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * https://developers.facebook.com/docs/whatsapp/api/webhooks
 * 
 * @author lalittanwar
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class InBoundWrapper implements Serializable {
	private static final long serialVersionUID = 7766790295486098869L;
	@ApiMockModelProperty(example = "message", value = "Type of Event",
			allowableValues = "messages,actions,events,statuses")
	public String type;
	public InBoundMeta meta;
	public List<InBoundContact> contacts;
	public List<InBoundMsg> messages;
	public List<InBoundAction> actions;
	public List<InBoundEvent> events;
	public List<InBoundMsgStatus> statuses;

	public InBoundWrapper type(String type) {
		this.type = type;
		return this;
	}
}
