package com.boot.jx.payg;

import com.boot.jx.dict.PayGServiceCode;
import com.boot.utils.ArgUtil;

/**
 * 
 * @deprecated use {@link PayGParams}
 * 
 * @author lalittanwar
 *
 */
@Deprecated
public class Payment {
	String docId;
	String docNo;
	String docFinYear;

	String netPayableAmount;
	String merchantTrackId;
	Object product;
	PayGServiceCode pgCode;

	public String getDocNo() {
		return docNo;
	}

	public void setDocNo(String docNo) {
		this.docNo = docNo;
	}

	public String getDocFinYear() {
		return docFinYear;
	}

	public void setDocFinYear(Object docFinYear) {
		this.docFinYear = ArgUtil.parseAsString(docFinYear);
	}

	public String getNetPayableAmount() {
		return netPayableAmount;
	}

	public void setNetPayableAmount(Object netPayableAmount) {
		this.netPayableAmount = ArgUtil.parseAsString(netPayableAmount);
	}

	public String getMerchantTrackId() {
		return merchantTrackId;
	}

	public void setMerchantTrackId(Object merchantTrackId) {
		this.merchantTrackId = ArgUtil.parseAsString(merchantTrackId);
	}

	public PayGServiceCode getPgCode() {
		return pgCode;
	}

	public void setPgCode(PayGServiceCode payGServiceCode) {
		this.pgCode = payGServiceCode;
	}

	public Object getProduct() {
		return product;
	}

	public void setProduct(Object product) {
		this.product = product;
	}

	public String getDocId() {
		return docId;
	}

	public void setDocId(String docId) {
		this.docId = docId;
	}

	public void setDocFinYear(String docFinYear) {
		this.docFinYear = docFinYear;
	}

	public void setNetPayableAmount(String netPayableAmount) {
		this.netPayableAmount = netPayableAmount;
	}

	public void setMerchantTrackId(String merchantTrackId) {
		this.merchantTrackId = merchantTrackId;
	}
}
