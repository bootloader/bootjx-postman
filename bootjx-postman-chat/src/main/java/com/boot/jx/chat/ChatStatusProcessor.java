package com.boot.jx.chat;

import java.util.Comparator;

import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.boot.jx.AppConfig;
import com.boot.jx.AppContextUtil;
import com.boot.jx.aws.SQSQueueProcessor;
import com.boot.jx.inbound.InBound.InBoundHandler;
import com.boot.jx.postman.doc.MessageDoc;
import com.boot.jx.postman.guard.MessageFailureGuard;
import com.boot.jx.postman.model.MessageReport;
import com.boot.jx.postman.store.MessageContext;
import com.boot.jx.postman.store.MessageStore;
import com.boot.jx.stomp.StompTunnelService;
import com.boot.jx.tunnel.TunnelService;
import com.boot.jx.tunnel.task.BatchJobProvider.BatchJobPool;
import com.boot.jx.tunnel.task.BatchJobProvider.BatchJobPool.STAGES;
import com.boot.jx.utils.PostManUtil;
import com.boot.utils.ArgUtil;
import com.boot.utils.UniqueID;

@Component
public class ChatStatusProcessor extends SQSQueueProcessor<MessageReport> {

	private static final String CHAT_STATUS_QUEUE_NAME = "chat-status";

	private static final Logger LOGGER = LoggerFactory.getLogger(ChatStatusProcessor.class);

	@Autowired
	private AppConfig appConfig;

	@Override
	public String getQueueName() {
		return CHAT_STATUS_QUEUE_NAME;
	}

	@Override
	public boolean isQueueEnabled() {
		return ArgUtil.isEqual(appConfig.getAppType(), "POSTMAN");
	}

	// Default is, only for postman value is set 1000 in props
	@Scheduled(fixedDelayString = "${queue.chat-status.process.delay:360000}")
	public void scheduler() {
		process();
	}

	public static class MessageReportComparator implements Comparator<MessageReport> {
		@Override
		public int compare(MessageReport o1, MessageReport o2) {
			return (int) (o1.getChangeStamp() - o2.getChangeStamp());
		}
	}

	@Autowired
	private MessageStore messageStore;

	@Autowired
	private StompTunnelService stompTunnelService;

	@Autowired(required = false)
	private InBoundHandler inBoundHandler;

	@Autowired
	private ChatProxyManager proxyManager;

	@Autowired(required = false)
	private MessageContext messageContext;

	@Autowired(required = false)
	private MessageFailureGuard messageFailureGuard;

	@Autowired(required = false)
	private RedissonClient redisson;

	@Autowired
	private TunnelService tunnelService;

	private BatchJobPool jobPool;

	public BatchJobPool jobPool() {
		if (this.jobPool == null) {
			this.jobPool = new BatchJobPool(getQueueName(), tunnelService, redisson, appConfig.getAppName());
		}
		return this.jobPool;
	}

	public String messageGroupId(MessageReport report) {
		String contactId = PostManUtil.createContactId(report.contact());
		return AppContextUtil.getTenant() + ":" + contactId;
	}

	@Override
	public void process(MessageReport messageReport) {
		// Check for Proxy Account
		String contactId = PostManUtil.createContactId(messageReport.contact());
		if (ArgUtil.is(contactId)) {
			String proxy = proxyManager.get(contactId);
			if (ArgUtil.is(proxy)) {
				AppContextUtil.clear();
				AppContextUtil.setTenant(proxy);
				String sessionId = UniqueID.generateString();
				AppContextUtil.setSessionId(sessionId);
				AppContextUtil.getTraceId(true, true);
				AppContextUtil.resetTraceTime();
				AppContextUtil.init();
			}
		}
		MessageDoc m = messageStore.updateStatusFast(messageReport);

		if (messageFailureGuard != null) {
			try {
				messageFailureGuard.recordFailedReport(m, messageReport);
			} catch (Exception ex) {
				LOGGER.warn("messageFailureGuard.recordFailedReport skipped: {}", ex.getMessage());
			}
		}

		if (ArgUtil.is(m)) {
			LOGGER.debug("@update MessageId({}) BulkSessionId({})", m.getMessageId(), m.getBulkSessionId());
		} else {
			LOGGER.warn("@update MessageId({}) BulkSessionId({}) : No MessageDoc", messageReport.getMessageId(),
					messageReport.getSessionBulkId());
		}

		if (ArgUtil.is(inBoundHandler)) {
			messageContext.setMessageReport(messageReport);
			if (ArgUtil.is(messageContext) && ArgUtil.is(m)) {
				LOGGER.debug("@update MessageId({}) BulkSessionId({}) : ContextSet", m.getMessageId(),
						m.getBulkSessionId());
				messageContext.setMessageDoc(m);
			}
			inBoundHandler.doHandle(messageReport);
		} else {
			stompTunnelService.sendToAll("/message/update/status", messageReport);
		}

		jobPool().incCounter(STAGES.status, 1, messageReport.contact().getChannelType(),
				messageReport.origin().getAppType(), ArgUtil.parseAsString(messageReport.getStatus()));
	}

}
