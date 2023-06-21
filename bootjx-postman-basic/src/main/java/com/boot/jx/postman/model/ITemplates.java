package com.boot.jx.postman.model;

import java.util.Map;

import com.boot.jx.postman.model.Notipy.ChannelType;
import com.boot.jx.postman.model.PostManFile.PDFConverter;

public class ITemplates {

	public interface BasicTemplate {

		public Map<String, Object> options();

		public Map<String, Object> meta();

		public String getDesc();

		public String getTemplate();

		public String getName();

		public String getCode();

		public String getId();

		default public String getCategoryType() {
			return null;
		}

	}

	public interface BasicExternalTemplate {

		public String getCode();

		public void setCode(String code);

		public Map<String, Object> getTemplate();

		public Map<String, Object> getVarMap();

		default public BasicExternalTemplate code(String code) {
			this.setCode(code);
			return this;
		}
	}

	public interface ITemplate extends BasicTemplate {

		boolean isThymleaf();

		public default boolean isGeneric() {
			return false;
		};

		PDFConverter getConverter();

		String getFileName();

		public default ChannelType getChannel() {
			return null;
		}

		public default String getSampleJSON() {
			return this.getFileName() + ".json";
		}

		public default String getHtmlFile() {
			return "html/" + getFileName();
		}

		public default String getSMSFile() {
			return "html/sms/" + getFileName();
		}

		public default String getJsonFile() {
			return "json/" + getFileName();
		}

	}

	public static ITemplate getTemplate(String templateStr) {
		return new TemplateGeneric(templateStr);
	}

	public static class TemplateGeneric implements ITemplate {

		String fileName;

		public TemplateGeneric(String fileName) {
			this.fileName = fileName;
		}

		@Override
		public boolean isThymleaf() {
			return true;
		}

		@Override
		public boolean isGeneric() {
			return true;
		}

		@Override
		public PDFConverter getConverter() {
			return null;
		}

		@Override
		public String getFileName() {
			return this.fileName;
		};

		@Override
		public String toString() {
			return this.fileName;
		};

		@Override
		public String getSampleJSON() {
			return "template-generic.json";
		}

		@Override
		public Map<String, Object> options() {
			return null;
		}

		@Override
		public Map<String, Object> meta() {
			return null;
		}

		@Override
		public String getDesc() {
			return null;
		}

		@Override
		public String getTemplate() {
			return null;
		}

		@Override
		public String getName() {
			return this.fileName;
		}

		@Override
		public String getCode() {
			return null;
		}

		@Override
		public String getId() {
			return null;
		}

	}

	/**
	 * This Enum class is created just for the backward compatibility so that we can
	 * move away from enum based templates towards DB templates or Runtime templates
	 * 
	 * @author lalittanwar
	 *
	 */
	public static enum TemplateDefaultEnum implements ITemplate {
		DEFAULT("DEFAULT"), CONTACT_US("ContactForm"),;

		String fileName;
		PDFConverter converter;
		String sampleJSON;
		boolean thymleaf = true;
		ChannelType channel = null;

		@Override
		public String getFileName() {
			return fileName;
		}

		@Override
		public String getHtmlFile() {
			return "html/" + getFileName();
		}

		@Override
		public String getSMSFile() {
			return "html/sms/" + getFileName();
		}

		@Override
		public String getJsonFile() {
			return "json/" + getFileName();
		}

		TemplateDefaultEnum(String fileName, PDFConverter converter, String sampleJSON, ChannelType channel) {
			this.fileName = fileName;
			this.converter = converter;
			this.sampleJSON = sampleJSON;
			if (this.converter == PDFConverter.JASPER) {
				this.thymleaf = false;
			}
			this.channel = channel;
		}

		TemplateDefaultEnum(String fileName, PDFConverter converter, String sampleJSON) {
			this(fileName, converter, sampleJSON, null);
		}

		TemplateDefaultEnum(String fileName, PDFConverter converter) {
			this(fileName, converter, null, null);
		}

		TemplateDefaultEnum(String fileName, ChannelType channel) {
			this(fileName, null, null, channel);
		}

		TemplateDefaultEnum(String fileName) {
			this(fileName, null, null, null);
		}

		@Override
		public PDFConverter getConverter() {
			return converter;
		}

		@Override
		public String getSampleJSON() {
			if (sampleJSON == null) {
				return this.fileName + ".json";
			}
			return sampleJSON;
		}

		@Override
		public boolean isThymleaf() {
			return thymleaf;
		}

		@Override
		public ChannelType getChannel() {
			return channel;
		}

		public String toString() {
			return this.name();
		}

		@Override
		public Map<String, Object> options() {
			return null;
		}

		@Override
		public Map<String, Object> meta() {
			return null;
		}

		@Override
		public String getDesc() {
			return null;
		}

		@Override
		public String getTemplate() {
			return null;
		}

		@Override
		public String getName() {
			return fileName;
		}

		@Override
		public String getCode() {
			return this.name();
		}

		@Override
		public String getId() {
			return fileName;
		}

	}

}
