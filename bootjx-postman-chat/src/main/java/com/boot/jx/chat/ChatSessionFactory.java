package com.boot.jx.chat;

import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.boot.jx.postman.PMConstants.CHAT_MODE;
import com.boot.jx.postman.PMEnvironment.PMDomainConfig;
import com.boot.jx.postman.doc.ChatContactDoc;
import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.doc.MessageDoc;
import com.boot.jx.postman.model.MessageDefinitions.Contactable;
import com.boot.jx.postman.model.MessageDefinitions.IMessage;
import com.boot.jx.postman.model.MessageDefinitions.IMessageExtended;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.TagDocument;
import com.boot.jx.postman.query.ChatContactQuery;
import com.boot.jx.postman.query.ChatSessionQuery;
import com.boot.jx.postman.store.ContactStore;
import com.boot.jx.postman.store.MessageContext;
import com.boot.jx.postman.store.MessageContextStore;
import com.boot.jx.postman.store.MessageStore;
import com.boot.jx.postman.store.SessionStore;
import com.boot.jx.utils.PostManUtil;
import com.boot.utils.ArgUtil;
import com.boot.utils.TimeUtils;

@Component
public class ChatSessionFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(ChatSessionFactory.class);

	@Autowired
	private SessionStore sessionStore;

	@Autowired
	private MessageStore messageStore;

	@Autowired
	private PMDomainConfig pmDomainConfig;

	@Autowired
	private MessageContext messageContext;

	@Autowired
	private ChatUtility chatUtility;

	@Autowired
	private ContactStore contactStore;

	@Autowired
	private MessageContextStore messageContextStore;

	public ChatSessionDoc getChatSessionByContactId(String contactId, String ticketHash) {

		if (!ArgUtil.is(contactId)) {
			return null;
		}

		ChatSessionDoc chatSessionDoc = null;

		if (ArgUtil.is(ticketHash)) {
			chatSessionDoc = sessionStore.getSessionPrimeByTicketHash(contactId, ticketHash);

		} else {
			ChatContactDoc chatContactDoc = sessionStore.getContact(contactId);
			if (ArgUtil.is(chatContactDoc)) {
				String sessionId = chatContactDoc.getSessionId();
				if (ArgUtil.is(sessionId)) {
					chatSessionDoc = sessionStore.getSession(sessionId);
				}
			}
		}

		if (sessionStore.isSessionValid(chatSessionDoc)) {
			return chatSessionDoc;
		}

		return null;
	}

	public ChatSessionDoc getChatSession(String sessionId, String ticketHash) {

		if (!ArgUtil.is(sessionId)) {
			return null;
		}

		ChatSessionDoc chatSessionDoc = sessionStore.getSession(sessionId);

		if (sessionStore.isSessionValid(chatSessionDoc)) {
			return chatSessionDoc;
		}

		if (!ArgUtil.is(chatSessionDoc)) {
			return null;
		}

		return getChatSessionByContactId(chatSessionDoc.getContactId(), ticketHash);
	}

	public ChatSessionDoc getChatSession(IMessage sessionMessage) {

		sessionMessage.timer().log("ssn:0");

		String ticketHash = sessionMessage.session().getTicketHash();

		// SESSION FIND BY SESSION_ID
		ChatSessionDoc chatSessionDoc = getChatSession(sessionMessage.getSessionId(), ticketHash);

		if (ArgUtil.is(chatSessionDoc)) {
			return chatSessionDoc;
		}

		Contactable contact = PostManUtil.getContactMeta(sessionMessage.contact());

		if (!ArgUtil.is(contact.getContactId())) {
			// CONTACT CONNANOT BE FOUND
			if (ArgUtil.is(sessionMessage.getSessionId())) {
				chatSessionDoc = sessionStore.getSession(sessionMessage.getSessionId());
				if (ArgUtil.is(chatSessionDoc)) {
					contact.copyFrom(chatSessionDoc.contact());
					contact.setContactId(chatSessionDoc.getContactId());
				}
			}
			if (!ArgUtil.is(contact.getContactId())) {
				return null;
			}
		}

		sessionMessage.timer().log("ssn:1");
		// CONTACT FIND BY SESSION_ID
		ChatContactDoc chatContactDoc = sessionStore.getContact(contact.getContactId());

		// CONTACT CREATION
		boolean updated = false;
		if (ArgUtil.isEmpty(chatContactDoc)) {
			// System.out.println("CONTACT CREATION");
			ChatContactQuery chatContactQuery = new ChatContactQuery(contact.getContactId());
			chatContactQuery.update(contact);
			chatContactQuery.updateCreatedStamp();
			// chatContactDoc = sessionStore.save(chatContactQuery.getDoc());
			chatContactDoc = sessionStore.save(chatContactQuery);
			updated = true;
		} else if (!ArgUtil.is(chatContactDoc.getChannelType(), sessionMessage.contact().getChannelType())
				// Regular Sync
				|| TimeUtils.isExpired(chatContactDoc.getMasterSyncStamp(), ChatContactQuery.MASTER_SYNC_UPDATE)) {
			ChatContactQuery chatContactQuery = new ChatContactQuery(chatContactDoc);
			chatContactQuery.update(contact);
			updated = ArgUtil.is(chatContactQuery.getUpdate());
			sessionStore.updateFirst(chatContactQuery);
		}

		messageContextStore.setChatContactDoc(chatContactDoc);

		if (updated) {
			contactStore.saveMaster(chatContactDoc);
		}

		// SESSION FiND BY CONTACT_ID
		chatSessionDoc = getChatSessionByContactId(contact.getContactId(), ticketHash);

		if (ArgUtil.is(chatSessionDoc)) {
			return chatSessionDoc;
		}

		// Try with Reply Id
		if (ArgUtil.is(sessionMessage.getReplyIdExt()) && PostManUtil.IS_TRACK_BY_REPLY_ID(contact.getChannelType())) {
			MessageDoc prev = messageStore.findOneByMessageIdExt(sessionMessage.getReplyIdExt(),
					contact.getContactType());
			if (ArgUtil.is(prev) && ArgUtil.is(prev.getSessionId())) {
				// SESSION FIND BY SESSION_ID - Try Again
				chatSessionDoc = getChatSession(prev.getSessionId(), ticketHash);
				if (ArgUtil.is(chatSessionDoc)) {
					return chatSessionDoc;
				}
			}
		}

		sessionStore.inactiveAllPreviousSessions(contact.getContactId(), ticketHash);

		// SESSION CREATION
		// System.out.println("SESSION CREATION");
		chatSessionDoc = new ChatSessionDoc();
		chatSessionDoc.setContactId(contact.getContactId());
		chatSessionDoc.setContactType(sessionMessage.contact().getContactType());
		chatSessionDoc.setChannel(sessionMessage.contact().getChannelType());
		chatSessionDoc.setLane(sessionMessage.contact().getLane());
		chatSessionDoc
				.setMode(ArgUtil.nonEmpty(sessionMessage.session().getMode(), sessionMessage.route().getSendMode()));
		chatSessionDoc.setActive(true);
		chatSessionDoc.setPrimary(true);
		chatSessionDoc.contact().setName(chatContactDoc.getName());
		chatSessionDoc.contact().copyFrom(chatContactDoc);
		chatSessionDoc.setTicketHash(sessionMessage.session().getTicketHash());
		if (ArgUtil.is(sessionMessage.getOrder())) {
			chatSessionDoc.setOrder(sessionMessage.getOrder());
		}
		chatSessionDoc.setSubject(sessionMessage.getSubject());
		chatSessionDoc.setRoutingId(PostManUtil.ROUTING_ID("",
				ArgUtil.nonEmpty(sessionMessage.route().getQueueCode(), sessionMessage.session().getQueue())));
		sessionMessage.session().setFirstMessage(true);
		return sessionStore.saveSession(chatSessionDoc);
	}

	public ChatSessionDoc linkSession(ChatSessionDoc chatSessionDoc, IMessage inboxMessage) {
		inboxMessage.timer().log("lnkd:0");
		if (!ArgUtil.is(chatSessionDoc)) {
			return null;
		}

		if (PostManUtil.isInBound(inboxMessage)) {
			chatSessionDoc.setLastInComingStamp(inboxMessage.getTimestamp());
			// Query Update for Session
			ChatSessionQuery chatSessionDocQuery = new ChatSessionQuery(chatSessionDoc);
			chatSessionDocQuery.setLastInComingStamp(chatSessionDoc.getLastInComingStamp());

			if (ArgUtil.isEmptyValue(chatSessionDoc.getFirstInComingStamp())) {
				chatSessionDocQuery.setFirstInComingStamp(inboxMessage.getTimestamp());
			}

			// Assign Queue (skip for log-type calls - they don't affect session routing)
			if (!PostManUtil.isLogTypeCall(inboxMessage) && !chatUtility.inQueue(chatSessionDoc)) {
				String defaultQueue = inboxMessage.route().getQueueCode();
				if (ArgUtil.is(defaultQueue)) {
					chatSessionDocQuery.setQueue(defaultQueue);
					chatSessionDocQuery.setMode(inboxMessage.route().getSendMode());
				} else {
					defaultQueue = pmDomainConfig.getDefaultInboundQueue(inboxMessage.contact());
					chatSessionDocQuery.setQueue(defaultQueue);
				}
			}

			if (ArgUtil.is(inboxMessage.getOrder())) {
				chatSessionDocQuery.setOrder(inboxMessage.getOrder());
				chatSessionDoc.setOrder(inboxMessage.getOrder());
			}

			TagDocument messageTags = tagsFrom(inboxMessage);
			if (ArgUtil.is(messageTags)) {
				TagDocument sessionTags = chatSessionDoc.getTags();
				if (!ArgUtil.is(sessionTags)) {
					sessionTags = new TagDocument();
					chatSessionDoc.setTags(sessionTags);
				}
				sessionTags.merge(messageTags);
				chatSessionDocQuery.setTags(sessionTags);
			}

			sessionStore.updateFirst(chatSessionDocQuery);

			// Query Update for Contact
			ChatContactQuery chatContactQuery = new ChatContactQuery(chatSessionDoc.getContactId());
			chatContactQuery.setLastInBoundStamp(inboxMessage.getTimestamp());
			chatContactQuery.setSessionId(chatSessionDoc.getSessionId());
			chatContactQuery.update(inboxMessage.contact());
			sessionStore.updateFirst(chatContactQuery);
		} else if (PostManUtil.isOutBound(inboxMessage)) {
			ChatSessionQuery chatSessionDocQuery = new ChatSessionQuery(chatSessionDoc);

			// Assign Queue
			if (!chatUtility.inQueue(chatSessionDoc)) {

				String defaultQueue = inboxMessage.route().getQueueCode();
				if (ArgUtil.is(defaultQueue)) {
					chatSessionDocQuery.setQueue(defaultQueue);
					chatSessionDocQuery.setMode(inboxMessage.route().getSendMode());
				} else {
					defaultQueue = pmDomainConfig.getDefaultInboundQueue(inboxMessage.contact(),
							CHAT_MODE.from(inboxMessage.route().getSendMode()));
					chatSessionDocQuery.setQueue(defaultQueue);
				}
			}

			sessionStore.updateFirst(chatSessionDocQuery);

			// Query Update for Contact
			ChatContactDoc doc = contactStore.getContactById(chatSessionDoc.getContactId());
			ChatContactQuery chatContactQuery = null;
			if (doc == null) {
				chatContactQuery = new ChatContactQuery(chatSessionDoc.getContactId());
			} else {
				chatContactQuery = new ChatContactQuery(doc);
			}
			chatContactQuery.setSessionId(chatSessionDoc.getSessionId());
			sessionStore.updateFirst(chatContactQuery);
			contactStore.store(doc); // it will clear the store in case we dont have actual doc

		}
		sessionStore.updateMessageFromSession(chatSessionDoc, inboxMessage);
		inboxMessage.timer().log("lnkd:z");
		return chatSessionDoc;
	}

	private TagDocument tagsFrom(IMessage inboxMessage) {
		if (inboxMessage instanceof InboxMessage) {
			return ((InboxMessage) inboxMessage).getTags();
		}
		return null;
	}

	public ChatSessionDoc linkSession(IMessageExtended inboxMessage) {
		ChatSessionDoc session = getChatSession(inboxMessage);
		inboxMessage.timer().log("ssn");
		return linkSession(session, inboxMessage);
	}

	/**
	 * this is equivalent to linksession for outbound message
	 * 
	 * @param inboxMessage
	 * @return
	 */
	public ChatSessionDoc linkSessionSendNewOutBound(IMessage inboxMessage) {

		if (PostManUtil.IS_MULTI_THREAD(inboxMessage.contact().getChannelType())
				&& ArgUtil.isNotEmpty(inboxMessage.getSubject())) {
			String ticketHash = null;
			try {
				ticketHash = PostManUtil.createTicketHash(inboxMessage);
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			inboxMessage.session().setTicketHash(ticketHash);
		}
		ChatSessionDoc session = getChatSession(inboxMessage);
		return linkSession(session, inboxMessage);
	}

	public void push(MessageDoc msgDoc, IMessage iMessage) {
		if (PostManUtil.isInBound(msgDoc.getType()) || PostManUtil.isOutBound(msgDoc.getType())) {
			try {
				ChatSessionQuery chatSessionDocQuery = messageContext.session();
				long now = System.currentTimeMillis();

				if (PostManUtil.isInBound(msgDoc.getType())) {

					ChatSessionDoc session = chatSessionDocQuery.getDoc();
					if (!session.stamps().containsKey(ChatSessionDoc.FIRST_INBOUND_STAMP)) {
						chatSessionDocQuery.setStamp(ChatSessionDoc.FIRST_INBOUND_STAMP, now);
					}

					String FIRST_INBOUND_STAMP_MODE = ChatSessionDoc.FIRST_INBOUND_STAMP + "_" + session.getMode();
					if (!session.stamps().containsKey(FIRST_INBOUND_STAMP_MODE)) {
						chatSessionDocQuery.setStamp(FIRST_INBOUND_STAMP_MODE, now);
					}
					chatSessionDocQuery.setStamp(ChatSessionDoc.LAST_INBOUND_STAMP, now);
					chatSessionDocQuery.setStamp(ChatSessionDoc.LAST_INBOUND_STAMP + "_" + session.getMode(), now);

					chatSessionDocQuery.setLastInBoundMsg(msgDoc, iMessage.contact().getContactType());

				} else if (PostManUtil.isOutBound(msgDoc.getType())) {
					ChatSessionDoc session = chatSessionDocQuery.getDoc();
					if (!session.stamps().containsKey(ChatSessionDoc.FIRST_OUTBOUND_STAMP)) {
						chatSessionDocQuery.setStamp(ChatSessionDoc.FIRST_OUTBOUND_STAMP, now);
					}

					String FIRST_OUTBOUND_STAMP_MODE = ChatSessionDoc.FIRST_OUTBOUND_STAMP + "_" + session.getMode();
					if (!session.stamps().containsKey(FIRST_OUTBOUND_STAMP_MODE)) {
						chatSessionDocQuery.setStamp(FIRST_OUTBOUND_STAMP_MODE, now);
					}
					chatSessionDocQuery.setStamp(ChatSessionDoc.LAST_OUTBOUND_STAMP, now);
					chatSessionDocQuery.setStamp(ChatSessionDoc.LAST_OUTBOUND_STAMP + "_" + session.getMode(), now);

					chatSessionDocQuery.setLastOutBoundMsg(msgDoc, iMessage.contact().getContactType());
				}

				chatSessionDocQuery.setLastMsg(msgDoc, iMessage.contact().getContactType());
				sessionStore.updateFirst(chatSessionDocQuery);
			} catch (Exception e) {
				LOGGER.error("SessionStore.push", e);
			}
		}
	}

}
