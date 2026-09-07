package com.boot.jx.mock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.boot.jx.mongo.CommonMongoTemplate;
import com.boot.jx.postman.PMConstants.CHANNEL_TYPE;
import com.boot.jx.postman.guard.MessageFailureGuard;
import com.boot.jx.postman.doc.ChatContactDoc;
import com.boot.jx.postman.doc.MessageDoc;
import com.boot.jx.postman.model.Message.Status;
import com.boot.jx.postman.model.MessageReport;
import com.boot.jx.postman.model.MessageReport.MessageReportError;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.query.ChatContactQuery;
import com.boot.model.MapModel;
import com.boot.utils.ArgUtil;
import com.boot.utils.TimeUtils;

/**
 * Skips Meta API calls for MARKETING messages after a real mockable failure, by
 * replaying a hardcoded error within a configured window. Distinct from
 * {@link MockMessengerImpl}, which handles mock phone numbers and synthetic
 * success/webhook flows.
 * <p>
 * Inbound {@code FAILD} webhooks write {@link ChatContactDoc#getLatestErrors()}
 * directly so the replay window is durable across instances. Outbound API
 * {@code SENT_ERR} capture piggybacks on the {@link ChatContactQuery} update in
 * {@code ConnectorHandlerFactory}.
 */
@Component
public class MessageFailureGuardImpl implements MessageFailureGuard {

	private static final String MARKETING = "MARKETING";
	private static final String CODE_PREFIX = "Code:";

	private static final Map<String, MockErrorConfig> REGISTRY = new HashMap<>();
	static {
		REGISTRY.put("131049",
				new MockErrorConfig(TimeUtils.toMillis("24h"), "131049",
						"This message was not delivered to maintain healthy ecosystem engagement.",
						"Marketing message blocked by Meta frequency capping."));

		REGISTRY.put("131026", new MockErrorConfig(TimeUtils.toMillis("8d"), "131026",
				"Message undeliverable", "Recipient is not available to receive this message."));

		REGISTRY.put("131056", new MockErrorConfig(TimeUtils.toMillis("6h"), "131056",
				"(#131056) (Business Account, Consumer Account) pair rate limit hit",
				"Too many messages sent from the sender phone number to the same recipient phone number in a short period of time."));
	}

	@Autowired
	private CommonMongoTemplate commonMongoTemplate;

	private boolean isMarketingMsg(OutboxMessage outboxMessage) {
		return isMarketing(outboxMessage.categoryType());
	}

	private boolean isMarketing(String categoryType) {
		return MARKETING.equalsIgnoreCase(categoryType);
	}

	/**
	 * LOAD: sets {@code outbox.replayErrorCode} when contact has a valid registered
	 * error.
	 */
	@Override
	public void resolve(OutboxMessage outboxMessage, ChatContactDoc contact) {
		if (!isMarketingMsg(outboxMessage) || !ArgUtil.is(contact)) {
			return;
		}
		Map<String, Long> errors = contact.getLatestErrors();
		if (!ArgUtil.is(errors) || errors.isEmpty()) {
			return;
		}

		String pickedCode = null;
		long pickedStamp = 0L;

		for (Map.Entry<String, Long> entry : errors.entrySet()) {
			String code = entry.getKey();
			Long stamp = entry.getValue();
			if (!ArgUtil.is(code) || stamp == null) {
				continue;
			}
			MockErrorConfig cfg = REGISTRY.get(code);
			if (cfg == null || TimeUtils.isExpired(stamp, cfg.windowMs)) {
				continue;
			}
			if (stamp >= pickedStamp) {
				pickedStamp = stamp;
				pickedCode = code;
			}
		}
		if (!ArgUtil.is(pickedCode)) {
			return;
		}

		outboxMessage.setReplayErrorCode(pickedCode);
		outboxMessage.meta().put(MessageFailureGuard.META_REPLAY_ERROR_CODE, pickedCode);
	}

	/**
	 * API / {@code getMessageId()} error path: transient code for {@code capture()} only.
	 * Does not set {@code meta.replayErrorCode} — that marks an armed replay skip.
	 */
	@Override
	public void escalate(OutboxMessage outboxMessage, String errorCode) {
		if (!ArgUtil.is(errorCode)) {
			return;
		}
		outboxMessage.setReplayErrorCode(errorCode);
	}

	@Override
	public MapModel enforce(OutboxMessage outboxMessage) {
		if (!isMarketingMsg(outboxMessage)) {
			return null;
		}
		String code = outboxMessage.getReplayErrorCode();
		if (!ArgUtil.is(code)) {
			return null;
		}
		MockErrorConfig cfg = REGISTRY.get(code);
		return cfg == null ? null : cfg.buildBody();
	}

	/**
	 * SYNC CATCH: mutates {@code contactQuery} only; factory persists via existing
	 * updateFirst.
	 */
	@Override
	public void capture(OutboxMessage outboxMessage, ChatContactQuery contactQuery) {
		if (contactQuery == null || !isMarketingMsg(outboxMessage)) {
			return;
		}
		if (isReplayResolved(outboxMessage)) {
			return;
		}
		if (!ArgUtil.is(outboxMessage.getStatus(), Status.SENT_ERR)) {
			return;
		}
		String code = outboxMessage.getReplayErrorCode();
		if (!ArgUtil.is(code)) {
			return;
		}
		MockErrorConfig cfg = REGISTRY.get(code);
		if (cfg == null) {
			return;
		}
		stampLatestErrorIfNeeded(contactQuery, code, cfg);
	}

	/**
	 * REPLAY: mutates outbox to {@code FAILD} in memory before
	 * {@code createOrUpdate}.
	 */
	@Override
	public boolean applyReplayFailure(OutboxMessage outboxMessage) {
		if (!isReplayResolved(outboxMessage)) {
			return false;
		}
		if (!ArgUtil.is(outboxMessage.getStatus(), Status.SENT_ERR)) {
			return false;
		}
		String code = ArgUtil.parseAsString(outboxMessage.meta().get(MessageFailureGuard.META_REPLAY_ERROR_CODE));
		if (!ArgUtil.is(code)) {
			return false;
		}
		outboxMessage.updateStatus(Status.SENT);  
		outboxMessage.updateStatus(Status.FAILD);
		outboxMessage.logs().clear(); // added because few errors have extra handling done in getmessageId.
		outboxMessage.logs().add(CODE_PREFIX + code);
		return true;
	}

	/**
	 * WEBHOOK CATCH: persists {@code latestErrors} on the contact from a
	 * delivery-status {@code FAILD} webhook. One partial {@code updateFirst} — no
	 * in-memory bridge.
	 */
	@Override
	public void recordFailedReport(MessageDoc doc, MessageReport report) {
		if (doc == null || report == null) {
			return;
		}
		if (!ArgUtil.is(report.getStatus(), Status.FAILD)) {
			return;
		}
		if (!CHANNEL_TYPE.WACFB.equals(report.contact().getChannelType())) {
			return;
		}
		if (!isMarketing(ArgUtil.parseAsString(doc.meta().get("categoryType")))) {
			return;
		}
		String code = errorCode(report);
		MockErrorConfig cfg = REGISTRY.get(code);
		if (cfg == null || !ArgUtil.is(code)) {
			return;
		}
		String contactId = doc.getContactId();
		if (!ArgUtil.is(contactId)) {
			return;
		}
		ChatContactQuery contactQuery = new ChatContactQuery(contactId);
		stampLatestError(contactQuery, code);
		commonMongoTemplate.updateFirst(contactQuery);
	}

	private boolean isReplayResolved(OutboxMessage outboxMessage) {
		return ArgUtil.is(outboxMessage.meta().get(MessageFailureGuard.META_REPLAY_ERROR_CODE));
	}

	private void stampLatestError(ChatContactQuery contactQuery, String code) {
		contactQuery.set("latestErrors." + code, System.currentTimeMillis());
	}

	private void stampLatestErrorIfNeeded(ChatContactQuery contactQuery, String code, MockErrorConfig cfg) {
		Map<String, Long> map = contactQuery.getDoc().getLatestErrors();
		if (ArgUtil.is(map)) {
			Long existing = map.get(code);
			if (existing != null && !TimeUtils.isExpired(existing, cfg.windowMs)) {
				return;
			}
		}
		stampLatestError(contactQuery, code);
	}

	private String errorCode(MessageReport report) {
		List<MessageReportError> errors = report.getErrors();
		if (ArgUtil.is(errors) && ArgUtil.is(errors.get(0).getCode())) {
			return errors.get(0).getCode();
		}
		String reason = report.getReason();
		if (reason != null && reason.startsWith(CODE_PREFIX)) {
			return reason.substring(CODE_PREFIX.length());
		}
		return null;
	}

	private static final class MockErrorConfig {
		private final long windowMs;
		private final String code;
		private final String title;
		private final String details;

		MockErrorConfig(long windowMs, String code, String title, String details) {
			this.windowMs = windowMs;
			this.code = code;
			this.title = title;
			this.details = details;
		}

		MapModel buildBody() {
			Map<String, Object> errorData = new HashMap<>();
			errorData.put("details", details);
			Map<String, Object> error = new HashMap<>();
			error.put("code", code);
			error.put("message", title);
			error.put("error_data", errorData);
			MapModel body = MapModel.createInstance();
			body.put("error", error);
			return body;
		}
	}
}
