package com.boot.jx.postman.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.boot.jx.postman.model.MessageDefinitions.Contactable;
import com.boot.jx.postman.model.MessageDefinitions.SessionId;
import com.boot.model.MapModel;

public class PMArgs implements SessionId, Serializable {
	private static final long serialVersionUID = -1002441635191227105L;

	private String checksum;
	private String sessionId;
	private String assignToQueueCode;
	private String assignToDeptCode;
	private String assignToAgentCode;
	private String assignToBotCode;
	private List<String> assignToSkillCodes;
	private Contactable contact;
	private MapModel data;
	private Object params;
	private String note;

	public MapModel data() {
		if (this.data == null) {
			this.data = MapModel.createInstance();
		}
		return this.data;
	}

	public PMArgs params(Object params) {
		this.params = params;
		return this;
	}

	public PMArgs sessionId(String sessionId) {
		this.sessionId = sessionId;
		return this;
	}

	public PMArgs note(String note) {
		this.note = note;
		return this;
	}

	public PMArgs assignToQueueCode(String queueCode) {
		this.assignToQueueCode = queueCode;
		return this;
	}

	public PMArgs assignToDeptCode(String deptCode) {
		this.assignToDeptCode = deptCode;
		return this;
	}

	public PMArgs assignToAgentCode(String agentCode) {
		this.assignToAgentCode = agentCode;
		return this;
	}

	public PMArgs assignToBotCode(String botCode) {
		this.assignToBotCode = botCode;
		return this;
	}

	public PMArgs contact(Contactable contact) {
		this.contact = contact;
		return this;
	}

	public Contactable contact() {
		if (this.contact == null) {
			this.contact = new ContactMeta();
		}
		return this.contact;
	}

	public Contactable getContact() {
		return contact;
	}

	public void setContact(Contactable contact) {
		this.contact = contact;
	}

	public MapModel getData() {
		return data;
	}

	public void setData(MapModel data) {
		this.data = data;
	}

	public String getAssignToDeptCode() {
		return assignToDeptCode;
	}

	public void setAssignToDeptCode(String assignToDeptCode) {
		this.assignToDeptCode = assignToDeptCode;
	}

	public String getAssignToAgentCode() {
		return assignToAgentCode;
	}

	public void setAssignToAgentCode(String assignToAgentCode) {
		this.assignToAgentCode = assignToAgentCode;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getChecksum() {
		return checksum;
	}

	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

	public Object getParams() {
		return params;
	}

	public void setParams(Object params) {
		this.params = params;
	}

	public String getAssignToQueueCode() {
		return assignToQueueCode;
	}

	public void setAssignToQueueCode(String assignToQueueCode) {
		this.assignToQueueCode = assignToQueueCode;
	}

	public String getAssignToBotCode() {
		return assignToBotCode;
	}

	public void setAssignToBotCode(String assignToBotCode) {
		this.assignToBotCode = assignToBotCode;
	}

	public List<String> assignToSkillCodes() {
		if (this.assignToSkillCodes == null) {
			this.assignToSkillCodes = new ArrayList<String>();
		}
		return this.assignToSkillCodes;
	}

	public List<String> getAssignToSkillCodes() {
		return assignToSkillCodes;
	}

	public void setAssignToSkillCodes(List<String> assignToSkillCodes) {
		this.assignToSkillCodes = assignToSkillCodes;
	}

	public PMArgs assignToSkillCode(String... skillCode) {
		this.assignToSkillCodes();
		for (String string : skillCode) {
			this.assignToSkillCodes.add(string);
		}
		return this;
	}

	public PMArgs assignToSkillCodes(List<String> skillCodes) {
		this.assignToSkillCodes = skillCodes;
		return this;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}
}
