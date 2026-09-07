package com.boot.jx.postman.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.URL;

import org.junit.Test;

import com.boot.jx.postman.PMConstants.CHAT_STATUS;
import com.boot.jx.postman.doc.ChatContactDoc;
import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.doc.MessageDoc;
import com.boot.jx.postman.dto.ChatMessageDTO;
import com.boot.jx.postman.dto.ChatSessionDTO;
import com.boot.jx.postman.dto.ContactDTO;
import com.boot.jx.postman.model.ContactMeta;
import com.boot.utils.FileUtil;
import com.boot.utils.JsonUtil;

/**
 * Pins DTO mapping from Mongo documents — critical for API contract stability.
 */
public class ChatDTOUtilTest {

	@Test
	public void getContactDTO_mapsAllIdentityFields() {
		ChatContactDoc contact = new ChatContactDoc();
		contact.setContactId("wa919930104050_918828218374");
		contact.setContactType("WHATSAPP");
		contact.setChannelType("wac360");
		contact.setLane("918828218374");
		contact.setName("Test User");
		contact.setEmail("test@example.com");
		contact.setCsid("919930104050");
		contact.setSessionId("sess123");
		contact.setLastInBoundStamp(1000L);
		contact.setLastOutBoundStamp(2000L);

		ContactDTO dto = ChatDTOUtil.getContactDTO(contact);

		assertEquals("wa919930104050_918828218374", dto.getContactId());
		assertEquals("WHATSAPP", dto.getContactType());
		assertEquals("wac360", dto.getChannelType());
		assertEquals("918828218374", dto.getLane());
		assertEquals("Test User", dto.getName());
		assertEquals("test@example.com", dto.getEmail());
		assertEquals("sess123", dto.getSessionId());
		assertEquals(1000L, dto.getLastInBoundStamp());
		assertEquals(2000L, dto.getLastOutBoundStamp());
	}

	@Test
	public void getContactDTO_nullDoc_returnsEmptyDto() {
		ContactDTO dto = ChatDTOUtil.getContactDTO((ChatContactDoc) null);
		assertNotNull(dto);
		assertNull(dto.getContactId());
	}

	@Test
	public void getContactMeta_mapsNamePhoneEmail() {
		ChatContactDoc contact = new ChatContactDoc();
		contact.setName("Alice");
		contact.setEmail("alice@test.com");
		contact.setContactType("EMAIL");

		ContactMeta meta = ChatDTOUtil.getContactMeta(contact);
		assertEquals("Alice", meta.getName());
		assertEquals("alice@test.com", meta.getEmail());
		assertEquals("EMAIL", meta.getContactType());
	}

	@Test
	public void getChatSessionDTO_fromSampleFixture() {
		URL url = FileUtil.getResource("sample/chat_session_doc.json", ChatDTOUtilTest.class);
		ChatSessionDoc session = JsonUtil.parse(FileUtil.read(url), ChatSessionDoc.class);

		ChatSessionDTO dto = ChatDTOUtil.getChatSessionDTO(session);

		assertEquals("66ea5d4d8bde5b00010134d1", dto.getSessionId());
		assertEquals("lt", dto.getAssignedToAgent());
		assertEquals("TechSupport", dto.getAssignedToDept());
		assertEquals("Kiran", dto.getName());
		assertEquals("OPEN", dto.getStatus());
		assertEquals(true, dto.isActive());
		assertNotNull(dto.msg());
	}

	@Test
	public void getChatSessionDTO_derivesStatusWhenMissing() {
		ChatSessionDoc session = new ChatSessionDoc();
		session.setActive(false);
		session.setExpired(false);
		session.setResolved(false);

		ChatSessionDTO dto = ChatDTOUtil.getChatSessionDTO(session);
		assertEquals(CHAT_STATUS.CLOSED.toString(), dto.getStatus());
	}

	@Test
	public void latestMessage_picksNewerTimestamp() {
		MessageDoc older = new MessageDoc();
		older.setTimestamp(100L);
		MessageDoc newer = new MessageDoc();
		newer.setTimestamp(200L);

		assertEquals(newer, ChatDTOUtil.latestMessage(older, newer));
		assertEquals(newer, ChatDTOUtil.latestMessage(newer, older));
		assertEquals(newer, ChatDTOUtil.latestMessage(null, newer));
		assertEquals(older, ChatDTOUtil.latestMessage(older, null));
	}

	@Test
	public void getChatMessageDTO_nullDoc_returnsNull() {
		assertNull(ChatDTOUtil.getChatMessageDTO((MessageDoc) null));
	}

	@Test
	public void getChatMessageDTO_mapsMessageFields() {
		MessageDoc doc = new MessageDoc();
		doc.setMessageId("msg1");
		doc.setSessionId("sess1");
		doc.setMessage("Hello");
		doc.setTimestamp(5000L);
		doc.setType("O");
		doc.setAgent("agent1");

		ChatMessageDTO dto = ChatDTOUtil.getChatMessageDTO(doc);
		assertEquals("msg1", dto.getMessageId());
		assertEquals("sess1", dto.getSessionId());
		assertEquals("Hello", dto.getText());
		assertEquals(5000L, dto.getTimestamp());
		assertEquals("O", dto.getType());
	}

}
