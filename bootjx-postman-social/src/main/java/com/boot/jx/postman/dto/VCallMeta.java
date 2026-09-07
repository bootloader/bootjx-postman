package com.boot.jx.postman.dto;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Voice/call permission state for a chat contact. 
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VCallMeta implements Serializable {

	private static final long serialVersionUID = 1L;

	private boolean permission;
	private long grantedAt;

	public boolean isPermission() {
		return permission;
	}

	public void setPermission(boolean permission) {
		this.permission = permission;
	}

	public long getGrantedAt() {
		return grantedAt;
	}

	public void setGrantedAt(long grantedAt) {
		this.grantedAt = grantedAt;
	}
}
