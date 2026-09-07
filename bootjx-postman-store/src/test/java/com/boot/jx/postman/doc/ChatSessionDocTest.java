package com.boot.jx.postman.doc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.junit.Test;

import com.boot.utils.FileUtil;
import com.boot.utils.JsonUtil;

/**
 * Regression coverage for {@link ChatSessionDoc} JSON serde using production sample
 * fixture — catches Jackson/Mongo mapping breaks on upgrade.
 */
public class ChatSessionDocTest {

	@Test
	public void deserializeFromSampleFixture() {
		URL url = FileUtil.getResource("sample/chat_session_doc.json", ChatSessionDocTest.class);
		ChatSessionDoc session = JsonUtil.parse(FileUtil.read(url), ChatSessionDoc.class);

		assertNotNull(session);
		assertEquals("66ea5d4d8bde5b00010134d1", session.getSessionId());
		assertEquals("WHATSAPP", session.getContactType());
		assertEquals("wac360", session.getChannel());
		assertEquals("918689909204", session.getLane());
		assertEquals("Kiran", session.getContactName());
		assertEquals("TechSupport", session.getAssignedToDept());
		assertEquals("lt", session.getAssignedToAgent());
		assertEquals("agentdesk2", session.getAssignedToQueue());
		assertTrue(session.isActive());
		assertTrue(session.isInitd());
		assertFalse(session.isResolved());
		assertFalse(session.isExpired());
		assertEquals("OPEN", session.getStatus());
		assertEquals("AGENT", session.getMode());
		assertEquals(47L, session.getVersion().longValue());
		assertNotNull(session.getContact());
		assertEquals("wa919769657783_918689909204", session.contactId());
	}

	@Test
	public void jsonRoundTrip_preservesCoreFields() {
		URL url = FileUtil.getResource("sample/chat_session_doc.json", ChatSessionDocTest.class);
		ChatSessionDoc original = JsonUtil.parse(FileUtil.read(url), ChatSessionDoc.class);
		ChatSessionDoc roundTripped = JsonUtil.parse(JsonUtil.toJson(original), ChatSessionDoc.class);

		assertEquals(original.getSessionId(), roundTripped.getSessionId());
		assertEquals(original.getContactType(), roundTripped.getContactType());
		assertEquals(original.getAssignedToAgent(), roundTripped.getAssignedToAgent());
		assertEquals(original.getStatus(), roundTripped.getStatus());
		assertEquals(original.isActive(), roundTripped.isActive());
		assertEquals(original.getLastInComingStamp(), roundTripped.getLastInComingStamp());
		assertEquals(original.getLastOutGoingStamp(), roundTripped.getLastOutGoingStamp());
	}

}
