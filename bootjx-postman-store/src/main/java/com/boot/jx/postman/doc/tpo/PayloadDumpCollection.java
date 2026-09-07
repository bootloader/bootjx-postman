package com.boot.jx.postman.doc.tpo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.dict.ContactType;
import com.boot.jx.mongo.CommonDocInterfaces.TimeStampIndex.UpdatedTimeStampDoc;

@Document(collection = "PAYLOAD_DUMP")
public class PayloadDumpCollection extends UpdatedTimeStampDoc implements Serializable {

	private static final long serialVersionUID = -2060292937777425005L;

	@Id
	private String dumpId;

	@Indexed
	private String type;

	@Indexed
	private ContactType contactType;

	@Indexed
	private String channelType;

	@Indexed
	private String channelId;

	private boolean isAutoCreatedChannel;

	private Map<String, Object> dump;

	List<Object> incomingRequest;

	private List<Object> logs;

	public void setIncomingRequest(List<Object> incomingRequest) {
		this.incomingRequest = incomingRequest;
	}

	public List<Object> getIncomingRequest() {
		return incomingRequest;
	}

	public String getDumpId() {
		return dumpId;
	}

	public void setDumpId(String dumpId) {
		this.dumpId = dumpId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public ContactType getContactType() {
		return contactType;
	}

	public void setContactType(ContactType contactType) {
		this.contactType = contactType;
	}

	public String getChannelType() {
		return channelType;
	}

	public void setChannelType(String channelType) {
		this.channelType = channelType;
	}

	public String getChannelId() {
		return channelId;
	}

	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}

	public Map<String, Object> getDump() {
		return dump;
	}

	public void setDump(Map<String, Object> dump) {
		this.dump = dump;
	}

	public boolean isAutoCreatedChannel() {
		return isAutoCreatedChannel;
	}

	public void setAutoCreatedChannel(boolean isAutoCreatedChannel) {
		this.isAutoCreatedChannel = isAutoCreatedChannel;
	}

	public List<Object> getLogs() {
		return logs;
	}

	public void setLogs(List<Object> logs) {
		this.logs = logs;
	}

	public List<Object> logs() {
		if (this.getLogs() == null) {
			this.setLogs(new ArrayList<Object>());
		}
		return this.getLogs();
	}

}
