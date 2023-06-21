package com.boot.jx.postman.model;

import java.util.Map;

import com.boot.jx.dict.FileFormat;
import com.boot.jx.model.CommonFile;
import com.boot.jx.postman.model.ITemplates.ITemplate;
import com.boot.utils.ArgUtil;
import com.boot.utils.JsonUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PostManFile extends CommonFile {

	private static final long serialVersionUID = -3165262414318034816L;

	public enum PDFConverter {
		AMXFS, FS, FOP, ITEXT5, ITEXT7, JASPER
	}

	private PDFConverter converter;

	public PostManFile() {
		super();
	}

	@SuppressWarnings("unchecked")
	public PostManFile(ITemplate template, Object data, FileFormat fileType) {
		super();
		this.setITemplate(template);
		this.setFileFormat(fileType);
		this.setModel(JsonUtil.fromJson(JsonUtil.toJson(data), Map.class));
	}

	public PostManFile(Object data, FileFormat fileType) {
		super();
		this.setFileFormat(fileType);
		this.setModel(JsonUtil.toJsonMap(data));
	}

	@JsonIgnore
	public void setITemplate(ITemplate template) {
		this.template().setId(ArgUtil.parseAsString(template));
		this.template().setCode(template.getCode());
	}

	@JsonIgnore
	public ITemplate getITemplate() {
		return ITemplates.getTemplate(this.template().getId());
	}

	public PDFConverter getConverter() {
		return converter;
	}

	public void setConverter(PDFConverter converter) {
		this.converter = converter;
	}

}
