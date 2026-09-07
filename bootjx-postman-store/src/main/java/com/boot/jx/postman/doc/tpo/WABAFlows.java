package com.boot.jx.postman.doc.tpo;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = WABAFlows.COLLECTION_NAME)
@TypeAlias("Flow")
public class WABAFlows implements Serializable {

	public static final String COLLECTION_NAME = "TP_WABA_FLOWS";

	private static final long serialVersionUID = 1L;

	@Id
	private String id;

	private String wabaId;
	private String flowId;

	private Map<String, Object> meta;
	private Map<String, Object> details;
	private Map<String, Object> json;
    private List<Map<String, String>> fieldMeta;
	

    public List<Map<String, String>> getFieldMeta() {
        return fieldMeta;
    }

    public void setFieldMeta(List<Map<String, String>> fieldMeta) {
        this.fieldMeta = fieldMeta;
    }

	

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

	public String getFlowId() {
		return flowId;
	}

	public void setFlowId(String flowId) {
		this.flowId = flowId;
	}

	public Map<String, Object> getMeta() {
		return meta;
	}

	public void setMeta(Map<String, Object> meta) {
		this.meta = meta;
	}

	public Map<String, Object> getDetails() {
		return details;
	}

	public void setDetails(Map<String, Object> details) {
		this.details = details;
	}

	public Map<String, Object> getJson() {
		return json;
	}

	public void setJson(Map<String, Object> json) {
		this.json = json;
	}

	

	

}