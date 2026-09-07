package com.boot.jx.bot;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.boot.jx.AppContextUtil;
import com.boot.jx.chat.ChatService;
import com.boot.jx.postman.ClientApp;
import com.boot.jx.postman.PMConstants;
import com.boot.jx.postman.PMConstants.APP_TYPE;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.doc.ChatPromise;
import com.boot.jx.postman.doc.ChatPromise.State;
import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.manager.ChatLogger;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.ext.InBoundEvent;
import com.boot.jx.postman.store.MessageContext;
import com.boot.jx.utils.PostManUtil;
import com.boot.utils.ArgUtil;
import com.boot.utils.ClazzUtil;
import com.boot.utils.Constants;
import com.boot.utils.EntityDtoUtil;
import com.boot.utils.StringUtils;
import com.boot.utils.StringUtils.StringMatcher;

@Component
public class BotEngine {

	private static final Logger LOGGER = LoggerFactory.getLogger(BotEngine.class);

	@Autowired(required = false)
	List<ChatController> chatControllers;

	protected final TreeMap<String, MethodWrapper> eventToMethodsMap = new TreeMap<String, MethodWrapper>();
	List<MethodWrapper> eventToMethodsList = new ArrayList<MethodWrapper>();

	private final Map<String, MethodWrapper> methodNameMap = new HashMap<>();
	/**
	 * A List of names of the methods which are part of any conversation.
	 */
	private final Map<String, ChatController> filtersMap = new HashMap<String, ChatController>();

	@Autowired
	private ChatService botService;

	@Autowired
	private PMEnvironment pmEnvironment;

	@Autowired
	private MessageContext messageContext;

	@Autowired
	private ChatLogger logManager;

	private boolean chatBotDefined;

	public boolean isChatBotDefined() {
		return chatBotDefined;
	}

	public ChatController getBotByCode(String botCode) {
		return filtersMap.get("botCode#" + botCode);
	}

	@PostConstruct
	public void mapping() {
		if (ArgUtil.isEmpty(chatControllers)) {
			return;
		}

		for (ChatController chatController : chatControllers) {
			chatBotDefined = true;
			Class<?> c = AopProxyUtils.ultimateTargetClass(chatController);
			String controllerName = c.getName();
			chatController.setMeta(controllerName);

			BotController botControllerAnnot = ClazzUtil.getAnnotation(c, BotController.class);

			filtersMap.put("controllerName#" + controllerName, chatController);
			Method[] methods = c.getMethods();
			for (Method method : methods) {
				if (method.isAnnotationPresent(ChatMapping.class)) {
					ChatMapping controller = method.getAnnotation(ChatMapping.class);
					String key = controller.key();
					String[] patternStr = controller.pattern();
					int patternFlags = controller.patternFlags();
					String next = controller.next();

					MethodWrapper methodWrapper = new MethodWrapper();
					methodWrapper.setMethod(method);
					methodWrapper.setPriority(controller.priority());

					Pattern[] patterns = new Pattern[patternStr.length];

					int length = 0;
					for (int i = 0; i < patternStr.length; i++) {
						patterns[i] = Pattern.compile(patternStr[i], patternFlags);
						length = Math.min(Math.max(patternStr[i].length(), length), patternStr[i].length());
					}

					methodWrapper.setLength(length);
					methodWrapper.setPattern(patterns);
					methodWrapper.setNext(next);
					methodWrapper.setController(controllerName);
					methodWrapper.setKey(key);
					methodWrapper.setLane(botControllerAnnot.lane());
					methodWrapper.setBotName(botControllerAnnot.name());
					methodWrapper.setBotCode(botControllerAnnot.code());

					for (String botCode : botControllerAnnot.code()) {
						filtersMap.put("botCode#" + botCode, chatController);
					}

					// for (String event : events) {
					eventToMethodsList.add(methodWrapper);
					// eventToMethodsMap.put(key, methodWrapper);
					// }
					methodNameMap.put(method.getName(), methodWrapper);
				}
			}
		}
		Collections.sort(eventToMethodsList);
		for (MethodWrapper methodWrapper : eventToMethodsList) {
			eventToMethodsMap.put(methodWrapper.getController() + "#" + methodWrapper.getKey(), methodWrapper);
		}

	}

	/**
	 * Search for a method whose {@link ChatMapping#pattern()} match with the
	 * {@code Event} text or payload received from Slack/Facebook and also filter
	 * out the methods in {@code methodWrappers} whose {@link ChatMapping#pattern()}
	 * do not match.
	 *
	 * @param text           is the message from the user
	 * @param methodWrappers
	 * @return the MethodWrapper whose method pattern match with that of the slack
	 *         message received, {@code null} if no such method is found.
	 */
	protected MethodWrapper getMethodWithMatchingPatternAndFilterUnmatchedMethods(InboxMessage event) {

		if (ArgUtil.isEmpty(event.getMessage()) && ArgUtil.isEmpty(event.getAttachments())) {
			return null;
		}

		String text = ArgUtil.nonEmpty(event.getMessage(), Constants.BLANK).toUpperCase();
		StringMatcher matcher = new StringMatcher(text);

		String botCodePrefix = pmEnvironment.config().prefsEntry("postman.bot.code")
				.asString(AppContextUtil.getTenant());

		ClientApp app = messageContext.clientApp();

		String botCode = botCodePrefix;
		if (ArgUtil.is(app)) {
			if (!APP_TYPE.BOT.name().equalsIgnoreCase(app.getAppType())) {
				botCode = "bot_" + StringUtils.trim(StringUtils.toLowerCase(app.getAppType()));
			} else {
				String botFlow = ArgUtil.parseAsString(app.props().get("botCode"), app.getQueue());
				if (ArgUtil.is(botFlow) && !ArgUtil.areEqual(app.getQueue(), PMConstants.DEFAULT.BOT_QUEUE_CODE)) {
					botCode = botCodePrefix + "_" + botFlow;
				}
			}
		}

		for (MethodWrapper methodWrapper : eventToMethodsList) {
			Pattern[] patterns = methodWrapper.getPattern();
			if (patterns.length > 0) {
				for (int i = 0; i < patterns.length; i++) {
					if (ArgUtil.isEqual(botCode, methodWrapper.getBotCode())) {
						if (matcher.isMatch(patterns[i]) && ArgUtil.is(ArgUtil.parseAsString(patterns[i]))) {
							event.setMatcher(matcher);
							return methodWrapper;
						}
					}
				}
			}
		}

		for (MethodWrapper methodWrapper : eventToMethodsList) {
			Pattern[] patterns = methodWrapper.getPattern();
			if (patterns.length > 0) {
				for (int i = 0; i < patterns.length; i++) {
					if (ArgUtil.isEmptyArray(methodWrapper.getBotCode())
							|| ArgUtil.isEqual(Constants.BLANK, methodWrapper.getBotCode())
							|| ArgUtil.isEqual(methodWrapper.getBotCode(), botCode)) {
						if (matcher.isMatch(patterns[i]) && ArgUtil.is(ArgUtil.parseAsString(patterns[i]))) {
							event.setMatcher(matcher);
							return methodWrapper;
						}
					}
				}
			}
		}
		return null;
	}

	public void invokeMethods(InboxMessage inboxMessageOriginal) {

		String contactId = PostManUtil.createContactId(inboxMessageOriginal);
		InboxMessage inboxMessage = EntityDtoUtil.entityToDto(inboxMessageOriginal, new InboxMessage());

		MessageContext messageContext = botService.loadChatContext(contactId, inboxMessage);

		if (!botService.beforeMessageHandler()) {
			return;
		}

		String nextHandler = messageContext.chat().getNextHandler();
		messageContext.chat().setNextHandler(null);
		invokeMethods(contactId, inboxMessage, nextHandler);

		int limit = 10;

		ChatPromise promise = nextPromise();
		while (ArgUtil.is(promise) && limit > -1) {
			if (State.CREATED.equals(promise.getState())) {
				promise.setState(State.TRIGGERED);
				InboxMessage inboxMessageTriggered = EntityDtoUtil.entityToDto(inboxMessageOriginal,
						new InboxMessage());
				nextHandler = invokeMethods(contactId, inboxMessageTriggered, promise.getTarget());
			} else if (State.CAPTURED.equals(promise.getState())) {
				promise.setState(State.RETURNED);
				InboxMessage inboxMessageCompleted = EntityDtoUtil.entityToDto(inboxMessageOriginal,
						new InboxMessage());
				inboxMessageCompleted.setMessage(promise.getMessage());
				inboxMessageCompleted.setMessageId(promise.getMessageId());
				nextHandler = invokeMethods(contactId, inboxMessageCompleted, promise.getSource());
			} else if (State.COMPLETED.equals(promise.getState())) {
				botService.context().chat().getPromise().remove(promise.getTarget());
				botService.commitChatContext(contactId, nextHandler, inboxMessageOriginal);
			}
			limit--;
			promise = nextPromise();
		}

	}

	private ChatPromise nextPromise() {
		Map<String, ChatPromise> promises = botService.context().chat().getPromise();

		if (ArgUtil.is(promises)) {
			for (Entry<String, ChatPromise> promiseEntry : promises.entrySet()) {
				ChatPromise promise = promiseEntry.getValue();
				if (State.CREATED.equals(promise.getState())) {
					return promise;
				}
			}

			for (Entry<String, ChatPromise> promiseEntry : promises.entrySet()) {
				ChatPromise promise = promiseEntry.getValue();
				if (State.CAPTURED.equals(promise.getState())) {
					return promise;
				}
			}
		}
		return null;
	}

	private String invokeMethods(String contactId, InboxMessage inboxMessage, String nextHandler) {
		try {

			MethodWrapper matchedMethod = null;

			if (ArgUtil.is(nextHandler)) {
				matchedMethod = eventToMethodsMap.get(nextHandler);
			}
			if (ArgUtil.isEmpty(matchedMethod)) {
				matchedMethod = getMethodWithMatchingPatternAndFilterUnmatchedMethods(inboxMessage);
			}
			if (ArgUtil.is(matchedMethod)) {
				nextHandler = matchedMethod.getKey();
				LOGGER.debug("Handler: " + nextHandler);
				botService.context().setCurrentHandler(nextHandler);
				Method method = matchedMethod.getMethod();
				ChatController controller = filtersMap.get("controllerName#" + matchedMethod.getController());
				// LOGGER.info("Target Handler : " + method.getName());
				logManager.addTrace(inboxMessage, "invokeMethods", matchedMethod.getController());
				List<Class<?>> prmTyps = Arrays.asList(method.getParameterTypes());
				if (prmTyps.contains(InboxMessage.class) && prmTyps.contains(StringMatcher.class)) {
					method.invoke(controller, inboxMessage, inboxMessage.getMatcher());
				} else {
					method.invoke(controller, inboxMessage);
				}
			} else {
				warn("No Chat Controller Matched");
			}
		} catch (ChatException ce) {
			debug("Target Handler : " + ce.getTargetHandler());
		} catch (InvocationTargetException e) {
			error("Error invoking controller: ", e.getCause());
		} catch (Exception e) {
			error("Error invoking controller: ", e);
		}
		botService.commitChatContext(contactId, nextHandler, inboxMessage);
		return nextHandler;
	}

	public void invokeMethods(ChatSessionDoc sessionDoc, InBoundEvent assignEvent) {
		try {

			if (InBoundEvent.EVENT_TYPE.SESSION_ROUTED.equals(assignEvent.type)) {
				String botCodePrefix = pmEnvironment.config().prefsEntry("postman.bot.code")
						.asString(AppContextUtil.getTenant());
				ClientApp app = messageContext.clientApp(assignEvent.sessionRouted.targetQueue, sessionDoc.contact());
				String botCode = botCodePrefix;

				if (ArgUtil.is(app)) {
					if (!APP_TYPE.BOT.name().equalsIgnoreCase(app.getAppType())) {
						botCode = "bot_" + StringUtils.trim(StringUtils.toLowerCase(app.getAppType()));
					} else {
						String botFlow = ArgUtil.parseAsString(app.props().get("botCode"), app.getQueue());
						if (ArgUtil.is(botFlow)
								&& !ArgUtil.areEqual(app.getQueue(), PMConstants.DEFAULT.BOT_QUEUE_CODE)) {
							botCode = botCodePrefix + "_" + botFlow;
						}
					}
				}
				ChatController controller = getBotByCode(botCode);
				if (ArgUtil.is(controller)) {

					if (assignEvent.sessionRouted.sessionStart) {
						controller.onSessionStart(assignEvent);
					} else {
						controller.onSessionRoute(assignEvent);
					}
					botService.commitChatContext(sessionDoc, assignEvent);
				} else {
					warn("No Chat Controller Matched for botCode#" + botCode);
				}
			}

		} catch (ChatException ce) {
			debug("Target Handler : " + ce.getTargetHandler());
		} catch (Exception e) {
			error("Error invoking controller: ", e);
		}

	}

	/**
	 * Invoke the methods with matching {@link ChatMapping#events()} and
	 * {@link ChatMapping#pattern()} in events received from Slack/Facebook.
	 *
	 * @param event received from facebook
	 */
	@Async
	public void invokeMethodsAsync(InboxMessage inboxMessageOriginal) {
		invokeMethods(inboxMessageOriginal);
	}

	public void invokeMethods(InBoundEvent assignEvent) {
		ChatSessionDoc session = messageContext.session().getDoc();
		botService.loadChatContext(assignEvent.getContactId(), assignEvent);
		invokeMethods(session, assignEvent);
	}

	private void error(String string, Throwable cause) {
		logManager.error(cause);
		LOGGER.error(string, cause);
	}

	private void debug(String string) {
		logManager.debug(string);
		LOGGER.debug(string);
	}

	private void warn(String string) {
		logManager.warn(string);
		LOGGER.warn(string);
	}
}
