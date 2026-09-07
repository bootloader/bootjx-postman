package com.boot.jx.postman.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.boot.jx.dict.ContactType;
import com.boot.jx.postman.PMConstants.CHAT_ASSIGN_GROUP;
import com.boot.jx.postman.PMConstants.CHAT_MODE;
import com.boot.jx.postman.PMConstants.CHAT_STATE;
import com.boot.jx.postman.PMConstants.CHAT_STATUS;
import com.boot.jx.postman.doc.QuickTag;
import com.boot.utils.ArgUtil;
import com.boot.utils.StringUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionSearchQuery {

	public static enum TAG_TYPE {
		IN // Me,Team,Others
		, ON // Channels
		, IS // State
		, TO // AGENT,BOT,WEBHOOK
		, STATUS // Status
	}

	public String text;
	public String textSearch;
	public TreeSet<CHAT_ASSIGN_GROUP> tabs;
	public TreeSet<ContactType> contactTypes;
	public TreeSet<CHAT_STATE> states;
	public TreeSet<CHAT_STATUS> status;
	public TreeSet<CHAT_MODE> modes;
	public TreeSet<QuickTag> tags;
	public TreeSet<String> channels;

	public long fromStamp;
	public long toStamp;
	public int limit;

	public String agentCode;
	public String agentDept;

	public String sortBy;
	public String sortDir;
	public int pageNo;
	public int pageSize;
	public boolean count;
	public String assignToSearch;

	public boolean archive = false;

	public TreeSet<CHAT_ASSIGN_GROUP> tabs() {
		if (tabs == null) {
			this.tabs = new TreeSet<CHAT_ASSIGN_GROUP>();
		}
		return this.tabs;
	}

	public TreeSet<CHAT_STATE> states() {
		if (states == null) {
			this.states = new TreeSet<CHAT_STATE>();
		}
		return this.states;
	}

	public TreeSet<CHAT_STATUS> status() {
		if (status == null) {
			this.status = new TreeSet<CHAT_STATUS>();
		}
		return this.status;
	}

	public TreeSet<QuickTag> tags() {
		if (tags == null) {
			this.tags = new TreeSet<QuickTag>();
		}
		return this.tags;
	}

	public TreeSet<ContactType> contactTypes() {
		if (contactTypes == null) {
			this.contactTypes = new TreeSet<ContactType>();
		}
		return this.contactTypes;
	}

	public TreeSet<String> channels() {
		if (channels == null) {
			this.channels = new TreeSet<String>();
		}
		return this.channels;
	}

	public TreeSet<CHAT_MODE> modes() {
		if (modes == null) {
			this.modes = new TreeSet<CHAT_MODE>();
		}
		return this.modes;
	}

	public boolean contains(CHAT_ASSIGN_GROUP tab) {
		return this.tabs().contains(tab);
	}

	public boolean containsAll(CHAT_ASSIGN_GROUP... tabs) {
		for (CHAT_ASSIGN_GROUP tab : tabs) {
			if (!this.tabs().contains(tab)) {
				return false;
			}
		}
		return true;
	}

	public boolean containsAny(CHAT_ASSIGN_GROUP... tabs) {
		for (CHAT_ASSIGN_GROUP tab : tabs) {
			if (this.contains(tab)) {
				return true;
			}
		}
		return false;
	}

	public boolean contains(CHAT_STATE state) {
		return this.states().contains(state);
	}

	public boolean containsAny(CHAT_STATE... state) {
		for (CHAT_STATE chat_STATE : state) {
			if (this.states().contains(chat_STATE)) {
				return true;
			}
		}
		return false;
	}

	public boolean contains(CHAT_STATUS status) {
		return this.status().contains(status);
	}

	public boolean contains(CHAT_MODE mode) {
		return this.modes().contains(mode);
	}

	public boolean containsAny(CHAT_MODE... modes) {
		for (CHAT_MODE mode : modes) {
			if (this.contains(mode)) {
				return true;
			}
		}
		return false;
	}

	public boolean contains(ContactType contactType) {
		return this.contactTypes().contains(contactType);
	}

	public SessionSearchQuery add(CHAT_ASSIGN_GROUP tab) {
		if (ArgUtil.is(tab)) {
			this.tabs().add(tab);
		}
		return this;
	}

	public SessionSearchQuery add(CHAT_STATE state) {
		if (ArgUtil.is(state)) {
			this.states().add(state);
		}
		return this;
	}

	public SessionSearchQuery add(CHAT_STATUS status) {
		if (ArgUtil.is(status)) {
			this.status().add(status);
		}
		return this;
	}

	public SessionSearchQuery add(ContactType contactType) {
		if (ArgUtil.is(contactType)) {
			this.contactTypes().add(contactType);
		}
		return this;
	}

	public SessionSearchQuery add(CHAT_MODE mode) {
		if (ArgUtil.is(mode)) {
			this.modes().add(mode);
		}
		return this;
	}

	public SessionSearchQuery parse(String query) {

		List<String> tokens = Arrays.stream(StringUtils.split(query, "\\s")).filter(token -> ArgUtil.is(token))
				.collect(Collectors.toCollection(ArrayList::new));

		StringJoiner sj = new StringJoiner(" ");

		for (String tokenStr : tokens) {
			String[] tokenStrs = tokenStr.split(":");

			if (tokenStrs.length > 1) {
				TAG_TYPE tagType = ArgUtil.parseAsEnumT(tokenStrs[0], TAG_TYPE.class);

				if (ArgUtil.isEqual(tagType, null, TAG_TYPE.IN)) {
					CHAT_ASSIGN_GROUP tab = ArgUtil.parseAsEnumT(tokenStrs[1], CHAT_ASSIGN_GROUP.class);
					if (tab != null) {
						this.tabs().add(tab);
					}
				}

				if (ArgUtil.isEqual(tagType, null, TAG_TYPE.ON)) {
					ContactType type = ArgUtil.parseAsEnumT(tokenStrs[1], ContactType.class);
					if (type != null) {
						this.contactTypes().add(type);
					}
				}

				if (ArgUtil.isEqual(tagType, null, TAG_TYPE.IS)) {
					CHAT_STATE state = ArgUtil.parseAsEnumT(tokenStrs[1], CHAT_STATE.class);
					if (state != null) {
						this.states().add(state);
					}
				}

				if (ArgUtil.isEqual(tagType, null, TAG_TYPE.IS)) {
					CHAT_STATUS status = ArgUtil.parseAsEnumT(tokenStrs[1], CHAT_STATUS.class);
					if (status != null) {
						this.status().add(status);
					}
				}

				if (ArgUtil.isEqual(tagType, null, TAG_TYPE.TO)) {
					CHAT_MODE status = ArgUtil.parseAsEnumT(tokenStrs[1], CHAT_MODE.class);
					if (status != null) {
						this.modes().add(status);
					}
				}

			} else {
				sj.add(tokenStr);
			}

			this.text = sj.toString().replace("*", "").trim();;

		}
		return this;
	}

	// SHORTCUTS

	public boolean hasClosed() {
		return this.contains(CHAT_STATE.CLOSED) || this.contains(CHAT_STATUS.CLOSED);
	}

	public boolean hasExpired() {
		return this.contains(CHAT_STATE.EXPIRED) || this.contains(CHAT_STATUS.EXPIRED);
	}

}