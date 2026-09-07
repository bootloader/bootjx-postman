package com.boot.jx.payg;

import java.math.BigDecimal;

public class BranchApplicationDto {
	
	private BigDecimal applicationId;
	private String paymentType;

	public String getPaymentType() {
		return paymentType;
	}

	public void setPaymentType(String paymentType) {
		this.paymentType = paymentType;
	}

	public BigDecimal getApplicationId() {
		return applicationId;
	}

	public void setApplicationId(BigDecimal applicationId) {
		this.applicationId = applicationId;
	}

}
