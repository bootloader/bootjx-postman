package com.boot.jx.postman;

import com.boot.jx.dict.ContactType;
import com.boot.utils.ArgUtil;
import com.boot.utils.TimeUtils;

public class PMConstants {

	public final class DEFAULT {
		public static final String SYSTEM = "__SYSTEM__";
		public static final String NO_DEPT = "__DEPT__";
		public static final String NO_USER = "__USER__";
		public static final String BOT_QUEUE_CODE = "basic_bot";
		public static final String AGENT_QUEUE_CODE = "agent_desk";
		public static final String ADMIN_QUEUE_CODE = "admin_panel";
		public static final String BOT_FLOW_QUEUE_CODE = "bot_flow";
		public static final String APP_FLOW_QUEUE_CODE = "app_flow";
		public static final String FEEDBACK_QUEUE_CODE = "feedback";
	}

	public final static class USER_ROLE {

		/**
		 * Internal User, having full access to all domains
		 */
		public static final String DUPER_USER = "DUPER_USER";

		/**
		 * Internal Users, having access to all domains, equals to DUPER_USER but no
		 * access to assign roles
		 */
		public static final String SUPER_MANAGER = "SUPER_MANAGER";

		/**
		 * Internal DEV Users, having access to all domains, equals to DUPER_USER but no
		 * access to assign roles
		 */
		public static final String SUPER_DEV = "SUPER_DEV";

		/**
		 * Internal Users, having access to limited domain with all features.
		 */
		public static final String BUSINESS_MANAGER = "BUSINESS_MANAGER";

		/**
		 * Internal Users, having WABA-Access to assigned/created waba channels
		 */
		public static final String WABA_MANAGER = "WABA_MANAGER";
		/**
		 * User who can create domains for clients and has Admin-Access to those domains
		 */
		public static final String BUSINESS_PARTNER = "BUSINESS_PARTNER";

		/**
		 * User who can login to domain manager and has Admin-Access to that one domain
		 */
		public static final String BUSINESS_USER = "BUSINESS_USER";

		public static final String BLOCKED_USER = "BLOCKED_USER";

		public static final String ADMIN = "ADMIN";
		public static final String MODERATOR = "MODERATOR";
		public static final String AGENT = "AGENT";

		public static final String[] ALl_ROLES = { DUPER_USER, SUPER_MANAGER, SUPER_DEV, BUSINESS_MANAGER, WABA_MANAGER,
				WABA_MANAGER, BUSINESS_PARTNER, BUSINESS_USER, ADMIN, MODERATOR, AGENT, BLOCKED_USER };

		public static final String ALl_ROLES_STRING = ALl_ROLES.toString();

		public static final String ALLOWED = "DUPER_USER,SUPER_MANAGER,SUPER_DEV,BUSINESS_MANAGER,WABA_MANAGER,BUSINESS_PARTNER,BUSINESS_USER,ADMIN,AGENT,BLOCKED_USER";

		public static final String[] CAN_ACCESS_ALL_DOMAINS = { DUPER_USER, SUPER_MANAGER, SUPER_DEV };

	}

	public final class USER_SHIP_TYPE {
		public static final String OA_OWNER = "OA_OWNER";
		public static final String OA_ADMIN = "OA_ADMIN";
		public static final String OA_MEMBER = "OA_MEMBER";
	}

	public enum APP_MODULES {

		ADMIN("/admin"), AGENT("/agent"), CALENDAR("/nexus/calendar"), SOCIAL("/nexus/social"),
		LEAD_MANAGEMENT("/admin/app/scriptus/lead"), FEEDBACK_MANAGEMENT("/nexuz/tikat");

		private String path;

		APP_MODULES(String path) {
			this.path = path;
		}

		public String getPath() {
			return path;
		}
	}

	public static class MESSAGE_SOURCE_CATEGARY {
		public static final String MESSAGE = "MESSAGE";
		public static final String SOCIAL = "SOCIAL";
		public static final String ADS = "ADS";
		public static final String CAMPAIGN = "CAMPAIGN";
	}

	public static class MESSAGE_SOURCE_TYPE {
		public static final String INBOUND = "INBOUND";
		public static final String OUTBOUND = "OUTBOUND";
		public static final String STORY = "STORY";
		public static final String POST = "POST";
		public static final String FEEDBACK = "FEEDBACK";
	}

	public static class MESSAGE_BOUND_TYPE {
		public static final String INBOUND = "I";
		public static final String INBOUND_IMPORTED = "Ii";
		public static final String INBOUND_CALL = "Ic";

		public static final String OUTBOUND = "O";
		public static final String OUTBOUND_IMPORTED = "Oi";
		public static final String OUTBOUND_CALL = "Oc";

		public static final String LOG = "L";
		public static final String LOG_CALL = "Lc";
		
		/** Cross-channel outbound visibility log in x-session (not a channel delivery). */
		public static final String LOG_CROSS_OUTBOUND = "Lxo";

		/**
		 * Returns full type
		 * 
		 * @param typeSign
		 * @return
		 */
		public static String typeToName(String typeSign) {
			if (ArgUtil.is(typeSign, INBOUND)) {
				return "inbound";
			} else if (ArgUtil.is(typeSign, OUTBOUND)) {
				return "outbound";
			} else if (ArgUtil.is(typeSign, INBOUND_CALL)) {
				return "inbound_call";
			} else if (ArgUtil.is(typeSign, OUTBOUND_CALL)) {
				return "outbound_call";
			}
			return null;
		}
	}

	public final class CHANNEL_TYPE {
		public static final String NONE = "none";
		public static final String WA_GUPSHUP_LEGACY = "GUPSHUPW";
		public static final String TELEGRAM = "tg";
		public static final String TWITTER = "tw";
		public static final String FACEBOOK = "fb";
		public static final String WEB = "web";
		public static final String INSTAGRAM = "ig";
		public static final String EMAIL = "mailto";
		public static final String SMS_TWILIO = "smstw";
		public static final String SMS = "sms";
		/** Telinfy / GreenAds RCS (hub.telinfy.com) **/
		public static final String RCS_TELINFY = "rcstlf";
		public static final String OA = "oa";
		public static final String PUSHAPP = "pa";
		public static final String FIREBASE = "firebase";
		public static final String OUTLOOK = "outlook";
		public static final String GMAIL = "gmail";
		public static final String IMAP = "imap";

		/** ClickPost shipment tracking webhooks **/
		public static final String CLICKPOST = "clickpost";

		// WhatsApp Channels
		public static final String WA_GUPSHUP = "wags";
		public static final String WA_360D = "wa360";
		/** WABA cloud via 360d **/
		public static final String WA_360DC = "wac360";
		/** WABA cloud via FB **/
		public static final String WACFB = "wacfb";

	}

	public enum CHANNEL_TYPE_ENUM {
		none, tg, tw, fb, wags, wa360, web, ig, mailto, sms, rcstlf, wac360, oa, firebase, wacfb, outlook, gmail, imap,
		pa, clickpost
	}

	public static enum CHAT_STATUS {
		OPEN, UNASSIGNED, URGENT, ONHOLD, ATTENTION, EXPIRED, RESOLVED, CLOSED;
	}

	public static enum CHAT_ASSIGN_GROUP {
		UNASSIGNED, ME, TEAM, ORG, HISTORY;
	}

	public static enum CHAT_STATE {
		ACTIVE, OUTBOUND, CLOSED, EXPIRED, UNREAD, WAITING, WAITING_LONG, NEED_ATTENTION, UNATTENDED;
	}

	public static enum CHAT_MODE {
		AGENT, BOT, SCRIPTUS, PUSH, WEBHOOK, NONE;

		public static boolean isPushOnly(String mode) {
			if (!ArgUtil.is(mode)) {
				return true;
			}

			switch (mode) {
			case "PUSH":
			case "NONE":
				return true;
			}

			return false;
		}

		public static CHAT_MODE from(String chatMode) {
			return ArgUtil.parseAsEnumT(chatMode, CHAT_MODE.class, null);
		}

	}

	public static enum APP_TYPE {
		AGENT(CHAT_MODE.AGENT), BOT(CHAT_MODE.BOT), WEBHOOK(CHAT_MODE.WEBHOOK),

		// Bot modes
		TEAM_ROUTER(CHAT_MODE.BOT), APP_ROUTER(CHAT_MODE.BOT), APP_SWITCH(CHAT_MODE.BOT), QUICK_GALLERY(CHAT_MODE.BOT),
		QUICK_MENU(CHAT_MODE.BOT), FEEDBACK(CHAT_MODE.BOT), AVAMO(CHAT_MODE.BOT), FAQ(CHAT_MODE.BOT),

		// WebHooks
		APP_SCRIPT(CHAT_MODE.WEBHOOK),

		// SCRIPTUS MODES
		FEEDBACK_V2(CHAT_MODE.SCRIPTUS), BOTFLOW(CHAT_MODE.SCRIPTUS), AGENT_GPT(CHAT_MODE.SCRIPTUS),
		APPFLOW(CHAT_MODE.SCRIPTUS),

		// Agent Modes
		MITEL(CHAT_MODE.AGENT),

		// Others
		MOENGAGE(CHAT_MODE.PUSH), PUSHAPP(CHAT_MODE.PUSH), DEFAULT(CHAT_MODE.PUSH);

		private CHAT_MODE chatMode;
		private boolean deperecated;

		APP_TYPE(CHAT_MODE chatMode) {
			this.chatMode = chatMode;
		}

		APP_TYPE(CHAT_MODE chatMode, boolean deperecated) {
			this.chatMode = chatMode;
			this.deperecated = deperecated;
		}

		public CHAT_MODE getMode() {
			return this.chatMode;
		}

		public boolean is(CHAT_MODE chatMode) {
			return this.chatMode == chatMode;
		}

		public boolean isDeperecated() {
			return this.deperecated;
		}

		public static APP_TYPE from(Object appType) {
			return ArgUtil.parseAsEnumT(appType, APP_TYPE.class, APP_TYPE.DEFAULT);
		}
	}

	public static class MESSAGE_COMPOSE_TYPE {
		public static final String ACTION = "ACTION";
		public static final String SEND = "SEND";
		public static final String REPLY = "REPLY";
		public static final String PUSH = "PUSH";

		public static final String SEND_CODE = "N";
		public static final String REPLY_CODE = "R";
		public static final String PUSH_CODE = "P";

	}

	public static class MESSAGE_SEND_TYPE {
		public static final String PUSH_MESSAGE = "PM";
		public static final String SESSION_MESSAGE = "SM";
	}

	public static class MEDIATEMPLATE {
		public static final String MEDIA_TEMPLATE = "mediaTemplate";
	}

	public final class MESSAGE_SENDER_TYPE {
		public static final String AGENT = "AGENT";
		public static final String SYSTEM = "SYSTEM";
		public static final String BOT = "BOT";
		public static final String API = "API";
		public static final String ADMIN = "ADMIN";
	}

	public static class MESSAGE_FORMAT_TYPE {
		public static final String TEXT = "text";
		public static final String TEXT_SYSTEM = "system";
		public static final String TEXT_INTERACTIVE = "interactive";
		public static final String TEXT_BUTTON = "button";

		public static final String AUDIO = "audio";
		public static final String VOICE = "voice";

		public static final String DOCUMENT = "document";
		public static final String IMAGE = "image";
		public static final String STICKER = "sticker";
		public static final String LOCATION = "location";
		public static final String CONTACTS = "contacts";
		public static final String VIDEO = "video";
	}

	public static class ASSIGNMENT_RULE {
		public static final String MANUAL = "MANUAL";
		public static final String ROUND_ROBIN = "ROUND_ROBIN";
		public static final String STRICT_DEFAULT = "STRICT_DEFAULT";
	}

	public static class AVAILABILITY_RULE {
		// public static final String LOGGEDIND = "LOGGEDIN";
		public static final String ACTIVE = "ACTIVE";
		public static final String ONLINE = "ONLINE";
	}

	public static class CHAT_SESSION_STICKY {
		public static final String NONE = "NONE";
		public static final String ONAVAILABLE = "ONAVAILABLE";
		public static final String STRICT = "STRICT"; // TODO:-
	}

	public static String CHANNEL_TYPE(String contactType, String channel) {
		CHANNEL_TYPE_ENUM channelEnum = ArgUtil.parseAsEnumT(channel, CHANNEL_TYPE_ENUM.class, null);
		if (ArgUtil.is(channelEnum)) {
			return channel;
		}
		if (ContactType.FACEBOOK.toString().equals(contactType)) {
			return CHANNEL_TYPE.FACEBOOK;
		} else if (ContactType.TWITTER.toString().equals(contactType)) {
			return CHANNEL_TYPE.TWITTER;
		} else if (ContactType.TELEGRAM.toString().equals(contactType)) {
			return CHANNEL_TYPE.TELEGRAM;
		} else if (ContactType.WEBSITE.toString().equals(contactType)) {
			return CHANNEL_TYPE.WEB;
		} else if (ContactType.WHATSAPP.toString().equals(contactType)) {
			if (CHANNEL_TYPE.WA_GUPSHUP_LEGACY.equals(channel)) {
				return CHANNEL_TYPE.WA_GUPSHUP;
			} else if (CHANNEL_TYPE.WA_360DC.equals(channel)) {
				return CHANNEL_TYPE.WA_360DC;
			} else if (CHANNEL_TYPE.WA_360D.equals(channel)) {
				return CHANNEL_TYPE.WA_360D;
			} else if (CHANNEL_TYPE.WACFB.equals(channel)) {
				return CHANNEL_TYPE.WACFB;
			}
			return CHANNEL_TYPE.WA_360DC;
		}
		return null;
	}

	public static String CHANNEL_TYPE(ContactType contactType, String channel) {
		return CHANNEL_TYPE(ArgUtil.parseAsString(contactType), channel);
	}

	public static ContactType CONTACT_TYPE(String channel) {
		if (!ArgUtil.is(channel)) {
			return ContactType.WEBSITE;
		}
		switch (channel) {
		case CHANNEL_TYPE.WA_360D:
		case CHANNEL_TYPE.WA_360DC:
		case CHANNEL_TYPE.WA_GUPSHUP:
		case CHANNEL_TYPE.WA_GUPSHUP_LEGACY:
		case CHANNEL_TYPE.WACFB:
			return ContactType.WHATSAPP;
		case CHANNEL_TYPE.FACEBOOK:
			return ContactType.FACEBOOK;
		case CHANNEL_TYPE.INSTAGRAM:
			return ContactType.INSTAGRAM;
		case CHANNEL_TYPE.TWITTER:
			return ContactType.TWITTER;
		case CHANNEL_TYPE.WEB:
			return ContactType.WEBSITE;
		case CHANNEL_TYPE.SMS:
		case CHANNEL_TYPE.SMS_TWILIO:
			return ContactType.SMS;
		case CHANNEL_TYPE.RCS_TELINFY:
			return ContactType.RCS;
		case CHANNEL_TYPE.EMAIL:
			return ContactType.EMAIL;
		case CHANNEL_TYPE.CLICKPOST:
			return ContactType.APPLICATION;
		default:
			return ContactType.WEBSITE;
		}
	}

	public static class CHAT_SESSION_ACTIONS {
		public static final String ADD_STICKY_NOTE = "ADD_STICKY_NOTE";
		public static final String RESOLVE = "RESOLVE";
	}

	public static class DEFAULT_VALUES {
		public static final long POSTMAN_AGENT_TAB_HISTORY_PERIOD = TimeUtils.toMillis("1d");
		public static final long POSTMAN_AGENT_TAB_HISTORY_PERIOD_MAX = TimeUtils.toMillis("30d");
		public static final String MEDIA_TEMPLATE_STYLE = "background-color:white;width:400px;min-height:200px";
	}

	public final class PostManUrls {

		private PostManUrls() {
		}

		public static final String SEND_MESSAGE_BOX = "/postman/messagebox/send";
		public static final String SEND_SMS = "/postman/sms/send";
		public static final String SEND_EMAIL = "/postman/email/send";
		public static final String SEND_EMAIL_BULK = "/postman/email/sendBulk";
		public static final String SEND_EMAIL_DB = "/postman/email/";
		public static final String SEND_EMAIL_OLD = "/email/api/send/transaction/email";
		public static final String SEND_EMAIL_SUPPORT = "/postman/email/support";
		public static final String NOTIFY_SLACK = "/postman/slack/notify";
		public static final String NOTIFY_PUSH = "/postman/push/notify";
		public static final String NOTIFY_PUSH_BULK = "/postman/push/bulk_notify";
		public static final String NOTIFY_PUSH_SUBSCRIBE = "/postman/subscribe/{topic}";
		public static final String NOTIFY_SLACK_EXCEP = "/postman/slack/exception";
		public static final String NOTIFY_SLACK_EXCEP_REPORT = "/postman/slack/excep_report";
		public static final String PROCESS_TEMPLATE = "/postman/template/process";
		public static final String PROCESS_TEMPLATE_FILE = "/postman/template/file";
		public static final String PROCESS_TEMPLATE_FILE_LOCAL = "local";
		public static final String WHATS_APP_SEND = "/postman/whatsapp/send";
		public static final String WHATS_APP_SEND_BULK = "/postman/whatsapp/send_bulk";
		public static final String WHATS_APP_RESEND = "/postman/whatsapp/resend";
		public static final String WHATS_APP_STATUS = "/postman/whatsapp/status";
		public static final String WHATS_APP_STATS = "/postman/whatsapp/stats";
		public static final String WHATS_APP_POLL = "/postman/whatsapp/poll";
		public static final String SHORT_LINK = "/postman/shortlink";

		public static final String GEO_LOC = "/geo/location";
		public static final String EVENT_PUBLISH = "/event/publish/{event}/{id}";

		public static final String LIST_TENANT = "/postman/list/tenant";
		public static final String LIST_NATIONS = "/postman/list/nations";
		public static final String LIST_BRANCHES = "/postman/list/branches";

		public static final String DOC_UPLOAD_URL = "/upload/url";
		public static final String DOC_UPLOAD_FILE = "/upload/file";
		public static final String DOC_VALIDATE_ID = "/validate/id";
		public static final String DOC_IMAGE_BY_ID = "/image/{image_id}.{ext}";
		public static final String DOC_VIEW_BY_ID = "/view/{image_id}.{ext}";
		public static final String DOC_VIEW_BY_ID_JSON = "/view/{image_id}.json";
		public static final String DOC_URL_BY_ID = "/url/{image_id}.{ext}";
		public static final String DOC_SCAN_ID = "/scan/id";

	}

	public static class PROPERTIES {
		public static final String POSTMAN_AGENT_TAB_ORG = "postman.agent.tab.org";
		public static final String POSTMAN_CHAT_SESSION_TIMEOUT = "postman.chat.session.timeout";
		public static final String POSTMAN_CHAT_FEEDBACK_QUEUE = "postman.chat.feedback.queue";
		public static final String POSTMAN_AGENT_CHAT_AUTOREPLY_RESOLVED = "postman.agent.chat.autoreply.resolved";
		public static final String POSTMAN_AGENT_TAB_NONAGENT = "postman.agent.tab.nonagent";
		public static final String POSTMAN_AGENT_2FA_ENABLED = "postman.agent.2fa";
		public static final String POSTMAN_AGENT_2FA_CHANNEL = "postman.agent.2fa.channel";
		public static final String POSTMAN_AGENT_2FA_TOTP_REQUIRED = "postman.agent.2fa.totp.required";
		public static final String POSTMAN_CHAT_WEB_CHANNEL = "postman.chat.web.channel";
		public static final String POSTMAN_CHAT_WEB_QUEUE = "postman.chat.web.queue.enabled";
		public static final String POSTMAN_AGENT_TAB_LEVEL = "postman.agent.tab.level";

	}

	public static class ParamKeys {
		public static final String X_API_CODE = "x-api-code";
		public static final String X_API_ID = "x-api-id";
		public static final String X_API_KEY = "x-api-key";

	}

	public static final String COLLECTION_NAME = "MESSAGE_";
	public static final String CHAT_SESSION = "CHAT_SESSION";

	public static final String CONTACTS = "contacts";
	public static final String PLUS_SYM = "+";

	public static class FILE_TYPE {
		public static String CSV = "text/csv";
		public static String EXCEL = "application/vnd.ms-excel";
		public static String XLS = "application/xls";
		public static String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
	}

	public static enum CAMPAIGN_MODE {
		AGENT, BOT, DEFAULT;

		public static CAMPAIGN_MODE from(String campaignMode) {
			return ArgUtil.parseAsEnumT(campaignMode, CAMPAIGN_MODE.class, DEFAULT);
		}

	}

	public static final String NOT_AVALIABE = "Not Available";

}
