package com.boot.jx.postman.doc;
import java.io.Serializable;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.mongo.CommonDocInterfaces.SimpleDocument;
import com.boot.jx.mongo.CommonDocInterfaces.TimeStampIndex.TimeStampDoc;
import com.boot.model.UtilityModels.JsonIgnoreNull;
import com.boot.model.UtilityModels.JsonIgnoreUnknown;

@Document(collection ="TP_WABA_ANALYTICS")
@TypeAlias("WabaAnalyticsDoc")
public class WabaAnalyticsDoc extends TimeStampDoc
implements Serializable, SimpleDocument, JsonIgnoreUnknown, JsonIgnoreNull {
		
	/**
	 * 
	 */
	private static final long serialVersionUID = 2590616414999414312L;
	@Id
	String id;
	String wabaId;
	String number;
	long start;
	long end;
	long conversation;
	String phone_number;
	String country;
	String conversation_type;
	String conversation_category;
	double cost;
	String tenant;
	String currency;
	Map<String,Object> date ;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getWabaId() {
		return wabaId;
	}
	public void setWabaId(String wabaId) {
		this.wabaId = wabaId;
	}
	public String getNumber() {
		return number;
	}
	public void setNumber(String number) {
		this.number = number;
	}
	public long getStart() {
		return start;
	}
	public void setStart(long start) {
		this.start = start;
	}
	public long getEnd() {
		return end;
	}
	public void setEnd(long end) {
		this.end = end;
	}
	public long getConversation() {
		return conversation;
	}
	public void setConversation(long conversation) {
		this.conversation = conversation;
	}
	public String getPhone_number() {
		return phone_number;
	}
	public void setPhone_number(String phone_number) {
		this.phone_number = phone_number;
	}
	public String getCountry() {
		return country;
	}
	public void setCountry(String country) {
		this.country = country;
	}
	public String getConversation_type() {
		return conversation_type;
	}
	public void setConversation_type(String conversation_type) {
		this.conversation_type = conversation_type;
	}
	public String getConversation_category() {
		return conversation_category;
	}
	public void setConversation_category(String conversation_category) {
		this.conversation_category = conversation_category;
	}
	public double getCost() {
		return cost;
	}
	public void setCost(double cost) {
		this.cost = cost;
	}
	public String getTenant() {
		return tenant;
	}
	public void setTenant(String tenant) {
		this.tenant = tenant;
	}
	public String getCurrency() {
		return currency;
	}
	public void setCurrency(String currency) {
		this.currency = currency;
	}
	public Map<String, Object> getDate() {
		return date;
	}
	public void setDate(Map<String, Object> date) {
		this.date = date;
	}
	
	

}
