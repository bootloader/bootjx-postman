package com.boot.jx.postman.plugin;

import java.util.HashMap;
import java.util.Map;

import com.boot.jx.postman.PMConstants.CHANNEL_TYPE;
import com.boot.jx.postman.PMEnvironment.AChannelConfig;
import com.boot.jx.postman.PMEnvironment.AChannelDetails;
import com.boot.jx.postman.plugin.ClickPostPlugin.ClickPostConfigDetails;
import com.boot.jx.postman.plugin.EmailPlugin.EmailConfigDetails;
import com.boot.jx.postman.plugin.FacebookPlugin.FacebookConfigDetails;
import com.boot.jx.postman.plugin.FirebasePlugin.FirebaseConfigDetails;
import com.boot.jx.postman.plugin.GmailPlugin.GmailConfigDetails;
import com.boot.jx.postman.plugin.ImapPlugin.ImapConfigDetails;
import com.boot.jx.postman.plugin.InstagramPlugin.InstagramConfig;
import com.boot.jx.postman.plugin.OAPlugin.OAConfigDetails;
import com.boot.jx.postman.plugin.OutlookPlugin.OutlookConfigDetails;
import com.boot.jx.postman.plugin.PushAppPlugin.PushAppConfigDetails;
import com.boot.jx.postman.plugin.RCSPlugin.RCSConfigDetails;
import com.boot.jx.postman.plugin.SMSPlugin.SMSConfigDetails;
import com.boot.jx.postman.plugin.TelegramPlugin.TelegramConfigDetails;
import com.boot.jx.postman.plugin.TwilioSMSPlugin.TwilioConfigDetails;
import com.boot.jx.postman.plugin.TwitterPlugin.TwitterConfigDetails;
import com.boot.jx.postman.plugin.WA360CloudPlugin.WA360CloudConfigDetails;
import com.boot.jx.postman.plugin.WA360Plugin.WA360ConfigDetails;
import com.boot.jx.postman.plugin.WAGupShupPlugin.GupShupConfigDetails;
import com.boot.jx.postman.plugin.WacfbPlugin.WACFBConfigDetails;
import com.boot.jx.postman.plugin.WebPlugin.WebConfigDetails;
import com.boot.utils.ArgUtil;

public class ChannelConfig extends AChannelConfig {

	private static final long serialVersionUID = -254797155595466825L;
	private String lane;
	private String sublane;

	private FacebookConfigDetails facebook;
	private TwitterConfigDetails twitter;
	private TelegramConfigDetails telegram;
	private GupShupConfigDetails gupshup;
	private InstagramConfig instagram;
	private WA360ConfigDetails wa360d;
	private WebConfigDetails web;
	private FirebaseConfigDetails firebase;
	private EmailConfigDetails email;
	private TwilioConfigDetails twilio;
	private SMSConfigDetails sms;
	private RCSConfigDetails rcs;
	private WA360CloudConfigDetails wa360dc;
	private WACFBConfigDetails wacfb;
	private OAConfigDetails oa;
	private PushAppConfigDetails pushapp;
	private OutlookConfigDetails outlook;
	private GmailConfigDetails gmail;
	private ImapConfigDetails imap;
	private ClickPostConfigDetails clickpost;

	private boolean isAutoCreated;
	private boolean isMaster;
	private boolean isInboundAllowed;
	private boolean isOutboundAllowed;

	private boolean isPushAllowed;
	private boolean isPushOnlyApproved;
	private boolean isPushFreeTextAllowed;
	private boolean isPushToNewContactAllowed;
	private boolean isWebhookManual;
	private boolean pushLocalStore;

	private Map<String, Object> meta;

	private String masterChannelId;
	private String channelConfigTempId;

	private Object error;

	private String callbackPath;
	private String unhandledInboundForward;
	protected String domain;
	private String domainProxy;

	public String getLane() {
		return lane;
	}

	@Override
	public boolean isPushAllowed() {
		return this.isPushAllowed;
	}

	@Override
	public boolean isPushOnlyApproved() {
		return this.isPushOnlyApproved;
	}

	@Override
	public boolean isPushFreeTextAllowed() {
		return this.isPushFreeTextAllowed;
	}

	@Override
	public boolean isPushToNewContactAllowed() {
		return this.isPushToNewContactAllowed;
	}

	public void setLane(String lane) {
		this.lane = lane;
	}

	public FacebookConfigDetails getFacebook() {
		return facebook;
	}

	public void setFacebook(FacebookConfigDetails facebook) {
		this.facebook = facebook;
	}

	public InstagramConfig getInstagram() {
		return instagram;
	}

	public void setInstagram(InstagramConfig instagram) {
		this.instagram = instagram;
	}

	public TwitterConfigDetails getTwitter() {
		return twitter;
	}

	public void setTwitter(TwitterConfigDetails twitter) {
		this.twitter = twitter;
	}

	public TelegramConfigDetails getTelegram() {
		return telegram;
	}

	public void setTelegram(TelegramConfigDetails telegram) {
		this.telegram = telegram;
	}

	public GupShupConfigDetails getGupshup() {
		return gupshup;
	}

	public void setGupshup(GupShupConfigDetails gupshup) {
		this.gupshup = gupshup;
	}

	public ChannelConfig disabled(boolean isDisabled) {
		this.setDisabled(isDisabled);
		return this;
	}

	public WA360ConfigDetails getWa360d() {
		return wa360d;
	}

	public void setWa360d(WA360ConfigDetails wa360d) {
		this.wa360d = wa360d;
	}

	public WebConfigDetails getWeb() {
		return web;
	}

	public void setWeb(WebConfigDetails web) {
		this.web = web;
	}

	public String getCallbackPath() {
		return callbackPath;
	}

	public void setCallbackPath(String callbackPath) {
		this.callbackPath = callbackPath;
	}

	public void setPushAllowed(boolean isPushAllowed) {
		this.isPushAllowed = isPushAllowed;
	}

	public void setPushOnlyApproved(boolean isPushOnlyApproved) {
		this.isPushOnlyApproved = isPushOnlyApproved;
	}

	public void setPushFreeTextAllowed(boolean isPushFreeTextAllowed) {
		this.isPushFreeTextAllowed = isPushFreeTextAllowed;
	}

	public void setPushToNewContactAllowed(boolean isPushToNewContactAllowed) {
		this.isPushToNewContactAllowed = isPushToNewContactAllowed;
	}

	public boolean isPushLocalStore() {
		return pushLocalStore;
	}

	public void setPushLocalStore(boolean pushLocalStore) {
		this.pushLocalStore = pushLocalStore;
	}

	public ClickPostConfigDetails getClickpost() {
		return clickpost;
	}

	public void setClickpost(ClickPostConfigDetails clickpost) {
		this.clickpost = clickpost;
	}

	@Override
	public boolean isWebhookManual() {
		return this.isWebhookManual;
	}

	public void setWebhookManual(boolean isWebhookManual) {
		this.isWebhookManual = isWebhookManual;
	}

	public EmailConfigDetails getEmail() {
		return email;
	}

	public void setEmail(EmailConfigDetails email) {
		this.email = email;
	}

	public TwilioConfigDetails getTwilio() {
		return twilio;
	}

	public void setTwilio(TwilioConfigDetails twilio) {
		this.twilio = twilio;
	}

	public Object getError() {
		return error;
	}

	public void setError(Object error) {
		this.error = error;
	}

	public SMSConfigDetails getSms() {
		return sms;
	}

	public void setSms(SMSConfigDetails sms) {
		this.sms = sms;
	}

	public RCSConfigDetails getRcs() {
		return rcs;
	}

	public void setRcs(RCSConfigDetails rcs) {
		this.rcs = rcs;
	}

	public WA360CloudConfigDetails getWa360dc() {
		return wa360dc;
	}

	public void setWa360dc(WA360CloudConfigDetails wa360dc) {
		this.wa360dc = wa360dc;
	}

	public OAConfigDetails getOa() {
		return oa;
	}

	public void setOa(OAConfigDetails oa) {
		this.oa = oa;
	}

	public String getUnhandledInboundForward() {
		return unhandledInboundForward;
	}

	public void setUnhandledInboundForward(String unhandledInboundForward) {
		this.unhandledInboundForward = unhandledInboundForward;
	}

	public FirebaseConfigDetails getFirebase() {
		return firebase;
	}

	public void setFirebase(FirebaseConfigDetails firebase) {
		this.firebase = firebase;
	}

	public WACFBConfigDetails getWacfb() {
		return wacfb;
	}

	public void setWacfb(WACFBConfigDetails wacfb) {
		this.wacfb = wacfb;
	}

	public boolean isMaster() {
		return isMaster;
	}

	public void setMaster(boolean isMaster) {
		this.isMaster = isMaster;
	}

	public boolean isAutoCreated() {
		return isAutoCreated;
	}

	public void setAutoCreated(boolean isAutoCreated) {
		this.isAutoCreated = isAutoCreated;
	}

	public AChannelDetails details() {
		switch (getChannelType()) {
		case CHANNEL_TYPE.WACFB:
			return this.getWacfb();
		case CHANNEL_TYPE.WA_360DC:
			return this.getWa360dc();
		case CHANNEL_TYPE.WA_360D:
			return this.getWa360d();
		case CHANNEL_TYPE.WEB:
			return this.getWeb();
		case CHANNEL_TYPE.FACEBOOK:
			return this.getFacebook();
		case CHANNEL_TYPE.TWITTER:
			return this.getTwitter();
		case CHANNEL_TYPE.TELEGRAM:
			return this.getTelegram();
		case CHANNEL_TYPE.OUTLOOK:
			return this.getOutlook();
		case CHANNEL_TYPE.IMAP:
			return this.getImap();
		case CHANNEL_TYPE.GMAIL:
			return this.getGmail();
		case CHANNEL_TYPE.CLICKPOST:
			return this.getClickpost();
		case CHANNEL_TYPE.RCS_TELINFY:
			return this.getRcs();
		default:
			return null;
		}
	}

	public OutlookConfigDetails getOutlook() {
		return outlook;
	}

	public void setOutlook(OutlookConfigDetails outlook) {
		this.outlook = outlook;
	}

	public Map<String, Object> getMeta() {
		return meta;
	}

	public Map<String, Object> meta() {
		if (!ArgUtil.is(this.meta)) {
			this.meta = new HashMap<String, Object>();
		}
		return this.meta;
	}

	public void setMeta(Map<String, Object> meta) {
		this.meta = meta;
	}

	public String getChannelConfigTempId() {
		return channelConfigTempId;
	}

	public void setChannelConfigTempId(String channelConfigTempId) {
		this.channelConfigTempId = channelConfigTempId;
	}

	public String getMasterChannelId() {
		return masterChannelId;
	}

	public void setMasterChannelId(String masterChannelId) {
		this.masterChannelId = masterChannelId;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getDomainProxy() {
		return domainProxy;
	}

	public void setDomainProxy(String domainProxy) {
		this.domainProxy = domainProxy;
	}

	public GmailConfigDetails getGmail() {
		return gmail;
	}

	public void setGmail(GmailConfigDetails gmail) {
		this.gmail = gmail;
	}

	public ImapConfigDetails getImap() {
		return imap;
	}

	public void setImap(ImapConfigDetails imap) {
		this.imap = imap;
	}

	public PushAppConfigDetails getPushapp() {
		return pushapp;
	}

	public void setPushapp(PushAppConfigDetails pushapp) {
		this.pushapp = pushapp;
	}

	public String getSublane() {
		return sublane;
	}

	public void setSublane(String sublane) {
		this.sublane = sublane;
	}

	public void setInboundAllowed(boolean isInboundAllowed) {
		this.isInboundAllowed = isInboundAllowed;
	}

	public boolean isInboundAllowed() {
		return isInboundAllowed;
	}

	public boolean isOutboundAllowed() {
		return isOutboundAllowed;
	}

	public void setOutboundAllowed(boolean isOutboundAllowed) {
		this.isOutboundAllowed = isOutboundAllowed;
	}
}
