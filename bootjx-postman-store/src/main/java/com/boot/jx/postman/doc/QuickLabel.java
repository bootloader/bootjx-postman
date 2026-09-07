package com.boot.jx.postman.doc;

import java.io.Serializable;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.model.AuditCreateEntity.AuditableEntity;
import com.boot.jx.mongo.CommonDocInterfaces.ADocumentDTO;
import com.boot.jx.mongo.CommonDocInterfaces.ResourceDocument;
import com.boot.jx.mongo.ResourceDocumentKeyDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Document(collection = "DICT_QUICK_LABEL")
@TypeAlias("QuickLabel")
@JsonDeserialize(as = QuickLabel.class, keyUsing = ResourceDocumentKeyDeserializer.class)
public class QuickLabel implements Serializable, AuditableEntity, ResourceDocument, ADocumentDTO<QuickLabel> {

	private static final long serialVersionUID = 2845094878124818820L;
	@Id
	private String id;
	private String title;
	private String category;

	@Indexed(unique = true, sparse = true)
	private String code;

	private String createdBy;
	private Long createdStamp;

	private String updatedBy;
	private Long updatedStamp;

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public Long getCreatedStamp() {
		return createdStamp;
	}

	public void setCreatedStamp(Long createdStamp) {
		this.createdStamp = createdStamp;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	public Long getUpdatedStamp() {
		return updatedStamp;
	}

	public void setUpdatedStamp(Long updatedStamp) {
		this.updatedStamp = updatedStamp;
	}

	@Override
	public ADocumentDTO<QuickLabel> newInstance() {
		return new QuickLabel();
	}
}
