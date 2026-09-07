package com.boot.jx.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import com.boot.jx.agent.AgentService;
import com.boot.jx.chat.ChatService;
import com.boot.jx.chat.ChatSessionService;
import com.boot.jx.postman.ClientApp;
import com.boot.jx.postman.PMConstants;
import com.boot.jx.postman.PMConstants.MESSAGE_SENDER_TYPE;
import com.boot.jx.postman.doc.ChatContactDoc;
import com.boot.jx.postman.doc.ChatPromise;
import com.boot.jx.postman.doc.ChatPromise.PromiseCondition;
import com.boot.jx.postman.doc.ChatPromise.Result;
import com.boot.jx.postman.doc.ChatPromise.State;
import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.manager.ChatLogger;
import com.boot.jx.postman.manager.ChatSessionManager;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.model.PMArgs;
import com.boot.jx.postman.model.ext.InBoundEvent;
import com.boot.jx.postman.store.MessageContext;
import com.boot.jx.postman.store.MessageStore.EVENTS;
import com.boot.jx.postman.store.SessionStore;
import com.boot.model.MapModel;
import com.boot.utils.ArgUtil;

public class ChatController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ChatController.class);

	@Autowired
	protected ChatService chatService;

	@Autowired
	private MessageContext messageContext;

	@Autowired
	private AgentService agentService;

	@Autowired
	private ChatSessionManager chatSessionManager;

	@Autowired
	protected SessionStore sessionStore;

	@Autowired
	protected ChatLogger logManager;

	@Lazy
	@Autowired
	private ChatSessionService chatSessionService;

	public String controllerName;

	private void setRoutingDetails(OutboxMessage waMessage) {
		ClientApp app = context().clientApp();
		if (ArgUtil.is(app)) {
			waMessage.route().setQueueCode(app.getQueue());
			waMessage.route().setSendMode(app.getAppMode());
			waMessage.route().setSenderApp(app.getAppType());
		} else {
			waMessage.route().setQueueCode(PMConstants.DEFAULT.BOT_QUEUE_CODE);
			waMessage.route().setSendMode(PMConstants.CHAT_MODE.BOT.toString());
			waMessage.route().setSenderApp(PMConstants.APP_TYPE.BOT.name());
		}
		waMessage.route().setSenderType(MESSAGE_SENDER_TYPE.BOT);
		waMessage.session().setAgent(chatService.getClientConfig().getDefaultSender());

		if (ArgUtil.is(context().getCurrentHandler())) {
			waMessage.meta().put("handler", context().getCurrentHandler());
		}
	}

	public void reply(OutboxMessage message) {
		try {
			setRoutingDetails(message);
			chatService.reply(context().getInboxMessage(), message);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void reply(String message) {
		reply(new OutboxMessage().message(message));
	}

	public void send(OutboxMessage waMessage) {
		setRoutingDetails(waMessage);
		if (ArgUtil.is(waMessage.getContact())) {
			ChatContactDoc chatContactDoc = sessionStore.getContact(waMessage);
			chatService.send(chatContactDoc, waMessage);
		} else {
			chatService.send(messageContext.contact().getDoc(), waMessage);
		}
	}

	public void botScore(Integer botScore) {
		chatService.botScore(messageContext.session().getDoc(), botScore);
	}

	public void agentScore(Integer agentScore) {
		chatService.agentScore(messageContext.session().getDoc(), agentScore);
	}

	public void resolveSession() {
		chatSessionManager.resolveSession(messageContext.session().getDoc());
	}

	public void closeSession() {
		chatSessionService.closeSession(messageContext.session().getDoc(), null);
	}

	public void next(String key) {
		String handelrName = key;
		if (ArgUtil.is(this.controllerName)) {
			handelrName = this.controllerName + "#" + key;
		}
		context().chat().setNextHandler(handelrName);
	}

	public boolean previous(String key) {
		return ArgUtil.areEqual(context().chat().getPrevHandler(), key);
	}

	public ChatPromise require(String key, ChatPromise.PromiseCondition... conditions) {
		if (ArgUtil.is(conditions)) {
			for (PromiseCondition condition : conditions) {
				if (condition.isCondition()) {
					ChatPromise promise = new ChatPromise();
					promise.setState(State.COMPLETED);
					promise.setResult(condition.getResult());
					return promise;
				}
			}
		}

		ChatPromise promise = context().chat().promise().get(key);
		if (ArgUtil.is(promise) && State.RETURNED.equals(promise.getState())) {
			promise.setState(State.COMPLETED);
			return promise;
		} else {
			promise = new ChatPromise();
			promise.setSource(context().getCurrentHandler());

			if (ArgUtil.is(context().getInboxMessage())) {
				promise.setMessage(context().getInboxMessage().getMessage());
				promise.setMessageId(context().getInboxMessage().getMessageId());
			}

			promise.setResult(Result.NONE);
			promise.setState(State.CREATED);
			promise.setTarget(key);
			context().chat().promise().put(key, promise);
			throw new ChatException(key).targetHandler(key);
		}
	}

	public ChatPromise resolve(String key) {
		ChatPromise promise = context().chat().promise().get(key);
		if (ArgUtil.is(promise)) {
			promise.setResult(Result.RESOLVED);
			promise.setState(State.CAPTURED);
		}
		return promise;
	}

	public ChatPromise reject(String key) {
		ChatPromise x = context().chat().promise().get(key);
		if (ArgUtil.is(x)) {
			x.setResult(Result.REJECTED);
			x.setState(State.CAPTURED);
		}
		return x;
	}

	public void routeSession(String queueCode) {
		ChatSessionDoc session = messageContext.session().getDoc();
		chatSessionService.routeSession(session,
				new PMArgs().assignToQueueCode(queueCode).contact(session.contact()).sessionId(session.getSessionId()));
	}

	public void routeSession(String queueCode, Object params) {
		ChatSessionDoc session = messageContext.session().getDoc();
		chatSessionService.routeSession(session, new PMArgs().assignToQueueCode(queueCode).contact(session.contact())
				.sessionId(session.getSessionId()).params(params));
	}

	public void routeSessionToDefaultQueue() {
		ChatSessionDoc session = messageContext.session().getDoc();
		chatSessionService.routeSession(session);
	}

	public void assignToAgentDepartment(String deptCode) {
		ChatSessionDoc session = messageContext.session().getDoc();
		ClientApp thisApp = messageContext.clientApp();
		if (ArgUtil.is(thisApp)) {
			String agent_queue = ArgUtil.parseAsString(thisApp.props().get("agent_queue"),
					PMConstants.DEFAULT.AGENT_QUEUE_CODE);
			chatSessionService.routeSession(session, new PMArgs().assignToQueueCode(agent_queue)
					.contact(session.contact()).sessionId(session.getSessionId()).assignToDeptCode(deptCode));
		} else {
			LOGGER.warn("@assignToAgentDepartment : NoAppFound in messageContext");
		}

	}

	public void assignToAgentSkill(String... skillCode) {
		ChatSessionDoc session = messageContext.session().getDoc();
		ClientApp thisApp = messageContext.clientApp();
		String agent_queue = ArgUtil.parseAsString(thisApp.props().get("agent_queue"),
				PMConstants.DEFAULT.AGENT_QUEUE_CODE);
		chatSessionService.routeSession(session, new PMArgs().assignToQueueCode(agent_queue).contact(session.contact())
				.sessionId(session.getSessionId()).assignToSkillCode(skillCode));

	}

	public void assignToDefaultAgent() {
		assignToAgentDepartment(null);
	}

	public void setMeta(String controllerName) {
		this.controllerName = controllerName;
	}

	public void onSessionRoute(InBoundEvent assignEvent) {
		logManager.addTrace(assignEvent, EVENTS.ON_SESSION_ROUTE, assignEvent.sessionRouted);
	}

	public void onSessionStart(InBoundEvent assignEvent) {
		logManager.addTrace(assignEvent, EVENTS.ON_SESSION_START, assignEvent.sessionRouted);
	}

	public void onPostOutboundMessage(MapModel mapModel) {
	}

	public MessageContext context() {
		return this.messageContext;
	}

}
