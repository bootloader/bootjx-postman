package com.boot.jx.postman.doc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.mongo.CommonDocInterfaces.TimeStampIndex.UpdatedTimeStampDoc;
import com.boot.jx.mongo.CommonDocInterfaces.TimeStampIndex.UpdatedTimeStampIndexSupport;
import com.boot.jx.postman.PMEnvironment.FullView;
import com.boot.jx.postman.dto.ChatMessageDTO;
import com.boot.jx.postman.model.MessageDefinitions.Contactable;
import com.boot.jx.postman.model.MessageDefinitions.SessionObject;
import com.boot.jx.postman.model.MessageOrder;
import com.boot.jx.postman.model.TagDocument;
import com.boot.jx.swagger.ApiMockModelProperty;
import com.boot.jx.utils.PostManUtil;
import com.boot.utils.ArgUtil;
import com.fasterxml.jackson.annotation.JsonView;

@Document(collection = "CHAT_SESSION")
@TypeAlias("ChatSessionDoc")
@CompoundIndexes({
		// route indexs
		@CompoundIndex(name = "lastMsg_Stamp", def = "{ 'msg.lastMsg.timestamp': 1 }"),
		@CompoundIndex(name = "route_sendMode", def = "{ 'msg.lastMsg.route.sendMode': 1 }"),
		@CompoundIndex(name = "route_senderType", def = "{ 'msg.lastMsg.route.senderType': 1 }"),
		@CompoundIndex(name = "lastOutBoundMsg_Stamp", def = "{ 'msg.lastOutBoundMsg.timestamp': 1 }"),
		@CompoundIndex(name = "lastOutBoundMsg_Stamp_desc", def = "{ 'msg.lastOutBoundMsg.timestamp': -1 }"),

		// required
		@CompoundIndex(name = "idx_primary_active_updated",
				def = "{'primary': 1, 'active': 1, 'updated.day': -1, 'lastInComingStamp': -1}"),

		// New indexes from MongoDB
		@CompoundIndex(name = "idx_main_query", def = "{ 'active': 1, 'resolved': 1 }"),

		@CompoundIndex(name = "idx_dept_ag", def = "{ 'assignedToDept': 1, 'assignedToAgent': 1 }"),

		@CompoundIndex(name = "idx_in_out", def = "{ 'lastInComingStamp': -1, 'lastOutGoingStamp': -1 }"),

// better to keep smaller indexes since our fields are dynamic

//		@CompoundIndex(
//		        name = "chat_session_optimized_index",
//		        def = "{'updated.day': -1, 'active': 1, 'resolved': 1, 'primary': 1, 'mode': 1, 'lastInComingStamp': -1, 'lastOutGoingStamp': -1, 'assignedToAgent': 1, 'assignedToDept': 1}"
//		    ),
//		@CompoundIndex(
//		        name = "chat_session_timestamps_idx",
//		        def = "{'updated.hour': -1, 'startSessionStamp': -1, 'closeSessionStamp': -1, 'lastResponseStamp': -1, 'assignedAgentStamp': -1, 'agentSessionStamp': -1, 'assignedDeptStamp': -1, 'firstResponseStamp': -1, 'lastInComingStamp': -1, " +
//		              "'sessionExpiryStamp': -1}"
//		    ),
//		@CompoundIndex(
//			    name = "idx_session_timestamps_optimized",
//			    def = "{'updated.hour': -1, 'startSessionStamp': -1, 'closeSessionStamp': -1, 'agentSessionStamp': -1, 'lastInComingStamp': -1, 'firstResponseStamp': -1, 'sessionExpiryStamp': -1, 'assignedAgentStamp': -1, 'assignedDeptStamp': -1}"
//			),

})

public class ChatSessionDoc extends UpdatedTimeStampDoc implements SessionObject, Serializable {

	private static final long serialVersionUID = 1L;

	public static final String FIRST_INBOUND_STAMP = "firstInBound";
	public static final String LAST_INBOUND_STAMP = "lastInBound";
	public static final String FIRST_OUTBOUND_STAMP = "firstOutBound";
	public static final String LAST_OUTBOUND_STAMP = "lastOutBound";
	/** Customer Care window expiry **/
	public static final String CUSTOMER_CARE_WIN_EXP_STAMP = "ccwExpiry";

	@Id
	private String sessionId;

	@Indexed
	private String ticketHash;
	
	/** Latest order snapshot; overwritten on each inbound that carries order. */
	private MessageOrder order;

	private TagDocument tags;

	private String routingId;

	@Version
	private Long version;

	@ApiMockModelProperty(example = "wa919930104050_918828218374", required = false,
			value = "format like {{ContactType.getShortCode}}{{csid}}_{{lane}}")
	@Indexed
	private String contactId;

	@Deprecated
	private String contactType;
	@Deprecated
	private String channel;
	@Deprecated
	private String lane;

	@Deprecated
	private String contactName;

	private ContactDetailDoc contact;

	private String subject;

	@Indexed(sparse = true)
	private String assignedToDept;

	@Indexed(sparse = true)
	private String assignedToAgent;

	private String assignedToBot;

	private String assignedToQueue;

	@Indexed
	private boolean active;
	private boolean initd;
	@Indexed
	private boolean resolved;
	private boolean expired;

	private boolean primary;

	@Indexed(name = "startSessionStamp_desc", direction = IndexDirection.DESCENDING)
	private long startSessionStamp;
	@Indexed(name = "fistResponseStamp_desc", direction = IndexDirection.DESCENDING)
	private long fistResponseStamp;

	@Indexed(name = "firstInComingStamp_desc", direction = IndexDirection.DESCENDING)
	private long agentSessionStamp;

	private long firstInComingStamp;
	private long firstOutGoingStamp;

	@Indexed(name = "lastInComingStamp_desc", direction = IndexDirection.DESCENDING)
	private long lastInComingStamp;
	@Indexed(name = "lastOutGoingStamp_desc", direction = IndexDirection.DESCENDING)
	private long lastOutGoingStamp;

	@Indexed(name = "assignedDeptStamp_desc", direction = IndexDirection.DESCENDING)
	private long assignedDeptStamp;
	@Indexed(name = "assignedAgentStamp_desc", direction = IndexDirection.DESCENDING)
	private long assignedAgentStamp;

	@Indexed(name = "lastResponseStamp_desc", direction = IndexDirection.DESCENDING)
	private long lastResponseStamp;

	private long resolveSessionStamp;

	@Indexed(name = "closeSessionStamp_desc", direction = IndexDirection.DESCENDING)
	private long closeSessionStamp;

	/**
	 * @deprecated Use {@link UpdatedTimeStampIndexSupport#getUpdated()}
	 */
	@Deprecated
	private long updatedStamp;

	private Integer agentScore;
	private Integer botScore;

	@Indexed
	private String mode;

	@Indexed
	private String status;
	
	@Deprecated
	private String tagCategory;

	@Indexed
	private List<String> tagId;

	private Map<String, Object> store;
	private Map<String, Object> meta;
	private Map<String, Object> summary;

	@JsonView(FullView.class)
	private Map<String, ChatMessageDTO> msg;

	private Map<String, Long> stamps;
	private Map<String, Long> read;
	private Map<String, Object> feedback;

	// MessageStats
//	@DBRef
//	private MessageDoc lastInBoundMsg;
//
//	@DBRef
//	private MessageDoc lastOutBoundMsg;

	// @DBRef
//	private MessageDoc lastMsg;

	/**
	 * Should not add anything at this level
	 */
	@Deprecated
	/** session expiry stamp **/
	private long sessionExpiryStamp;

	/** waba convesation expirty stamp **/
	private Map<String, Object> tpMeta;

	private Map<String, Object> insights;

	public long getLastInComingStamp() {
		return lastInComingStamp;
	}

	public void setLastInComingStamp(long lastInComingStamp) {
		this.lastInComingStamp = lastInComingStamp;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getContactId() {
		return contactId;
	}

	public void setContactId(String contactId) {
		this.contactId = contactId;
	}

	public String contactId() {
		if (ArgUtil.is(contactId)) {
			return contactId;
		}
		return this.contact().getContactId();
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getAssignedToDept() {
		return assignedToDept;
	}

	public void setAssignedToDept(String assignedToDept) {
		this.assignedToDept = assignedToDept;
	}

	public String getAssignedToAgent() {
		return assignedToAgent;
	}

	public void setAssignedToAgent(String assignedToAgent) {
		this.assignedToAgent = assignedToAgent;
	}

	public boolean isInitd() {
		return initd;
	}

	public void setInitd(boolean initd) {
		this.initd = initd;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	public long getStartSessionStamp() {
		return startSessionStamp;
	}

	public void setStartSessionStamp(long startSessionStamp) {
		this.startSessionStamp = startSessionStamp;
	}

	public long getCloseSessionStamp() {
		return closeSessionStamp;
	}

	public void setCloseSessionStamp(long closeSessionStamp) {
		this.closeSessionStamp = closeSessionStamp;
	}

	public long getAssignedDeptStamp() {
		return assignedDeptStamp;
	}

	public void setAssignedDeptStamp(long assignedDeptStamp) {
		this.assignedDeptStamp = assignedDeptStamp;
	}

	public long getAssignedAgentStamp() {
		return assignedAgentStamp;
	}

	public void setAssignedAgentStamp(long assignedAgentStamp) {
		this.assignedAgentStamp = assignedAgentStamp;
	}

	public long getFistResponseStamp() {
		return fistResponseStamp;
	}

	public void setFistResponseStamp(long fistResponseStamp) {
		this.fistResponseStamp = fistResponseStamp;
	}

	public long getLastResponseStamp() {
		return lastResponseStamp;
	}

	public void setLastResponseStamp(long lastResponseStamp) {
		this.lastResponseStamp = lastResponseStamp;
	}

	public Integer getAgentScore() {
		return agentScore;
	}

	public void setAgentScore(Integer agentScore) {
		this.agentScore = agentScore;
	}

	public Integer getBotScore() {
		return botScore;
	}

	public void setBotScore(Integer botScore) {
		this.botScore = botScore;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public String getContactName() {
		return contactName;
	}

	public void setContactName(String contactName) {
		this.contactName = contactName;
	}

	public long getResolveSessionStamp() {
		return resolveSessionStamp;
	}

	public void setResolveSessionStamp(long resolveSessionStamp) {
		this.resolveSessionStamp = resolveSessionStamp;
	}

	public boolean isResolved() {
		return resolved;
	}

	public void setResolved(boolean resolved) {
		this.resolved = resolved;
	}

	@Deprecated
	public String getContactType() {
		if (!ArgUtil.is(contactType)) {
			return this.contact().getContactType();
		}
		return contactType;
	}

	@Deprecated
	public void setContactType(String contactType) {
		this.contactType = contactType;
	}

	@Deprecated
	public String getChannel() {
		if (!ArgUtil.is(channel)) {
			return this.contact().getChannelType();
		}
		return channel;
	}

	@Deprecated
	public void setChannel(String channel) {
		this.channel = channel;
	}

	@Deprecated
	public String getLane() {
		if (!ArgUtil.is(lane)) {
			return this.contact().getLane();
		}
		return lane;
	}

	@Deprecated
	public void setLane(String lane) {
		this.lane = lane;
	}

	public boolean isExpired() {
		return expired;
	}

	public void setExpired(boolean expired) {
		this.expired = expired;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public long getAgentSessionStamp() {
		return agentSessionStamp;
	}

	public void setAgentSessionStamp(long agentSessionStamp) {
		this.agentSessionStamp = agentSessionStamp;
	}

	public long getLastOutGoingStamp() {
		return lastOutGoingStamp;
	}

	public void setLastOutGoingStamp(long lastOutGoingStamp) {
		this.lastOutGoingStamp = lastOutGoingStamp;
	}

	@Override
	public String toString() {
		return String.format("[sessionId:%s]", this.sessionId);
	}

//	public MessageDoc getLastInBoundMsg() {
//		return lastInBoundMsg;
//	}
//
//	public void setLastInBoundMsg(MessageDoc lastInBoundMsg) {
//		this.lastInBoundMsg = lastInBoundMsg;
//	}
//
//	public MessageDoc getLastOutBoundMsg() {
//		return lastOutBoundMsg;
//	}
//
//	public void setLastOutBoundMsg(MessageDoc lastOutBoundMsg) {
//		this.lastOutBoundMsg = lastOutBoundMsg;
//	}

//	public MessageDoc getLastMsg() {
//		return lastMsg;
//	}
//
//	public void setLastMsg(MessageDoc lastMsg) {
//		this.lastMsg = lastMsg;
//	}

	/**
	 * @deprecated Use {@link UpdatedTimeStampSupport#getUpdated())}
	 */
	@Deprecated
	public long getUpdatedStamp() {
		return this.updatedStamp;
	}

	/**
	 * @deprecated Use
	 *             {@link UpdatedTimeStampIndexSupport#setUpdated(com.boot.jx.mongo.CommonDocInterfaces.TimeStampIndex)}
	 */
	@Deprecated
	public void setUpdatedStamp(long updatedStamp) {
		this.updatedStamp = updatedStamp;
	}

	@Override
	public long updatedStamp() {
		if (ArgUtil.is(this.getUpdated())) {
			return this.getUpdated().getStamp();
		}
		return this.updatedStamp;
	}

	public String getTagCategory() {
		return tagCategory;
	}

	public void setTagCategory(String tagCategory) {
		this.tagCategory = tagCategory;
	}

	public List<String> getTagId() {
		return tagId;
	}

	public void setTagId(List<String> tagId) {
		this.tagId = tagId;
	}

	public List<String> tagId() {
		if (ArgUtil.isEmpty(this.tagId))
			this.tagId = new ArrayList<String>();
		return tagId;
	}

	public long getFirstInComingStamp() {
		return firstInComingStamp;
	}

	public void setFirstInComingStamp(long firstInComingStamp) {
		this.firstInComingStamp = firstInComingStamp;
	}

	public long getFirstOutGoingStamp() {
		return firstOutGoingStamp;
	}

	public void setFirstOutGoingStamp(long firstOutGoingStamp) {
		this.firstOutGoingStamp = firstOutGoingStamp;
	}

	public boolean isPrimary() {
		return primary;
	}

	public void setPrimary(boolean primary) {
		this.primary = primary;
	}

	public ContactDetailDoc getContact() {
		return contact;
	}

	public void setContact(ContactDetailDoc contact) {
		this.contact = contact;
	}

	public Contactable contact() {
		if (this.contact == null) {
			this.contact = new ContactDetailDoc();
		}
		return this.contact;
	}

	public String getAssignedToQueue() {
		return assignedToQueue;
	}

	public void setAssignedToQueue(String assignedToQueue) {
		this.assignedToQueue = assignedToQueue;
	}

	public Map<String, Object> getStore() {
		return store;
	}

	public void setStore(Map<String, Object> store) {
		this.store = store;
	}

	public Map<String, Object> store() {
		if (this.store == null) {
			this.store = new HashMap<String, Object>();
		}
		return store;
	}

	public Map<String, Object> getMeta() {
		return meta;
	}

	public void setMeta(Map<String, Object> meta) {
		this.meta = meta;
	}

	public String getAssignedToBot() {
		return assignedToBot;
	}

	public void setAssignedToBot(String assignedToBot) {
		this.assignedToBot = assignedToBot;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getTicketHash() {
		return ticketHash;
	}

	public void setTicketHash(String ticketHash) {
		this.ticketHash = ticketHash;
	}
	
	public MessageOrder getOrder() {
		return order;
	}

	public void setOrder(MessageOrder order) {
		this.order = order;
	}

	public TagDocument getTags() {
		return tags;
	}

	public void setTags(TagDocument tags) {
		this.tags = tags;
	}

	public Map<String, ChatMessageDTO> getMsg() {
		return msg;
	}

	public void setMsg(Map<String, ChatMessageDTO> msg) {
		this.msg = msg;
	}

	public Map<String, ChatMessageDTO> msg() {
		if (this.msg == null) {
			this.msg = new HashMap<String, ChatMessageDTO>();
		}
		return this.msg;
	}

	public Map<String, Long> getStamps() {
		return stamps;
	}

	public void setStamps(Map<String, Long> stamps) {
		this.stamps = stamps;
	}

	public Map<String, Long> stamps() {
		if (this.stamps == null) {
			this.stamps = new HashMap<String, Long>();
		}
		return this.stamps;
	}

	public Map<String, Long> getRead() {
		return read;
	}

	public void setRead(Map<String, Long> read) {
		this.read = read;
	}

	public Map<String, Long> read() {
		if (this.read == null) {
			this.read = new HashMap<String, Long>();
		}
		return this.read;
	}

	public Map<String, Object> getFeedback() {
		return feedback;
	}

	public void setFeedback(Map<String, Object> feedback) {
		this.feedback = feedback;
	}

	public String getRoutingId() {
		return routingId;
	}

	public void setRoutingId(String routingId) {
		this.routingId = routingId;
	}

	public ChatMessageDTO lastMsg() {
		return this.msg().get("lastMsg");
	}

	public ChatMessageDTO lastOutBoundMsg() {
		return this.msg().get("lastOutBoundMsg");
	}

	public ChatMessageDTO lastInBoundMsg() {
		return this.msg().get("lastInBoundMsg");
	}

	public void refreshStamps() {
		if (!ArgUtil.is(this.lastOutGoingStamp)) {
			ChatMessageDTO lastOutBoundMsg = this.lastOutBoundMsg();
			ChatMessageDTO lastMsg = this.lastMsg();
			if (lastOutBoundMsg != null && PostManUtil.isOutBound(lastOutBoundMsg.getType())) {
				this.lastOutGoingStamp = lastOutBoundMsg().getTimestamp();
			} else if (lastMsg != null && PostManUtil.isOutBound(lastMsg.getType())) {
				this.lastOutGoingStamp = lastMsg.getTimestamp();
			}
		}

		if (!ArgUtil.is(this.lastInComingStamp)) {
			ChatMessageDTO lastInBoundMsg = this.lastInBoundMsg();
			ChatMessageDTO lastMsg = this.lastMsg();

			if (lastInBoundMsg != null && PostManUtil.isInBound(lastInBoundMsg.getType())) {
				this.lastInComingStamp = lastInBoundMsg.getTimestamp();
			} else if (lastMsg != null && PostManUtil.isInBound(lastMsg.getType())) {
				this.lastInComingStamp = lastMsg.getTimestamp();
			}
		}
	}

	public Map<String, Object> getSummary() {
		return summary;
	}

	public void setSummary(Map<String, Object> summary) {
		this.summary = summary;
	}

	public Map<String, Object> summary() {
		if (this.summary == null) {
			this.summary = new HashMap<String, Object>();
		}
		return summary;
	}

	public long getSessionExpiryStamp() {
		return sessionExpiryStamp;
	}

	@Deprecated
	public void setSessionExpiryStamp(long sessionExpiryStamp) {
		this.sessionExpiryStamp = sessionExpiryStamp;
	}

	public Map<String, Object> getTpMeta() {
		return tpMeta;
	}

	public void setTpMeta(Map<String, Object> tpMeta) {
		this.tpMeta = tpMeta;
	}

	public Map<String, Object> getInsights() {
		return insights;
	}

	public void setInsights(Map<String, Object> insights) {
		this.insights = insights;
	}

}
