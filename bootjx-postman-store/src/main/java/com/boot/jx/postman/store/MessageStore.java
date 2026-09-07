package com.boot.jx.postman.store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import com.boot.jx.AppConfig;
import com.boot.jx.AppContextUtil;
import com.boot.jx.dict.ContactType;
import com.boot.jx.exception.ApiHttpExceptions.ApiHttpException;
import com.boot.jx.exception.ApiHttpExceptions.ApiHttpServerException;
import com.boot.jx.mongo.CommonDocInterfaces.TimeStampIndex;
import com.boot.jx.mongo.CommonMongoQB.MQB;
import com.boot.jx.mongo.CommonMongoQueryBuilder;
import com.boot.jx.mongo.CommonMongoStore.PaginatedQuery;
import com.boot.jx.mongo.CommonMongoTemplate;
import com.boot.jx.mongo.MongoStore;
import com.boot.jx.mongo.MongoUtils.QueryMode;
import com.boot.jx.mongo.QA;
import com.boot.jx.postman.PMConstants;
import com.boot.jx.postman.PMConstants.MESSAGE_BOUND_TYPE;
import com.boot.jx.postman.PMConstants.MESSAGE_SOURCE_CATEGARY;
import com.boot.jx.postman.doc.ContactDetailDoc;
import com.boot.jx.postman.doc.MessageDoc;
import com.boot.jx.postman.doc.MessageDocAbstract;
import com.boot.jx.postman.doc.MessageHold;
import com.boot.jx.postman.doc.MessageHold.MessageHoldOriginal;
import com.boot.jx.postman.doc.MessageHold.MessageHoldRejected;
import com.boot.jx.postman.doc.tpo.WABAConversation;
import com.boot.jx.postman.model.Attachment;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.Message.Status;
import com.boot.jx.postman.model.MessageDefinitions.IMessageLoggable;
import com.boot.jx.postman.model.MessageReferral;
import com.boot.jx.postman.model.MessageReport;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.model.TagDocument;
import com.boot.jx.postman.query.ChatSessionQuery;
import com.boot.jx.postman.query.WABAConversationQuery;
import com.boot.jx.utils.PostManUtil;
import com.boot.model.MapModel;
import com.boot.utils.ArgUtil;
import com.boot.utils.CollectionUtil;
import com.boot.utils.TimeUtils;
import com.google.common.collect.Lists;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.result.UpdateResult;

@Component
public class MessageStore extends MongoStore {

	private static final Logger LOGGER = LoggerFactory.getLogger(MessageStore.class);

	public static enum EVENTS {
		ASGND_TO_DEPT, ASGND_TO_AGENT, ASGND_TO_QUEUE, UNASGND, PICKED_BY_AGENT, CLOSED_BY_AGENT, LABEL_ADDED,
		LABEL_REMOVED, STATUS_CHANGED, TAG_ADDED, TAG_REMOVED, AUTO_ASSIGN_REDISTRIBUTE,

		// OTHER ERROS
		INBOUND_FORWARD_ERROR,

		// Events
		ON_SESSION_START, ON_SESSION_ROUTE, ON_SESSION_IDLE, CALL_PERMISSION_REPLY,

		// ENDS
		DEFAULT;
	}

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private CommonMongoTemplate commonMongoTemplate;

	@Value("${postman.chat.session.timeout}")
	private String chatSessionTimeout;

	@Autowired
	AppConfig appConfig;

	@Autowired
	SessionStore sessionStore;

	@Autowired
	private MessageContextStore messageContextStore;

	public OutboxMessage createOutboxMessage() {
		return new OutboxMessage();
	}

	public static String getCollectionName(Object contactType) {
		return (MessageDoc.COLLECTION_NAME + "_" + ArgUtil.parseAsString(contactType, "OTHERS"));
	}

	private MessageDoc updateMessageDoc(InboxMessage inboxMessage, MessageDoc doc) {
		doc.setSubject(inboxMessage.getSubject());
		doc.setMessage(inboxMessage.getMessage());
		doc.setMessageTrail(inboxMessage.getMessageTrail());
		doc.setSessionId(inboxMessage.getSessionId());
		doc.setReferral(inboxMessage.getReferral());
		doc.setOrder(inboxMessage.getOrder());
		doc.setCall(inboxMessage.getCall());

		doc.setRoute(inboxMessage.getRoute());
		doc.setQueue(ArgUtil.nonEmpty(inboxMessage.route().getQueueCode(), inboxMessage.session().getQueue()));

		doc.setTags(inboxMessage.getTags());
		doc.setMessageIdExt(inboxMessage.getMessageIdExt());
		doc.setReplyTo(inboxMessage.getReplyTo());

		if (ArgUtil.is(inboxMessage.getReplyId())) {
			doc.setReplyId(inboxMessage.getReplyId());
		}

		doc.setReplyIdExt(inboxMessage.getReplyIdExt());

		// Additonals
		doc.setAttachments(inboxMessage.getAttachments());
		doc.setVccards(inboxMessage.getVccards());

		doc.setTrace(inboxMessage.getTrace());
		doc.setLogs(inboxMessage.getLogs());
		doc.form().putAll(inboxMessage.form());
		doc.stamps().put("session", ArgUtil.parseAsLong(inboxMessage.session().getSessionStamp(), 0L));

		return doc;
	}

	private MessageDoc createMessageDoc(InboxMessage inboxMessage) {
		ContactType contactType = inboxMessage.contact().type();

		MessageDoc doc = MessageDoc.instance(contactType);
		doc.setTraceId(AppContextUtil.getTraceId());
		doc.setContactId(PostManUtil.createContactId(inboxMessage));
		doc.setReferral(inboxMessage.getReferral());
		doc.setOrder(inboxMessage.getOrder());
		if (inboxMessage.getCall() != null) {
			String direction = inboxMessage.getCall().getDirection();
			if ("BUSINESS_INITIATED".equals(direction)) {
				// Business initiated call → Outbound Call
				doc.setType(PMConstants.MESSAGE_BOUND_TYPE.OUTBOUND_CALL);
			} else {
				// USER_INITIATED or null/unknown → Inbound Call
				doc.setType(PMConstants.MESSAGE_BOUND_TYPE.INBOUND_CALL);
			}
		} else {
			doc.setType("I");
		}
		doc.setTimestamp(System.currentTimeMillis());
		doc.setTime(TimeStampIndex.now());

		doc.setFormatType(inboxMessage.getFormatType());
		doc.setFormatSubType(inboxMessage.getFormatSubType());

		ContactDetailDoc contact = new ContactDetailDoc();
		// contact.copyFrom(inboxMessage.contact()); TOO MUCH DATA
		if (ArgUtil.is(inboxMessage.contact().getName())) {
			contact.setName(inboxMessage.contact().getName());
		}
		if (ArgUtil.is(inboxMessage.contact().phone())) {
			contact.phone(inboxMessage.contact().phone());
		} else if (ArgUtil.is(inboxMessage.getFrom())) {
			contact.phone(inboxMessage.getFrom());
		}
		if (ArgUtil.is(inboxMessage.contact().getEmail())) {
			contact.setEmail(inboxMessage.contact().getEmail());
		}
		contact.setContactType(ArgUtil.parseAsString(contactType));
		contact.setChannelType(inboxMessage.contact().getChannelType());
		// Persist csid for inbound as well
		if (ArgUtil.is(inboxMessage.contact().getCsid())) {
			contact.setCsid(inboxMessage.contact().getCsid());
		}
		// Persist lane for inbound as well (including call messages IC/OC)
		if (ArgUtil.is(inboxMessage.contact().getLane())) {
			contact.setLane(inboxMessage.contact().getLane());
		}
		doc.setContact(contact);

		updateMessageDoc(inboxMessage, doc);

		return doc;
	}

	public MessageDoc findByMessageId(String messageId, Object contactType) {
		return mongoTemplate.findById(messageId, MessageDoc.class, getCollectionName(contactType));
	}

	public MessageDoc findOneByMessageIdExt(String messageIdExt, String contactType) {
		Query query2 = new Query();
		query2.addCriteria(Criteria.where("messageIdExt").is(messageIdExt)).with(Sort.by(Direction.ASC, "timestamp"));
		MessageDoc messages = mongoTemplate.findOne(query2, MessageDoc.class, getCollectionName(contactType));
		return messages;
	}

	public List<MessageDoc> findAllByMessageIdExt(String messageIdExt, String contactType) {
		Query query2 = new Query();
		query2.addCriteria(Criteria.where("messageIdExt").is(messageIdExt)).with(Sort.by(Direction.ASC, "timestamp"));
		return mongoTemplate.find(query2, MessageDoc.class, getCollectionName(contactType));
	}

	MessageDoc findMessageDoc(IMessageLoggable inboxMessage) {
		if (!ArgUtil.is(inboxMessage)) {
			return null;
		}

		if (ArgUtil.is(messageContextStore.getMessageDoc())
				&& ArgUtil.is(messageContextStore.getMessageDoc().getMessageId(), inboxMessage.getMessageId())
				&& ArgUtil.is(messageContextStore.getMessageDoc().getContact())
				&& ArgUtil.is(messageContextStore.getMessageDoc().getContact().getContactType(),
						inboxMessage.contact().getContactType())) {
			return messageContextStore.getMessageDoc();
		}

		if (ArgUtil.is(inboxMessage.getMessageId())) {
			return mongoTemplate.findById(inboxMessage.getMessageId(), MessageDoc.class,
					getCollectionName(inboxMessage.contact().type()));
		} else if (ArgUtil.is(inboxMessage.getMessageIdExt())) {
			CommonMongoQueryBuilder builder = new CommonMongoQueryBuilder();
			builder.where("messageIdExt", inboxMessage.getMessageIdExt());
			return mongoTemplate.findOne(builder.getQuery(), MessageDoc.class,
					getCollectionName(inboxMessage.contact().type()));
		} else {
			return null;
		}
	}

//	public MessageDoc findMessageDoc(OutboxMessage outMessage) {
//		if (ArgUtil.is(outMessage.getMessageId())) {
//			return mongoTemplate.findById(outMessage.getMessageId(), MessageDoc.class,
//					getCollectionName(outMessage.contact().type()));
//		}
//		return null;
//	}

	public MessageDoc findOrCreateMessageDoc(InboxMessage inboxMessage) {
		MessageDoc doc = findMessageDoc(inboxMessage);
		if (!ArgUtil.is(doc)) {
			doc = createMessageDoc(inboxMessage);
			if (ArgUtil.is(doc) && ArgUtil.is(doc.getReplyIdExt()) && !ArgUtil.is(doc.getReplyId())) {
				MessageDoc replyTo = findOneByMessageIdExt(doc.getReplyIdExt(),
						inboxMessage.contact().getContactType());
				if (ArgUtil.is(replyTo)) {
					doc.referral().setSourceCategory(MESSAGE_SOURCE_CATEGARY.MESSAGE);
					doc.referral().setSourceType(MESSAGE_BOUND_TYPE.typeToName(replyTo.getType()));
					if (ArgUtil.is(replyTo.getMessageId())) {
						doc.setReplyId(replyTo.getMessageId());
						doc.replyTo().put("messageId", replyTo.getMessageId());
						doc.referral().setMessageId(replyTo.getMessageId());
					}
					if (ArgUtil.is(replyTo.getMessageIdExt())) {
						doc.setReplyIdExt(replyTo.getMessageIdExt());
						doc.replyTo().put("messageIdExt", replyTo.getMessageIdExt());
						doc.referral().setMessageIdExt(replyTo.getMessageIdExt());
					}
					if (ArgUtil.is(replyTo.getBulkSessionId())) {
						doc.replyTo().put("bulkSessionId", replyTo.getBulkSessionId());
						doc.referral().setBulkId(replyTo.getBulkSessionId());
					}
					if (ArgUtil.is(replyTo.getMessage())) {
						String msg = replyTo.getMessage();
						doc.referral().setBody(msg.length() > 120 ? msg.substring(0, 120) + "..." : msg);
					}
					
					if (ArgUtil.is(replyTo.getTemplate())) {
						doc.referral().setTemplateCode(replyTo.getTemplate());
					}

					if (ArgUtil.is(replyTo.getAttachments())) {
						Attachment att = replyTo.getAttachments().get(0);
						if (ArgUtil.is(att.getMediaType())) {
							doc.referral().setMediaType(att.getMediaType().toLowerCase());
						}
						doc.referral().setMediaUrl(att.getMediaURL());
						doc.referral().setTitle(att.getMediaCaption());
						doc.referral().setMediaName(att.getMediaName());
						doc.referral().setMediaCaption(att.getMediaCaption());
						doc.referral().setMediaType(att.getMediaType());
					}
				}

			}
		}
		return doc;
	}

	public MessageDoc findAndUpdateMessageDoc(InboxMessage inboxMessage) {
		MessageDoc doc = findOrCreateMessageDoc(inboxMessage);
		if (ArgUtil.is(doc.getMessageId())) {
			doc = updateMessageDoc(inboxMessage, doc);
		}
		saveMessage(doc, inboxMessage.contact().type());
		inboxMessage.setMessageId(doc.getMessageId());
		return doc;
	}

	public MessageDoc createOrUpdate(InboxMessage inboxMessage) {
		MessageDoc doc = findAndUpdateMessageDoc(inboxMessage);
		inboxMessage.setMessageId(doc.getMessageId());
		return doc;
	}

	public void setTemplate(InboxMessage inboxMessage, String template) {
		MessageDoc doc = findOrCreateMessageDoc(inboxMessage);
		doc.setTemplate(template);
		saveMessage(doc, inboxMessage.contact().type());
		inboxMessage.setMessageId(doc.getMessageId());
	}

	public void setTags(InboxMessage inboxMessage, TagDocument tags) {
		MessageDoc doc = findOrCreateMessageDoc(inboxMessage);
		doc.setTags(tags);
		saveMessage(doc, inboxMessage.contact().type());
		inboxMessage.setMessageId(doc.getMessageId());
	}

	public void setHandler(InboxMessage inboxMessage, String handler) {
		CommonMongoQueryBuilder builder = new CommonMongoQueryBuilder();
		if (ArgUtil.is(inboxMessage.getMessageId())) {
			builder.whereIdSafe(inboxMessage.getMessageId());
			builder.update().set("handler", handler);
			builder.update().set("meta.handler", handler);
			mongoTemplate.updateFirst(builder.getQuery(), builder.getUpdate(), MessageDoc.class,
					MessageStore.getCollectionName(inboxMessage.contact().type()));
		} else {
			MessageDoc doc = findOrCreateMessageDoc(inboxMessage);
			doc.setHandler(handler);
			saveMessage(doc, inboxMessage.contact().type());
			inboxMessage.setMessageId(doc.getMessageId());
		}
	}

	// Out Going Messages
	private MessageDoc updateMessageDoc(OutboxMessage outMessage, MessageDoc doc) {

		doc.setRoute(outMessage.getRoute());
		doc.setQueue(ArgUtil.nonEmpty(outMessage.route().getQueueCode(), outMessage.session().getQueue()));
		doc.setAgent(ArgUtil.nonEmpty(outMessage.route().getSenderCode(), outMessage.session().getAgent()));
		// if (ArgUtil.is(outMessage.getTemplate())) {
		doc.setTemplate(outMessage.templateCode());
		doc.setTemplateId(outMessage.templateId());
		doc.setHsm(outMessage.getHsm());
		doc.setModel(outMessage.getModel());
		// } else {
		doc.setSubject(outMessage.getSubject());
		doc.setMessage(outMessage.getMessage());
		// }
		doc.setAttachments(outMessage.getAttachments());
		doc.setVccards(outMessage.getVccards());

		doc.setSessionId(outMessage.getSessionId());
		doc.setMessageIdRef(outMessage.getMessageIdRef());
		doc.setMessageIdResend(outMessage.getMessageIdResend());

		doc.setTrace(outMessage.getTrace());
		doc.setLogs(outMessage.getLogs());
		doc.setMessageIdExt(outMessage.getMessageIdExt());
		doc.setReplyTo(outMessage.getReplyTo());
		doc.setStatus(ArgUtil.parseAsString(outMessage.getStatus()));
		doc.setDump(outMessage.getDump());
		doc.origin().from(outMessage.origin());

		doc.stamps().putAll(outMessage.stamps());
		doc.stamps().put("session", ArgUtil.parseAsLong(outMessage.session().getSessionStamp(), 0L));
		doc.meta().putAll(outMessage.meta());
		doc.options().putAll(outMessage.options());
		doc.setTimeout(outMessage.getTimeout());
		if (ArgUtil.is(outMessage.getTimer())) {
			doc.setTimer(outMessage.getTimer());
		}

		// Incase it was missed
		doc.setContactId(PostManUtil.createContactId(outMessage));
		doc.setBulkSessionId(outMessage.getBulkSessionId());

		if (ArgUtil.is(outMessage.getMessageIdRef())) {
			MessageReferral ref = setMessageReferal(outMessage, doc);
			if (ArgUtil.is(outMessage.getReferral())) {
				MessageReferral crossRef = outMessage.getReferral();
				ref.setChannelId(crossRef.getChannelId());
				ref.setSessionId(crossRef.getSessionId());
				ref.setMessageId(ArgUtil.nonEmpty(crossRef.getMessageId(), ref.getMessageId()));
			}
			doc.setReferral(ref);
		} else if (ArgUtil.is(outMessage.getReferral())) {
			// Preserve explicit referral pointers for cross-channel linkage.
			doc.setReferral(outMessage.getReferral());
		}

		return doc;
	}

	public MessageDoc createMessageDoc(OutboxMessage outMessage) {
		ContactType contactType = outMessage.contact().type();
		MessageDoc doc = MessageDoc.instance(contactType);
		doc.setTraceId(AppContextUtil.getTraceId());

		if (!ArgUtil.is(doc.getMessageId()) && ArgUtil.is(outMessage.getMessageId())) {
			doc.setMessageId(outMessage.getMessageId());
		}

		doc.setMessageIdResend(outMessage.getMessageIdResend());

		if (ArgUtil.is(outMessage.getAction())) {
			doc.setType(ArgUtil.nonEmpty(outMessage.getType(), "A"));
			doc.setAction(outMessage.getAction());
		} else {
			doc.setType(ArgUtil.nonEmpty(outMessage.getType(), "O"));
		}
		doc.setFormatType(outMessage.getFormatType());
		doc.setFormatSubType(outMessage.getFormatSubType());

		doc.setTimestamp(System.currentTimeMillis());
		doc.setTime(TimeStampIndex.now());

		String to = CollectionUtil.getOne(outMessage.getTo());
		// doc.setContactId(PostManUtil.createContactId(outMessage));

		ContactDetailDoc contact = new ContactDetailDoc();
		contact.copyFrom(outMessage.contact());
		contact.phone(to);
		contact.setContactType(ArgUtil.parseAsString(outMessage.contact().getContactType()));
		doc.setContact(contact);

		if (ArgUtil.is(outMessage.getBulkSessionId())) {
			doc.setBulkSessionId(outMessage.getBulkSessionId());
		}

		updateMessageDoc(outMessage, doc);

		return doc;
	}

	private MessageDoc findOrCreateMessageDoc(OutboxMessage outMessage) {
		MessageDoc doc = findMessageDoc(outMessage);
		if (!ArgUtil.is(doc)) {
			if (ArgUtil.is(outMessage.getMessageIdPre())) {
				outMessage.setMessageId(outMessage.getMessageIdPre());
			}
			doc = createMessageDoc(outMessage);
		}
		return doc;
	}

	public MessageDoc findAndUpdateMessageDoc(OutboxMessage outMessage) {
		MessageDoc doc = findOrCreateMessageDoc(outMessage);
		if (ArgUtil.is(doc.getMessageId())) {
			doc = updateMessageDoc(outMessage, doc);
		}
		saveMessage(doc, outMessage.contact().type());
		outMessage.setMessageId(doc.getMessageId());
		return doc;
	}

	public MessageDoc createOrUpdate(OutboxMessage outMessage) {
		MessageDoc doc = findAndUpdateMessageDoc(outMessage);
		outMessage.setMessageId(doc.getMessageId());
		return doc;
	}

	public MessageDoc note(OutboxMessage outboxMessage, String agent) {
		MessageDoc doc = createMessageDoc(outboxMessage);
		doc.setType("N");
		doc.setAgent(agent);
		saveMessage(doc, outboxMessage.contact().type());
		return doc;
	}

	public List<MessageDoc> findBySessionId(String sessionId, String contactType) {
		Query query2 = new Query();
		query2.addCriteria(Criteria.where("sessionId").is(sessionId)).with(Sort.by(Direction.ASC, "timestamp"));
		List<MessageDoc> messages = mongoTemplate.find(query2, MessageDoc.class, getCollectionName(contactType));
		return messages;
	}

	/**
	 * This method is optimized for big Queries, in case you are fetching more than
	 * 5K messages in single query
	 * 
	 * @param query
	 * @param contactType
	 * @return
	 */
	public List<MessageDoc> findAllNotPaged(Query query, ContactType contactType) {
		List<MessageDoc> messages = readOnly().collection(getCollectionName(contactType)).projection(query)
				.batchSize(10_000).asList(MessageDoc.class);
		return messages;
	}

	public List<MessageDoc> findByBulkSessionId(String bulkSessionId, ContactType contactType) {
		Query query2 = new Query();
		query2.addCriteria(Criteria.where("bulkSessionId").is(bulkSessionId))
				.with(Sort.by(Direction.ASC, "timestamp"));
		// Apply the Hint (use the name of your index)
		// query2.withHint("bulk_timestamp");
		List<MessageDoc> messages = findAllNotPaged(query2, contactType);
		// List<MessageDoc> messages = mongoTemplate.find(query2, MessageDoc.class,
		// getCollectionName(contactType));
		return messages;
	}

	public List<MessageDoc> findByBulkSessionIdPaged(String bulkSessionId, ContactType contactType, int pageNo,
			int pageSize, String sortBy, String sortDir) {

		MapModel extparams = MapModel.createInstance();

		if (ArgUtil.is(bulkSessionId)) {
			extparams.put("bulkSessionId", bulkSessionId);
		}

		PaginatedQuery<MessageDoc> query = commonMongoTemplate
				.getPages(PaginatedQuery.select(MessageDoc.class, getCollectionName(contactType)).pageNo(pageNo)
						.pageSize(pageSize).sortBy(sortBy).sortDir(sortDir).extraParams(extparams).count());

//		PaginatedQuery<MessageDoc> query = commonMongoTemplate
//				.getPages(PaginatedQuery.select(MessageDoc.class, getCollectionName(contactType))//
//						.where("bulkSessionId", bulkSessionId) //
//						.sort(Direction.ASC, "timestamp").count());

		return query.getResults();
	}

	public List<MessageDoc> findByBulkSessionIdPaged(String bulkSessionId, ContactType contactType, int pageNo,
			int pageSize, String sortBy, String sortDir, String status) {

		MapModel extparams = MapModel.createInstance();

		if (ArgUtil.is(bulkSessionId)) {
			extparams.put("bulkSessionId", bulkSessionId);
		}
		if (ArgUtil.is(status)) {
			extparams.put("status", status);
		}
		PaginatedQuery<MessageDoc> query = commonMongoTemplate
				.getPages(PaginatedQuery.select(MessageDoc.class, getCollectionName(contactType)).pageNo(pageNo)
						.pageSize(pageSize).sortBy(sortBy).sortDir(sortDir).extraParams(extparams));
		return query.getResults();
	}

	public List<MessageDoc> findByBulkSessionIdWithRplyCount(String bulkSessionId, ContactType contactType) {

//		Query query2 = new Query();
//		query2.addCriteria(Criteria.where("type").in('I', 'R'));
//		query2.addCriteria(new Criteria().orOperator(Criteria.where("replyTo.bulkSessionId").is(bulkSessionId),
//				Criteria.where("referral.bulkId").is(bulkSessionId)));
//		query2.with(Sort.by(Direction.ASC, "timestamp"));
//		List<MessageDoc> messages1 = mongoTemplate.find(query2, MessageDoc.class, getCollectionName(contactType));
//		System.out.println("OLD messages1 count :"+messages1.size());

		Map<String, MessageDoc> uniqueMap = new LinkedHashMap<>();

		Query q1 = new Query();
		q1.addCriteria(Criteria.where("type").in("I", "R"));
		q1.addCriteria(Criteria.where("replyTo.bulkSessionId").is(bulkSessionId));
		q1.with(Sort.by(Direction.ASC, "timestamp"));
		// q1.withHint("type_replyBulk_ts");

		Query q2 = new Query();
		q2.addCriteria(Criteria.where("type").in("I", "R"));
		q2.addCriteria(Criteria.where("referral.bulkId").is(bulkSessionId));
		q2.with(Sort.by(Direction.ASC, "timestamp"));
		// q2.withHint("type_replyBulk_ts");

		readOnly().find(q1, MessageDoc.class, getCollectionName(contactType))
				.forEach(m -> uniqueMap.put(m.getMessageId(), m));

		readOnly().find(q2, MessageDoc.class, getCollectionName(contactType))
				.forEach(m -> uniqueMap.put(m.getMessageId(), m));

		List<MessageDoc> messages = new ArrayList<>(uniqueMap.values());

		return messages;

	}

	public void fixStatus(ContactType contactType, MessageDoc messageDoc, String reason) {
		if (ArgUtil.not(messageDoc)) {
			return;
		}
//		SCHLD, QUED, CRTD, CRTD_EXC, INIT, DRFTD, SEND, SENT, SENT_ERR, SENT_EXC, SENTX, SENTX_ERR, DLVRD, READ, CLKD,
//		RPLD, NSENT, BLCKD, BNCD, LIMIT, FAILD, DELTD, CCWIN,
		for (Status pendingStatus : Status.FINAL_SNED_ORDER) {
			if (messageDoc.stamps().containsKey(pendingStatus.name())) {
				updateStatus(contactType, messageDoc, pendingStatus, reason);
				return;
			}
		}

	}

	public void updateStatus(ContactType contactType, MessageDoc messageDoc, Status status, String reason) {
		CommonMongoQueryBuilder builder = new CommonMongoQueryBuilder();

		if (ArgUtil.is(messageDoc.getMessageId())) {
			builder.whereIdSafe(messageDoc.getMessageId());
		} else if (ArgUtil.is(messageDoc.getMessageIdExt())) {
			builder.where("messageIdExt", messageDoc.getMessageIdExt());
		} else if (ArgUtil.is(messageDoc.getMessageIdRef())) {
			builder.where("messageIdRef", messageDoc.getMessageIdRef());
		} else {
			return;
		}

		builder.set("status", status);
		builder.set("stamps." + status.toString(), System.currentTimeMillis());
		if (ArgUtil.is(reason)) {
			builder.update().push("logs", reason);
		}
		updateFirst(builder.getQuery(), builder.getUpdate(), MessageDoc.class, getCollectionName(contactType));
	}

	public long updateStatusById(ContactType contactType, List<String> messagesId, Status status, String reason) {
		if (messagesId.isEmpty()) {
			return 0;
		}
		List<Object> mongoIds = new ArrayList<>(messagesId.size() * 2);
		for (String id : messagesId) {
			mongoIds.add(id);
			if (ObjectId.isValid(id)) {
				mongoIds.add(new ObjectId(id));
			}
		}

		MQB<MessageDoc> builder = MQB.select(MessageDoc.class, getCollectionName(contactType));
		builder.set("status", status);
		builder.set("stamps." + status.toString(), System.currentTimeMillis());
		if (ArgUtil.is(reason)) {
			builder.update().push("logs", reason);
		}
		builder.where("_id").in(mongoIds);
		return updateMulti(builder).getModifiedCount();
	}

	private QueryMode buildMessageQuery(MessageReport messageReport, CommonMongoQueryBuilder builder) {
		LOGGER.debug("updateStatus {} {} {}", messageReport.getMessageId(), messageReport.contact().getChannelType(),
				messageReport.getStatus(), messageReport.getSessionId());
		QueryMode queryMode = QueryMode.SINGLE;
		if (ArgUtil.is(messageReport.getMessageId())) {
			builder.whereIdSafe(messageReport.getMessageId());
		} else if (ArgUtil.is(messageReport.getMessageIdExt())) {
			builder.where("messageIdExt", messageReport.getMessageIdExt());
		} else if (ArgUtil.is(messageReport.getMessageIdRef())) {
			builder.where("messageIdRef", messageReport.getMessageIdRef());
		} else if (ArgUtil.is(messageReport.contact().getCsid()) && messageReport.getWatermarkStamp() > 0L) {
			String contactId = PostManUtil.CONTACT_ID(messageReport.contact());
			builder.where(
					// Main Condition
					Criteria.where("contactId").is(contactId).and("stamps." + messageReport.getStatus().toString())
							.exists(false).andOperator(
									// Range
									Criteria.where("timestamp").lt(messageReport.getWatermarkStamp()),
									Criteria.where("timestamp").gt(TimeUtils.beforeTimeMillis("24hr"))));
			queryMode = QueryMode.MULTI;
		} else {
			queryMode = QueryMode.NONE;

		}
		return queryMode;
	}

	public MessageDoc updateStatus(MessageReport messageReport) {

		CommonMongoQueryBuilder builder = new CommonMongoQueryBuilder();
		MessageDoc m = null;
		QueryMode queryMode = buildMessageQuery(messageReport, builder);
		if (queryMode == QueryMode.NONE) {
			return m;
		}

		String collectionName = getCollectionName(messageReport.contact().getContactType());

		if (ArgUtil.is(messageReport.getStatus())) {
			builder.set("status", messageReport.getStatus());
			builder.set("stamps." + messageReport.getStatus().toString(), messageReport.getChangeStamp());

			if (ArgUtil.is(messageReport.getReason())) {
				builder.update().push("logs", messageReport.getReason());
			}

			if (ArgUtil.is(messageReport.getStatus()) && messageReport.getStatus() == Status.DELTD) {
				builder.set("message", null);
			}

			UpdateResult result;

			if (queryMode == QueryMode.MULTI) {
				result = mongoTemplate.updateMulti(builder.getQuery(), builder.getUpdate(), MessageDoc.class,
						collectionName);
			} else {
				result = mongoTemplate.updateFirst(builder.getQuery(), builder.getUpdate(), MessageDoc.class,
						collectionName);
			}

			if (result.getModifiedCount() > 1) {
				builder.sortBy("timestamp", Direction.ASC).limit(result.getModifiedCount());
				List<MessageDoc> messsages = mongoTemplate.find(builder.getQuery(), MessageDoc.class, collectionName);
				if (ArgUtil.is(messsages)) {
					m = messsages.get(messsages.size() - 1);
					if (ArgUtil.is(m)) {
						updateMessageReport(messageReport, m);
					}
				}
			} else {
				m = mongoTemplate.findOne(builder.getQuery(), MessageDoc.class, collectionName);
				if (ArgUtil.is(m)) {
					updateMessageReport(messageReport, m);
					/** added for session update **/
					updateSessionExpiryStamp(messageReport, m);
					updateTpWaba(messageReport, m);
				}

			}
			// LOGGER.info(JsonUtil.toJson(builder));
			/** update session expiry timestamp **/
		}
		return m;
	}

	public MessageDoc updateStatusFast(MessageReport messageReport) {

		CommonMongoQueryBuilder builder = new CommonMongoQueryBuilder();
		MessageDoc m = null;
		QueryMode queryMode = buildMessageQuery(messageReport, builder);
		if (queryMode == QueryMode.NONE) {
			return m;
		}

		String collectionName = getCollectionName(messageReport.contact().getContactType());
		Status toStatus = messageReport.getStatus();;

		if (queryMode == QueryMode.MULTI) {
			builder.sortBy("timestamp", Direction.ASC).limit(50);
			List<MessageDoc> messsages = mongoTemplate.find(builder.getQuery(), MessageDoc.class, collectionName);
			if (ArgUtil.is(messsages)) {
				m = messsages.get(messsages.size() - 1);
			}
		} else {
			m = mongoTemplate.findOne(builder.getQuery(), MessageDoc.class, collectionName);
		}

		if (ArgUtil.not(m)) {
			return m;
		}
		if (!Status.validUpdate(m.getStatus(), messageReport.getStatus())) {
			toStatus = null;
		}

		if (ArgUtil.is(messageReport.getStatus())) {
			if (ArgUtil.is(toStatus)) {
				m.setStatus(messageReport.getStatus().name());
				builder.set("status", messageReport.getStatus());
			}

			m.stamps().put(messageReport.getStatus().toString(), messageReport.getChangeStamp());
			builder.set("stamps." + messageReport.getStatus().toString(), messageReport.getChangeStamp());

			if (ArgUtil.is(messageReport.getReason())) {
				m.logs().add(messageReport.getReason());
				builder.update().push("logs", messageReport.getReason());
			}

			if (ArgUtil.is(messageReport.getStatus()) && messageReport.getStatus() == Status.DELTD) {
				m.setMessage(null);
				builder.set("message", null);
			}

			UpdateResult result;

			if (queryMode == QueryMode.MULTI) {
				result = mongoTemplate.updateMulti(builder.getQuery(), builder.getUpdate(), MessageDoc.class,
						collectionName);
			} else {
				result = mongoTemplate.updateFirst(builder.getQuery(), builder.getUpdate(), MessageDoc.class,
						collectionName);
			}

			if (result.getModifiedCount() > 1) {
				updateMessageReport(messageReport, m);
			} else {
				updateMessageReport(messageReport, m);
				/** added for session update **/
				updateSessionExpiryStamp(messageReport, m);
				updateTpWaba(messageReport, m);
			}
		}
		return m;
	}

	private void updateMessageReport(MessageReport messageReport, MessageDoc m) {
		messageReport.from(m);
		messageReport.setSessionId(m.getSessionId());
		messageReport.setSessionBulkId(m.getBulkSessionId());
		messageReport.session().setQueue(m.getQueue());
		messageReport.setReferral(m.getReferral());

		// UPdate Template Details
		messageReport.setType(m.getType());
		if (ArgUtil.is(m.getHsm())) {
			messageReport.setTemplateId(m.getHsm().getId());
			messageReport.setTemplateCode(m.getHsm().getCode());
		}
	}

	public void insert(List<MessageDoc> messages, ContactType contactType) {
		/**
		 * for (MessageDoc messageDoc : messages) { mongoTemplate.save(messageDoc,
		 * MessageStore.getCollectionName(contactType)); //System.out.println("phone:
		 * "+messageDoc.getContact().getPhone()); } return;
		 **/
		int n = 500;
		// Calculate the total number of partitions of size `n` each
		int m = messages.size() / n;
		if (messages.size() % n != 0) {
			m++;
		}
		// partition the list into sublists of size `n` each
		List<List<MessageDoc>> itr = Lists.partition(messages, n);
		for (int i = 0; i < m; i++) {
			mongoTemplate.insert(itr.get(i), MessageStore.getCollectionName(contactType));
		}

	}

	public List<MessageDoc> find(Query query, ContactType contactType) {
		return mongoTemplate.find(query, MessageDoc.class, MessageStore.getCollectionName(contactType));
	}

	public MessageDoc findById(String id, ContactType contactType) {
		return mongoTemplate.findById(id, MessageDoc.class, MessageStore.getCollectionName(contactType));
	}

	public MessageDoc saveMessage(MessageDoc msg, ContactType contactType) {
		if (!ArgUtil.is(msg.getAppType())) {
			msg.setAppType(appConfig.getAppType());
		}
		if (!ArgUtil.is(msg.getAppVenv())) {
			msg.setAppVenv(appConfig.getAppVenv());
		}
		mongoTemplate.save(msg, MessageStore.getCollectionName(contactType));
		return msg;
	}

	public MessageDocAbstract save(MessageDocAbstract msg) {
		if (!ArgUtil.is(msg.getAppType())) {
			msg.setAppType(appConfig.getAppType());
		}
		if (!ArgUtil.is(msg.getAppVenv())) {
			msg.setAppVenv(appConfig.getAppVenv());
		}

		if (msg.getTime() == null) {
			msg.setTime(TimeStampIndex.now());
		}

		mongoTemplate.save(msg);
		return msg;
	}

	public void reject(InboxMessage inboxMessageOriginal, Throwable e) {
		String contactId = PostManUtil.CONTACT_ID(inboxMessageOriginal.contact());
		MessageHoldRejected rejectedMessage = new MessageHoldRejected();
		rejectedMessage.setInboxMessage(inboxMessageOriginal);
		rejectedMessage.setContactId(contactId);
		rejectedMessage.setTimestamp(System.currentTimeMillis());
		rejectedMessage.setAppType(appConfig.getAppType());
		rejectedMessage.setAppVenv(appConfig.getAppVenv());

		rejectedMessage.setReason(e.getMessage());
		StackTraceElement[] traces = e.getStackTrace();
		if (traces.length > 0 && traces[0].toString().length() > 0) {
			for (StackTraceElement trace : traces) {
				rejectedMessage.logs().add(trace.toString());
			}
		}

		if (e instanceof ApiHttpServerException || e instanceof ApiHttpException) {
			rejectedMessage.setHttpResp(MapModel.from(((ApiHttpException) e).getResponse().getBody()).toMap());
		}

		mongoTemplate.save(rejectedMessage, MessageHold.COLLECTION_REJECTED);
	}

	public void reject(InboxMessage inboxMessageOriginal, String reason, String... details) {
		String contactId = PostManUtil.CONTACT_ID(inboxMessageOriginal.contact());
		MessageHoldRejected rejectedMessage = new MessageHoldRejected();
		rejectedMessage.setInboxMessage(inboxMessageOriginal);
		rejectedMessage.setContactId(contactId);
		rejectedMessage.setTimestamp(System.currentTimeMillis());
		rejectedMessage.setAppType(appConfig.getAppType());
		rejectedMessage.setAppVenv(appConfig.getAppVenv());

		rejectedMessage.setReason(reason);
		for (String detail : details) {
			rejectedMessage.logs().add(detail);
		}

		mongoTemplate.save(rejectedMessage, MessageHold.COLLECTION_REJECTED);
	}

	public void original(InboxMessage inboxMessageOriginal) {
		String contactId = PostManUtil.CONTACT_ID(inboxMessageOriginal.contact());
		MessageHold hold = new MessageHoldOriginal();
		hold.setInboxMessage(inboxMessageOriginal);
		hold.setContactId(contactId);
		hold.setTimestamp(System.currentTimeMillis());
		hold.setAppType(appConfig.getAppType());
		hold.setAppVenv(appConfig.getAppVenv());
		commonMongoTemplate.save(hold, MessageHold.COLLECTION_ORIGINAL);
	}

	public void hold(InboxMessage inboxMessageOriginal) {
		String contactId = PostManUtil.CONTACT_ID(inboxMessageOriginal.contact());
		MessageHold hold = new MessageHold();
		hold.setInboxMessage(inboxMessageOriginal);
		hold.setContactId(contactId);
		hold.setTimestamp(System.currentTimeMillis());
		hold.setAppType(appConfig.getAppType());
		hold.setAppVenv(appConfig.getAppVenv());
		mongoTemplate.save(hold);
	}

	public List<InboxMessage> releaseBySession(InboxMessage inboxMessageOriginal) {
		String contactId = PostManUtil.CONTACT_ID(inboxMessageOriginal.contact());
		CommonMongoQueryBuilder builder = new CommonMongoQueryBuilder();
		builder.where(Criteria.where("contactId").is(contactId).and("appType").is(appConfig.getAppType()).and("appVenv")
				.is(appConfig.getAppVenv()));
		builder.set("sessionId", inboxMessageOriginal.getSessionId());
		mongoTemplate.updateMulti(builder.getQuery(), builder.getUpdate(), MessageHold.class);

		CommonMongoQueryBuilder builder2 = new CommonMongoQueryBuilder();
		builder2.where(Criteria.where("contactId").is(contactId).and("sessionId")
				.is(inboxMessageOriginal.getSessionId()).and("appType").is(appConfig.getAppType())).sortBy("timestamp");
		List<MessageHold> docs = mongoTemplate.findAllAndRemove(builder2.getQuery(), MessageHold.class);
		List<InboxMessage> x = docs.stream().map(d -> d.getInboxMessage()).collect(Collectors.toList());
		return x;
	}

	public void updateSessionExpiryStamp(MessageReport report, MessageDoc m) {
		if (ArgUtil.is(m.getSessionId())) {
			if (ArgUtil.is(m.getSessionId()) && ArgUtil.is(report.getTpMeta())) {
				ChatSessionQuery chatSessionQuery = new ChatSessionQuery(m.getSessionId());

				Map<String, Object> tpMeta = report.getTpMeta();
				Long ccwExpiryLong = tpMeta.get("ccwExpiry") == null ? 0L
						: Long.parseLong(tpMeta.get("ccwExpiry").toString());
				ccwExpiryLong = ccwExpiryLong * 1000;
				/**
				 * @deperecated
				 */
				// sessionDoc.setSessionExpiryStamp(ccwExpiryLong);
				chatSessionQuery.set("sessionExpiryStamp", ccwExpiryLong);

				// in Millis
				tpMeta.put("ccwExpiryMillis", ccwExpiryLong);
				tpMeta.put("sessionExpiryStamp", ccwExpiryLong);
				chatSessionQuery.setTpMeta(tpMeta);
				commonMongoTemplate.updateFirst(chatSessionQuery);
			}
		}
	}

	public void updateTpWaba(MessageReport report, MessageDoc m) {
		LOGGER.debug("getSessionId { 1 } :" + m.getSessionId());
		if (ArgUtil.is(m.getSessionId()) && ArgUtil.is(report.getTpMeta())) {
			String sessionId = m.getSessionId();
			String tpWabaId = report.getTpMeta().get("wabaConvesationId").toString();
			LOGGER.debug("tpWabaId { 2  }:" + tpWabaId);
			if (ArgUtil.is(tpWabaId)) {
				WABAConversation wabaDoc = commonMongoTemplate.findByIdSafeCheck(tpWabaId, WABAConversation.class);
				if (ArgUtil.is(wabaDoc)) {
					WABAConversationQuery builder = new WABAConversationQuery(tpWabaId);
					builder.set("chatSession.chatSessionId", sessionId);
					commonMongoTemplate.updateFirst(builder);
				}

			}

		}

	}

	public MessageReferral setMessageReferal(OutboxMessage outMessage, MessageDoc doc) {
		MessageReferral ref = new MessageReferral();
		CommonMongoQueryBuilder builder = new CommonMongoQueryBuilder();
		builder.where("messageIdExt", outMessage.getMessageIdRef());
		String collectionName = getCollectionName(outMessage.contact().getContactType());

		MessageDoc messsage = mongoTemplate.findOne(builder.getQuery(), MessageDoc.class, collectionName);
		if (messsage != null) {
			ref.setMessageId(messsage.getMessageId());
			ref.setMessageIdExt(messsage.getMessageIdExt());
			ref.setSourceType(PMConstants.MESSAGE_BOUND_TYPE.typeToName(messsage.getType()));

			if (ArgUtil.is(messsage.getAttachments())) {
				Attachment att = messsage.getAttachments().get(0);
				if (ArgUtil.is(att.getMediaType())) {
					ref.setMediaType(att.getMediaType().toLowerCase());
				}
				ref.setMediaUrl(att.getMediaURL());
				ref.setTitle(att.getMediaCaption());
				ref.setMediaName(att.getMediaName());
				ref.setMediaCaption(att.getMediaCaption());

			} else {
				ref.setMediaType(ArgUtil.parseAsString(messsage.getFormatType(), PMConstants.MESSAGE_FORMAT_TYPE.TEXT));
			}

			if (ArgUtil.is(messsage.getMessage())) {
				String msg = messsage.getMessage();
				ref.setBody(msg.length() > 120 ? msg.substring(0, 120) + "..." : msg);
			}

			ref.setBulkId(outMessage.getBulkSessionId());
		}

		return ref;
	}

	public Map<String, Long> getCountsPerStatusByBulkSession(String bulkSessionId, ContactType contactType,
			String status) {

		Criteria c = Criteria.where("bulkSessionId").is(bulkSessionId);
		if (ArgUtil.is(status)) {
			c.and("status").is(status);
		}
		QA list = new QA().add(Aggregation.match(c), Aggregation.group("status").count().as("count"));

		// System.out.println(JsonUtil.toJson(list.piplines()));

		MongoCursor<Document> cursor = readOnly().collection(MessageStore.getCollectionName(contactType))
				.aggregate(list).iterator();

		Map<String, Long> counts = new HashMap<String, Long>();
		while (cursor.hasNext()) {
			Document object = cursor.next();
			if (ArgUtil.is(object)) {
				Status statuz = ArgUtil.parseAsEnumT(object.get("_id"), Status.class);
				if (ArgUtil.is(statuz)) {
					long count = ArgUtil.parseAsLong(object.get("count"), 0L);
					counts.put(ArgUtil.parseAsString(statuz), count);
				}
			}
		}
		return counts;
	}

	public Map<String, Long> getCountsPerStatusByBulkSession(String bulkSessionId, ContactType contactType) {
		return getCountsPerStatusByBulkSession(bulkSessionId, contactType, null);
	}

}
