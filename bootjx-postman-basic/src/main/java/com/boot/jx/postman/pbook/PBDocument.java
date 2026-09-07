package com.boot.jx.postman.pbook;

import java.io.Serializable;

import com.boot.model.UtilityModels.JsonIgnoreUnknown;
import com.boot.model.UtilityModels.UniqueIndex;
import com.boot.utils.ArgUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PBDocument implements Serializable, Comparable<PBDocument>, JsonIgnoreUnknown, UniqueIndex<PBDocument> {

	
	private static final long serialVersionUID = 13406808264190167L;
	public String uuid;
	public String name;
	public String title;
	public String type;
	public String label;
	public String url;
	public String fileFormat;
	public String fileType;
	
	@Override
	public String uuid() {
		return this.uuid;
	}

	@Override
	public String uuid(String uuid) {
		if (ArgUtil.not(this.uuid)) {
			this.uuid = uuid;
		}
		return this.uuid;
	}

	@Override
	public PBDocument update(PBDocument fromObject) {
		this.name = fromObject.getName();
		this.type = fromObject.getType();
		this.title =fromObject.getTitle();
		this.label = fromObject.getLabel();
		this.url = fromObject.getUrl();
		this.fileFormat=fromObject.getFileFormat();
		this.fileType = fromObject.getFileType();
		return this;
	}

	@Override
	public int compareTo(PBDocument o) {
		if (o == null) {
			return 1;
		}
		return this.toString().compareTo(o.toString());
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getFileFormat() {
		return fileFormat;
	}

	public void setFileFormat(String fileFormat) {
		this.fileFormat = fileFormat;
	}

	public String getFileType() {
		return fileType;
	}

	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

}
