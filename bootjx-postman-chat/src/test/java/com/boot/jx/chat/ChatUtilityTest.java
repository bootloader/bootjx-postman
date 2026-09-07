package com.boot.jx.chat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.boot.jx.postman.ClientApp;
import com.boot.jx.postman.PMConstants;
import com.boot.jx.postman.PMConstants.APP_TYPE;
import com.boot.jx.postman.PMConstants.CHAT_MODE;
import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.model.ext.SessionBoundEvent;

/**
 * Pins down {@link ChatUtility} push/queue/campaign routing decisions.
 */
public class ChatUtilityTest {

	private ChatUtility chatUtility;

	@Before
	public void setUp() {
		chatUtility = new ChatUtility();
	}

	@Test
	public void isPushOnly_sessionWithNoQueue_returnsTrue() {
		ChatSessionDoc session = new ChatSessionDoc();
		assertTrue(chatUtility.isPushOnly(session));
	}

	@Test
	public void isPushOnly_sessionInAgentModeWithQueue_returnsFalse() {
		ChatSessionDoc session = new ChatSessionDoc();
		session.setAssignedToQueue("support");
		session.setMode(PMConstants.CHAT_MODE.AGENT.toString());
		assertFalse(chatUtility.isPushOnly(session));
	}

	@Test
	public void isPushOnly_pushApp_returnsTrue() {
		assertTrue(chatUtility.isPushOnly(clientApp(PMConstants.CHAT_MODE.PUSH.toString())));
	}

	@Test
	public void isCampaignPossible_webhookApp_returnsTrue() {
		assertTrue(chatUtility.isCampaignPossible(clientApp(PMConstants.CHAT_MODE.WEBHOOK.toString())));
	}

	@Test
	public void inQueue_assignedSession_returnsTrue() {
		ChatSessionDoc session = new ChatSessionDoc();
		session.setAssignedToQueue("inbox");
		assertTrue(chatUtility.inQueue(session));
	}

	@Test
	public void isPublishSessionBoundEventPostInbound_bulkSession_returnsTrue() {
		ClientApp app = clientApp(PMConstants.CHAT_MODE.AGENT.toString());
		SessionBoundEvent event = new SessionBoundEvent();
		event.sessionBulkId = "bulk-123";
		assertTrue(chatUtility.isPublishSessionBoundEventPostInbound(app, event));
	}

	private static ClientApp clientApp(String appMode) {
		return new ClientApp() {
			private static final long serialVersionUID = 1L;

			@Override
			public String getAppMode() {
				return appMode;
			}

			@Override
			public String getId() {
				return null;
			}

			@Override
			public String getKey() {
				return null;
			}

			@Override
			public String getAppHook() {
				return null;
			}

			@Override
			public String getAppHookFrwrd() {
				return null;
			}

			@Override
			public String getKeyName() {
				return null;
			}

			@Override
			public String getQueue() {
				return null;
			}

			@Override
			public String getKeyVersion() {
				return null;
			}

			@Override
			public Long getVersion() {
				return null;
			}

			@Override
			public String getAppType() {
				return null;
			}

			@Override
			public String getWebhook() {
				return null;
			}

			@Override
			public String getForward() {
				return null;
			}

			@Override
			public Map<String, Object> getProps() {
				return Collections.emptyMap();
			}

			@Override
			public Map<String, Object> props() {
				return Collections.emptyMap();
			}

			@Override
			public Map<String, Object> getSecret() {
				return Collections.emptyMap();
			}

			@Override
			public Map<String, Object> secret() {
				return Collections.emptyMap();
			}

			@Override
			public boolean isShared() {
				return false;
			}

			@Override
			public boolean isReadOnly() {
				return false;
			}

			@Override
			public boolean isAgentApp() {
				return false;
			}

			@Override
			public boolean isPreDefinedBot() {
				return false;
			}

			@Override
			public boolean isFeedbackApp() {
				return false;
			}

			@Override
			public boolean isWebhookApp() {
				return false;
			}

			@Override
			public boolean isCustomApp() {
				return false;
			}

			@Override
			public boolean equals(CHAT_MODE mode) {
				return false;
			}

			@Override
			public boolean equals(APP_TYPE appType) {
				return false;
			}

			@Override
			public Map<String, Object> getConfig() {
				return Collections.emptyMap();
			}

			@Override
			public Map<String, Object> config() {
				return Collections.emptyMap();
			}

			@Override
			public Map<String, Object> custom() {
				return Collections.emptyMap();
			}

			@Override
			public boolean isDisabled() {
				return false;
			}

			@Override
			public boolean isTimeOutPossible() {
				return false;
			}
		};
	}

}
