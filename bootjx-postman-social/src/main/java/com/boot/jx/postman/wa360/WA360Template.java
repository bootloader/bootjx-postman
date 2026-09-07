package com.boot.jx.postman.wa360;

import java.io.Serializable;
import java.util.List;

import com.boot.model.UtilityModels.JsonIgnoreUnknown;

public class WA360Template implements Serializable, JsonIgnoreUnknown {
    private static final long serialVersionUID = -3743792916425816429L;
    
    private String id;

    private String category;

    private String language;

    private String name;

    private String namespace;

    private String status;

    private String rejected_reason;

    private List<Object> components;
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategory() {
	return category;
    }

    public void setCategory(String category) {
	this.category = category;
    }

    public String getName() {
	return name;
    }

    public void setName(String name) {
	this.name = name;
    }

    public String getNamespace() {
	return namespace;
    }

    public void setNamespace(String namespace) {
	this.namespace = namespace;
    }

    public String getStatus() {
	return status;
    }

    public void setStatus(String status) {
	this.status = status;
    }

    public String getRejected_reason() {
	return rejected_reason;
    }

    public void setRejected_reason(String rejected_reason) {
	this.rejected_reason = rejected_reason;
    }

    public List<Object> getComponents() {
	return components;
    }

    public void setComponents(List<Object> components) {
	this.components = components;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
