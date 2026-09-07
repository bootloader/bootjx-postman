package com.boot.jx.postman.doc;

import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.utils.StringUtils.StringMatcher;

/**
 * Ensures Spring Data Mongo can map postman document and message POJOs — catches
 * annotation/proxy changes on Spring Boot upgrades.
 */
public class PostmanDocMappingTest {

	private final MongoMappingContext context = new MongoMappingContext();

	public PostmanDocMappingTest() throws Exception {
		context.setSimpleTypeHolder(new SimpleTypeHolder(
				new HashSet<>(Arrays.asList(StringMatcher.class, Pattern.class, Matcher.class)),
				SimpleTypeHolder.DEFAULT));
		context.afterPropertiesSet();
	}

	@Test
	public void mapsCoreSessionAndMessageDocuments() {
		assertNotNull(context.getPersistentEntity(ChatSessionDoc.class));
		assertNotNull(context.getPersistentEntity(MessageDoc.class));
		assertNotNull(context.getPersistentEntity(MessageDoc.MessageDocWA.class));
		assertNotNull(context.getPersistentEntity(ChatContactDoc.class));
		assertNotNull(context.getPersistentEntity(CustomerProfileDoc.class));
		assertNotNull(context.getPersistentEntity(HSMTemplateDoc.class));
		assertNotNull(context.getPersistentEntity(OrderContactDoc.class));
	}

	@Test
	public void mapsInboxAndOutboxMessageModels() {
		assertNotNull(context.getPersistentEntity(InboxMessage.class));
		assertNotNull(context.getPersistentEntity(OutboxMessage.class));
	}

	@Test
	public void mapsMessageHoldWithNestedInboxMessage() {
		assertNotNull(context.getPersistentEntity(MessageHold.class));
		assertNotNull(context.getPersistentEntity(MessageHold.MessageHoldOriginal.class));
	}

}
