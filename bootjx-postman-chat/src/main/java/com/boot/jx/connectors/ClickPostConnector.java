package com.boot.jx.connectors;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.boot.jx.dict.ContactType;
import com.boot.jx.logger.LoggerService;
import com.boot.jx.postman.PMConstants.CHANNEL_TYPE;
import com.boot.jx.postman.doc.ChatContactDoc;
import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.doc.OrderContactDoc;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.Message;
import com.boot.jx.postman.model.Message.Status;
import com.boot.jx.postman.model.MessageBoxEvent;
import com.boot.jx.postman.model.MessageDefinitions.Contactable;
import com.boot.jx.postman.model.MessageOrder;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.model.outbound.OutBoundMsgBasic.ApiOutBoundMsg;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.plugin.ChannelPluginProvider.ConnectorMapping;
import com.boot.jx.postman.plugin.ClickPostPlugin;
import com.boot.jx.postman.plugin.ClickPostPlugin.ClickPostConfigDetails;
import com.boot.jx.postman.store.SessionStore;
import com.boot.jx.rest.RestService;
import com.boot.jx.utils.PostManUtil;
import com.boot.model.MapModel;
import com.boot.utils.ArgUtil;
import com.boot.utils.CryptoUtil;

/**
 * ClickPost webhook → logging session on clickpost channel; forwards tracking
 * event to Scriptus.
 */
@Component
@ConnectorMapping(contactType = ContactType.APPLICATION, channel = CHANNEL_TYPE.CLICKPOST)
public class ClickPostConnector extends AbstractConnector<ClickPostConfigDetails, ClickPostPlugin> {

	private static final Logger LOGGER = LoggerService.getLogger(ClickPostConnector.class);

	/** Injected by InBoundController from URL {channelKey}. */
	public static final String PATH_CHANNEL_KEY = "__channelKey";

	/** Injected by InBoundController from HTTP header X-Api-Key. */
	public static final String HDR_X_API_KEY = "__xApiKey";

	private static final String OUTBOUND_BLOCKED = "ClickPost channel is inbound-only (webhook log)";

	private static final String SHOPIFY_GRAPHQL_PATH = "/admin/api/2024-01/graphql.json";

	private static final String SHOPIFY_ORDER_QUERY = "query getOrder($query: String!) { orders(first: 1, query: $query) { edges { node { "
			+ "id name customer { id firstName lastName email phone } } } } }";

	@Autowired
	private RestService restService;

	@Autowired
	private SessionStore sessionStore;

	@Override
	public boolean beforeSend(ChannelConfig channelConfig, ChatContactDoc chatContactDoc, OutboxMessage outboxMessage,
			ApiOutBoundMsg outBoundMsg) {
		outboxMessage.updateStatus(Message.Status.SENT_ERR);
		outboxMessage.logs().add(OUTBOUND_BLOCKED);
		return false;
	}

	@Override
	public Status onSend(ChannelConfig channelConfig, ChatContactDoc chatContactDoc, OutboxMessage outboxMessage,
			ApiOutBoundMsg outBoundMsg) {
		// ConnectorHandler contract; beforeSend returns false so this is not invoked.
		return Message.Status.SENT;
	}

	@Override
	public MessageBoxEvent inboundMessageBoxEvent(ChannelConfig channelConfig, MapModel requestMap,
			MessageBoxEvent messageBoxEvent) {

		// --- channel key validation ---
		String pathChannelKey = requestMap.getString(PATH_CHANNEL_KEY);
		if (ArgUtil.is(pathChannelKey) && !pathChannelKey.equals(channelConfig.getChannelKey())) {
			return messageBoxEvent;
		}

		// --- API key validation ---
		ClickPostConfigDetails details = channelConfig.getClickpost();
		if (ArgUtil.is(details) && ArgUtil.is(details.getWebhookApiKey())) {
			String headerKey = requestMap.getString(HDR_X_API_KEY);
			if (!details.getWebhookApiKey().equals(headerKey)) {
				return messageBoxEvent;
			}
		}

		// --- parse ClickPost payload fields ---
		String waybill = requestMap.getString("waybill");

		String orderId = requestMap.entry("additional").pathEntry("order_id").asString();
		if (ArgUtil.is(orderId)) {
			orderId = orderId.trim();
		}
		if (!ArgUtil.is(orderId)) {
			return messageBoxEvent;
		}

		String status = requestMap.getString("status");
		String remark = requestMap.getString("remark");
		String statusCode = ArgUtil.parseAsString(requestMap.get("clickpost_status_code"), "0");
		String statusBucket = ArgUtil.parseAsString(requestMap.get("clickpost_status_bucket"));

		// --- order → contact registry lookup (skip Shopify on hit) ---
		OrderContactDoc orderContact = sessionStore.findOrderContact(orderId);
		Map<String, Object> customer = null;
		String orderContactId = null;

		if (ArgUtil.is(orderContact) && ArgUtil.is(orderContact.getContactId())) {
			orderContactId = orderContact.getContactId();
		} else {
			// --- Shopify customer lookup (first time for this order only) ---
			customer = fetchShopifyCustomer(orderId, details);
			if (!ArgUtil.is(customer)) {
				LOGGER.warn("ClickPost skipped: Shopify customer not found channelId={} orderId={}", channelConfig.getChannelId(),
						orderId);
				return messageBoxEvent;
			}
		}

		LOGGER.info("ClickPost inbound: channelId={} orderId={} waybill={} status={} orderContactHit={}",
				channelConfig.getChannelId(), orderId, waybill, status, ArgUtil.is(orderContactId));

		// --- delegate inbox build (same as WA / Gmail / Telegram) ---
		try {
			InboxMessage inboxMessage = toInboxMessage(channelConfig, orderId, status, remark, statusCode,
					statusBucket, customer, orderContactId);

			// --- persist ORDER_CONTACT on first Shopify resolution ---
			if (!ArgUtil.is(orderContactId) && ArgUtil.is(inboxMessage.contact().getContactId())) {
				sessionStore.saveOrderContact(orderId, inboxMessage.contact().getContactId());
			}

			return messageBoxEvent.addInboxMessage(inboxMessage);
		} catch (Exception e) {
			LOGGER.error("ClickPost skipped: failed to build inbox channelId={} orderId={}", channelConfig.getChannelId(),
					orderId, e);
			return messageBoxEvent;
		}
	}

	/**
	 * Builds a ClickPost {@link InboxMessage} from validated webhook fields.
	 * <p>
	 * Called only after {@code order_id} validation and either ORDER_CONTACT hit or
	 * successful Shopify customer lookup in {@link #inboundMessageBoxEvent}. Plain {@code orderId}
	 * is stored on {@link MessageOrder}; {@link #messageIdExt(String, String)} stays plain for
	 * idempotency; session {@code ticketHash} uses MD5 of {@code orderId}.
	 *
	 * @param channelConfig channel configuration
	 * @param orderId       trimmed Shopify order id (plain, e.g. {@code 199343})
	 * @param status        ClickPost status text (stored on order + message)
	 * @param remark        ClickPost remark (appended to message body, not stored on order)
	 * @param statusCode    ClickPost status code
	 * @param statusBucket  ClickPost status bucket
	 * @param customer      Shopify customer map when registry misses; {@code null} on ORDER_CONTACT hit
	 * @param orderContactId Mehery contactId from ORDER_CONTACT; {@code null} on registry miss
	 * @return inbox message ready for {@link MessageBoxEvent#addInboxMessage}
	 * @throws Exception if inbox build fails (e.g. MD5 unavailable)
	 */
	private InboxMessage toInboxMessage(ChannelConfig channelConfig, String orderId, String status, String remark,
			String statusCode, String statusBucket, Map<String, Object> customer, String orderContactId)
			throws Exception {

		// --- create default message from channel ---
		InboxMessage inboxMessage = this.createInboxMessage(channelConfig);

		// --- set order (plain orderId; status fields for session.order) ---
		MessageOrder order = new MessageOrder();
		order.setOrderId(orderId);
		order.setStatus(status);
		order.setStatusCode(statusCode);
		order.setStatusBucket(statusBucket);
		inboxMessage.setOrder(order);

		// --- set contact: ORDER_CONTACT hit OR Shopify customer ---
		if (ArgUtil.is(orderContactId)) {
			inboxMessage.contact().setContactId(orderContactId);
		} else {
			applyShopifyCustomerToContact(inboxMessage.contact(), customer);
		}
		PostManUtil.updateContactMeta(inboxMessage.contact());

		// --- set message identity and body (remark in message text only) ---
		inboxMessage.setMessageIdExt(messageIdExt(orderId, statusCode));
		inboxMessage.setMessage(ArgUtil.nonEmpty(status, "") + " - " + ArgUtil.nonEmpty(remark, ""));

		// --- session key: MD5(plain orderId) for contactId + ticketHash lookup ---
		inboxMessage.session().setTicketHash(CryptoUtil.getMD5Hash(orderId));

		return inboxMessage;
	}

	@Override
	public OutboxMessage initSession(ChatSessionDoc session, InboxMessage inboxMessage) {
		return null;
	}

	/**
	 * Idempotency key for clickpost log lines and WA/email mirror lines (Phase
	 * 2.3). Format: {@code clickpost:{orderId}:{statusCode}}
	 */
	public static String messageIdExt(String orderId, String statusCode) {
		return "clickpost:" + orderId + ":" + ArgUtil.parseAsString(statusCode, "0");
	}

	private static void applyShopifyCustomerToContact(Contactable contact, Map<String, Object> customer) {
		MapModel cm = MapModel.from(customer);
		String csid = shopifyGidNumeric(cm.getString("id"));
		if (!ArgUtil.is(csid)) {
			return;
		}
		contact.setCsid(csid);
		String firstName = cm.getString("firstName");
		String lastName = cm.getString("lastName");
		String name = (ArgUtil.nonEmpty(firstName, "") + " " + ArgUtil.nonEmpty(lastName, "")).trim();
		if (ArgUtil.is(name)) {
			contact.setName(name);
		}
		String phone = cm.getString("phone");
		if (ArgUtil.is(phone)) {
			contact.setPhone(phone);
		}
		
		String email = cm.getString("email");
		if (ArgUtil.is(email)) {
			contact.setEmail(email);
		}
	}

	/** Shopify GID e.g. {@code gid://shopify/Customer/8277791932637} → numeric id. */
	private static String shopifyGidNumeric(String gid) {
		if (!ArgUtil.is(gid)) {
			return null;
		}
		int idx = gid.lastIndexOf('/');
		return idx >= 0 ? gid.substring(idx + 1) : gid;
	}

	/**
	 * ClickPost {@code order_id} (e.g. 199343) → Shopify search query (e.g.
	 * {@code name:#199343}).
	 */
	private static String shopifyOrderNameQuery(String orderId) {
		String trimmed = orderId.trim();
		return trimmed.startsWith("#") ? "name:" + trimmed : "name:#" + trimmed;
	}

	/**
	 * @return Shopify customer map, or {@code null} when lookup is not configured or
	 *         fails
	 */
	private Map<String, Object> fetchShopifyCustomer(String orderId, ClickPostConfigDetails config) {
		if (!ArgUtil.is(orderId) || !ArgUtil.is(config)) {
			return null;
		}
		String storeUrl = ArgUtil.is(config.getShopifyUrl()) ? config.getShopifyUrl().trim() : null;
		String token = config.getShopifyToken();
		if (!ArgUtil.is(storeUrl) || !ArgUtil.is(token)) {
			return null;
		}
		String nameQuery = shopifyOrderNameQuery(orderId);
		try {
			Map<String, Object> body = new LinkedHashMap<>();
			Map<String, Object> variables = new LinkedHashMap<>();
			variables.put("query", nameQuery);
			body.put("query", SHOPIFY_ORDER_QUERY);
			body.put("variables", variables);

			MapModel response = restService.ajax(storeUrl).path(SHOPIFY_GRAPHQL_PATH)
					.header("X-Shopify-Access-Token", token).postJson(body).asMapModel();

			if (!ArgUtil.is(response)) {
				return null;
			}
			if (ArgUtil.is(response.get("errors"))) {
				return null;
			}

			List<Map<String, Object>> edges = response.pathEntry("data").pathEntry("orders").pathEntry("edges")
					.asListOfMap();
			if (!ArgUtil.is(edges) || edges.isEmpty()) {
				return null;
			}
			MapModel order = MapModel.from(edges.get(0)).pathEntry("node").asMapModel();

			if (!order.pathEntry("customer").exists()) {
				return null;
			}
			Map<String, Object> customer = order.pathEntry("customer").asMap();
			if (!ArgUtil.is(customer) || customer.isEmpty()) {
				return null;
			}
			return customer;
		} catch (Exception e) {
			return null;
		}
	}

}
