package com.boot.jx.postman.doc.ticket;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.mongo.CommonDocInterfaces.IDocument;
import com.boot.jx.mongo.CommonDocInterfaces.IdNumberSupport;
import com.boot.jx.mongo.CommonDocInterfaces.TimeStampIndex.UpdatedTimeStampDoc;
import com.boot.jx.postman.model.Assignment;
import com.boot.jx.postman.model.ContactMeta;
import com.boot.jx.postman.model.MessageDefinitions.Contactable;
import com.boot.utils.ArgUtil;

@Document(collection = "TICKET")
@TypeAlias("CustomerTicket")

@CompoundIndexes({
		// route indexs
		@CompoundIndex(name = "assigned_dept_code", def = "{ 'assigned.deptCode': 1 }"),
		@CompoundIndex(name = "assigned_agent_code", def = "{ 'assigned.agentCode': 1 }")
//
})
public class CustomerTicketDoc extends UpdatedTimeStampDoc implements Serializable, IdNumberSupport, IDocument {

	private static final long serialVersionUID = 1L;

	@Id
	private String ticketId;

	@Indexed(sparse = true)
	private String ticketHash;

	private long idNumber;

	@Indexed(unique = true, sparse = true)
	private String code;

	@Version
	private Long version;

	private ContactMeta contact;

	private Assignment assigned;

	@Indexed
	private String status;

	private String subject;
	private String startnote;
	private String endnote;

	@Indexed
	private List<String> tagId;

	public Map<String, List<Object>> details;

	public String getSessionId() {
		return ticketId;
	}

	public void setSessionId(String sessionId) {
		this.ticketId = sessionId;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return String.format("[sessionId:%s]", this.ticketId);
	}

	public List<String> getTagId() {
		return tagId;
	}

	public void setTagId(List<String> tagId) {
		this.tagId = tagId;
	}

	public List<String> tagId() {
		if (ArgUtil.isEmpty(this.tagId))
			this.tagId = new ArrayList<String>();
		return tagId;
	}

	public ContactMeta getContact() {
		return contact;
	}

	public void setContact(ContactMeta contact) {
		this.contact = contact;
	}

	public Contactable contact() {
		if (this.contact == null) {
			this.contact = new ContactMeta();
		}
		return this.contact;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getTicketHash() {
		return ticketHash;
	}

	public void setTicketHash(String ticketHash) {
		this.ticketHash = ticketHash;
	}

	public String getTicketId() {
		return ticketId;
	}

	public void setTicketId(String ticketId) {
		this.ticketId = ticketId;
	}

	public Map<String, List<Object>> getDetails() {
		return details;
	}

	public void setDetails(Map<String, List<Object>> details) {
		this.details = details;
	}

	public Map<String, List<Object>> details() {
		if (this.details == null) {
			this.details = new HashMap<String, List<Object>>();
		}
		return details;
	}

	public void addDetail(String key, Object value) {
		List<Object> values = this.details().get(key);
		if (values == null) {
			values = new ArrayList<Object>();
			this.details().put(key, values);
		}
		values.add(values);
	}

	public String getStartnote() {
		return startnote;
	}

	public void setStartnote(String startnote) {
		this.startnote = startnote;
	}

	public String getEndnote() {
		return endnote;
	}

	public void setEndnote(String endnote) {
		this.endnote = endnote;
	}

	public long getIdNumber() {
		return idNumber;
	}

	public void setIdNumber(long idNumber) {
		this.idNumber = idNumber;
	}

	public Assignment getAssigned() {
		return assigned;
	}

	public void setAssigned(Assignment assigned) {
		this.assigned = assigned;
	}
}
