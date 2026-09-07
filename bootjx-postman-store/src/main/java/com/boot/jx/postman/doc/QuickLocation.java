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
import com.boot.jx.swagger.ApiMockModelProperty;
import com.boot.utils.ArgUtil;
import com.mongodb.DBObject;

@Document(collection = "DICT_QUICK_LOCATION")
@TypeAlias("QuickLocation")
public class QuickLocation implements Serializable, AuditableEntity, QuickGalleryItem {

	private static final long serialVersionUID = 4305626168451568306L;

	@Id
	private String id;

	/**
	 * Alias - name
	 */
	@ApiMockModelProperty(example = "Main Building", value = "location-name")
	private String title;

	@Indexed(unique = true, sparse = true)
	private String code;

	private String category;

	@ApiMockModelProperty(example = "1 Hacker Way, Menlo Park, CA, 94025", value = "Address Text")
	public String address;

	@ApiMockModelProperty(example = "12.909090", value = "latitude")
	public String latitude;

	@ApiMockModelProperty(example = "6.908080", value = "longitude")
	public String longitude;

	@ApiMockModelProperty(example = "Location URL", value = "location-url")
	public String url;

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

	public QuickLocation category(String category) {
		this.category = category;
		return this;
	}

	public QuickLocation from(DBObject doc) {
		this.category = ArgUtil.parseAsString(doc.get("category"));
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

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getLatitude() {
		return latitude;
	}

	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	public String getLongitude() {
		return longitude;
	}

	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}

}
