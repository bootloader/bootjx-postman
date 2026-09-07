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
import com.boot.utils.ArgUtil;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Document(collection = "DICT_QUICK_TAG")
@TypeAlias("QuickTag")
@JsonDeserialize(as = QuickTag.class, keyUsing = ResourceDocumentKeyDeserializer.class)
public class QuickTag
		implements Serializable, AuditableEntity, ResourceDocument, ADocumentDTO<QuickTag>, Comparable<QuickTag> {

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

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getCode() {
		return code;
	}

	@Override
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

	public Long getUpdatedStamp() {
		return updatedStamp;
	}

	public void setUpdatedStamp(Long updatedStamp) {
		this.updatedStamp = updatedStamp;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	@Override
	public ADocumentDTO<QuickTag> newInstance() {
		return new QuickTag();
	}

	@Override
	public int compareTo(QuickTag o) {
		if (ArgUtil.is(this.getId()) && ArgUtil.is(o.getId())) {
			return this.getId().compareTo(o.getId());
		} else if (ArgUtil.is(this.getCode()) && ArgUtil.is(o.getCode())) {
			return this.getCode().compareTo(o.getCode());
		} else if (ArgUtil.is(this.getTitle()) && ArgUtil.is(o.getTitle())) {
			return this.getTitle().compareTo(o.getTitle());
		} else {
			return this.toString().compareTo(o.toString());
		}
	}
}
