package com.boot.jx.postman.doc;

import java.io.Serializable;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.model.AuditCreateEntity.AuditableEntity;
import com.boot.jx.postman.store.QuickStore.QuickGalleryItem;
import com.boot.utils.ArgUtil;

@Document(collection = "DICT_QUICK_AXN")
@TypeAlias("QuickAction")
public class QuickAction implements Serializable, AuditableEntity, QuickGalleryItem {
	private static final long serialVersionUID = -5649094988762846983L;

	@Id
	private String id;
	private String title;
	private String category;

	@Indexed(unique = true, sparse = true)
	private String code;

	private String action;

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

	public String getAction() {
		return ArgUtil.nonEmpty(code, action);
	}

	public void setAction(String action) {
		this.code = action;
	}

	public String getCode() {
		return ArgUtil.nonEmpty(code, action);
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

}
