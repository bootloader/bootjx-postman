package com.boot.jx.inbound;

import java.util.List;

import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import com.boot.jx.AppConfig;
import com.boot.jx.cache.CacheBox;
import com.boot.jx.chat.ChatSessionService;
import com.boot.jx.def.ICacheBox;
import com.boot.jx.mongo.CommonMongoQueryBuilder;
import com.boot.jx.postman.doc.MessageHold;
import com.boot.jx.postman.doc.MessageHold.MessageHoldQueue;
import com.boot.jx.postman.manager.ChatLogger;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.PMArgs;
import com.boot.jx.postman.model.ext.InBoundEvent;
import com.boot.jx.postman.store.MessageStore;
import com.boot.jx.tunnel.ITunnelDefs.TunnelTask;
import com.boot.jx.tunnel.task.ATaskLimiter;
import com.boot.jx.utils.PostManUtil;
import com.boot.utils.ArgUtil;
import com.boot.utils.UniqueID;

@Component
public class InboundBottler extends ATaskLimiter {

	private static final String MESSAGE_DEQUEUE = "MESSAGE_DEQUEUE";

	private CacheBox<String> holdManager;

	@Autowired(required = false)
	private RedissonClient redisson;

	public ICacheBox<String> hold() {
		if (holdManager == null) {
			this.holdManager = CacheBox.getInstance("InboundBottler-" + appConfig.getAppType() + "-Hold-", redisson);
		}
		return this.holdManager;
	}

	@Autowired
	private InBoundService inBoundService;

	@Autowired
	private ChatSessionService chatSessionService;

	@Autowired
	private AppConfig appConfig;

	@Autowired
	private MessageStore messageStore;

	@Autowired
	private ChatLogger logManager;

	public void push(InboxMessage inboxMessage) {
		String contactId = PostManUtil.CONTACT_ID(inboxMessage.contact());
		String onhold = hold().get(contactId);

		logManager.addTrace(inboxMessage, "InboundBottler:push");

		if (ArgUtil.isEqual(onhold, "QUEUING")) { // QUEING
			queue(contactId, new MessageHoldQueue().inboxMessage(inboxMessage));
			throttle(new TunnelTask().name(MESSAGE_DEQUEUE).id(contactId).intervalSeconds(2));
		} else { // DEQUEING
			hold().put(contactId, "QUEUING");
			logManager.addTrace(inboxMessage, "InboundBottler:push:invoked");
			inBoundService.invokeMethodsSync(inboxMessage);
			hold().put(contactId, "DEQUEUING");
		}
		onhold = hold().get(contactId);
		if (!ArgUtil.isEqual(onhold, "QUEUING")) {
			this.dequeue(contactId);
		}

	}

	public InBoundEvent sessionEvent(InBoundEvent event, PMArgs pmArgs) {
		String contactId = PostManUtil.CONTACT_ID(event.contact());
		String onhold = hold().get(contactId);

		if (ArgUtil.isEqual(onhold, "QUEUING")) {
			queue(contactId, new MessageHoldQueue().event(event).pmArgs(pmArgs));
			throttle(new TunnelTask().name(MESSAGE_DEQUEUE).id(contactId).intervalSeconds(2));
		} else {
			hold().put(contactId, "QUEUING");
			chatSessionService.sessionEvent(event, pmArgs);
			hold().put(contactId, "DEQUEUING");
		}
		onhold = hold().get(contactId);
		if (!ArgUtil.isEqual(onhold, "QUEUING")) {
			this.dequeue(contactId);
		}

		return event;
	}

	private void dequeue(String contactId) {
		String batch = UniqueID.generateString();
		CommonMongoQueryBuilder builder = new CommonMongoQueryBuilder();
		builder.where(Criteria.where("contactId").is(contactId).and("appType").is(appConfig.getAppType()).and("batch")
				.exists(false)).sortBy("timestamp", Direction.ASC);
		builder.set("batch", batch);
		messageStore.updateMulti(builder.getQuery(), builder.update(), MessageHoldQueue.class,
				MessageHoldQueue.COLLECTION_QUEUED);

		CommonMongoQueryBuilder builder2 = new CommonMongoQueryBuilder();
		builder2.where(Criteria.where("contactId").is(contactId).and("appType").is(appConfig.getAppType()).and("batch")
				.is(batch)).sortBy("timestamp", Direction.ASC);
		List<MessageHoldQueue> docs = messageStore.findAllAndRemove(builder2.getQuery(), MessageHoldQueue.class,
				MessageHoldQueue.COLLECTION_QUEUED);

		for (MessageHoldQueue doc : docs) {
			if (ArgUtil.is(doc)) {
				if (ArgUtil.is(doc.getInboxMessage())) {
					logManager.addTrace(doc.getInboxMessage(), "InboundBottler:dequeue:batch=" + batch);
					inBoundService.invokeMethodsSync(doc.getInboxMessage());
				} else if (ArgUtil.is(doc.getEvent())) {
					chatSessionService.sessionEvent(doc.getEvent(), doc.getPmArgs());
				}
			}
		}
	}

	private void queue(String contactId, MessageHold hold) {
		hold.setContactId(contactId);
		hold.setTimestamp(System.currentTimeMillis());
		hold.setAppType(appConfig.getAppType());
		if (ArgUtil.is(hold.getInboxMessage())) {
			logManager.addTrace(hold.getInboxMessage(), "InboundBottler:queue");
		}
		messageStore.save(hold);
	}

	@Override
	public void doTaskSafely(TunnelTask task) {
		if (MESSAGE_DEQUEUE.equals(task.getName())) {
			String contactId = task.getId();
			hold().put(contactId, "DEQUEUING");
			this.dequeue(contactId);
		}
	}

}
