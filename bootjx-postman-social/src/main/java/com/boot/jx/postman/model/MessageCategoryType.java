package com.boot.jx.postman.model;

public class MessageCategoryType {

	@Deprecated
	public static final String SOA = "SOA";
	@Deprecated
	public static final String IT = "IT";

	// MessageTypes
	public static final String ACCOUNT_UPDATE = "ACCOUNT_UPDATE";
	public static final String ALERT_UPDATE = "ALERT_UPDATE";
	public static final String APPOINTMENT_UPDATE = "APPOINTMENT_UPDATE";
	public static final String AUTO_REPLY = "AUTO_REPLY";

	public static final String ISSUE_RESOLUTION = "ISSUE_RESOLUTION";
	public static final String PAYMENT_UPDATE = "PAYMENT_UPDATE";
	public static final String PERSONAL_FINANCE_UPDATE = "PERSONAL_FINANCE_UPDATE";
	public static final String RESERVATION_UPDATE = "RESERVATION_UPDATE";

	public static final String SHIPPING_UPDATE = "SHIPPING_UPDATE";
	public static final String TICKET_UPDATE = "TICKET_UPDATE";
	public static final String TRANSPORTATION_UPDATE = "TRANSPORTATION_UPDATE";

	// Extra
	public static final String HUMAN_AGENT = "HUMAN_AGENT";
	public static final String CUSTOMER_FEEDBACK = "CUSTOMER_FEEDBACK";

}