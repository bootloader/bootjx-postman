package com.boot.jx.postman;

import com.boot.jx.logger.AuditEvent;
import com.boot.utils.EnumType;

public class PMAuditEvent extends AuditEvent<PMAuditEvent> {

    public PMAuditEvent(EventType type) {
	super(type);
    }

    private static final long serialVersionUID = 8827531092425201809L;

    public static enum Type implements EventType {
	DEFAULT_EVENT, INBOUND_ERROR;

	public static final EnumType DEFAULT = DEFAULT_EVENT;

	@Override
	public EventMarker marker() {
	    return null;
	}
    }

}