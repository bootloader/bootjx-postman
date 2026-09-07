package com.boot.jx.utils;

import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.boot.jx.dict.ContactType;
import com.boot.jx.model.CommonFile;
import com.boot.jx.postman.PMConstants;
import com.boot.jx.postman.PMConstants.CHANNEL_TYPE;
import com.boot.jx.postman.PMConstants.CHAT_MODE;
import com.boot.jx.postman.PMEnvironment.AChannelConfig;
import com.boot.jx.postman.model.ContactMeta;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.MessageCall;
import com.boot.jx.postman.model.MessageDefinitions.Contactable;
import com.boot.jx.postman.model.MessageDefinitions.IMessage;
import com.boot.jx.postman.model.MessageReferral;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.model.PMArgs;
import com.boot.jx.postman.model.ext.InBoundEvent;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.utils.ArgUtil;
import com.boot.utils.CryptoUtil;
import com.boot.utils.Random;
import com.boot.utils.StringUtils;
import com.boot.utils.UniqueID;

public class PostManUtil {
	public static final String SUBJECT_CLEANER_STR = "^([\\[\\(] *)?(?i)(RE?S?|REPLY|FYI|RIF|I|FS|VB|RV|ENC|ODP|PD|YNT|ILT|SV|VS|VL|AW|WG|ΑΠ|ΣΧΕΤ|ΠΡΘ|תגובה|הועבר|主题|转发|FWD|Forward?) *([-:;)\\]][ :;\\])-]*|$)|\\]+ *$";
	public static final Pattern SUBJECT_CLEANER = Pattern.compile(SUBJECT_CLEANER_STR);

	public static ResponseEntity<byte[]> download(CommonFile file) {
		return ResponseEntity.ok().contentLength(file.getBody().length)
				.header("Content-Disposition", "attachment; filename=" + file.getName())
				.contentType(MediaType.valueOf(file.getFileFormat().getContentType())).body(file.getBody());
	}

	public static ResponseEntity<byte[]> render(CommonFile file) {
		return ResponseEntity.ok().contentLength(file.getBody().length)
				.contentType(MediaType.valueOf(file.getFileFormat().getContentType())).body(file.getBody());
	}

	public static String createCsid(Contactable contact) {
		if (ArgUtil.is(contact.getCsid())) {
			return contact.getCsid();
		}
		ContactType contactType = ArgUtil.parseAsEnumT(contact.getContactType(), ContactType.class);
		if (ArgUtil.is(contactType)) {
			switch (contactType) {
			case WHATSAPP:
			case SMS:
				return contact.phone();
			case EMAIL:
				return contact.getEmail();
			default:
				break;
			}
		}
		return contact.getCsid();
	}

	private static String createContactId(ContactType contactType, String id, String lane) {
		if (ContactType.WHATSAPP.equals(contactType)) {
			return "wa" + id + "_" + lane;
		} else if (ContactType.FACEBOOK.equals(contactType)) {
			return "fb" + id + "_" + lane;
		} else if (ContactType.TWITTER.equals(contactType)) {
			return "tw" + id + "_" + lane;
		} else if (ContactType.TELEGRAM.equals(contactType)) {
			return "tg" + id + "_" + lane;
		} else if (ArgUtil.is(contactType)) {
			return contactType.getShortCode() + id + "_" + lane;
		}
		return id;
	}

	public static String createContactId(Contactable contact) {
		if (ArgUtil.is(contact.getContactId())) {
			return contact.getContactId();
		}
		String csid = createCsid(contact);
		return createContactId(ArgUtil.parseAsEnumT(contact.getContactType(), ContactType.class), csid,
				contact.getLane());
	}

	public static String createContactId(IMessage inboxMessage) {
		return createContactId(inboxMessage.contact());
	}

	public static String createContactId(InBoundEvent assignEvent) {
		if (ArgUtil.is(assignEvent.contactId)) {
			return assignEvent.contactId;
		}
		return createContactId(assignEvent.contact());
	}

	public static Contactable updateContactMeta(Contactable contact) {
		if (!ArgUtil.is(contact.getCsid())) {
			contact.setCsid(createCsid(contact));
		}
		if (!ArgUtil.is(contact.getContactId()) // if ContactId is Missing
				&& ArgUtil.is(contact.getContactType()) // ContactType is for contactId
				&& ArgUtil.is(contact.getCsid()) // CSID is for contactId
				&& ArgUtil.is(contact.getLane()) // Lane is for contactId
		) {
			contact.setContactId(createContactId(contact));
		}
		return contact;
	}

	public static Contactable getContactMeta(Contactable contact) {
		Contactable contactMeta = new ContactMeta();
		contactMeta.copyFrom(contact);
		return updateContactMeta(contactMeta);
	}

	public static Contactable getContactMeta(Contactable contact, String contactId) {
		Contactable contactMeta = getContactMeta(contact);
		if (!ArgUtil.is(contactMeta.getContactId()) && ArgUtil.is(contactId)) {
			contactMeta.setContactId(contactId);
		}
		return contactMeta;
	}

	public static String generateCheckSum(InboxMessage inboxMessage) {
		String checkString = inboxMessage.contact().getContactId() + inboxMessage.getSessionId()
				+ inboxMessage.getMessageId() + inboxMessage.getMessage();
		try {
			return CryptoUtil.getMD5Hash(checkString);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return e.getMessage();
		}
	}

	public static boolean hasValidCheckSum(InboxMessage inboxMessage) {
		return ArgUtil.areEqual(inboxMessage.getChecksum(), generateCheckSum(inboxMessage));
	}

	public static String generateCheckSum(PMArgs params) {
		String checkString = params.contact().getContactId() + params.getSessionId();
		try {
			return CryptoUtil.getMD5Hash(checkString);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return e.getMessage();
		}
	}

	public static boolean hasValidCheckSum(PMArgs params) {
		return ArgUtil.areEqual(params.getChecksum(), generateCheckSum(params));
	}

	public static String generateCheckSum(InBoundEvent event) {
		String checkString = event.contact().getContactId() + event.getSessionId() + event.type;
		try {
			return CryptoUtil.getMD5Hash(checkString);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return e.getMessage();
		}
	}

	public static boolean hasValidCheckSum(InBoundEvent event) {
		return ArgUtil.areEqual(event.getChecksum(), generateCheckSum(event));
	}

	public static boolean isInBound(String type) {
		return ArgUtil.isEqual(type, PMConstants.MESSAGE_BOUND_TYPE.INBOUND,
				PMConstants.MESSAGE_BOUND_TYPE.INBOUND_IMPORTED, PMConstants.MESSAGE_BOUND_TYPE.INBOUND_CALL);
	}

	public static boolean isInBound(IMessage inboxMessage) {
		if (ArgUtil.is(inboxMessage.getType())) {
			return isInBound(inboxMessage.getType());
		}
		return inboxMessage instanceof InboxMessage;
	}

	public static boolean isOutBound(String type) {
		return ArgUtil.isEqual(type, PMConstants.MESSAGE_BOUND_TYPE.OUTBOUND,
				PMConstants.MESSAGE_BOUND_TYPE.OUTBOUND_IMPORTED, PMConstants.MESSAGE_BOUND_TYPE.OUTBOUND_CALL);
	}

	public static boolean isOutBound(IMessage inboxMessage) {
		if (ArgUtil.is(inboxMessage.getType())) {
			return isOutBound(inboxMessage.getType());
		}
		return inboxMessage instanceof OutboxMessage;
	}

	/**
	 * True if message produces a log-type doc (Lc); skip queue assignment and full
	 * post-processing.
	 */
	public static boolean isLogTypeCall(IMessage message) {
		if (message instanceof InboxMessage) {
			MessageCall call = ((InboxMessage) message).getCall();
			return call != null && MessageCall.EVENTS.CALL_PERMISSION_REPLY.equals(call.getEvent());
		}
		return false;
	}

	public static boolean isBotMode(IMessage inboxMessage) {
		return CHAT_MODE.BOT.toString().equals(inboxMessage.session().getMode());
	}

	public static boolean isAgentMode(IMessage inboxMessage) {
		return CHAT_MODE.AGENT.toString().equals(inboxMessage.session().getMode());
	}

	public static String ROUTING_ID(String sessionId, String queueCode) {
		return String.format("%s_%s_%s", sessionId, queueCode, UniqueID.generateString());
	}

	public static String CONTACT_ID(Contactable contactable) {
		return createContactId(contactable);
	}

	public static String CONTACT_ID(ChannelConfig channelConfig, String csid) {
		return createContactId(channelConfig.getContactType(), csid, channelConfig.getLane());
	}

	public static String CHANNEL_ID_FORMAT(String channelType, String lane) {
		return String.format("%s:%s", channelType, lane).toLowerCase();
	}

	public static String CHANNEL_ID(String contactType, String channelType, String lane) {
		if (!ArgUtil.is(channelType)) {
			channelType = PMConstants.CHANNEL_TYPE(contactType, channelType);
		}
		if (CHANNEL_TYPE.WA_GUPSHUP_LEGACY.equals(channelType)) {
			channelType = CHANNEL_TYPE.WA_GUPSHUP;
		}
		return CHANNEL_ID_FORMAT(channelType, lane);
	}

	public static String CHANNEL_ID(String channelType, String lane) {
		return CHANNEL_ID(null, channelType, lane);
	}

	public static String CHANNEL_ID_ENCODED(String channelType, String lane) {
		if (CHANNEL_TYPE.WA_360DC.equalsIgnoreCase(channelType)) {
			return CHANNEL_ID(channelType, lane).replace(":", "=");
		}
		return CHANNEL_ID(channelType, lane);
	}

	public static String CHANNEL_ID_DECODED(String channelId) {
		return channelId.replace("=", ":");
	}

	public static String CHANNEL_ID(Contactable contactable) {
		return CHANNEL_ID(contactable.getContactType(), contactable.getChannelType(), contactable.getLane());
	}

	public static String CHANNEL_ID(ChannelConfig channel) {
		return CHANNEL_ID(ArgUtil.parseAsString(channel.getContactType()), channel.getChannelType(), channel.getLane());
	}

	/** True when referral.channelId differs from the outbound send channel. */
	public static boolean isCrossChannelReferral(MessageReferral referral, String outboundChannelId) {
		return ArgUtil.is(referral) && ArgUtil.is(referral.getChannelId()) && ArgUtil.is(outboundChannelId)
				&& !outboundChannelId.equals(referral.getChannelId());
	}

	public static Contactable parseChannelId(String channelId) {
		if (!ArgUtil.is(channelId)) {
			return null;
		}

		String channelIdDecoded = CHANNEL_ID_DECODED(channelId);
		String[] channelIds = channelIdDecoded.split(":");

		if (channelIds.length < 2) {
			return null;
		}

		Contactable contactMeta = new ContactMeta();
		contactMeta.setContactType(ArgUtil.parseAsString(PMConstants.CONTACT_TYPE(channelIds[0])));
		contactMeta.setLane(channelIds[1]);
		contactMeta.setChannelType(channelIds[0]);
		return contactMeta;
	}

	public static String CHANNEL_TYPE_FALLBACK(String channel) {
		if (ArgUtil.not(channel)) {
			return null;
		}
		switch (channel) {
		case CHANNEL_TYPE.WA_360D:
			return CHANNEL_TYPE.WACFB;
		case CHANNEL_TYPE.WA_360DC:
			return CHANNEL_TYPE.WACFB;
		case CHANNEL_TYPE.WA_GUPSHUP:
			return CHANNEL_TYPE.WACFB;
		default:
			return null;
		}
	}

	public static String CHANNEL_ID_FALLBACK(String channelId) {
		Contactable c = PostManUtil.parseChannelId(channelId);
		String channelType = CHANNEL_TYPE_FALLBACK(c.getChannelType());
		c.setChannelType(channelType);
		return PostManUtil.CHANNEL_ID(c);
	}

	public static String UNIQUE_API_KEY() {
		return String.format("%s%s", UniqueID.generateString(), Random.randomAlphaNumeric(10));
	}

	public static String CHANNEL_CALLBACK_PATH(String accountKey, AChannelConfig channelConfig) {
		return String.format("ext/inbound/%s/%s/callback/%s/%s/%s",
				ArgUtil.nonEmpty(channelConfig.getApiVersion(), "v2"), channelConfig.getChannelType(), accountKey,
				CHANNEL_ID_ENCODED(channelConfig.getChannelType(), channelConfig.getLane()),
				channelConfig.getChannelKey());
	}

	public static String ON_DEPT_ASSIGN_TOPIC(String dept) {
		return "/dept/onassign-" + dept;
	}

	public static boolean IS_TRACK_BY_REPLY_ID(String channelType) {
		if (PMConstants.CHANNEL_TYPE.EMAIL.equals(channelType)) {
			return true;
		}
		return false;
	}

	public static boolean IS_MULTI_THREAD(String channelType) {
		if (ArgUtil.is(channelType, PMConstants.CHANNEL_TYPE.EMAIL, PMConstants.CHANNEL_TYPE.GMAIL,
				PMConstants.CHANNEL_TYPE.OUTLOOK, PMConstants.CHANNEL_TYPE.IMAP)) {
			return true;
		}
		return false;
	}

	public static boolean IS_SINGLE_THREAD(String channelType) {
		return !IS_MULTI_THREAD(channelType);
	}

	public static String createTicketHash(IMessage inboxMessage) throws NoSuchAlgorithmException {
		String subject = StringUtils.normalizeSpace(inboxMessage.getSubject().replaceFirst(SUBJECT_CLEANER_STR, ""));
		String conatctid = PostManUtil.CONTACT_ID(inboxMessage.contact());
		return CryptoUtil.getMD5Hash(conatctid + "-" + StringUtils.trim(subject));
	}

}
