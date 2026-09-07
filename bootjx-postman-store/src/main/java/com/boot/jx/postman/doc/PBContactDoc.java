package com.boot.jx.postman.doc;

import java.io.Serializable;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.dict.ContactType;
import com.boot.jx.postman.pbook.PBEmail;
import com.boot.jx.postman.pbook.PBName;
import com.boot.jx.postman.pbook.PBPhone;
import com.boot.utils.ArgUtil;

@Document(collection = "PB_CONTACT")
@TypeAlias("PBContactDoc")
@CompoundIndexes({
		// route indexs
		@CompoundIndex(name = "phone_phone", def = "{ 'phone.phone': 1 }"),
		@CompoundIndex(name = "email_email", def = "{ 'email.email': 1 }") //
})
public class PBContactDoc implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	private String contactId;

	@Indexed
	private String csid;

	@Indexed
	private ContactType type;

	private PBName name;

	private PBPhone phone;
	private PBEmail email;

	private long changestamp;

	public long getChangestamp() {
		return changestamp;
	}

	public void setChangestamp(long changestamp) {
		this.changestamp = changestamp;
	}

	public String getContactId() {
		return contactId;
	}

	public void setContactId(String contactId) {
		this.contactId = contactId;
	}

	@Override
	public String toString() {
		return String.format("[%s:%s]", ArgUtil.parseAsString(type, ContactType.EMPTY.toString()), this.csid);
	}

	public String getCsid() {
		return csid;
	}

	public void setCsid(String csid) {
		this.csid = csid;
	}

	public ContactType getType() {
		return type;
	}

	public void setType(ContactType type) {
		this.type = type;
	}

	public PBName getName() {
		return name;
	}

	public void setName(PBName name) {
		this.name = name;
	}

	public PBPhone getPhone() {
		return phone;
	}

	public void setPhone(PBPhone phone) {
		this.phone = phone;
	}

	public PBEmail getEmail() {
		return email;
	}

	public void setEmail(PBEmail email) {
		this.email = email;
	}

}
