package com.boot.jx.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.boot.jx.AppContextUtil;
import com.boot.jx.aws.SQSQueueProcessor;
import com.boot.jx.aws.SqsQueueManager.SQSQueue;
import com.boot.jx.postman.client.CommonServiceClient;
import com.boot.jx.postman.model.ext.SessionBoundEvent;

@Component
public class SessionBoundEventQueue extends SQSQueueProcessor<SessionBoundEvent> {
	private static final Logger LOGGER = LoggerFactory.getLogger(SessionBoundEventQueue.class);

	private static final SQSQueue QUEUE = new SQSQueue("session-bound-event").fifo().contentBasedDeduplication();

	@Autowired
	private CommonServiceClient commonServiceClient;

	@Override
	public SQSQueue initQueue(String queueName) {
		return QUEUE;
	}

	@Override
	public SessionBoundEvent preQueue(SessionBoundEvent event) {
		event.fixEvent();
		return event;
	}

	@Override
	public String messageGroupId(SessionBoundEvent report) {
		return report.getContactId() + "-" + report.getSessionId();
	}

	@Override
	public String messageDeduplicationId(SessionBoundEvent report) {
		return AppContextUtil.getTenant() + ":" + report.uniqueId();
	}

//	@Scheduled(fixedDelay = 1500)
//	public void scheduler() {
//		process();
//	}

	@Override
	public void process(SessionBoundEvent message) {
		// LOGGER.info("AppCOn" + AppContextUtil.getTenant());
		commonServiceClient.publishSessionBoundEvent(message);
	}

	@Override
	public String getQueueName() {
		return "session-bound-event";
	}

}
