package com.boot.jx.bot;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wrapper class for methods annotated with {@link ChatMapping}.
 */
public class MethodWrapper implements Comparable<MethodWrapper> {
	private String controller;
	private Method method;
	private Pattern[] pattern;
	private String botName;
	private String[] botCode;
	private String lane;
	private Matcher matcher;
	private String next;
	String key;
	private int length;
	private int priority;
	private boolean onSessionRoute;

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public Pattern[] getPattern() {
		return pattern;
	}

	public void setPattern(Pattern[] pattern) {
		this.pattern = pattern;
	}

	public Matcher getMatcher() {
		return matcher;
	}

	public void setMatcher(Matcher matcher) {
		this.matcher = matcher;
	}

	public String getNext() {
		return next;
	}

	public void setNext(String next) {
		this.next = next;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		MethodWrapper that = (MethodWrapper) o;

		if (!method.equals(that.method))
			return false;
		if (pattern != null ? !pattern.equals(that.pattern) : that.pattern != null)
			return false;
		if (matcher != null ? !matcher.equals(that.matcher) : that.matcher != null)
			return false;
		return next != null ? next.equals(that.next) : that.next == null;

	}

	@Override
	public int hashCode() {
		return Objects.hash(method, pattern, matcher, next);
	}

	public String getController() {
		return controller;
	}

	public void setController(String controller) {
		this.controller = controller;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getLane() {
		return lane;
	}

	public void setLane(String lane) {
		this.lane = lane;
	}

	@Override
	public int compareTo(MethodWrapper o) {
		if (this.priority == o.getPriority()) {
			return o.getLength() - this.length;
		}
		return o.getPriority() - this.priority;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	@Override
	public String toString() {
		return this.controller + " " + this.method.getName() + " " + this.key + " " + this.priority + " " + this.length;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public String[] getBotCode() {
		return botCode;
	}

	public void setBotCode(String[] botCode) {
		this.botCode = botCode;
	}

	public String getBotName() {
		return botName;
	}

	public void setBotName(String botName) {
		this.botName = botName;
	}

	public boolean isOnSessionRoute() {
		return onSessionRoute;
	}

	public void setOnSessionRoute(boolean onSessionRoute) {
		this.onSessionRoute = onSessionRoute;
	}

}