package com.boot.jx.inbound;

import org.springframework.scheduling.annotation.Async;

import com.boot.jx.postman.PMConstants.CHAT_MODE;
import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.MessageReport;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.model.PMArgs;
import com.boot.jx.postman.model.ext.InBoundEvent;
import com.boot.jx.postman.store.MessageContext;
import com.boot.model.MapModel.NodeEntry;

public class InBound {

	public interface InBoundProcessor {
		public InboxMessage process(InboxMessage inboxMessage);
	}

	public interface InBoundFilter {

		public boolean doFilter(InboxMessage inboxMessage);
	}

	public interface InBoundHandler {

		public CHAT_MODE mode();

		public MessageContext context();

		public void onMessage(InboxMessage inboxMessage, ChatSessionDoc session);

		default public void afterMessage(InboxMessage inboxMessage, ChatSessionDoc session) {
		}

		default public void onMessageSync(InboxMessage inboxMessage, ChatSessionDoc session) {
			this.onMessage(inboxMessage, session);
			this.afterMessage(inboxMessage, session);
		}

		@Async
		default public void onMessageAsync(InboxMessage inboxMessage, ChatSessionDoc session) {
			context().setInboxMessage(inboxMessage);
			context().session(session);
			this.onMessage(inboxMessage, session);
			this.afterMessage(inboxMessage, session);
		}

		public void doHandle(MessageReport messageReport);

		public InBoundEvent onSessionEvent(InBoundEvent inBoundEvent, PMArgs pmArgs);

		public void onSessionRouteWrapper(InBoundEvent inBoundEvent, ChatSessionDoc sessionDoc, PMArgs pmArgs);

		public void onSessionRoute(InBoundEvent inBoundEvent, ChatSessionDoc sessionDoc, PMArgs pmArgs);

		default public void afterSessionRoute(InBoundEvent inBoundEvent, ChatSessionDoc sessionDoc, PMArgs pmArgs) {
		}

		default public void onSessionRouteSync(InBoundEvent inBoundEvent, ChatSessionDoc sessionDoc, PMArgs pmArgs) {
			this.onSessionRouteWrapper(inBoundEvent, sessionDoc, pmArgs);
			this.afterSessionRoute(inBoundEvent, sessionDoc, pmArgs);
		}

		@Async
		default public void onSessionRouteAsync(InBoundEvent inBoundEvent, ChatSessionDoc sessionDoc, PMArgs pmArgs) {
			context().setInBoundEvent(inBoundEvent);
			context().session(sessionDoc);
			this.onSessionRouteWrapper(inBoundEvent, sessionDoc, pmArgs);
			this.afterSessionRoute(inBoundEvent, sessionDoc, pmArgs);
		}

		public void onSessionStatus(InBoundEvent event, ChatSessionDoc chatSessionDoc);

		@Async
		default public void onSessionStatusAsync(InBoundEvent inBoundEvent, ChatSessionDoc sessionDoc) {
			this.onSessionStatus(inBoundEvent, sessionDoc);
		}

		public void onSessionResolve(InBoundEvent event, ChatSessionDoc chatSessionDoc);

		@Async
		default public void onSessionResolveAsync(InBoundEvent inBoundEvent, ChatSessionDoc sessionDoc) {
			this.onSessionResolve(inBoundEvent, sessionDoc);
		}

		public void onSessionClose(InBoundEvent event, ChatSessionDoc chatSessionDoc);

		@Async
		default public void onSessionCloseAsync(InBoundEvent inBoundEvent, ChatSessionDoc sessionDoc) {
			this.onSessionClose(inBoundEvent, sessionDoc);
		}

		// public void onSessionInit(InBoundEvent event, ChatSessionDoc chatSessionDoc);

		public void onSessionInit(InBoundEvent event, ChatSessionDoc chatSessionDoc, PMArgs pmArgs);

		public NodeEntry<InBoundEvent> assignSessionToAgent(PMArgs params, ChatSessionDoc session);

		void onTimeout(InBoundEvent event, ChatSessionDoc sessionDoc, PMArgs pmArgs);

	}

	public interface SessionAssginHandler {

		public PMArgs doAssign(ChatSessionDoc chatSessionDoc, PMArgs params);
	}

	public interface ChatSessionEvents {

		public NodeEntry<InBoundEvent> onSessionIdleOutBound(ChatSessionDoc session);

		public NodeEntry<InBoundEvent> onSessionIdleInBound(ChatSessionDoc session);

	}

	public interface MessageEvents {

		public NodeEntry<InBoundEvent> preMessageOutBound(OutboxMessage message);

		public NodeEntry<InBoundEvent> onMessageOutbound(OutboxMessage message);

		public NodeEntry<InBoundEvent> postMessageOutBound(OutboxMessage message);

		public NodeEntry<InBoundEvent> postMessageInBound(InboxMessage message);

		public NodeEntry<InBoundEvent> postMessageStatus(MessageReport messageReport);

		NodeEntry<InBoundEvent> postAction(OutboxMessage message);

	}

}
