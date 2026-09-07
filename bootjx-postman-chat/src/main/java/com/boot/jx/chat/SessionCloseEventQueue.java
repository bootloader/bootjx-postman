package com.boot.jx.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.boot.jx.AppContextUtil;
import com.boot.jx.aws.SQSQueueProcessor;
import com.boot.jx.postman.PMConstants.CHAT_STATUS;
import com.boot.jx.postman.model.ext.SessionBoundEvent;
import com.boot.jx.postman.model.ext.SessionBoundEvent.EVENT_TYPE;
import com.boot.utils.ArgUtil;

@Component
public class SessionCloseEventQueue extends SQSQueueProcessor<SessionBoundEvent> {

	private static final String SESSION_CLOSE_EVENT = "session-close-event";

	private static final Logger LOGGER = LoggerFactory.getLogger(SessionCloseEventQueue.class);

	@Override
	public String getQueueName() {
		return SESSION_CLOSE_EVENT;
	}

	public String messageGroupId(SessionBoundEvent report) {
		return AppContextUtil.getTenant();
		// return report.getContactId() + "-" + report.getSessionId();
	}

	@Autowired
	private ChatSessionService chatSessionService;

	@Scheduled(fixedDelay = 1500)
	public void scheduler() {
		process();
	}

	@Override
	public void process(SessionBoundEvent event) {
		CHAT_STATUS reasonStatus = ArgUtil.is(event.type, EVENT_TYPE.SESSION_EXPIRED) ? CHAT_STATUS.EXPIRED : null;
		chatSessionService.closeSession(event.getSessionId(), reasonStatus);
	}

}
