package com.boot.jx.postman.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.bson.Document;
import org.junit.Test;

import com.boot.jx.postman.PMConstants.CHAT_STATUS;
import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.doc.MessageDoc;
import com.boot.jx.postman.dto.ChatMessageDTO;
import com.boot.jx.postman.model.TagDocument;

/**
 * Pins session update builder mutations used by {@code SessionStore} on every
 * inbound/outbound message.
 */
public class ChatSessionQueryTest {

	@SuppressWarnings("unchecked")
	private static Object setField(ChatSessionQuery query, String key) {
		Document update = query.getUpdate().getUpdateObject();
		if (update.containsKey("$set")) {
			return ((Document) update.get("$set")).get(key);
		}
		return update.get(key);
	}

	@Test
	public void newDoc_setsSessionId() {
		ChatSessionQuery query = new ChatSessionQuery("abc123");
		assertEquals("abc123", query.getId());
		assertEquals("abc123", query.getId(query.newDoc("abc123")));
	}

	@Test
	public void setActive_updatesDocAndMongoSet() {
		ChatSessionDoc doc = new ChatSessionDoc();
		doc.setActive(true);
		ChatSessionQuery query = new ChatSessionQuery(doc);

		query.setActive(false);

		assertFalse(doc.isActive());
		assertEquals(false, setField(query, "active"));
	}

	@Test
	public void setExpired_updatesDocAndMongoSet() {
		ChatSessionDoc doc = new ChatSessionDoc();
		ChatSessionQuery query = new ChatSessionQuery(doc);

		query.setExpired(true);

		assertTrue(doc.isExpired());
		assertEquals(true, setField(query, "expired"));
	}

	@Test
	public void setStatus_updatesDocAndMongoSet() {
		ChatSessionDoc doc = new ChatSessionDoc();
		ChatSessionQuery query = new ChatSessionQuery(doc);

		query.setStatus(CHAT_STATUS.OPEN);

		assertEquals("OPEN", doc.getStatus());
		assertEquals("OPEN", setField(query, "status"));
	}

	@Test
	public void putAndRemove_storeKey() {
		ChatSessionDoc doc = new ChatSessionDoc();
		ChatSessionQuery query = new ChatSessionQuery(doc);

		query.put("ticket", "T-99");
		assertEquals("T-99", doc.store().get("ticket"));
		assertEquals("T-99", setField(query, "store.ticket"));

		query.remove("ticket");
		assertNull(doc.store().get("ticket"));
	}

	@Test
	public void setLastMsg_inbound_setsLastInBoundAndLastMsg() {
		ChatSessionDoc doc = new ChatSessionDoc();
		ChatSessionQuery query = new ChatSessionQuery(doc);

		ChatMessageDTO msg = new ChatMessageDTO();
		msg.setType("I");
		msg.setMessageId("in1");
		query.setLastMsg(msg);

		assertEquals("in1", ((ChatMessageDTO) setField(query, "msg.lastInBoundMsg")).getMessageId());
		assertEquals("in1", ((ChatMessageDTO) setField(query, "msg.lastMsg")).getMessageId());
	}

	@Test
	public void setLastMsg_outbound_setsLastOutBoundAndLastMsg() {
		ChatSessionDoc doc = new ChatSessionDoc();
		ChatSessionQuery query = new ChatSessionQuery(doc);

		ChatMessageDTO msg = new ChatMessageDTO();
		msg.setType("O");
		msg.setMessageId("out1");
		query.setLastMsg(msg);

		assertEquals("out1", ((ChatMessageDTO) setField(query, "msg.lastOutBoundMsg")).getMessageId());
		assertEquals("out1", ((ChatMessageDTO) setField(query, "msg.lastMsg")).getMessageId());
	}

	@Test
	public void setLastInBoundMsg_fromMessageDoc() {
		ChatSessionDoc doc = new ChatSessionDoc();
		ChatSessionQuery query = new ChatSessionQuery(doc);

		MessageDoc messageDoc = new MessageDoc();
		messageDoc.setMessageId("md1");
		messageDoc.setMessage("Hi");
		messageDoc.setType("I");
		messageDoc.setTimestamp(100L);

		query.setLastInBoundMsg(messageDoc, "WHATSAPP");

		assertEquals("md1", setField(query, "lastInBoundMsgId"));
		ChatMessageDTO dto = (ChatMessageDTO) setField(query, "msg.lastInBoundMsg");
		assertEquals("md1", dto.getMessageId());
		assertEquals("Hi", dto.getText());
	}

	@Test
	public void setTags_updatesDocAndMongoSet() {
		ChatSessionDoc doc = new ChatSessionDoc();
		ChatSessionQuery query = new ChatSessionQuery(doc);

		TagDocument tags = new TagDocument();
		tags.addLang("hi", "en");
		query.setTags(tags);

		assertEquals(2, doc.getTags().langs().size());
		assertEquals(tags, setField(query, "tags"));
	}

}
