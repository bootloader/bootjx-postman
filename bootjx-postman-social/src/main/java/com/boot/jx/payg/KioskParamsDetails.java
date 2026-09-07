package com.boot.jx.payg;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@JsonIgnoreProperties(ignoreUnknown = true)
public class KioskParamsDetails implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1526716053659080583L;
	private BigDecimal refundLimit =null;
	private BigDecimal applicationCountryId=null;
	private BigDecimal companyId =null;
	private BigDecimal branchCode =null;
	private String mUserName =null;
	private String ipAddress=null;
	private String newBagRef=null;
	private String applIds;
	private String paymentType;
	private String payToken;
	private String paymentId;
	private List<BranchApplicationDto> remittanceApplicationId =new ArrayList<>();
	
	public BigDecimal getRefundLimit() {
		return refundLimit;
	}
	public void setRefundLimit(BigDecimal refundLimit) {
		this.refundLimit = refundLimit;
	}
	public BigDecimal getApplicationCountryId() {
		return applicationCountryId;
	}
	public void setApplicationCountryId(BigDecimal applicationCountryId) {
		this.applicationCountryId = applicationCountryId;
	}
	public BigDecimal getCompanyId() {
		return companyId;
	}
	public void setCompanyId(BigDecimal companyId) {
		this.companyId = companyId;
	}
	public BigDecimal getBranchCode() {
		return branchCode;
	}
	public void setBranchCode(BigDecimal branchCode) {
		this.branchCode = branchCode;
	}
	public String getmUserName() {
		return mUserName;
	}
	public void setmUserName(String mUserName) {
		this.mUserName = mUserName;
	}
	public List<BranchApplicationDto> getRemittanceApplicationId() {
		return remittanceApplicationId;
	}
	public void setRemittanceApplicationId(List<BranchApplicationDto> remittanceApplicationId) {
		this.remittanceApplicationId = remittanceApplicationId;
	}
	public String getIpAddress() {
		return ipAddress;
	}
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	public String getNewBagRef() {
		return newBagRef;
	}
	public void setNewBagRef(String newBagRef) {
		this.newBagRef = newBagRef;
	}
	public String getApplIds() {
		return applIds;
	}
	public void setApplIds(String applIds) {
		this.applIds = applIds;
	}
	public String getPaymentType() {
		return paymentType;
	}
	public void setPaymentType(String paymentType) {
		this.paymentType = paymentType;
	}
	public String getPayToken() {
		return payToken;
	}
	public void setPayToken(String payToken) {
		this.payToken = payToken;
	}
	public String getPaymentId() {
		return paymentId;
	}
	public void setPaymentId(String paymentId) {
		this.paymentId = paymentId;
	}

}
