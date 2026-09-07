package com.boot.jx.postman.store;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.boot.jx.logger.LoggerService;
import com.boot.jx.postman.doc.ChatContactDoc;
import com.boot.jx.postman.doc.MessageDoc;
import com.boot.jx.scope.ThreadScoped;

@Component
@ThreadScoped
public class MessageContextStore {

	private static final Logger LOGGER = LoggerFactory.getLogger(MessageContextStore.class);

	private MessageDoc messageDoc;
	private ChatContactDoc chatContactDoc;

	public ChatContactDoc getChatContactDoc() {
		return chatContactDoc;
	}

	public ChatContactDoc getChatContactDoc(String contactId) {
		return chatContactDoc;
	}

	public void setChatContactDoc(ChatContactDoc chatContactDoc) {
		if (LoggerService.isLocalDebug())
			LOGGER.info("{} [CHAT_CONTACT]", chatContactDoc == null ? "clear" : "store");
		this.chatContactDoc = chatContactDoc;
	}

	public MessageDoc getMessageDoc() {
		return messageDoc;
	}

	public void setMessageDoc(MessageDoc messageDoc) {
		this.messageDoc = messageDoc;
	}

	public MessageContextStore from(MessageContextStore contextStore) {
		this.messageDoc = contextStore.getMessageDoc();
		this.chatContactDoc = contextStore.getChatContactDoc();
		if (LoggerService.isLocalDebug()) {
			LOGGER.info("{} [CHAT_CONTACT]", chatContactDoc == null ? "blank" : "copy");
		}
		return this;
	}

	@PostConstruct
	public void init() {
		// System.out.println("MessageContext:init");
	}

	@PreDestroy
	public void destroy() {
		this.chatContactDoc = null;
		this.messageDoc = null;
	}

}
