package com.boot.jx.postman.store;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.boot.jx.api.ApiResponseUtil;
import com.boot.jx.mongo.CommonMongoQB.MongoQueryBuilder;
import com.boot.jx.mongo.CommonMongoQB.QueryCriteria;
import com.boot.jx.mongo.CommonMongoQueryBuilder;
import com.boot.jx.mongo.MongoStore;
import com.boot.jx.postman.PMConstants;
import com.boot.jx.postman.PMConstants.CHAT_MODE;
import com.boot.jx.postman.PMConstants.CHAT_STATUS;
import com.boot.jx.postman.PMConstants.DEFAULT_VALUES;
import com.boot.jx.postman.PMEnvironment.PMClientConfig;
import com.boot.jx.postman.PMEnvironment.PMDomainConfig;
import com.boot.jx.postman.doc.ChatContactDoc;
import com.boot.jx.postman.doc.ChatProfileDoc;
import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.doc.MessageDoc;
import com.boot.jx.postman.doc.OrderContactDoc;
import com.boot.jx.postman.doc.QuickTag;
import com.boot.jx.postman.dto.ChatProfileDTO;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.MessageDefinitions.Contactable;
import com.boot.jx.postman.model.MessageDefinitions.IMessage;
import com.boot.jx.postman.model.MessageDefinitions.IMessageExtended;
import com.boot.jx.postman.model.MessageDefinitions.SessionInfo;
import com.boot.jx.postman.query.ChatSessionQuery;
import com.boot.utils.ArgUtil;
import com.boot.utils.DateUtil;
import com.boot.utils.EntityDtoUtil;
import com.boot.utils.TimeUtils;

@Component
public class SessionStore extends MongoStore {

	private static final Logger LOGGER = LoggerFactory.getLogger(SessionStore.class);

	@Autowired
	public PMClientConfig pmClientConfig;

	@Autowired
	private MessageContext messageContext;

	@Autowired
	private PMDomainConfig pmDomainConfig;

	@Autowired
	private ContactStore contactStore;

	public ChatContactDoc getContact(Contactable contactable) {
		return contactStore.getContact(contactable);
	}

	public ChatContactDoc getContact(IMessage inboxMessage) {
		return contactStore.getContact(inboxMessage);
	}

	public ChatContactDoc getContact(String contactId) {
		return contactStore.findContactById(contactId);
	}

	public ChatContactDoc save(ChatContactDoc chatContactDoc) {
		super.save(chatContactDoc);
		return chatContactDoc;
	}

	public ChatSessionDoc getSession(String sessionId) {
		return super.findById(sessionId, ChatSessionDoc.class);
	}

	public ChatSessionDoc getSessionPrimeByTicketHash(String contactId, String ticketHash) {
		MongoQueryBuilder<ChatSessionDoc> cmqb = MongoQueryBuilder.collection(ChatSessionDoc.class).where(
				Criteria.where("contactId").is(contactId).and("ticketHash").is(ticketHash).and("primary").is(true));
		return super.findOne(cmqb.getQuery(), ChatSessionDoc.class);
	}
	
	/** Plain order id → Mehery contactId (ClickPost Shopify cache). */
	public OrderContactDoc findOrderContact(String orderId) {
		if (!ArgUtil.is(orderId)) {
			return null;
		}
		return super.findById(orderId, OrderContactDoc.class);
	}

	/** Write-once mapping after first successful Shopify resolution for an order. */
	public void saveOrderContact(String orderId, String contactId) {
		if (!ArgUtil.is(orderId) || !ArgUtil.is(contactId)) {
			return;
		}
		OrderContactDoc doc = new OrderContactDoc();
		doc.setOrderId(orderId);
		doc.setContactId(contactId);
		doc.setCreatedAt(System.currentTimeMillis());
		super.save(doc);
	}

	public boolean isSessionValid(ChatSessionDoc chatSessionDoc) {
		if ((ArgUtil.isEmpty(chatSessionDoc) || !chatSessionDoc.isActive()) || chatSessionDoc.isExpired()
				|| PMConstants.CHAT_STATUS.EXPIRED.toString().equals(chatSessionDoc.getStatus())) {
			return false;
		}

		chatSessionDoc.refreshStamps();

		if (!ArgUtil.isEmptyValue(chatSessionDoc.getLastInComingStamp())
				&& (chatSessionDoc.getLastResponseStamp() > chatSessionDoc.getLastInComingStamp())) {
			return !TimeUtils.isExpired(chatSessionDoc.getLastInComingStamp(), pmClientConfig.getChatSessionTimeout());
		}

		if (ArgUtil.is(chatSessionDoc.getUpdated())) {
			return !TimeUtils.isExpired(chatSessionDoc.getUpdated().getStamp(), pmClientConfig.getChatSessionTimeout());
		}

		return true;
	}

	public ChatSessionDoc getValidSession(String sessionId) {
		ChatSessionDoc chatSessionDoc = getSession(sessionId);
		if (isSessionValid(chatSessionDoc)) {
			return chatSessionDoc;
		}
		return null;
	}

	public SessionInfo updateMessageFromSession(ChatSessionDoc chatSessionDoc, SessionInfo iMessage) {
		if (ArgUtil.isEmpty(iMessage.contact().getName())) {
			iMessage.contact().setName(chatSessionDoc.contact().getName());
		}
		// needs to be verified
		iMessage.contact().copyFrom(chatSessionDoc.contact());

		iMessage.contact().setContactId(chatSessionDoc.getContactId());
		iMessage.setSessionId(chatSessionDoc.getSessionId());
		iMessage.session().from(chatSessionDoc);
		iMessage.session().setSessionStamp(chatSessionDoc.updatedStamp());;

		// Router
		iMessage.route().setRouterId(chatSessionDoc.getRoutingId());

		return iMessage;
	}

	public IMessageExtended toSessionMessage(ChatSessionDoc session) {
		ChatContactDoc contact = getContact(session.getContactId());
		InboxMessage inboxMessage = new InboxMessage();

		if (ArgUtil.is(contact)) {
			inboxMessage.contact().copyFrom(contact);
			inboxMessage.contact().setContactType(contact.getContactType());
			inboxMessage.contact().setChannelType(contact.getChannelType());
			inboxMessage.contact().setLane(ArgUtil.nonEmpty(session.getLane(), contact.getLane()));
			inboxMessage.setFrom(contact.getCsid());
			inboxMessage.setFromName(contact.getName());
			inboxMessage.setSessionId(contact.getSessionId());
			inboxMessage.contact().setContactId(contact.getContactId());
		} else {
			ApiResponseUtil.addWarning("contact[" + session.getContactId() + "] does not exists from session["
					+ session.getSessionId() + "]");
		}

		inboxMessage.session().setQueue(session.getAssignedToQueue());
		inboxMessage.session().setMode(session.getMode());
		inboxMessage.session().setAgent(session.getAssignedToAgent());
		inboxMessage.session().setDept(session.getAssignedToDept());

		return inboxMessage;
	}

	public boolean inactiveAllPreviousSessions(String contactId, String ticketHash) {

		Criteria contactQ = Criteria.where("contactId").is(contactId);

		if (ArgUtil.is(ticketHash)) {
			contactQ.and("ticketHash").is(ticketHash);
		} else {
			contactQ.and("ticketHash").is(null);
		}

		Query query2 = new Query();
		query2.addCriteria(contactQ.orOperator(
				// is active
				Criteria.where("active").is(true),
				// or primary
				Criteria.where("primary").is(true)));
		Update update = new Update().set("active", false).set("primary", false).set("closeSessionStamp",
				System.currentTimeMillis());
		super.updateMulti(query2, update, ChatSessionDoc.class);
		return true;
	}

	@Deprecated
	public boolean closeAllPreviousSessions(String contactId) {
		return this.inactiveAllPreviousSessions(contactId, null);
	}

	public List<ChatSessionDoc> findChatSessionDocByAgent(String agentCode) {
		Query query2 = new Query();
		query2.addCriteria(Criteria.where("assignedToAgent").is(agentCode).and("active").is(true));
		return super.find(query2, ChatSessionDoc.class);
	}

	public List<ChatSessionDoc> findChatSessionDocByQuery(Query query) {
		return super.find(query, ChatSessionDoc.class);
	}

	public void expireChatSession() {
		Calendar cal = Calendar.getInstance();
		int offsetOur = (int) ((cal.getTimeInMillis() / 3600)
				% (TimeUtils.toHours(pmClientConfig.getChatSessionTimeout()) / 2));
		if (offsetOur == 0) {
			cal.add(Calendar.HOUR, -1 * (int) TimeUtils.toHours(pmClientConfig.getChatSessionTimeout()));
			MongoQueryBuilder<ChatSessionDoc> cmqb = MongoQueryBuilder.collection(ChatSessionDoc.class)
					.where(Criteria.where("active").is(true).and("lastInComingStamp").lt(cal.getTimeInMillis())
							.andOperator(new Criteria().orOperator(Criteria.where("resolved").exists(false),
									Criteria.where("resolved").is(false))))
					.set("expired", true).set("active", false).set("closeSessionStamp", System.currentTimeMillis());
			super.updateFirst(cmqb.getQuery(), cmqb.getUpdate(), ChatSessionDoc.class);
		}
	}

	public List<ChatSessionDoc> findSimilarChatSessionForContactId(String contactId, Long fromStamp, Long toStamp) {
		ChatContactDoc contact = getContact(contactId);

		List<ChatContactDoc> contacts = null;
		if (ArgUtil.is(contact) && !ArgUtil.areEmpty(contact.phone(), contact.getEmail())) {
			Query query1 = new Query();
			List<Criteria> orExpression = new ArrayList<Criteria>();
			if (ArgUtil.is(contact.phone())) {
				orExpression.add(Criteria.where("phone").is(contact.phone()));
			}
			if (ArgUtil.is(contact.getEmail())) {
				orExpression.add(Criteria.where("email").is(contact.getEmail()));
			}
			if (ArgUtil.is(contact.getProfileId())) {
				orExpression.add(Criteria.where("profileId").is(contact.getProfileId()));
			}
			query1.addCriteria(new Criteria().orOperator(orExpression.toArray(new Criteria[orExpression.size()])));
			contacts = super.find(query1, ChatContactDoc.class);
		}

		Calendar timeout = Calendar.getInstance();
		timeout.setTimeInMillis(timeout.getTimeInMillis() - DEFAULT_VALUES.POSTMAN_AGENT_TAB_HISTORY_PERIOD * 30);

		Query query2 = new Query();
		List<Criteria> orExpression = new ArrayList<Criteria>();

		if (ArgUtil.is(contacts)) {
			for (ChatContactDoc chatContactDoc : contacts) {
				orExpression.add(Criteria.where("contactId").is(chatContactDoc.getContactId()));
			}
		} else {
			orExpression.add(Criteria.where("contactId").is(contactId));
		}
		// Time Limit Criteria
		// Criteria tymCriteria = Criteria.where("updatedStamp");
		Criteria tymCriteria = Criteria.where("updated.stamp");
		if (fromStamp > 0L) {
			tymCriteria.gte(fromStamp);
		}
		if (toStamp > 0L) {
			tymCriteria.lt(toStamp);
		}

		if (fromStamp == 0L && toStamp == 0L) {
			tymCriteria.gte(timeout.getTimeInMillis());
		}

		query2.addCriteria(tymCriteria.orOperator(orExpression.toArray(new Criteria[orExpression.size()])));
		removeMsgFields(query2);
		return super.find(query2, ChatSessionDoc.class);
	}

	private void removeMsgFields(Query query2) {
		query2.fields().exclude("lastInBoundMsg").exclude("lastBotReply").exclude("lastAgentReply")
				.exclude("lastOutBoundMsg").exclude("lastMsg");
	}

	public List<ChatSessionDoc> findActiveChatSessionForContactId(String contactId) {
		Query query2 = new Query();
		query2.addCriteria(Criteria.where("contactId").is(contactId).and("active").is(true));
		return super.find(query2, ChatSessionDoc.class);
	}

	public ChatSessionDoc saveSession(ChatSessionDoc chatSessionDoc) {
		try {
			ChatSessionQuery query = new ChatSessionQuery(chatSessionDoc);
			if (ArgUtil.isEmpty(chatSessionDoc.getStartSessionStamp()) || chatSessionDoc.getStartSessionStamp() == 0L) {
				query.setStartSessionStamp(System.currentTimeMillis());
			}
			super.save(chatSessionDoc);
		} catch (Exception e) {
			ChatSessionDoc chatSessionDoc2 = super.findById(chatSessionDoc.getSessionId(), ChatSessionDoc.class);
			LOGGER.error(chatSessionDoc.getVersion() + " ~ " + chatSessionDoc2.getVersion(), e);
			if (chatSessionDoc.getVersion() == null) {
				// chatSessionDoc.setVersion(0);
				super.save(chatSessionDoc);
			} else {
				// chatSessionDoc.setVersion(chatSessionDoc2.getVersion()+1);
				super.save(chatSessionDoc);
			}
		}
		return chatSessionDoc;
	}

	public ChatSessionDoc initSession(ChatSessionDoc chatSessionDoc) {

		ChatContactDoc contactDoc = messageContext.contact().getDoc();

		chatSessionDoc.setInitd(true);
		chatSessionDoc.setContactName(contactDoc.getName());

		MongoQueryBuilder<ChatSessionDoc> builder = MongoQueryBuilder.collection(ChatSessionDoc.class)
				.whereId(chatSessionDoc.getSessionId());
		builder.set("initd", chatSessionDoc.isInitd());
		builder.set("contactName", ArgUtil.nonEmpty(chatSessionDoc.getContactName(), contactDoc.getName()));
		super.updateFirst(builder.getQuery(), builder.getUpdate(), ChatSessionDoc.class);

		return chatSessionDoc;
	}

	public ChatSessionDoc changeStatus(ChatSessionDoc chatSessionDoc, PMConstants.CHAT_STATUS status) {
		// Old Way of Doing it
		chatSessionDoc.setStatus(status.toString());
		MongoQueryBuilder<ChatSessionDoc> builder = MongoQueryBuilder.collection(ChatSessionDoc.class)
				.whereId(chatSessionDoc.getSessionId());
		builder.set("status", status.toString());
		super.updateFirst(builder.getQuery(), builder.getUpdate(), ChatSessionDoc.class);
		return chatSessionDoc;
	}

	public ChatSessionDoc resolveSession(ChatSessionDoc chatSessionDoc) {
		// Old Way of Doing it
		chatSessionDoc.setResolveSessionStamp(System.currentTimeMillis());
		chatSessionDoc.setResolved(true);

		MongoQueryBuilder<ChatSessionDoc> builder = MongoQueryBuilder.collection(ChatSessionDoc.class)
				.whereId(chatSessionDoc.getSessionId());
		builder.set("resolveSessionStamp", chatSessionDoc.getResolveSessionStamp());
		builder.set("resolved", chatSessionDoc.isResolved());
		builder.set("status", PMConstants.CHAT_STATUS.RESOLVED);
		builder.set("summary.assignedToAtResolve", chatSessionDoc.getAssignedToAgent());
		super.updateFirst(builder.getQuery(), builder.getUpdate(), ChatSessionDoc.class);
		return chatSessionDoc;
	}

	public ChatSessionDoc expireSession(ChatSessionDoc chatSessionDoc) {

		ChatSessionQuery query = new ChatSessionQuery(chatSessionDoc);

		query.setExpireSessionStamp(System.currentTimeMillis());
		query.setExpired(true);
		query.setStatus(PMConstants.CHAT_STATUS.EXPIRED);
		query.setSummary("assignedToAtExpire", chatSessionDoc.getAssignedToAgent());

		super.updateFirst(query.getQuery(), query.getUpdate(), ChatSessionDoc.class);
		return chatSessionDoc;
	}

	public ChatSessionDoc closeSession(ChatSessionDoc chatSessionDoc) {

		ChatSessionQuery query = new ChatSessionQuery(chatSessionDoc);

		query.setCloseSessionStamp(System.currentTimeMillis());
		query.setActive(false);
		query.setStatus(PMConstants.CHAT_STATUS.CLOSED);
		query.setSummary("assignedToAtClose", chatSessionDoc.getAssignedToAgent());

		super.updateFirst(query.getQuery(), query.getUpdate(), ChatSessionDoc.class);
		return chatSessionDoc;
	}

	public ChatSessionDoc deleteSession(ChatSessionDoc chatSessionDoc) {
		MongoQueryBuilder<ChatSessionDoc> builder = MongoQueryBuilder.collection(ChatSessionDoc.class)
				.where(QueryCriteria.whereId(chatSessionDoc.getSessionId()).and("channel").is("IMPORT"));
		super.remove(builder.getQuery(), ChatSessionDoc.class);

		MongoQueryBuilder<ChatSessionDoc> builder2 = MongoQueryBuilder.collection(ChatSessionDoc.class)
				.where(QueryCriteria.where("sessionId").is(chatSessionDoc.getSessionId()));
		super.remove(builder2.getQuery(), MessageDoc.class,
				MessageStore.getCollectionName(chatSessionDoc.getContactType()));
		return chatSessionDoc;
	}

	public ChatSessionDoc botScore(ChatSessionDoc chatSessionDoc, Integer botScore) {
		chatSessionDoc.setBotScore(botScore);

		MongoQueryBuilder<ChatSessionDoc> builder = MongoQueryBuilder.collection(ChatSessionDoc.class)
				.whereId(chatSessionDoc.getSessionId());
		builder.set("botScore", chatSessionDoc.getBotScore());
		super.updateFirst(builder.getQuery(), builder.getUpdate(), ChatSessionDoc.class);

		return chatSessionDoc;
	}

	public ChatSessionDoc agentScore(ChatSessionDoc chatSessionDoc, Integer agentScore) {
		chatSessionDoc.setAgentScore(agentScore);

		MongoQueryBuilder<ChatSessionDoc> builder = MongoQueryBuilder.collection(ChatSessionDoc.class)
				.whereId(chatSessionDoc.getSessionId());
		builder.set("agentScore", chatSessionDoc.getAgentScore());
		super.updateFirst(builder.getQuery(), builder.getUpdate(), ChatSessionDoc.class);

		return chatSessionDoc;
	}

	public ChatProfileDoc save(ChatProfileDoc doc) {
		super.save(doc);
		return doc;
	}

	public ChatProfileDoc save(ChatProfileDTO profile) {
		ChatProfileDoc doc = EntityDtoUtil.dtoToEntity(profile, new ChatProfileDoc());
		doc.setId(profile.getProfileId());
		return save(doc);
	}

	public void updateResponseTime(ChatSessionDoc chatSessionDoc) {
		if (ArgUtil.isEmptyValue(chatSessionDoc.getFistResponseStamp())) {
			chatSessionDoc.setFistResponseStamp(System.currentTimeMillis());
		}
		chatSessionDoc.setLastResponseStamp(System.currentTimeMillis());
		MongoQueryBuilder<ChatSessionDoc> builder = MongoQueryBuilder.collection(ChatSessionDoc.class)
				.whereId(chatSessionDoc.getSessionId());
		builder.set("fistResponseStamp", chatSessionDoc.getFistResponseStamp());
		builder.set("lastResponseStamp", chatSessionDoc.getLastResponseStamp());
		super.updateFirst(builder.getQuery(), builder.getUpdate(), ChatSessionDoc.class);
	}

	public void assignToAgent(ChatSessionDoc chatSessionDoc, String agentDept, String agentCode) {

		ChatSessionQuery builder = new ChatSessionQuery(chatSessionDoc);

		if (!ArgUtil.areEqual(chatSessionDoc.getAssignedToDept(), agentDept)) {
			// chatSessionDoc.setAssignedDeptStamp(System.currentTimeMillis());
			builder.setAssignedDeptStamp(System.currentTimeMillis());
		}
		chatSessionDoc.setMode(PMConstants.CHAT_MODE.AGENT.toString());
		chatSessionDoc.setAssignedToDept(agentDept);
		// chatSessionDoc.setAssignedAgentStamp(System.currentTimeMillis());
		builder.setAssignedAgentStamp(System.currentTimeMillis());
		chatSessionDoc.setAssignedToAgent(agentCode);
		// chatSessionDoc.setAssignedToQueue(PMConstants.DEFAULT.AGENT_QUEUE_CODE);
		if (chatSessionDoc.getAgentSessionStamp() == 0L) {
			chatSessionDoc.setAgentSessionStamp(chatSessionDoc.getAssignedAgentStamp());
		}

		// ChatSessionQuery builder = new
		// ChatSessionQuery(chatSessionDoc.getSessionId());
		// builder.set("mode", chatSessionDoc.getMode());
		builder.set("assignedToQueue", chatSessionDoc.getAssignedToQueue());
		builder.set("assignedToDept", chatSessionDoc.getAssignedToDept());
		// builder.set("assignedDeptStamp", chatSessionDoc.getAssignedDeptStamp());
		builder.set("assignedToAgent", chatSessionDoc.getAssignedToAgent());
		// builder.set("assignedAgentStamp", chatSessionDoc.getAssignedAgentStamp());
		builder.set("agentSessionStamp", chatSessionDoc.getAgentSessionStamp());
		updateFirst(builder);
	}

	public void assignToBot(ChatSessionDoc chatSessionDoc, String botName) {
		if (!ArgUtil.is(chatSessionDoc)) {
			LOGGER.error("Session Cannot Be Empty for bot {}", botName);
			return;
		}

		chatSessionDoc.setMode(PMConstants.CHAT_MODE.BOT.toString());
		chatSessionDoc.setAssignedToBot(botName);

		MongoQueryBuilder<ChatSessionDoc> builder = MongoQueryBuilder.collection(ChatSessionDoc.class)
				.whereId(chatSessionDoc.getSessionId());
		builder.set("mode", chatSessionDoc.getMode());
		builder.set("assignedToAgent", chatSessionDoc.getAssignedToAgent());
		super.updateFirst(builder.getQuery(), builder.getUpdate(), ChatSessionDoc.class);

	}

	public ChatSessionDoc updateQuickTags(ChatSessionDoc chatSessionDoc, List<String> tagIds) {
		chatSessionDoc.setTagId(tagIds);
		MongoQueryBuilder<ChatSessionDoc> builder = MongoQueryBuilder.collection(ChatSessionDoc.class)
				.whereId(chatSessionDoc.getSessionId());
		builder.set("tagId", tagIds);
		super.updateFirst(builder.getQuery(), builder.getUpdate(), ChatSessionDoc.class);
		return chatSessionDoc;
	}

	/**
	 * search by status
	 * 
	 * @param status
	 * @return
	 */
	public List<ChatSessionDoc> findByStatus(CHAT_STATUS status) {
		Query query2 = new Query();
		if (ArgUtil.is(status)) {
			query2.addCriteria(Criteria.where("status").is(status.toString()));
		}
		return super.find(query2, ChatSessionDoc.class);
	}

	/**
	 * Search by category
	 * 
	 * @param tagCategory
	 * @return
	 */
	public List<ChatSessionDoc> findByQuickTag(String tagCategory) {
		Query query2 = new Query();
		if (ArgUtil.is(tagCategory)) {
			query2.addCriteria(Criteria.where("tagId").is(tagCategory));
		}
		return super.find(query2, ChatSessionDoc.class);
	}

	/**
	 * search by status or by tag category
	 * 
	 * @param status
	 * @param tagCategory
	 * @param fromStamp
	 * @param toStamp
	 * @return
	 */
	public List<ChatSessionDoc> findByStatusOrQuickTag(List<CHAT_STATUS> status, List<String> tagCategory,
			long fromStamp, long toStamp) {
		List<String> statusLst = new ArrayList<>();;
		if ((status == null || status.isEmpty() || status.contains(null)) && (tagCategory == null
				|| tagCategory.isEmpty() || tagCategory.contains(null) && tagCategory.contains(""))) {
			// status = new ArrayList<>();
			// status.add(CHAT_STATUS.OPEN);
			statusLst.add(CHAT_STATUS.OPEN.toString());
		} else {
			for (CHAT_STATUS chatSt : status) {
				statusLst.add(chatSt.toString());
			}
		}

		Query query = new Query();

		query.addCriteria(Criteria.where("agentSessionStamp").gt(fromStamp).lt(toStamp));

		if (statusLst != null && !statusLst.isEmpty()) {
			query.addCriteria(Criteria.where("status").in(statusLst));
		}
		if (tagCategory != null && !tagCategory.isEmpty() && !tagCategory.contains(null) && !tagCategory.contains("")) {
			query.addCriteria(Criteria.where("tagId").in(tagCategory));
		}
		query.with(Sort.by(new Order(Direction.DESC, "agentSessionStamp")));
		removeMsgFields(query);
		LOGGER.debug("query {===}" + query);
		return super.find(query, ChatSessionDoc.class);
	}

	public List<ChatSessionDoc> findByStatusOrQuickTagV1(List<CHAT_STATUS> status, List<String> tagCategory,
			long startStampLong, long endStampLong) {
		List<String> statusLst = new ArrayList<>();;
		if ((status == null || status.isEmpty() || status.contains(null)) && (tagCategory == null
				|| tagCategory.isEmpty() || tagCategory.contains(null) && tagCategory.contains(""))) {
			// statusLst.add(CHAT_STATUS.OPEN.toString()); for all status
		} else {
			for (CHAT_STATUS chatSt : status) {
				statusLst.add(chatSt.toString());
			}
		}

		Criteria criteria = new Criteria();

		Query query2 = new Query();

		Criteria dateCriteria = new Criteria().orOperator(
				new Criteria().andOperator(Criteria.where("startSessionStamp").gt(startStampLong),
						Criteria.where("startSessionStamp").lt(endStampLong)),
				new Criteria().andOperator(Criteria.where("closeSessionStamp").gt(startStampLong),
						Criteria.where("closeSessionStamp").lt(endStampLong)),

				new Criteria().andOperator(Criteria.where("assignedDeptStamp").gt(startStampLong),
						Criteria.where("assignedDeptStamp").lt(endStampLong)),
				new Criteria().andOperator(Criteria.where("assignedAgentStamp").gt(startStampLong),
						Criteria.where("assignedAgentStamp").lt(endStampLong)),

				new Criteria().andOperator(Criteria.where("fistResponseStamp").gt(startStampLong),
						Criteria.where("fistResponseStamp").lt(endStampLong)),
				new Criteria().andOperator(Criteria.where("lastResponseStamp").gt(startStampLong),
						Criteria.where("lastResponseStamp").lt(endStampLong)),

				new Criteria().andOperator(Criteria.where("lastInComingStamp").gt(startStampLong),
						Criteria.where("lastInComingStamp").lt(endStampLong)));

		criteria.andOperator(dateCriteria);

		if (statusLst != null && !statusLst.isEmpty()) {
			query2.addCriteria(Criteria.where("status").in(statusLst));
		}
		if (tagCategory != null && !tagCategory.isEmpty() && !tagCategory.contains(null) && !tagCategory.contains("")) {
			query2.addCriteria(Criteria.where("tagId").in(tagCategory));
		}

		query2 = query2.addCriteria(criteria).with(Sort.by(Sort.Direction.DESC, "startSessionStamp"));
		List<ChatSessionDoc> messages = find(query2, ChatSessionDoc.class);
		LOGGER.debug("query {===}" + query2);
		return messages;

	}

	public String getLastAssignedAgent(Contactable contact) {
		MongoQueryBuilder<ChatSessionDoc> cmqb = MongoQueryBuilder.collection(ChatSessionDoc.class)
				.where(Criteria.where("contactId").is(contact.getContactId()).and("assignedToAgent").exists(true)
						.and("mode").is(CHAT_MODE.AGENT));
		cmqb.sortBy("startSessionStamp", Direction.DESC).limit(1).skipDBRef();
		ChatSessionDoc lastSession = super.findOne(cmqb.getQuery(), ChatSessionDoc.class);
		if (ArgUtil.is(lastSession)) {
			return lastSession.getAssignedToAgent();
		}
		return null;
	}

	public ChatSessionDoc getPreviousSession(Contactable contact) {
		MongoQueryBuilder<ChatSessionDoc> cmqb = MongoQueryBuilder.collection(ChatSessionDoc.class)
				.where(Criteria.where("contactId").is(contact.getContactId()));
		cmqb.sortBy("startSessionStamp", Direction.DESC).limit(1).skip(1).skipDBRef();
		return super.findOne(cmqb.getQuery(), ChatSessionDoc.class);
	}

	public ChatSessionDoc getPreviousSession(Contactable contact, long timestamp) {
		MongoQueryBuilder<ChatSessionDoc> cmqb = MongoQueryBuilder.collection(ChatSessionDoc.class)
				.where(Criteria.where("contactId").is(contact.getContactId()).and("startSessionStamp").lt(timestamp));
		cmqb.sortBy("startSessionStamp", Direction.DESC).limit(1)
				// .skip(1)
				.skipDBRef();
		return super.findOne(cmqb.getQuery(), ChatSessionDoc.class);
	}

	public ChatSessionDoc getPreviousSession(ChatSessionDoc session) {

		Criteria criteria = QueryCriteria.whereIdNot(session.getSessionId()).and("contactId").is(session.getContactId())
				.and("startSessionStamp").lt(session.getStartSessionStamp());

		if (ArgUtil.is(session.getTicketHash())) {
			criteria.and("ticketHash").is(session.getTicketHash());
		}

		MongoQueryBuilder<ChatSessionDoc> cmqb = MongoQueryBuilder.collection(ChatSessionDoc.class).where(criteria);
		cmqb.sortBy("startSessionStamp", Direction.DESC).limit(1)
				// .skip(1)
				.skipDBRef();

		// System.out.println("===" + cmqb.getQuery());

		return super.findOne(cmqb.getQuery(), ChatSessionDoc.class);
	}

	public List<ChatSessionDoc> findByStatusOrQuickTagV2(List<CHAT_STATUS> status, List<QuickTag> tagCategory,
			long fromStamp, long toStamp) {
		List<String> statusLst = new ArrayList<>();;
		if ((status == null || status.isEmpty() || status.contains(null)) && (tagCategory == null
				|| tagCategory.isEmpty() || tagCategory.contains(null) && tagCategory.contains(""))) {
			statusLst.add(CHAT_STATUS.OPEN.toString());
		} else {
			for (CHAT_STATUS chatSt : status) {
				statusLst.add(chatSt.toString());
			}
		}

		Query query = new Query();
		Criteria primaryCriteria = new Criteria();//
		List<Criteria> criterias = new ArrayList<Criteria>();

		if (fromStamp <= 0) {
			fromStamp = DateUtil.todayStartTime();
		}

		// primaryCriteria =
		// primaryCriteria.and("assignedAgentStamp").gt(fromStamp).lt(toStamp);
		primaryCriteria = primaryCriteria.and("agentSessionStamp").gt(fromStamp).lt(toStamp);
		criterias.add(primaryCriteria);

		if (statusLst != null && !statusLst.isEmpty()) {
			primaryCriteria = primaryCriteria.and("status").in(statusLst);
		}

		if (tagCategory != null && !tagCategory.isEmpty() && tagCategory.size() > 0) {
			MultiValueMap<String, String> tags = new LinkedMultiValueMap<String, String>();
			for (QuickTag tag : tagCategory) {
				tags.add(tag.getCategory(), tag.getId());
			}
			for (Entry<String, List<String>> tagEntry : tags.entrySet()) {
				criterias.add(Criteria.where("tagId").in(tagEntry.getValue()));
			}
		}

		query.addCriteria(primaryCriteria.andOperator(criterias.toArray(new Criteria[criterias.size()])))
				// Limit
				.with(Sort.by(Direction.DESC, "agentSessionStamp"));
		ApiResponseUtil.addLog(query.toString());
		// query.with(Sort.by(new Order(Direction.DESC, "assignedAgentStamp")));
		removeMsgFields(query);
		LOGGER.debug("query {===}" + query);

		return find(CommonMongoQueryBuilder.collection(ChatSessionDoc.class).query(query).skipDBRefByNames("lastMsg",
				"lastInBoundMsg", "lastOutBoundMsg", "lastBotReply", "lastAgentReply"));

		// return super.find(query, ChatSessionDoc.class);
	}

}
