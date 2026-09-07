package com.boot.jx.postman.doc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Reference;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.boot.jx.model.AuditCreateEntity;
import com.boot.jx.mongo.CommonDocInterfaces.OldDocVersion;

@Document(collection = HSMLanguage.COLLECTION_NAME)
@TypeAlias("HSMLanguage")
public class HSMLanguage implements Serializable, OldDocVersion<HSMLanguage>, AuditCreateEntity {

    public static final List<HSMLanguage> LIST = new ArrayList<HSMLanguage>();
    public static final String COLLECTION_NAME = "DICT_HSM_LANGUAGE";

    private static final long serialVersionUID = 5953299041958788771L;

    @Id
    private String id;

    private String isoAlpha2Code;

    private String isoAlpha3Code;

    @Indexed(unique = true)
    private String label;

    private String desc;

    private String icon;

    @Field("oldVersions")
    @Reference
    private List<HSMLanguage> oldVersions;

    private String createdBy;
    private Long createdStamp;

    @Override
    public List<HSMLanguage> getOldVersions() {
	return oldVersions;
    }

    @Override
    public void setOldVersions(List<HSMLanguage> oldVersions) {
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

    private HSMLanguage id(String id) {
	this.id = id;
	return this;
    }

    private HSMLanguage label(String label) {
	this.label = label;
	return this;
    }

    private HSMLanguage desc(String desc) {
	this.desc = desc;
	return this;
    }

    private HSMLanguage icon(String icon) {
	this.icon = icon;
	return this;
    }

    static {
	Locale[] locales = Locale.getAvailableLocales();
	for (Locale locale : locales) {
	    HSMLanguage lng = new HSMLanguage().id(locale.toString()).label(locale.getDisplayName());
	    lng.setIsoAlpha3Code(locale.getISO3Language());
	    lng.setIsoAlpha2Code(locale.getLanguage());
	    LIST.add(lng);
	}
    }

    public static List<HSMLanguage> values() {
	return LIST;
    }

    public String getIsoAlpha2Code() {
	return isoAlpha2Code;
    }

    public void setIsoAlpha2Code(String isoAlpha2Code) {
	this.isoAlpha2Code = isoAlpha2Code;
    }

    public String getIsoAlpha3Code() {
	return isoAlpha3Code;
    }

    public void setIsoAlpha3Code(String isoAlpha3Code) {
	this.isoAlpha3Code = isoAlpha3Code;
    }

}
