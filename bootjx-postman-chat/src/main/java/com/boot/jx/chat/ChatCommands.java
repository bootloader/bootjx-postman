package com.boot.jx.chat;

import java.util.HashMap;
import java.util.Map;

import com.boot.jx.postman.model.Message;
import com.boot.utils.ArgUtil;

public class ChatCommands {

	public static Map<String, String> strMapping = new HashMap<String, String>();

	public static void registerCommand(String command, String action) {
		strMapping.put(command, action);
	}

	public static void init() {
		registerCommand("/exit_chat", "RESOLVE");
		registerCommand("/add_stick_note", "ADD_STICKY_NOTE");
		registerCommand("RESOLVE", "RESOLVE");
	}

	public static String getCommand(Message<?> outboxMessage) {
		if (ArgUtil.is(outboxMessage.getAction())) {
			return strMapping.getOrDefault(outboxMessage.getAction(), outboxMessage.getAction());
		} else if (ArgUtil.is(outboxMessage.getMessage())) {
			return strMapping.get(outboxMessage.getMessage());
		} 
		return null;
	}

	static {
		init();
	}
}
