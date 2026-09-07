package com.boot.jx.postman.doc.ticket;

import java.io.Serializable;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.mongo.CommonDocInterfaces.ADocumentDTO;
import com.boot.jx.mongo.CommonDocInterfaces.ResourceDocument;
import com.boot.jx.mongo.ResourceDocumentKeyDeserializer;
import com.boot.jx.postman.store.QuickStore.QuickGalleryItem;
import com.boot.model.TimeModels.TimeStampSupportedModel;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Document(collection = "TICKET_STATUS")
@TypeAlias("CustomerTicketStatus")
@JsonDeserialize(as = CustomerTicketStatus.class, keyUsing = ResourceDocumentKeyDeserializer.class)
public class CustomerTicketStatus extends TimeStampSupportedModel
		implements Serializable, ResourceDocument, ADocumentDTO<CustomerTicketStatus>, QuickGalleryItem {

	private static final long serialVersionUID = 2845094878124818820L;

	public static CustomerTicketStatus OPEN = new CustomerTicketStatus("OPEN", "Open", true);
	public static CustomerTicketStatus IN_PROGRESS = new CustomerTicketStatus("IN_PROGRESS", "In Progress", true);
	public static CustomerTicketStatus RESOLVED = new CustomerTicketStatus("RESOLVED", "Resolved", true);
	public static CustomerTicketStatus CLOSED = new CustomerTicketStatus("CLOSED", "Closed", true);

	public CustomerTicketStatus(String code, String title, boolean readonly) {
		super();
		this.id = code;
		this.code = code;
		this.title = title;
		this.readonly = readonly;
	}

	public CustomerTicketStatus() {
		super();
	}

	@Id
	private String id;
	private String title;
	private String category;
	private boolean readonly;

	@Indexed(unique = true, sparse = true)
	private String code;

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@Override
	public ADocumentDTO<CustomerTicketStatus> newInstance() {
		return new CustomerTicketStatus();
	}

	public boolean isReadonly() {
		return readonly;
	}

	public void setReadonly(boolean readonly) {
		this.readonly = readonly;
	}
}
