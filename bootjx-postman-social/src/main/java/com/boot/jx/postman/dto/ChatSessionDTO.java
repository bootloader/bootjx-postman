package com.boot.jx.postman.dto;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.boot.jx.postman.model.ContactMeta;
import com.boot.jx.postman.model.MessageOrder;
import com.boot.model.TimeModels.ITimeStampIndex;
import com.boot.model.UtilityModels.JsonIgnoreNull;
import com.boot.model.UtilityModels.JsonIgnoreUnknown;
import com.boot.utils.ArgUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
//@JsonDeserialize(using = ChatSessionDTODeserializer.class)
public class ChatSessionDTO implements Serializable, JsonIgnoreNull, JsonIgnoreUnknown {

	private static final long serialVersionUID = 1L;

	private String sessionId;
	private String ticketHash;
	/** Latest order snapshot on this session (overwritten per inbound with order). */
	private MessageOrder order;
	private String routingId;
	private String subject;
	private String name;
	private String contactType;
	private String contactId;

	private String email;
	private String phone;

	private String profilePic;

	private String assignedToDept;
	private String assignedToAgent;
	private String assignedToQueue;
	private String assignedToBot;

	private long startSessionStamp;
	private long fistResponseStamp;

	private long agentSessionStamp;

	private long lastInComingStamp;

	private long assignedDeptStamp;
	private long assignedAgentStamp;

	private long lastResponseStamp;
	private long resolveSessionStamp;
	private long closeSessionStamp;

	private long updatedStamp;

	private boolean assigned;
	private boolean active;
	private boolean resolved;
	private boolean expired;

	private boolean primary;

	private String mode;
	private String status;
	private String state;
	private List<String> tagId;

	private ContactMeta contact;

	private List<ChatMessageDTO> messages;

	private Map<String, ChatMessageDTO> msg;

	private Map<String, Long> read;
	private Map<String, Long> stamps;

	private Map<String, Object> feedback;
	private Map<String, Object> meta;
	
	private Map<String,Object> insights;

	/**
	 * Should not add anything at this level, use tpMeta
	 */
	@Deprecated
	/** session expiry stamp **/
	private long sessionExpiryStamp;

	private Map<String, Object> tpMeta;

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	/**
	 * Contact name
	 * 
	 * @param name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Contact name
	 * 
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	public String getContactType() {
		if (!ArgUtil.is(contactType)) {
			return this.contact().getContactType();
		}
		return contactType;
	}

	public void setContactType(String contactType) {
		this.contactType = contactType;
	}

	public long getLastInComingStamp() {
		return lastInComingStamp;
	}

	public void setLastInComingStamp(long lastInComingStamp) {
		this.lastInComingStamp = lastInComingStamp;
	}

	public List<ChatMessageDTO> getMessages() {
		return messages;
	}

	public void setMessages(List<ChatMessageDTO> messages) {
		this.messages = messages;
	}

	public String getProfilePic() {
		return profilePic;
	}

	public void setProfilePic(String profilePic) {
		this.profilePic = profilePic;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public boolean isAssigned() {
		return assigned;
	}

	public void setAssigned(boolean assigned) {
		this.assigned = assigned;
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

	public String getContactId() {
		return contactId;
	}

	public void setContactId(String contactId) {
		this.contactId = contactId;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public long getStartSessionStamp() {
		return startSessionStamp;
	}

	public void setStartSessionStamp(long startSessionStamp) {
		this.startSessionStamp = startSessionStamp;
	}

	public long getFistResponseStamp() {
		return fistResponseStamp;
	}

	public void setFistResponseStamp(long fistResponseStamp) {
		this.fistResponseStamp = fistResponseStamp;
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

	public long getLastResponseStamp() {
		return lastResponseStamp;
	}

	public void setLastResponseStamp(long lastResponseStamp) {
		this.lastResponseStamp = lastResponseStamp;
	}

	public long getCloseSessionStamp() {
		return closeSessionStamp;
	}

	public void setCloseSessionStamp(long closeSessionStamp) {
		this.closeSessionStamp = closeSessionStamp;
	}

	public ContactMeta getContact() {
		return contact;
	}

	public void setContact(ContactMeta contact) {
		this.contact = contact;
	}

	public long getResolveSessionStamp() {
		return resolveSessionStamp;
	}

	public void setResolveSessionStamp(long resolvedSessionStamp) {
		this.resolveSessionStamp = resolvedSessionStamp;
	}

	public boolean isResolved() {
		return resolved;
	}

	public void setResolved(boolean resolved) {
		this.resolved = resolved;
	}

	public boolean isExpired() {
		return expired;
	}

	public void setExpired(boolean expired) {
		this.expired = expired;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
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

	public long getUpdatedStamp() {
		return updatedStamp;
	}

	public void setUpdatedStamp(long updatedStamp) {
		this.updatedStamp = updatedStamp;
	}

	public List<String> getTagId() {
		return tagId;
	}

	public void setTagId(List<String> tagId) {
		this.tagId = tagId;
	}

	public boolean isPrimary() {
		return primary;
	}

	public void setPrimary(boolean primary) {
		this.primary = primary;
	}

	public ContactMeta contact() {
		if (this.contact == null) {
			this.contact = new ContactDTO();
		}
		return this.contact;
	}

	public String getAssignedToQueue() {
		return assignedToQueue;
	}

	public void setAssignedToQueue(String assignedToQueue) {
		this.assignedToQueue = assignedToQueue;
	}

	public String getAssignedToBot() {
		return assignedToBot;
	}

	public void setAssignedToBot(String assignedToBot) {
		this.assignedToBot = assignedToBot;
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

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
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

	private ITimeStampIndex updated;

	public ITimeStampIndex getUpdated() {
		return updated;
	}

	public void setUpdated(ITimeStampIndex updated) {
		this.updated = updated;
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

	public Map<String, Long> getStamps() {
		return stamps;
	}

	public void setStamps(Map<String, Long> stamps) {
		this.stamps = stamps;
	}

	public Map<String, Object> getMeta() {
		return meta;
	}

	public void setMeta(Map<String, Object> meta) {
		this.meta = meta;
	}

	public long getSessionExpiryStamp() {
		return sessionExpiryStamp;
	}

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
