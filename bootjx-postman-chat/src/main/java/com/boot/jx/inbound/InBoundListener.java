package com.boot.jx.inbound;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.boot.jx.bot.BotEngine;
import com.boot.jx.dict.ContactType;
import com.boot.jx.postman.events.UserInboxEvent;
import com.boot.jx.postman.manager.ChatLogger;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.tunnel.ITunnelSubscriber;
import com.boot.jx.tunnel.TunnelEventMapping;
import com.boot.jx.tunnel.TunnelEventXchange;
import com.boot.utils.ArgUtil;
import com.boot.utils.EntityDtoUtil;

@TunnelEventMapping(byEvent = UserInboxEvent.class, scheme = TunnelEventXchange.TASK_WORKER)
public class InBoundListener implements ITunnelSubscriber<UserInboxEvent> {

	private static final Logger LOGGER = LoggerFactory.getLogger(InBoundListener.class);

	@Autowired
	private BotEngine botEngine;

	@Autowired
	private ChatLogger logManager;

	@Override
	public void onMessage(String channel, UserInboxEvent event) {
		InboxMessage inboxMessageOriginal = EntityDtoUtil.entityToDto(event, new InboxMessage());
		inboxMessageOriginal.contact().setContactType(
				ArgUtil.parseAsEnumT(event.getContactType(), ContactType.WHATSAPP, ContactType.class).toString());
		botEngine.invokeMethods(inboxMessageOriginal);
	}

}
