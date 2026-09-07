package com.boot.jx.postman.doc;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.mongo.CommonDocInterfaces.IDocument;
import com.boot.jx.mongo.CommonDocInterfaces.Patchable;
import com.boot.jx.mongo.CommonDocInterfaces.TimeStampIndex.TimeStampDoc;

@Document(collection = "WABA_ACCOUNT_BALANCE")
@TypeAlias("WabaAccountBalanceDoc")
public class WabaAccountBalanceDoc extends TimeStampDoc
		implements Serializable, Patchable<WabaAccountBalanceDoc>, IDocument {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2604878253251388988L;
	@Id
	String id;
	@Indexed(unique = true)
	String tenant;
	String wabaId;
	String number;
	String currencyCode;
	long timeStamp;
	double depositAmt = 0.0;
	double balanceAmt = 0.0;
	double totalMsgCost = 0.0;
	private List<WabaAccountBalanceDoc> oldVersion;

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

	public String getCurrencyCode() {
		return currencyCode;
	}

	public void setCurrencyCode(String currencyCode) {
		this.currencyCode = currencyCode;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public double getDepositAmt() {
		return depositAmt;
	}

	public void setDepositAmt(double depositAmt) {
		this.depositAmt = depositAmt;
	}

	public double getBalanceAmt() {
		return balanceAmt;
	}

	public void setBalanceAmt(double balanceAmt) {
		this.balanceAmt = balanceAmt;
	}

	public double getTotalMsgCost() {
		return totalMsgCost;
	}

	public void setTotalMsgCost(double totalMsgCost) {
		this.totalMsgCost = totalMsgCost;
	}

//	public List<DocVersion> getOldVersions() {
//		return oldVersions;
//	}
//	
//	public void setOldVersions(List<DocVersion> oldVersions) {
//		this.oldVersions = oldVersions;
//	}
	@Override
	public WabaAccountBalanceDoc patch() {
		WabaAccountBalanceDoc patch = new WabaAccountBalanceDoc();
		patch.setId(this.getId());
		return patch;
	}

	public String getTenant() {
		return tenant;
	}

	public void setTenant(String tenant) {
		this.tenant = tenant;
	}

	public List<WabaAccountBalanceDoc> getOldVersion() {
		return oldVersion;
	}

	public void setOldVersion(List<WabaAccountBalanceDoc> oldVersion) {
		this.oldVersion = oldVersion;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

}
