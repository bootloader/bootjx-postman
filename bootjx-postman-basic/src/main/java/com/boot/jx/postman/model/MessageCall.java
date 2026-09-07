package com.boot.jx.postman.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageCall implements Serializable {
	private static final long serialVersionUID = 1L;

	public static class EVENTS {
		public static String CALL_PERMISSION_REPLY = "call_permission_reply";
	}

	private String id;
	private String event;
	private String direction;
	private Long timestamp;
	private String sdpType;
	private String status;
	private Long endStamp;
	private Long duration;
	private String agentId;

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Long getEndStamp() {
		return endStamp;
	}

	public void setEndStamp(Long endStamp) {
		this.endStamp = endStamp;
	}

	public Long getDuration() {
		return duration;
	}

	public void setDuration(Long duration) {
		this.duration = duration;
	}

	public String getAgentId() {
		return agentId;
	}

	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getEvent() {
		return event;
	}

	public void setEvent(String event) {
		this.event = event;
	}

	public String getDirection() {
		return direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	public String getSdpType() {
		return sdpType;
	}

	public void setSdpType(String sdpType) {
		this.sdpType = sdpType;
	}

	@Override
	public String toString() {
		return "MessageCall{id=" + id + ", event=" + event + ", direction=" + direction + ", timestamp=" + timestamp
				+ ", sdpType=" + sdpType + ", status=" + status + ", endStamp=" + endStamp + ", duration=" + duration
				+ ", agentId=" + agentId + "}";
	}
}
