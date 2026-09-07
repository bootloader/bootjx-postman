package com.boot.jx;

import org.junit.Test;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.OutboxMessage;

/**
 * Legacy entry point — delegates to JUnit. See {@link com.boot.jx.postman.doc.PostmanDocMappingTest}
 * for full document mapping coverage.
 */
public class PojoTest {

	@Test
	public void messageClassMappingContextInitializes() throws Exception {
		MongoMappingContext context = new MongoMappingContext();
		context.afterPropertiesSet();
		context.getPersistentEntity(OutboxMessage.class);
		context.getPersistentEntity(InboxMessage.class);
	}

}
