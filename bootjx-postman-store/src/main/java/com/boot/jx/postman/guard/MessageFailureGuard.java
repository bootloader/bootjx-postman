package com.boot.jx.postman.guard;

import com.boot.jx.postman.PostmanPackages;
import com.boot.jx.postman.doc.ChatContactDoc;
import com.boot.jx.postman.doc.MessageDoc;
import com.boot.jx.postman.model.MessageReport;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.query.ChatContactQuery;

/**
 * Guards marketing WhatsApp sends after a known Meta failure: the contact keeps
 * a map of error codes and timestamps ({@code latestErrors}), and within a
 * configured window the next outbound send can replay that failure locally
 * instead of calling Meta.
 */
public interface MessageFailureGuard extends PostmanPackages.MessageFailureGuard {

	/**
	 * Meta key written on the outbox when a send is armed for replay. Presence of
	 * this value means “this failure is being replayed, not a fresh Meta attempt”.
	 */
	String META_REPLAY_ERROR_CODE = "replayErrorCode";

	/**
	 * Prepares an outbound marketing message for replay when the contact already
	 * has a recent, registered Meta error. Scans {@code contact.latestErrors},
	 * ignores unknown or expired codes, picks the newest valid entry, and copies
	 * that code onto the outbox as {@code replayErrorCode} and
	 * {@link #META_REPLAY_ERROR_CODE} in meta. If nothing qualifies, the outbox is
	 * left unchanged and the send proceeds normally.
	 */
	void resolve(OutboxMessage outboxMessage, ChatContactDoc contact);

	/**
	 * Remembers a real Meta API failure on the contact after an outbound send ends
	 * as {@code SENT_ERR}. Reads the error code from the outbox, checks it against
	 * the registered replay table, and stamps {@code contact.latestErrors.<code>}
	 * with the current time on the given contact query. Does nothing if the outbox
	 * was armed for replay ({@link #META_REPLAY_ERROR_CODE} present), because that
	 * failure was simulated rather than returned by Meta. Does not write the
	 * message document.
	 */
	void capture(OutboxMessage outboxMessage, ChatContactQuery contactQuery);

	/**
	 * Finalizes a replayed send on the outbox before it is persisted. When the
	 * outbox was armed for replay and the send path ended in {@code SENT_ERR}
	 * (simulated API error), promotes the message to {@code FAILD} and appends a
	 * {@code Code:NNNNNN} log entry so the stored record matches a real delivery
	 * failure. Mutates the outbox in memory only; persistence happens in the next
	 * save.
	 * 
	 * @return true if there is failure replay
	 */
	boolean applyReplayFailure(OutboxMessage outboxMessage);

	/**
	 * Learns from an inbound Meta delivery failure. When a saved marketing message
	 * receives a {@code FAILD} status report with a registered error code, stamps
	 * {@code latestErrors.<code>} on that message’s contact so a later outbound
	 * {@link #resolve} can arm replay within the code’s time window.
	 */
	void recordFailedReport(MessageDoc doc, MessageReport report);

}
