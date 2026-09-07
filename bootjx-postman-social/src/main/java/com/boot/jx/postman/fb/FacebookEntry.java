package com.boot.jx.postman.fb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FacebookEntry implements Serializable {
	private static final long serialVersionUID = 4844872478399699245L;
	private String id;
	private Long time;
	private List<FacebookMessaging> messaging = new ArrayList<>();
	private List<Map<String,Object>> changes = new ArrayList<>();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Long getTime() {
		return time;
	}

	public void setTime(Long time) {
		this.time = time;
	}

	public List<FacebookMessaging> getMessaging() {
		return messaging;
	}

	public void setMessaging(List<FacebookMessaging> messaging) {
		this.messaging = messaging;
	}

	public List<Map<String,Object>> getChanges() {
		return changes;
	}

	public void setChanges(List<Map<String,Object>> changes) {
		this.changes = changes;
	}
}
