package com.boot.jx.postman.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

import com.boot.utils.ArgUtil;
import com.boot.utils.Constants;
import com.boot.utils.JsonPath;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DocResult implements Serializable {
	private static final long serialVersionUID = -2503512814079715347L;

	public static JsonPath OCR_NAME = new JsonPath("NAME");

	public BigDecimal id;

	@JsonProperty(value = "FILE_ID")
	public BigDecimal fileId;

	@JsonProperty(value = "DOC_TYPE")
	public String docType;

	@JsonProperty(value = "DOC_ID")
	public String docId;

	@JsonProperty(value = "DOC_NUM")
	public String docNum;

	@JsonProperty(value = "USER_DIR")
	public String userDir;

	public String response;
	public String status;
	public String name;

	public String uploadUrl;

	@JsonProperty(value = "OCR")
	private Map<String, Object> ocr;

	public void setName(String name) {
		this.name = name;
	}

	public BigDecimal getId() {
		return ArgUtil.parseAsBigDecimal(fileId, id);
	}

	public void setId(BigDecimal id) {
		this.id = id;
	}

	public String getResponse() {
		return response;
	}

	public void setResponse(String response) {
		this.response = response;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public BigDecimal getFileId() {
		return fileId;
	}

	public void setFileId(BigDecimal fileId) {
		this.fileId = fileId;
	}

	@Deprecated
	public String getIdentity() {
		return this.docId;
	}

	@Deprecated
	public String getCivilID() {
		return this.docId;
	}

	@Deprecated
	public String getUserID() {
		return this.userDir;
	}

	public String getDocType() {
		return docType;
	}

	public void setDocType(String docType) {
		this.docType = docType;
	}

	public String getDocId() {
		return ArgUtil.nonEmpty(docNum, docId);
	}

	public void setDocId(String docId) {
		this.docId = docId;
	}

	public String getUserDir() {
		return userDir;
	}

	public void setUserDir(String userDir) {
		this.userDir = userDir;
	}

	public Map<String, Object> getOcr() {
		return ocr;
	}

	public void setOcr(Map<String, Object> ocr) {
		this.ocr = ocr;
	}

	public String getName() {
		return OCR_NAME.load(this.ocr, Constants.BLANK);
	}

	public String getDocNum() {
		return docNum;
	}

	public void setDocNum(String docNum) {
		this.docNum = docNum;
	}

	public String getUploadUrl() {
		return uploadUrl;
	}

	public void setUploadUrl(String uploadUrl) {
		this.uploadUrl = uploadUrl;
	}

	public DocResult uploadUrl(String uploadUrl) {
		this.uploadUrl = uploadUrl;
		return this;
	}

	public DocResult docType(String docType) {
		this.docType = docType;
		return this;
	}

}