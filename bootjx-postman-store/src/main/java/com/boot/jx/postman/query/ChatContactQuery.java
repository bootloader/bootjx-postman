package com.boot.jx.postman.query;

import com.boot.jx.mongo.CommonMongoQueryBuilder.DocQueryBuilder;
import com.boot.jx.postman.PMConstants;
import com.boot.jx.postman.doc.ChatContactDoc;
import com.boot.jx.postman.dto.VCallMeta;
import com.boot.jx.postman.model.MessageDefinitions.Contactable;
import com.boot.utils.ArgUtil;
import com.boot.utils.TimeUtils;

public class ChatContactQuery extends DocQueryBuilder<ChatContactDoc> {

	public static final long MASTER_SYNC_UPDATE = 1000 * 60 * 60 * 24 * 7;

	public ChatContactQuery(ChatContactDoc doc) {
		super(doc);
	}

	public ChatContactQuery(String contactId) {
		super(contactId);
	}

	@Override
	public String getId(ChatContactDoc doc) {
		return doc.getContactId();
	}

	@Override
	public ChatContactDoc newDoc(String id) {
		ChatContactDoc doc = new ChatContactDoc();
		doc.setContactId(id);
		return doc;
	}

	public String getName() {
		return this.getDoc().getName();
	}

	public String getPhone() {
		return this.getDoc().phone();
	}

	public String getEmail() {
		return this.getDoc().getEmail();
	}

	public String getLang() {
		return this.getDoc().prefs().getLang();
	}

	public Object get(String key) {
		return this.doc.store().get(key);
	}

	public ChatContactQuery put(String key, String object) {
		this.doc.store().put(key, object);
		this.set("store." + key, object);
		return this;
	}

	public ChatContactQuery remove(String key) {
		this.doc.store().remove(key);
		this.unset("store." + key);
		return this;
	}

	public ChatContactQuery setLang(String lang) {
		this.doc.prefs().setLang(lang);
		this.set("prefs.lang", lang);
		return this;
	}

	public ChatContactQuery setLastInBoundStamp(long timestamp) {
		this.doc.setLastInBoundStamp(timestamp);
		this.set("lastInBoundStamp", timestamp);
		return this;
	}

	public ChatContactQuery setLastOutBoundStamp(long timestamp) {
		this.doc.setLastOutBoundStamp(timestamp);
		this.set("lastOutBoundStamp", timestamp);
		return this;
	}

	public ChatContactQuery setContactId(String contactId) {
		if (ArgUtil.is(this.doc.getContactId(), contactId)) {
			return this;
		}
		this.doc.setContactId(contactId);
		this.set("contactId", contactId);
		return this;
	}

	public ChatContactQuery setContactType(String contactType) {
		if (ArgUtil.is(this.doc.getContactType(), contactType)) {
			return this;
		}
		this.doc.setContactType(contactType);
		this.set("contactType", contactType);
		return this;
	}

	public ChatContactQuery setChannelType(String channelType) {
		if (ArgUtil.is(this.doc.getChannelType(), channelType)) {
			return this;
		}
		this.doc.setChannelType(channelType);
		this.set("channelType", channelType);
		return this;
	}

	public ChatContactQuery setCsid(String csid) {
		if (ArgUtil.is(this.doc.getCsid(), csid)) {
			return this;
		}
		this.doc.setCsid(csid);
		this.set("csid", csid);
		return this;
	}

	public ChatContactQuery setLane(String lane) {
		if (ArgUtil.is(this.doc.getLane(), lane)) {
			return this;
		}
		this.doc.setLane(lane);
		this.set("lane", lane);
		return this;
	}

	public ChatContactQuery setSessionId(String sessionId) {
		if (ArgUtil.is(this.doc.getSessionId(), sessionId)) {
			return this;
		}
		this.doc.setSessionId(sessionId);
		this.set("sessionId", sessionId);
		return this;
	}

	public ChatContactQuery setLastOptInStamp(long lastOptInStamp) {
		this.doc.setLastOptInStamp(lastOptInStamp);
		this.set("lastOptInStamp", lastOptInStamp);
		return this;
	}

	public ChatContactQuery setChannel(String channel) {
		if (ArgUtil.is(this.doc.getChannelType(), channel)) {
			return this;
		}
		this.doc.setChannelType(channel);
		this.set("channel", channel);
		return this;
	}

	public ChatContactQuery setLastPushStamp(long lastPushStamp) {
		this.doc.setLastPushStamp(lastPushStamp);
		this.set("lastPushStamp", lastPushStamp);
		return this;
	}

	public ChatContactQuery setLastReplyStamp(long lastReplyStamp) {
		this.doc.setLastReplyStamp(lastReplyStamp);
		this.set("lastReplyStamp", lastReplyStamp);
		return this;
	}

	public ChatContactQuery setLastDAUStamp(String lastDAUStamp) {
		this.doc.setLastDAUStamp(lastDAUStamp);
		this.set("lastDAUStamp", lastDAUStamp);
		return this;
	}

	public ChatContactQuery setLastMAUStamp(String lastMAUStamp) {
		this.doc.setLastMAUStamp(lastMAUStamp);
		this.set("lastMAUStamp", lastMAUStamp);
		return this;
	}

	public ChatContactQuery setFirstOutBoundStamp(long firstOutBoundStamp) {
		this.doc.setFirstOutBoundStamp(firstOutBoundStamp);
		this.set("firstOutBoundStamp", firstOutBoundStamp);
		return this;
	}

	public ChatContactQuery setFirstInBoundStamp(long firstInBoundStamp) {
		this.doc.setFirstInBoundStamp(firstInBoundStamp);
		this.set("firstInBoundStamp", firstInBoundStamp);
		return this;
	}

	public ChatContactQuery setName(String name) {
		if (ArgUtil.is(this.doc.getName(), name)) {
			return this;
		}
		this.doc.setName(name);
		this.set("name", name);
		return this;
	}

	public ChatContactQuery setEmail(String email) {
		if (ArgUtil.is(this.doc.getEmail(), email)) {
			return this;
		}
		this.doc.setEmail(email);
		this.set("email", email);
		return this;
	}

	public ChatContactQuery setPhone(String phone) {
		if (ArgUtil.is(this.doc.getPhone(), phone)) {
			return this;
		}
		this.doc.phone(phone);
		this.set("phone", phone);
		return this;
	}

	public ChatContactQuery setProfileId(String profileId) {
		if (ArgUtil.is(this.doc.getProfileId(), profileId)) {
			return this;
		}
		this.doc.setProfileId(profileId);
		this.set("profileId", profileId);
		return this;
	}

	public ChatContactQuery setUserToken(String userToken) {
		if (ArgUtil.is(this.doc.user().getToken(), userToken)) {
			return this;
		}
		this.doc.user().setToken(userToken);
		this.set("user.token", userToken);
		return this;
	}

	public ChatContactQuery setUserCode(String userCode) {
		if (ArgUtil.is(this.doc.user().getCode(), userCode)) {
			return this;
		}
		this.doc.user().setCode(userCode);
		this.set("user.code", userCode);
		return this;
	}

	public ChatContactQuery setPhoneVerified(boolean phoneVerified) {
		this.doc.setPhoneVerified(true);
		this.set("phoneVerified", phoneVerified);
		return this;
	}

	public ChatContactQuery setEmailVerified(boolean emailVerified) {
		if (ArgUtil.is(this.doc.getEmailVerified(), emailVerified)) {
			return this;
		}
		this.doc.setEmailVerified(true);
		this.set("emailVerified", emailVerified);
		return this;
	}

	public ChatContactQuery setProfilePic(String profilePic) {
		if (ArgUtil.is(this.doc.getProfilePic(), profilePic)) {
			return this;
		}
		this.doc.setProfilePic(profilePic);
		this.set("profilePic", profilePic);
		return this;
	}

	public ChatContactQuery setVCallMeta(VCallMeta vCallMeta) {
		this.doc.setVCallMeta(vCallMeta);
		this.set("vCallMeta", vCallMeta);
		this.unset("isCallAllowed");
		return this;
	}

	public void updateLastOptInStamp() {
		long optinStamp = System.currentTimeMillis();
		this.doc.setLastOptInStamp(optinStamp);
		this.set("lastOptInStamp", optinStamp);
	}

	public void updateCreatedStamp() {
		long optinStamp = System.currentTimeMillis();
		this.doc.setCreatedStamp(optinStamp);
		update().setOnInsert("createdStamp", optinStamp);
	}

	public void updateFirstInBoundStamp() {
		long optinStamp = System.currentTimeMillis();
		this.doc.setFirstInBoundStamp(optinStamp);
		update().setOnInsert("firstInBoundStamp", optinStamp);
	}

	public ChatContactQuery update(Contactable contactable) {
		if (ArgUtil.is(contactable.getContactId())) {
			this.setContactId(ArgUtil.nonEmpty(contactable.getContactId(), this.doc.getContactId()));
		}
		if (ArgUtil.is(contactable.getContactType())) {
			this.setContactType(ArgUtil.nonEmpty(contactable.getContactType(), this.doc.getContactType()));
		}
		if (ArgUtil.is(contactable.getChannelType())) {
			this.setChannel(ArgUtil.nonEmpty(contactable.getChannelType(), this.doc.getChannelType()));
		}

		if (ArgUtil.is(contactable.getContactType()) || ArgUtil.is(contactable.getChannelType())) {
			this.setChannelType(ArgUtil.nonEmpty(
					PMConstants.CHANNEL_TYPE(contactable.getContactType(), contactable.getChannelType()),
					this.doc.getChannelType()));
		}

		if (ArgUtil.is(contactable.getCsid())) {
			this.setCsid(ArgUtil.nonEmpty(contactable.getCsid(), this.doc.getCsid()));
		}
		if (ArgUtil.is(contactable.getLane())) {
			this.setLane(ArgUtil.nonEmpty(contactable.getLane(), this.doc.getLane()));
		}
		if (ArgUtil.is(contactable.getName())) {
			this.setName(ArgUtil.nonEmpty(contactable.getName(), this.doc.getName()));
		}

		if (ArgUtil.is(contactable.getEmail())) {
			this.setEmail(ArgUtil.nonEmpty(contactable.getEmail(), this.doc.getEmail()));
		}

		if (ArgUtil.is(contactable.phone())) {
			this.setPhone(ArgUtil.nonEmpty(contactable.phone(), this.doc.phone()));
		}

		this.updateMasterSyncStamp();

		return this;
	}

	public ChatContactQuery setInfoEmail(String email) {
		if (ArgUtil.is(this.doc.info().getEmail(), email)) {
			return this;
		}
		this.doc.info().setEmail(email);
		this.set("info.email", email);
		return this;
	}

	public ChatContactQuery setInfoPhone(String phone) {
		if (ArgUtil.is(this.doc.info().getPhone(), phone)) {
			return this;
		}
		this.doc.info().setPhone(phone);
		this.set("info.phone", phone);
		return this;
	}

	public ChatContactQuery setInfoName(String name) {
		if (ArgUtil.is(this.doc.info().getName(), name)) {
			return this;
		}
		this.doc.info().setName(name);
		this.set("info.name", name);
		return this;
	}

	public ChatContactQuery updateMasterSyncStamp() {
		if (!ArgUtil.is(this.doc)) {
			return this;
		}

		if (!TimeUtils.isExpired(this.doc.getMasterSyncStamp(), MASTER_SYNC_UPDATE)) {
			return this;
		}
		long masterSyncStamp = System.currentTimeMillis();
		this.doc.setMasterSyncStamp(masterSyncStamp);
		this.set("masterSyncStamp", masterSyncStamp);
		return this;
	}

}
