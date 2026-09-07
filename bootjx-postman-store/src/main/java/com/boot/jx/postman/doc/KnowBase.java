package com.boot.jx.postman.doc;

import java.io.Serializable;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.mongo.CommonDocInterfaces.IdNumberSupport;
import com.boot.jx.mongo.CommonDocInterfaces.TimeStampIndex.TimeStampDoc;
import com.boot.jx.postman.store.QuickStore.QuickGalleryItem;

@Document(collection = "DICT_KNOW_BASE")
@TypeAlias("KnowBase")
public class KnowBase extends TimeStampDoc implements Serializable, QuickGalleryItem, IdNumberSupport {
	private static final long serialVersionUID = -5649094988762846983L;

	@Id
	private String id;

	@Indexed
	private String code;

	private long idNumber;

	@Indexed
	private String parentId;

	@Indexed
	private String type;

	private String title;
	private String category;
	private String startnote;
	private String content;
	private String endnote;

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

	public void setCode(String code) {
		this.code = code;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getCode() {
		return code;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getEndnote() {
		return endnote;
	}

	public void setEndnote(String endnote) {
		this.endnote = endnote;
	}

	public String getStartnote() {
		return startnote;
	}

	public void setStartnote(String startnote) {
		this.startnote = startnote;
	}

	public long getIdNumber() {
		return idNumber;
	}

	public void setIdNumber(long idNumber) {
		this.idNumber = idNumber;
	}

}
