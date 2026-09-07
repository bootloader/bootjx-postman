package com.boot.jx.postman.model;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.boot.jx.AppParam;
import com.boot.jx.dict.ContactType;
import com.boot.jx.dict.Language;
import com.boot.jx.postman.PostManException;
import com.boot.jx.scope.tnt.TenantContextHolder;
import com.boot.jx.scope.tnt.Tenants;
import com.boot.utils.ArgUtil;

public class PushMessage extends Message<PushMessage> {

	String pattern = "yyyy-MM-dd";
	SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

	public static final String TOPICS_PREFIX = "/topics/";
	private static final String FORMAT_TO_ALL = "%s-%s-all-%s";
	private static final String FORMAT_TO_NATIONALITY = "%s-%s-nationality-%s-%s";
	private static final String FORMAT_TO_USER = "%s-%s-user-%s-%s";
	private static final String FORMAT_TO_DATE = "%s-%s-%s-date-%s";
	private static final String FORMAT_TO_KEY = "%s-%s-key-%s-%s-%s";
	private static final String FORMAT_TO_FILTER = "%s-%s-filter-%s-%s";
	public static final String CONDITION_SEPRATOR = " || ";
	public static final String CONDITION_SEPRATOR_AND = " && ";

	public static final Pattern FORMAT_TO_ALL_PATTERN = Pattern.compile("/topics/(.+)-all$");
	public static final Pattern FORMAT_TO_NATIONALITY_PATTERN = Pattern.compile("/topics/(.+)-nationality-(.+)$");
	public static final Pattern FORMAT_TO_USER_PATTERN = Pattern.compile("/topics/(.+)-user-(.+)$");

	public static final Pattern FORMAT_TO_ALL_PATTERN_V2 = Pattern.compile("/topics/(.+)-(.+)-all$");
	public static final Pattern FORMAT_TO_NATIONALITY_PATTERN_V2 = Pattern
			.compile("/topics/(.+)-(.+)-nationality-(.+)$");
	public static final Pattern FORMAT_TO_USER_PATTERN_V2 = Pattern.compile("/topics/(.+)-(.+)-user-(.+)$");

	public static final Pattern FORMAT_TO_ALL_PATTERN_V3 = Pattern.compile("/topics/(.+)-(.+)-all-(.+)$");
	public static final Pattern FORMAT_TO_NATIONALITY_PATTERN_V3 = Pattern
			.compile("/topics/(.+)-(.+)-nationality-(.+)-(.+)$");
	public static final Pattern FORMAT_TO_USER_PATTERN_V3 = Pattern.compile("/topics/(.+)-(.+)-user-(.+)-(.+)$");

	public static final Pattern FORMAT_TO_KEY_PATTERN_V3 = Pattern.compile("/topics/(.+)-(.+)-key-(.+)-(.+)-(.+)$");

	private static final long serialVersionUID = -1354844357577261297L;
	private static final String ANY_VALUE = "~";

	Object result = null;

	String image = null;
	String link = null;

	boolean condition;

	public PushMessage() {
		super(ContactType.PUSH);
		this.condition = false;
	}

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public static String topic(String key, Object value) {
		return String.format(PushMessage.FORMAT_TO_FILTER, AppParam.APP_ENV.getValue(),
				TenantContextHolder.currentSite().toString(), key, ArgUtil.parseAsString(value)).toLowerCase();
	}

	public static String topicPath(String key, Object value) {
		return TOPICS_PREFIX + topic(key, value);
	}

	public void addToFilter(String key, Object value) {
		this.addTo(topicPath(key, value));
	}

	public void addToFilter(String key, Date date) {
		this.addTo(TOPICS_PREFIX + topic(key, simpleDateFormat.format(date)));
	}

	public void addTopic(String topic) {
		this.addTo(TOPICS_PREFIX + topic);
	}

	public void addToDate(String prefix, Date date) {
		if (ArgUtil.is(date)) {
			this.addTo(TOPICS_PREFIX
					+ String.format(FORMAT_TO_DATE, AppParam.APP_ENV.getValue(), TenantContextHolder.currentSite(),
							prefix, simpleDateFormat.format(date)).toLowerCase().replaceAll("\\s+", ""));
		}
	}

	public void addToTenant(String tenant, Language lang) {
		this.addTopic(String.format(PushMessage.FORMAT_TO_ALL, AppParam.APP_ENV.getValue(), tenant.toString(),
				Language.toString(lang, ANY_VALUE)).toLowerCase());
	}

	public void addToTenant(String tenant) {
		this.addToTenant(tenant, null);
	}

	public void addToEveryone() {
		this.addToTenant(TenantContextHolder.currentSite());
	}

	public void addToUser(BigDecimal userid, Language lang) {
		this.addTo(TOPICS_PREFIX
				+ String.format(FORMAT_TO_USER, AppParam.APP_ENV.getValue(), TenantContextHolder.currentSite(), userid,
						Language.toString(lang, ANY_VALUE)).toLowerCase().replaceAll("\\s+", ""));
	}

	public void addToUser(BigDecimal userid) {
		this.addToUser(userid, null);
	}

	public void addToCountry(String tenant, Object nationalityId, Language lang) {
		this.addTo(TOPICS_PREFIX + String.format(PushMessage.FORMAT_TO_NATIONALITY, AppParam.APP_ENV.getValue(),
				tenant.toString(), nationalityId, Language.toString(lang, ANY_VALUE)).toLowerCase());
	}

	public void addToCountry(String tenant, Object nationalityId) {
		addToCountry(tenant, nationalityId, null);
	}

	public void addToCountry(Object nationalityId) {
		addToCountry(TenantContextHolder.currentSite(), nationalityId);
	}

	public void addToCountry(Object nationalityId, Language lang) {
		addToCountry(TenantContextHolder.currentSite(), nationalityId, lang);
	}

	public void addToKey(String key, String value, Language lang) {
		this.addTo(TOPICS_PREFIX + String
				.format(PushMessage.FORMAT_TO_KEY, AppParam.APP_ENV.getValue(),
						TenantContextHolder.currentSite().toString(), key, value, Language.toString(lang, ANY_VALUE))
				.toLowerCase());
	}

	public void addToKey(String key, String value) {
		addToKey(key, value, null);
	}

	public boolean isCondition() {
		return condition;
	}

	public void setCondition(boolean condition) {
		this.condition = condition;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	String topicForAndroid;
	String topicForIos;
	String topicForWeb;
	private int totalConditions;

	public void toTopic() {
		topicForAndroid = null;
		topicForIos = null;
		topicForWeb = null;
		totalConditions = 0;

		if (ArgUtil.isEmpty(this.getContacts())) {
			throw new PostManException(PostManException.ErrorCode.NO_RECIPIENT_DEFINED);
		}

		if (this.getContacts().size() > 0) {
			StringJoiner orCondition = new StringJoiner(") || (");
			int totalOrConditions = 0;
			for (ContactMeta singleContact : this.getContacts()) {
				for (Map<String, Object> singleFilter : singleContact.getFilters()) {
					StringJoiner andCondition = new StringJoiner(PushMessage.CONDITION_SEPRATOR_AND);
					for (Entry<String, Object> entry : singleFilter.entrySet()) {
						andCondition.add("'" + PushMessage.topic(entry.getKey(), entry.getValue()) + "%sx%' in topics");
						totalConditions++;
					}
					// totalConditions++;
					totalOrConditions++;
					orCondition.add(andCondition.toString());
				}
			}
			String orConditionStr = orCondition.toString();
			if (totalOrConditions > 1) {
				orConditionStr = "(" + orConditionStr + ")";
			}
			topicForAndroid = orConditionStr.toString().replaceAll("%sx%", "_and");
			topicForIos = orConditionStr.toString().replaceAll("%sx%", "_ios");
			topicForWeb = orConditionStr.toString().replaceAll("%sx%", "_web");

			this.setCondition(true);
		}

		if (totalConditions > 5) {
			throw new PostManException(PostManException.ErrorCode.TOO_MANY_CONDITIONS);
		}
	}

	public static ContactMeta toContactV2(String topic) {
		ContactMeta c = new ContactMeta();

		Matcher m = PushMessage.FORMAT_TO_USER_PATTERN_V2.matcher(topic);
		if (m.find()) {
			c.setUserid(m.group(3));
			String tenant = Tenants.fromAsString(m.group(2), Tenants.DEFAULT);
			c.setTenant(tenant);
		} else {
			m = PushMessage.FORMAT_TO_NATIONALITY_PATTERN_V2.matcher(topic);
			if (m.find()) {
				c.setCountry(m.group(3));
				String tenant = Tenants.fromAsString(m.group(2), Tenants.DEFAULT);
				c.setTenant(tenant);
			} else {
				m = PushMessage.FORMAT_TO_ALL_PATTERN_V2.matcher(topic);
				if (m.find()) {
					String tenant = Tenants.fromAsString(m.group(2), Tenants.DEFAULT);
					c.setTenant(tenant);
				} else {
					return toContactV1(topic);
				}
			}
		}
		return c;
	}

	private static ContactMeta toContactV1(String topic) {
		ContactMeta c = new ContactMeta();

		Matcher m = PushMessage.FORMAT_TO_USER_PATTERN.matcher(topic);
		if (m.find()) {
			c.setUserid(m.group(2));
			String tenant = Tenants.fromAsString(m.group(1), Tenants.DEFAULT);
			c.setTenant(tenant);
		} else {
			m = PushMessage.FORMAT_TO_NATIONALITY_PATTERN.matcher(topic);
			if (m.find()) {
				c.setCountry(m.group(2));
				String tenant = Tenants.fromAsString(m.group(1), Tenants.DEFAULT);
				c.setTenant(tenant);
			} else {
				m = PushMessage.FORMAT_TO_ALL_PATTERN.matcher(topic);
				if (m.find()) {
					String tenant = Tenants.fromAsString(m.group(1), Tenants.DEFAULT);
					c.setTenant(tenant);
				}
			}
		}
		return c;
	}

	public String getTopicForAndroid() {
		return topicForAndroid;
	}

	public void setTopicForAndroid(String topicForAndroid) {
		this.topicForAndroid = topicForAndroid;
	}

	public String getTopicForIos() {
		return topicForIos;
	}

	public void setTopicForIos(String topicForIos) {
		this.topicForIos = topicForIos;
	}

	public String getTopicForWeb() {
		return topicForWeb;
	}

	public void setTopicForWeb(String topicForWeb) {
		this.topicForWeb = topicForWeb;
	}

	public int getTotalConditions() {
		return totalConditions;
	}

	public void setTotalConditions(int totalConditions) {
		this.totalConditions = totalConditions;
	}
}
