package com.boot.jx.postman.doc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Reference;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.boot.jx.model.AuditCreateEntity;
import com.boot.jx.mongo.CommonDocInterfaces.OldDocVersion;

@Document(collection = HSMContentType.COLLECTION_NAME)
@TypeAlias("HSMContentType")
public class HSMContentType implements Serializable, OldDocVersion<HSMContentType>, AuditCreateEntity {

	public static final List<HSMContentType> LIST = new ArrayList<HSMContentType>();
	public static final String COLLECTION_NAME = "DICT_HSM_CONTENT_TYPE";

	private static final long serialVersionUID = 5953299041958788771L;

	@Id
	private String id;

	@Indexed(unique = true)
	private String label;

	private String desc;

	private String icon;

	@Field("oldVersions")
	@Reference
	private List<HSMContentType> oldVersions;

	private String createdBy;
	private Long createdStamp;

	@Override
	public List<HSMContentType> getOldVersions() {
		return oldVersions;
	}

	@Override
	public void setOldVersions(List<HSMContentType> oldVersions) {
		this.oldVersions = oldVersions;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	private HSMContentType id(String id) {
		this.id = id;
		return this;
	}

	private HSMContentType label(String label) {
		this.label = label;
		return this;
	}

	private HSMContentType desc(String desc) {
		this.desc = desc;
		return this;
	}

	private HSMContentType icon(String icon) {
		this.icon = icon;
		return this;
	}

	static {

		LIST.add(new HSMContentType().id("TEXT").label("Text").desc("Send Plain Text").icon("fa fa-font"));

		LIST.add(new HSMContentType().id("DOCUMENT").label("Document").desc("PDF,WORD").icon("fa fa-file-alt"));

		LIST.add(new HSMContentType().id("VIDEO").label("Video").desc("Video").icon("fa fa-youtube"));

		LIST.add(new HSMContentType().id("IMAGE").label("Image").desc("Image").icon("fa fa-image"));

		LIST.add(new HSMContentType().id("LOCATION").label("Location").desc("Location").icon("fa fa-map-marker-alt"));

	}

	public static List<HSMContentType> values() {
		return LIST;
	}

}
