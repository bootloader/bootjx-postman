package com.boot.jx.postman.service;

import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.TagDocument;

public interface CommonNLPService {
	public InboxMessage addTags(InboxMessage inboxMessage, TagDocument tags);

	default public InboxMessage addTags(InboxMessage inboxMessage) {
		return this.addTags(inboxMessage, inboxMessage.getTags());
	}
}
