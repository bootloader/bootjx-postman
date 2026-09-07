package com.boot.jx.postman.model.ext;

import com.boot.jx.postman.model.MessageDefinitions.LoggableEntity;
import com.boot.jx.postman.model.MessageDefinitions.SessionInfo;
import com.boot.jx.postman.model.MessageDefinitions.TraceMessage;
import com.boot.jx.swagger.ApiMockModelProperty;

public class InBoundEvent extends SessionBoundEvent implements LoggableEntity, SessionInfo, TraceMessage {

	private static final long serialVersionUID = -8470839812429749401L;

	public InBoundEvent() {
		super();
	}

	public InBoundEvent eventCode(String eventCode) {
		this.type = eventCode;
		return this;
	}

	public static class SessionRouted {
		public String routingId;
		public boolean sessionStart;
		public String sourceQueue;
		public String targetQueue;

		@ApiMockModelProperty(value = "Additional params sent by Router")
		public Object params;
	}

	public static class SessionAssigned {
		public String oldDept;
		public String newDept;
		public String oldAgent;
		public String newAgent;
		public String oldBot;
		public String newBot;
	}

	public SessionRouted sessionRouted;

	public SessionAssigned sessionAssigned;

	public SessionAssigned sessionAssigned() {
		if (this.sessionAssigned == null) {
			this.sessionAssigned = new SessionAssigned();
		}
		return this.sessionAssigned;
	}

}