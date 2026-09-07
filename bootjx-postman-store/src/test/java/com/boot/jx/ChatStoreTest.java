package com.boot.jx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.data.mongodb.core.query.Criteria;

import com.boot.jx.dict.ContactType;
import com.boot.jx.mongo.CommonMongoQB.MongoQueryBuilder;
import com.boot.jx.mongo.CommonMongoQueryBuilder;
import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.doc.ContactDetailDoc;
import com.boot.jx.postman.doc.CustomerProfileDoc;
import com.boot.jx.postman.doc.MessageDoc;
import com.boot.jx.postman.doc.OrderContactDoc;
import com.boot.jx.postman.pbook.PBPhone;
import com.boot.jx.postman.store.MessageStore;
import com.boot.jx.postman.store.PhoneParseTest;
import com.boot.test.BaseMongoTestSetup;
import com.boot.utils.ArgUtil;

/**
 * Mongo integration regression tests for postman store documents. Requires embedded
 * Mongo (see {@link BaseMongoTestSetup}).
 */
public class ChatStoreTest extends BaseMongoTestSetup {

	@Test
	public void chatSession_saveAndReload() {
		ensureMongoConnection();

		ChatSessionDoc chatSessionDoc = new ChatSessionDoc();
		chatSessionDoc.setContact(new ContactDetailDoc());
		chatSessionDoc.setContactName("Integration Test");
		chatSessionDoc.setActive(true);

		mongoTemplate.save(chatSessionDoc);

		ChatSessionDoc session = mongoTemplate.findById(chatSessionDoc.getSessionId(), ChatSessionDoc.class);
		assertNotNull("Saved session should be reloadable by sessionId", session);
		assertEquals(chatSessionDoc.getSessionId(), session.getSessionId());
		assertEquals("Integration Test", session.getContactName());
		assertTrue(session.isActive());
		assertNotNull(session.getContact());
	}

	@Test
	public void messageDoc_saveToChannelCollectionAndReload() {
		ensureMongoConnection();

		MessageDoc msg = new MessageDoc();
		msg.setMessage("store regression");
		msg.setType("I");

		mongoTemplate.save(msg, MessageStore.getCollectionName(ContactType.DUMMY));

		MessageDoc loaded = mongoTemplate.findById(msg.getMessageId(), MessageDoc.class,
				MessageStore.getCollectionName(ContactType.DUMMY));

		assertNotNull("Message should persist in per-channel collection", loaded);
		assertEquals(msg.getMessageId(), loaded.getMessageId());
		assertEquals("store regression", loaded.getMessage());
		assertEquals("I", loaded.getType());
	}

	@Test
	public void orderContactDoc_saveAndReload() {
		ensureMongoConnection();

		OrderContactDoc orderContact = new OrderContactDoc();
		orderContact.setOrderId("shopify-order-42");
		orderContact.setContactId("wa919930104050_918828218374");
		orderContact.setCreatedAt(System.currentTimeMillis());

		mongoTemplate.save(orderContact);

		OrderContactDoc loaded = mongoTemplate.findById("shopify-order-42", OrderContactDoc.class);
		assertNotNull(loaded);
		assertEquals("wa919930104050_918828218374", loaded.getContactId());
		assertTrue(loaded.getCreatedAt() > 0);
	}

	@Test
	public void customerProfile_queryByParsedPhone() {
		ensureMongoConnection();

		PBPhone ph = PhoneParseTest.parsePhone(new PBPhone().phone("91993104050"));

		CustomerProfileDoc profile = new CustomerProfileDoc();
		profile.setCode("test-profile-" + System.nanoTime());
		java.util.Set<PBPhone> phones = new java.util.HashSet<>();
		PBPhone stored = new PBPhone();
		stored.setNationalNumber(ph.getNationalNumber());
		stored.setCountryCallingCode(ph.getCountryCallingCode());
		phones.add(stored);
		profile.setPhones(phones);
		mongoTemplate.save(profile);

		MongoQueryBuilder<CustomerProfileDoc> qb = CommonMongoQueryBuilder.collection(CustomerProfileDoc.class)
				.where(Criteria.where("phones").elemMatch(Criteria.where("nationalNumber").is(ph.getNationalNumber())
						.and("countryCallingCode").is(ph.getCountryCallingCode())));

		java.util.List<CustomerProfileDoc> profiles = mongoTemplate.find(qb.getQuery(), CustomerProfileDoc.class);
		assertTrue("Profile should be findable by normalized phone parts", ArgUtil.is(profiles));
		assertEquals(profile.getCode(), profiles.get(0).getCode());
	}

}
