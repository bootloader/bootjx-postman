package com.boot.jx.payg;

import java.math.BigDecimal;

import javax.validation.constraints.NotNull;

import com.boot.jx.api.ARespModel;
import com.boot.utils.JsonUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PayGModel extends ARespModel {

	private static final long serialVersionUID = -5470658301044728240L;

	private String trackId;
	private String udf1 = null;
	private String udf2 = null;
	@NotNull
	private String udf3 = null;
	private String udf4 = null;
	private String udf5 = null;
	private String errorCategory = null;
	private String error = null;
	private String errorText = null;
	@NotNull
	private String paymentId;
	private BigDecimal collectionFinanceYear;
	private BigDecimal collectionDocumentNumber;
	private BigDecimal collectionDocumentCode;
	@NotNull
	private BigDecimal applicationCountryId = null;
	private String statusUpdatedBy;
	
		
	public String getUdf1() {
		return udf1;
	}

	public void setUdf1(String udf1) {
		this.udf1 = udf1;
	}

	public String getUdf2() {
		return udf2;
	}

	public void setUdf2(String udf2) {
		this.udf2 = udf2;
	}

	public String getUdf3() {
		return udf3;
	}

	public void setUdf3(String udf3) {
		this.udf3 = udf3;
	}

	public String getUdf4() {
		return udf4;
	}

	public void setUdf4(String udf4) {
		this.udf4 = udf4;
	}

	public String getUdf5() {
		return udf5;
	}

	public void setUdf5(String udf5) {
		this.udf5 = udf5;
	}

	public String getPaymentId() {
		return paymentId;
	}

	public void setPaymentId(String paymentId) {
		this.paymentId = paymentId;
	}

	/**
	 * @return the errorText
	 */
	public String getErrorText() {
		return errorText;
	}

	/**
	 * @param errorText the errorText to set
	 */
	public void setErrorText(String errorText) {
		this.errorText = errorText;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public String getErrorCategory() {
		return errorCategory;
	}

	public void setErrorCategory(String codeCategory) {
		this.errorCategory = codeCategory;
	}

	public BigDecimal getCollectionFinanceYear() {
		return collectionFinanceYear;
	}

	public void setCollectionFinanceYear(BigDecimal collectionFinYear) {
		this.collectionFinanceYear = collectionFinYear;
	}

	public BigDecimal getCollectionDocumentNumber() {
		return collectionDocumentNumber;
	}

	public void setCollectionDocumentNumber(BigDecimal collectionDocNumber) {
		this.collectionDocumentNumber = collectionDocNumber;
	}

	public BigDecimal getCollectionDocumentCode() {
		return collectionDocumentCode;
	}

	public void setCollectionDocumentCode(BigDecimal collectionDocCode) {
		this.collectionDocumentCode = collectionDocCode;
	}

	public BigDecimal getApplicationCountryId() {
		return applicationCountryId;
	}

	public void setApplicationCountryId(BigDecimal countryId) {
		this.applicationCountryId = countryId;
	}

	public String getTrackId() {
		return trackId;
	}

	public void setTrackId(String trackId) {
		this.trackId = trackId;
	}

	@Override
	public String toString() {
		return "[" + JsonUtil.toJson(this) + "]";
	}

	public String getStatusUpdatedBy() {
		return statusUpdatedBy;
	}

	public void setStatusUpdatedBy(String statusUpdatedBy) {
		this.statusUpdatedBy = statusUpdatedBy;
	}
}
