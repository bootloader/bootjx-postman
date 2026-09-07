package com.boot.jx.postman.store;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.boot.jx.dict.ContactType;
import com.boot.jx.postman.doc.MessageDoc;

/**
 * Pins per-channel message collection naming — must stay stable across Mongo driver
 * and Spring Data upgrades.
 */
public class MessageStoreTest {

	@Test
	public void getCollectionName_usesMessagePrefixAndContactType() {
		assertEquals("MESSAGE_WHATSAPP", MessageStore.getCollectionName(ContactType.WHATSAPP));
		assertEquals("MESSAGE_FACEBOOK", MessageStore.getCollectionName(ContactType.FACEBOOK));
		assertEquals("MESSAGE_TELEGRAM", MessageStore.getCollectionName(ContactType.TELEGRAM));
		assertEquals("MESSAGE_INSTAGRAM", MessageStore.getCollectionName(ContactType.INSTAGRAM));
		assertEquals("MESSAGE_TWITTER", MessageStore.getCollectionName(ContactType.TWITTER));
		assertEquals("MESSAGE_WEBSITE", MessageStore.getCollectionName(ContactType.WEBSITE));
		assertEquals("MESSAGE_EMAIL", MessageStore.getCollectionName(ContactType.EMAIL));
		assertEquals("MESSAGE_SMS", MessageStore.getCollectionName(ContactType.SMS));
		assertEquals("MESSAGE_DUMMY", MessageStore.getCollectionName(ContactType.DUMMY));
	}

	@Test
	public void getCollectionName_acceptsStringContactType() {
		assertEquals("MESSAGE_WHATSAPP", MessageStore.getCollectionName("WHATSAPP"));
	}

	@Test
	public void getCollectionName_defaultsToOthersWhenNull() {
		assertEquals("MESSAGE_OTHERS", MessageStore.getCollectionName((Object) null));
	}

	@Test
	public void collectionNameConstant_matchesBaseDocument() {
		assertEquals("MESSAGE", MessageDoc.COLLECTION_NAME);
	}

}
