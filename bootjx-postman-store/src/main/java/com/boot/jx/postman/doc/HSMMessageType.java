package com.boot.jx.postman.doc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Reference;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.boot.jx.model.AuditCreateEntity;
import com.boot.jx.mongo.CommonDocInterfaces.OldDocVersion;

@Document(collection = HSMMessageType.COLLECTION_NAME)
@TypeAlias("HSMMessageType")
public class HSMMessageType implements Serializable, OldDocVersion<HSMMessageType>, AuditCreateEntity {

	public static final List<HSMMessageType> LIST = new ArrayList<HSMMessageType>();
	public static final String COLLECTION_NAME = "DICT_HSM_MESSAGE_TYPE";

	private static final long serialVersionUID = 5953299041958788771L;

	@Id
	private String id;

	@Indexed(unique = true)
	private String label;

	private String desc;

	private String icon;

	@Field("oldVersions")
	@Reference
	private List<HSMMessageType> oldVersions;

	private String createdBy;
	private Long createdStamp;

	@Override
	public List<HSMMessageType> getOldVersions() {
		return oldVersions;
	}

	@Override
	public void setOldVersions(List<HSMMessageType> oldVersions) {
		this.oldVersions = oldVersions;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public Long getCreatedStamp() {
		return createdStamp;
	}

	public void setCreatedStamp(Long createdStamp) {
		this.createdStamp = createdStamp;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	private HSMMessageType id(String id) {
		this.id = id;
		return this;
	}

	private HSMMessageType label(String label) {
		this.label = label;
		return this;
	}

	private HSMMessageType desc(String desc) {
		this.desc = desc;
		return this;
	}

	private HSMMessageType icon(String icon) {
		this.icon = icon;
		return this;
	}

	static {

		LIST.add(new HSMMessageType().id("account_update").label("Account Update")
				.desc("Let customers know about updates or changes to their account.").icon("fa fa-cog"));

		LIST.add(new HSMMessageType().id("alert_update").label("Alert Update")
				.desc("Send important updates or news to customers.").icon("fa fa-bell"));

		LIST.add(new HSMMessageType().id("appointment_update").label("Appointment Update")
				.desc("Send confirmations, reminders or other updates to customers about their appointments.")
				.icon("fa fa-calendar"));

		LIST.add(new HSMMessageType().id("auto_reply").label("Auto Reply")
				.desc("Send auto-replies to customers when your business isn't open").icon("fa fa-reply"));

		LIST.add(new HSMMessageType().id("issue_resolution").label("Issue Resolution")
				.desc("Respond to questions, concerns or feedback from customers about your business.")
				.icon("far fa-meh"));

		LIST.add(new HSMMessageType().id("payment_update").label("Payment Update")
				.desc("Send a message to customers about their payment.").icon("fa fa-credit-card"));

		LIST.add(new HSMMessageType().id("personal_finance_update").label("Personal Finance Update")
				.desc("Send a message to customers about their personal finances.").icon("fa fa-dollar-sign "));

		LIST.add(new HSMMessageType().id("reservation_update").label("Reservation Update")
				.desc("Send confirmations, reminders or other updates to customers about their reservations.")
				.icon("far fa-calendar"));

		LIST.add(new HSMMessageType().id("shipping_update").label("Shipping Update")
				.desc("Send shipping updates to customers about their orders.").icon("fa fa-truck"));

		LIST.add(new HSMMessageType().id("ticket_update").label("Ticket Update")
				.desc("Send ticketing information or updates to customers.").icon("fa fa-ticket"));

		LIST.add(new HSMMessageType().id("transportation_update").label("Transportation Update")
				.desc("Send transportation information or updates to customers.").icon("fa fa-plane"));

	}

	public static List<HSMMessageType> values() {
		return LIST;
	}

}
