package com.boot.jx.postman.gupshup;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.boot.jx.swagger.ApiMockModelProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GupShupDeliveryResp implements Serializable {

    private static final long serialVersionUID = 2208981728686809986L;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GupShupDeliveryDto implements Serializable {
	private static final long serialVersionUID = 5378482703874402475L;

	@ApiMockModelProperty(example = "3562707498794989059-328736121207676738",
		value = "Unique ID for each message 'causeId-msgId'")
	private String externalId;

	@ApiMockModelProperty(example = "DELIVERED", value = "Final status of the message",
		allowableValues = "SENT, DELIVERED, READ, FAILED")
	private String eventType;

	@ApiMockModelProperty(example = "1526347800000", value = "Time of event as a LONG number")
	private Long eventTs;

	@ApiMockModelProperty(example = "91989237777", value = "In case of WhatsApp, phone number of the recipient.")
	private String destAddr;

	@ApiMockModelProperty(example = "919223399999",
		value = "In case of WhatsApp, this is WhatsApp Business phone number.")
	private String srcAddr;

	@ApiMockModelProperty(example = "SUCCESS",
		value = "This is the response you will get depending on the eventType. Various"
			+ " causes and their explanation are below.",
		allowableValues = "SUCCESS, SENT, READ, OTHER,UNKNOWN_SUBSCRIBER,DEFERRED,BLOCKED_FOR_USER")
	private String cause;

	@ApiMockModelProperty(example = "25", value = "error code assigned to different delivery failure causes,")
	private String errorCode;

	@ApiMockModelProperty(example = "DELIVERED", value = "Final status of the message",
		allowableValues = "WHATSAPP, SMS")
	private String channel;

	public String getExternalId() {
	    return externalId;
	}

	public void setExternalId(String externalId) {
	    this.externalId = externalId;
	}

	public String getEventType() {
	    return eventType;
	}

	public void setEventType(String eventType) {
	    this.eventType = eventType;
	}

	public Long getEventTs() {
	    return eventTs;
	}

	public void setEventTs(Long eventTs) {
	    this.eventTs = eventTs;
	}

	public String getDestAddr() {
	    return destAddr;
	}

	public void setDestAddr(String destAddr) {
	    this.destAddr = destAddr;
	}

	public String getSrcAddr() {
	    return srcAddr;
	}

	public void setSrcAddr(String srcAddr) {
	    this.srcAddr = srcAddr;
	}

	public String getCause() {
	    return cause;
	}

	public void setCause(String cause) {
	    this.cause = cause;
	}

	public String getErrorCode() {
	    return errorCode;
	}

	public void setErrorCode(String errorCode) {
	    this.errorCode = errorCode;
	}

	public String getChannel() {
	    return channel;
	}

	public void setChannel(String channel) {
	    this.channel = channel;
	}
    }

    public GupShupDeliveryResp() {
	super();
	this.response = new ArrayList<GupShupDeliveryDto>();
    }

    private List<GupShupDeliveryDto> response;

    public List<GupShupDeliveryDto> getResponse() {
	return response;
    }

    public void setResponse(List<GupShupDeliveryDto> response) {
	this.response = response;
    }

}
