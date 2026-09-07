package com.boot.jx.postman.doc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.boot.jx.dict.ContactType;
import com.boot.jx.postman.doc.MessageDoc.MessageDocEmail;
import com.boot.jx.postman.doc.MessageDoc.MessageDocFB;
import com.boot.jx.postman.doc.MessageDoc.MessageDocIG;
import com.boot.jx.postman.doc.MessageDoc.MessageDocTG;
import com.boot.jx.postman.doc.MessageDoc.MessageDocTW;
import com.boot.jx.postman.doc.MessageDoc.MessageDocWA;
import com.boot.jx.postman.doc.MessageDoc.MessageDocWeb;

/**
 * Pins channel-specific {@link MessageDoc} subtype selection used when persisting
 * inbound/outbound messages.
 */
public class MessageDocTest {

	@Test
	public void instance_returnsChannelSpecificSubclass() {
		assertTrue(MessageDoc.instance(ContactType.WHATSAPP) instanceof MessageDocWA);
		assertTrue(MessageDoc.instance(ContactType.FACEBOOK) instanceof MessageDocFB);
		assertTrue(MessageDoc.instance(ContactType.TELEGRAM) instanceof MessageDocTG);
		assertTrue(MessageDoc.instance(ContactType.INSTAGRAM) instanceof MessageDocIG);
		assertTrue(MessageDoc.instance(ContactType.TWITTER) instanceof MessageDocTW);
		assertTrue(MessageDoc.instance(ContactType.WEBSITE) instanceof MessageDocWeb);
		assertTrue(MessageDoc.instance(ContactType.EMAIL) instanceof MessageDocEmail);
	}

	@Test
	public void instance_returnsBaseMessageDocForUnmappedChannels() {
		assertEquals(MessageDoc.class, MessageDoc.instance(ContactType.SMS).getClass());
		assertEquals(MessageDoc.class, MessageDoc.instance(ContactType.DUMMY).getClass());
		assertEquals(MessageDoc.class, MessageDoc.instance(ContactType.PUSH).getClass());
	}

}
