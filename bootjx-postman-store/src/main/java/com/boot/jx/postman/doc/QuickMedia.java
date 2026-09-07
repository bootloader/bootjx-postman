package com.boot.jx.postman.doc;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.model.AuditCreateEntity.AuditableEntity;
import com.boot.jx.postman.store.QuickStore.QuickGalleryItem;
import com.boot.utils.ArgUtil;
import com.mongodb.DBObject;

@Document(collection = "DICT_QUICK_MEDIA")
@TypeAlias("QuickMedia")
public class QuickMedia implements Serializable, AuditableEntity, QuickGalleryItem {

	private static final long serialVersionUID = 7942286016346691701L;

	@Id
	private String id;

	private String title;

	@Indexed(unique = true, sparse = true)
	private String code;

	private String category;

	private String content;

	private String url;
	private String type;
	private String format;
	private String mimeType;

	private Map<String, Object> meta;

	private String createdBy;
	private Long createdStamp;

	private String updatedBy;
	private Long updatedStamp;

	public String getId() {
		return id;
	}

	public void setId(String name) {
		this.id = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Map<String, Object> getMeta() {
		return meta;
	}

	public void setMeta(Map<String, Object> meta) {
		this.meta = meta;
	}

	public Map<String, Object> meta() {
		if (this.meta == null) {
			this.meta = new HashMap<String, Object>();
		}
		return this.meta;
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

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public QuickMedia category(String category) {
		this.category = category;
		return this;
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

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public QuickMedia from(DBObject doc) {
		this.category = ArgUtil.parseAsString(doc.get("category"));
		return this;
	}

	public QuickMedia from(org.bson.Document doc) {
		this.category = ArgUtil.parseAsString(doc.get("category"));
		return this;
	}
}
