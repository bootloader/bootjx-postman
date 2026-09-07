
package com.boot.jx.dummy;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.boot.jx.bot.BotController;
import com.boot.jx.bot.ChatController;
import com.boot.jx.bot.ChatMapping;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.utils.StringUtils.StringMatcher;

@BotController(name = "DummyBot")
@ConditionalOnProperty(value = "app.local.dummy.bot.enabled", havingValue = "true")
public class DummyChatController extends ChatController {

    @ChatMapping(key = "***dummyStart***", pattern = "^*$")
    public void transferToAgent(InboxMessage inboxMessage, StringMatcher matcher) {
    	reply("Hello this is my reply to " + inboxMessage.getMessage());
		send(new OutboxMessage().message("Hello this is my send to " + inboxMessage.getMessage()));
    }

}
