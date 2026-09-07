package com.boot.jx.agent;

import com.boot.utils.StringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.dto.ChatMessageDTO;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.OutboxMessage;

@Component
public class AgentService {

	@Autowired(required = false)
	private AgentChatHandler agentChatHandler;
	
	public InboxMessage onMessage(InboxMessage inboxMessage) {
		return agentChatHandler.onMessageReceive(inboxMessage);
	}

	public ChatMessageDTO sendMessage(ChatSessionDoc sessionDoc, OutboxMessage outboxMessage) {
		outboxMessage.setMessage(StringUtils.trim(outboxMessage.getMessage()));
		return agentChatHandler.onSend(sessionDoc, outboxMessage);
	}

}
