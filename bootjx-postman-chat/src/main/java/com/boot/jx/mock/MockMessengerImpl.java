package com.boot.jx.mock;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.boot.jx.AppContextUtil;
import com.boot.jx.aws.SqsQueueManager;
import com.boot.jx.aws.SqsQueueManager.SQSQueue;
import com.boot.jx.inbound.InBoundRouter;
import com.boot.jx.postman.PMConstants.CHANNEL_TYPE;
import com.boot.jx.postman.PostmanPackages.MockMessenger;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.model.MapModel;
import com.boot.utils.JsonUtil;
import com.boot.utils.PhoneUtil;

import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;

@Component
public class MockMessengerImpl implements MockMessenger {

	private static final Logger LOGGER = LoggerFactory.getLogger(MockMessengerImpl.class);
	private static final SQSQueue MOCK_QUEUE = new SQSQueue("whatsapp-mock-queue");
	private final Random random = new Random();

	@Autowired
	private SqsQueueManager queueManager;

	@Autowired
	@Lazy
	private InBoundRouter inBoundRouter;

	@Override
	public boolean isMockNumber(String number) {
		if (number == null) {
			return false;
		}
		return PhoneUtil.parse(number).isMock();
//		String cleanNumber = number.replace("+", "");
//		return (cleanNumber.startsWith("155501") || cleanNumber.startsWith("999")) && cleanNumber.length() == 12;
	}

	@Override
	public MapModel send(MapModel req, ChannelConfig channelConfig) {
		String mockMessageId = "wamid." + UUID.randomUUID().toString().replace("-", "");
		String recipientId = req.getString("to");

		String domain = AppContextUtil.getTenant();
		String channelId = channelConfig.getChannelId();

		String wabaId = channelConfig.getWacfb().getWabaId();
		String phoneId = channelConfig.getWacfb().getPhoneNumberId();
		String phoneNumber = channelConfig.getWacfb().getNumber();

		try {
			MapModel payload = MapModel.createInstance();
			payload.put("messageId", mockMessageId);
			payload.put("recipientId", recipientId);
			payload.put("domain", domain);
			payload.put("channelId", channelId);

			payload.put("wabaId", wabaId);
			payload.put("phoneId", phoneId);
			payload.put("phoneNumber", phoneNumber);

			payload.put("nextAction", "SENT");

			pushToMockQueue(payload);
			LOGGER.info("Mock msg pushed to SQS. ID: {} for Channel: {}", mockMessageId, channelId);
		} catch (Exception e) {
			LOGGER.error("Failed to push mock message to SQS", e);
		}

		// Return fake Meta success response
		return MapModel.createInstance().put("messages",
				List.of(MapModel.createInstance().put("id", mockMessageId).toMap()));
	}

	@Scheduled(fixedDelay = 2000)
	public void pollMockQueue() {
		ReceiveMessageResponse response = queueManager.poll(MOCK_QUEUE);

		if (response == null || !response.hasMessages()) {
			return;
		}

		for (Message msg : response.messages()) {
			try {
				MapModel req = MapModel.from(msg.body());
				String action = req.getString("nextAction", "SENT");

				switch (action) {
				case "SENT":
					onSend(req);
					break;
				case "DELIVER":
					onDeleiver(req);
					break;
				case "READ":
					onRead(req);
					break;
				case "FAILED":
					onFailed(req);
					break;
				}

				queueManager.dequeue(MOCK_QUEUE, msg);
			} catch (Exception e) {
				LOGGER.error("Mock SQS Polling failed for message {}", msg.messageId(), e);
			}
		}
	}

	@Override
	public void onSend(MapModel req) {
		fireWebhookToRouter(req, "sent");

		if (random.nextInt(100) < 5) {
			req.put("nextAction", "FAILED");
		} else {
			req.put("nextAction", "DELIVER");
		}
		pushToMockQueue(req);
	}

	@Override
	public void onDeleiver(MapModel req) {
		fireWebhookToRouter(req, "delivered");

		if (random.nextInt(100) < 5) {
			req.put("nextAction", "FAILED");
		} else {
			req.put("nextAction", "READ");
		}
		pushToMockQueue(req);
	}

	@Override
	public void onRead(MapModel req) {
		fireWebhookToRouter(req, "read");
		// End of funnel
	}

	public void onFailed(MapModel req) {
		fireWebhookToRouter(req, "failed");
		// End of funnel
	}

	// --- Helper Methods ---

	private void fireWebhookToRouter(MapModel req, String status) {
		String messageId = req.getString("messageId");
		String recipientId = req.getString("recipientId");
		String domain = req.getString("domain");
		String channelId = req.getString("channelId");

		String wabaId = req.getString("wabaId");
		String phoneId = req.getString("phoneId");
		String phoneNumber = req.getString("phoneNumber");

		// Pass them into the payload builder
		MapModel metaWebhookPayload = buildMetaWebhookPayload(messageId, recipientId, status, wabaId, phoneId,
				phoneNumber);

		try {
			inBoundRouter.inboundMessageEventAsync(domain, CHANNEL_TYPE.WACFB, channelId, metaWebhookPayload.toMap());
			LOGGER.info("Fired mock '{}' webhook to router for ID: {}", status, messageId);
		} catch (Exception e) {
			LOGGER.error("Failed to route mock webhook for {}", messageId, e);
		}
	}

	private MapModel buildMetaWebhookPayload(String messageId, String recipientId, String status, String wabaId,
			String phoneId, String phoneNumber) {
		MapModel statusNode = MapModel.createInstance().put("id", messageId).put("status", status)
				.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000)).put("recipient_id", recipientId);

		// Mimic Meta error format if failed
		if ("failed".equals(status)) {
			statusNode.put("errors", List.of(
					MapModel.createInstance().put("code", "131056").put("title", "Delivery failed via mock").toMap()));
		}

		// Add the metadata block with the analytics flag and real phone details
		MapModel valueNode = MapModel.createInstance().put("messaging_product", "whatsapp")
				.put("metadata",
						MapModel.createInstance().put("display_phone_number", phoneNumber)
								.put("phone_number_id", phoneId).put("is_mock_traffic", true) // <--- The Analytics Mock
																								// Flag
								.toMap())
				.put("statuses", List.of(statusNode.toMap()));

		MapModel changesNode = MapModel.createInstance().put("value", valueNode.toMap()).put("field", "messages");

		MapModel entryNode = MapModel.createInstance().put("id", wabaId).put("changes", List.of(changesNode.toMap()));

		return MapModel.createInstance().put("object", "whatsapp_business_account").put("entry",
				List.of(entryNode.toMap()));
	}

	private void pushToMockQueue(MapModel payload) {
		try {
			String jsonBody = JsonUtil.toJson(payload.toMap());
			SendMessageBatchRequestEntry entry = SendMessageBatchRequestEntry.builder()
					.id("mock-" + UUID.randomUUID().toString().substring(0, 8)).messageBody(jsonBody).build();

			queueManager.send(MOCK_QUEUE, Collections.singletonList(entry));
		} catch (Exception e) {
			LOGGER.error("Failed to requeue mock message", e);
		}
	}
}