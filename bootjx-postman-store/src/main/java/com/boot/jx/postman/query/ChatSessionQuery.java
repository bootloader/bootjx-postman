package com.boot.jx.postman.query;

import java.util.List;
import java.util.Map;

import com.boot.jx.mongo.CommonMongoQueryBuilder.DocQueryBuilder;
import com.boot.jx.postman.PMConstants.CHAT_STATUS;
import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.doc.MessageDoc;
import com.boot.jx.postman.dto.ChatMessageDTO;
import com.boot.jx.postman.model.MessageDefinitions.Contactable;
import com.boot.jx.postman.model.MessageOrder;
import com.boot.jx.postman.model.TagDocument;
import com.boot.jx.postman.service.ChatDTOUtil;
import com.boot.jx.utils.PostManUtil;
import com.boot.model.MapModel.MapEntry;
import com.boot.model.SafeKeyHashMap;
import com.boot.utils.ArgUtil;

public class ChatSessionQuery extends DocQueryBuilder<ChatSessionDoc> {

	public ChatSessionQuery(ChatSessionDoc doc) {
		super(doc);
	}

	public ChatSessionQuery(String docId) {
		super(docId);
	}

	@Override
	public ChatSessionDoc newDoc(String id) {
		ChatSessionDoc doc = new ChatSessionDoc();
		doc.setSessionId(id);
		return doc;
	}

	@Override
	public String getId(ChatSessionDoc doc) {
		return doc.getSessionId();
	}

	@Override
	public String getId() {
		return this.doc == null ? null : this.doc.getSessionId();
	}

	public Object get(String key) {
		return this.doc.store().get(key);
	}

	public ChatSessionQuery put(String key, String object) {
		this.doc.store().put(key, object);
		this.set("store." + key, object);
		return this;
	}

	public ChatSessionQuery remove(String key) {
		this.doc.store().remove(key);
		this.unset("store." + key);
		return this;
	}

	public ChatSessionQuery setActive(boolean active) {
		if (ArgUtil.is(this.doc.isActive(), active)) {
			return this;
		}
		this.doc.setActive(active);
		this.set("active", active);
		return this;
	}

	public ChatSessionQuery setExpired(boolean expired) {
		if (ArgUtil.is(this.doc.isExpired(), expired)) {
			return this;
		}
		this.doc.setExpired(expired);
		this.set("expired", expired);
		return this;
	}

	public ChatSessionQuery setStatus(CHAT_STATUS status) {
		if (ArgUtil.is(this.doc.getStatus(), status)) {
			return this;
		}
		this.doc.setStatus(status.toString());
		this.set("status", status.toString());
		return this;
	}

	public ChatSessionQuery setStamp(String key, long object) {
		this.doc.stamps().put(key, object);
		this.set("stamps." + key, object);
		return this;
	}

	public ChatSessionQuery setSummary(String key, Object object) {
		this.doc.summary().put(key, object);
		this.set("summary." + key, object);
		return this;
	}

	public ChatSessionQuery setFirstInComingStamp(long firstInComingStamp) {
		this.doc.setFirstInComingStamp(firstInComingStamp);
		this.set("firstInComingStamp", firstInComingStamp);
		return this;
	}

	public ChatSessionQuery setFirstOutGoingStamp(long firstOutGoingStamp) {
		this.doc.setFirstOutGoingStamp(firstOutGoingStamp);
		this.set("firstOutGoingStamp", firstOutGoingStamp);
		return this;
	}

	public ChatSessionQuery setLastResponseStamp(long timestamp) {
		this.doc.setLastResponseStamp(timestamp);
		this.set("lastResponseStamp", timestamp);
		return this;
	}

	public ChatSessionQuery setLastInComingStamp(long timestamp) {
		this.doc.setLastInComingStamp(timestamp);
		this.set("lastInComingStamp", timestamp);
		return this;
	}
	
	/** Latest order only — included in same Mongo $set as other session fields. */
	public ChatSessionQuery setOrder(MessageOrder order) {
		this.doc.setOrder(order);
		this.set("order", order);
		return this;
	}

	public ChatSessionQuery setTags(TagDocument tags) {
		this.doc.setTags(tags);
		this.set("tags", tags);
		return this;
	}

	public ChatSessionQuery setLastOutGoingStamp(long timestamp) {
		this.doc.setLastOutGoingStamp(timestamp);
		this.set("lastOutGoingStamp", timestamp);
		return this;
	}

	public ChatSessionQuery setLastInBoundMsg(MessageDoc lastInBoundMsg, String contactType) {
		// this.doc.setLastInBoundMsg(lastInBoundMsg);
		this.set("lastInBoundMsgId", lastInBoundMsg.getMessageId());
		// this.ref("lastInBoundMsg", lastInBoundMsg.getMessageId(),
		// MessageStore.getCollectionName(contactType));
		this.set("msg.lastInBoundMsg", ChatDTOUtil.getChatMessageDTO(lastInBoundMsg));
		return this;
	}

	public ChatSessionQuery setLastOutBoundMsg(MessageDoc lastOutBoundMsg, String contactType) {
		// this.doc.setLastOutBoundMsg(lastOutBoundMsg);
		// this.ref("lastOutBoundMsg", lastOutBoundMsg.getMessageId(),
		// MessageStore.getCollectionName(contactType));
		this.set("msg.lastOutBoundMsg", ChatDTOUtil.getChatMessageDTO(lastOutBoundMsg));
		return this;
	}

	public ChatSessionQuery setLastMsg(ChatMessageDTO msgDto) {
		if (PostManUtil.isOutBound(msgDto.getType())) {
			this.set("msg.lastOutBoundMsg", msgDto);
			this.set("msg.lastMsg", msgDto);
		} else if (PostManUtil.isInBound(msgDto.getType())) {
			this.set("msg.lastInBoundMsg", msgDto);
			this.set("msg.lastMsg", msgDto);
		}
		return this;
	}

	public ChatSessionQuery setLastMsg(MessageDoc lastMsg, String contactType) {
		// this.doc.setLastMsg(lastMsg);
		// this.ref("lastMsg", lastMsg.getMessageId(),
		// MessageStore.getCollectionName(contactType));
		this.set("msg.lastMsg", ChatDTOUtil.getChatMessageDTO(lastMsg));
		return this;
	}

	public ChatSessionQuery setTagId(List<String> tagIds) {
		this.doc.setTagId(tagIds);
		this.set("tagId", tagIds);
		return this;
	}

	public ChatSessionQuery setQueue(String queue) {
		if (ArgUtil.is(this.doc.getAssignedToQueue(), queue)) {
			return this;
		}
		this.doc.setAssignedToQueue(queue);
		this.set("assignedToQueue", queue);
		return this;
	}

	public ChatSessionQuery setMode(String mode) {
		if (ArgUtil.is(this.doc.getMode(), mode)) {
			return this;
		}
		this.doc.setMode(mode);
		this.set("mode", mode);
		return this;
	}

	public ChatSessionQuery read(String agent) {
		String key = SafeKeyHashMap.sanitizeKey(agent);
		long now = System.currentTimeMillis();
		this.doc.read().put(key, now);
		this.set("read." + key, now);
		this.skipStampUpdate();
		return this;
	}

	public ChatSessionQuery update(Contactable contactable) {
		this.doc.contact().copyFrom(contactable);
		this.set("contact", this.doc.contact());
		return this;
	}

	public MapEntry getEntry(String key) {
		return new MapEntry(get(key));
	}

	public ChatSessionQuery setTpMeta(Map<String, Object> tpMeta) {
		this.doc.setTpMeta(tpMeta);
		this.set("tpMeta", tpMeta);
		return this;
	}

	public ChatSessionQuery setStartSessionStamp(long currentTimeMillis) {
		this.doc.setStartSessionStamp(currentTimeMillis);
		this.doc.stamps().put("sessionStart", currentTimeMillis);
		return this;
	}

	public ChatSessionQuery setCloseSessionStamp(long currentTimeMillis) {
		this.doc.setCloseSessionStamp(currentTimeMillis);
		this.doc.stamps().put("sessionClose", currentTimeMillis);
		return this;
	}

	public ChatSessionQuery setExpireSessionStamp(long currentTimeMillis) {
		this.doc.setSessionExpiryStamp(currentTimeMillis);
		this.doc.stamps().put("sessionExpire", currentTimeMillis);
		return this;
	}

	public ChatSessionQuery setAssignedDeptStamp(long currentTimeMillis) {
		this.doc.setAssignedDeptStamp(currentTimeMillis);
		this.set("assignedDeptStamp", currentTimeMillis);
		this.doc.stamps().put("assignedDept", currentTimeMillis);
		return this;
	}

	public ChatSessionQuery setAssignedAgentStamp(long currentTimeMillis) {
		this.doc.setAssignedAgentStamp(currentTimeMillis);
		this.set("assignedAgentStamp", currentTimeMillis);
		this.doc.stamps().put("assignedAgent", currentTimeMillis);
		return this;
	}

}
