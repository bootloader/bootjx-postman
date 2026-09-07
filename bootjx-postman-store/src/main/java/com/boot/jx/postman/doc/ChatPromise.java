package com.boot.jx.postman.doc;

import java.io.Serializable;

public class ChatPromise implements Serializable {

	private static final long serialVersionUID = 1L;

	public static enum Result {
		NONE, RESOLVED, REJECTED, IGNORED
	}

	public static enum State {
		CREATED, TRIGGERED, CAPTURED, RETURNED, COMPLETED, CLOSED;
	}

	public static class PromiseCondition {
		public PromiseCondition(boolean condition, Result result) {
			super();
			this.condition = condition;
			this.result = result;
		}

		Result result;
		boolean condition;

		public Result getResult() {
			return result;
		}

		public void setResult(Result result) {
			this.result = result;
		}

		public boolean isCondition() {
			return condition;
		}

		public void setCondition(boolean condition) {
			this.condition = condition;
		}
	}

	public ChatPromise() {
		super();
	}

	private Result result;
	private State state;
	private String source;
	private String target;
	private String message;
	private String messageId;

	public Result getResult() {
		return result;
	}

	public void setResult(Result result) {
		this.result = result;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public static PromiseCondition resolveIf(boolean condition) {
		return new PromiseCondition(condition, Result.RESOLVED);
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

}
