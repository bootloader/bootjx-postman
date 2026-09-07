package com.boot.jx.postman.doc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.model.AuditCreateEntity;
import com.boot.jx.postman.model.Attachment;
import com.boot.jx.postman.model.ITemplates.BasicTemplate;
import com.boot.jx.postman.model.ResourceMeta;
import com.boot.jx.postman.store.QuickStore.QuickGalleryItem;
import com.boot.utils.ArgUtil;

@Document(collection = HSMTemplateDoc.COLLECTION_NAME)
@TypeAlias("HSMTemplate")
public class HSMTemplateDoc implements Serializable, BasicTemplate, AuditCreateEntity, ResourceMeta, QuickGalleryItem {

	public static final String COLLECTION_NAME = "DICT_HSM_TEMPLATES";
	public static final String COLLECTION_NAME_TRASH = "TRASH_DICT_HSM_TEMPLATES";

	private static final long serialVersionUID = 5953299041958788771L;

	@Id
	private String id;

	@Indexed(unique = true)
	private String name;

	@Indexed
	private String code;

	@Indexed
	private String contactType;

	@Indexed
	private String lang;

	private String category;
	private String categoryType;
	private String categorySubType;

	private String desc;

	// message structure
	private String formatType;
	private String header;
	private String body;
	private String footer;
	protected Map<String, Object> options;
	private List<Attachment> attachments;

	private String template;
	private Map<String, Object> meta;
	protected Map<String, Object> model;
	
	private Map<String, Object> profileVarMap;

	public static class ApprovedChannels {
		public String channelId;
		public String templateId;
		public String status;
	}
	
	public static class TptInfo {
		public String channelType;
		public String channelId;
		public String dltContentId;
	}

	private List<ApprovedChannels> approved;
	
	private List<TptInfo> tptinfo;

	private String createdBy;
	private Long createdStamp;

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	@Override
	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	@Override
	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public Map<String, Object> getMeta() {
		return meta;
	}

	@Override
	public void setMeta(Map<String, Object> meta) {
		this.meta = meta;
	}

	@Override
	public Map<String, Object> meta() {
		if (this.meta == null) {
			this.meta = new HashMap<String, Object>();
		}
		return this.meta;
	}

	public Map<String, Object> getOptions() {
		return options;
	}

	public void setOptions(Map<String, Object> options) {
		this.options = options;
	}

	@Override
	public Map<String, Object> options() {
		if (this.options == null) {
			this.options = new HashMap<String, Object>();
		}
		return this.options;
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

	public String getHeader() {
		return header;
	}

	public void setHeader(String header) {
		this.header = header;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getContactType() {
		return contactType;
	}

	public void setContactType(String contactType) {
		this.contactType = contactType;
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public List<Attachment> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<Attachment> attachments) {
		this.attachments = attachments;
	}

	public List<Attachment> attachments() {
		if (this.attachments == null) {
			this.attachments = new ArrayList<Attachment>();
		}
		return attachments;
	}

	public HSMTemplateDoc attachment(Attachment... attachments) {
		for (Attachment file : attachments) {
			this.attachments().add(file);
		}
		return this;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getFooter() {
		return footer;
	}

	public void setFooter(String footer) {
		this.footer = footer;
	}

	public String getCategoryType() {
		if (categoryType == null) {
			return ArgUtil.parseAsString(this.meta().get("messageType"));
		}
		return categoryType;
	}

	public void setCategoryType(String categoryType) {
		this.categoryType = categoryType;
	}

	public String getFormatType() {
		if (formatType == null) {
			return ArgUtil.parseAsString(this.meta().get("contentType"));
		}
		return formatType;
	}

	public void setFormatType(String formatType) {
		this.formatType = formatType;
	}

	public Map<String, Object> getModel() {
		return model;
	}

	public void setModel(Map<String, Object> model) {
		this.model = model;
	}

	public List<ApprovedChannels> getApproved() {
		return approved;
	}

	public void setApproved(List<ApprovedChannels> approved) {
		this.approved = approved;
	}
	
	public List<TptInfo> getTptinfo() {
		return tptinfo;
	}

		public void setTptinfo(List<TptInfo> tptinfo) {
		this.tptinfo = tptinfo;
	}

	public HSMTemplateDoc approved(String channelId, String templateId, String status) {
		if (this.approved == null) {
			this.approved = new ArrayList<ApprovedChannels>();
		}
		for (ApprovedChannels approvedChannels : approved) {
			if (ArgUtil.areEqual(approvedChannels.channelId, channelId)
					&& ArgUtil.areEqual(approvedChannels.templateId, templateId)) {
				approvedChannels.status = status;
				return this;
			}
		}
		ApprovedChannels approvedChannel = new ApprovedChannels();
		approvedChannel.channelId = channelId;
		approvedChannel.templateId = templateId;
		approvedChannel.status = status;
		this.approved.add(approvedChannel);

		this.approved = this.approved.stream().filter(link -> ArgUtil.is(link.status)).collect(Collectors.toList());;

		return this;
	}
	
	
	public HSMTemplateDoc addTptInfo(String channelType, String channelId, String dltContentId) {
		if (this.tptinfo == null) {
		this.tptinfo = new ArrayList<TptInfo>();
		}
		// Remove existing entry for this channelId if exists
		this.tptinfo.removeIf(info -> ArgUtil.areEqual(info.channelId, channelId));

		// Add new entry
		TptInfo tptInfo = new TptInfo();
		tptInfo.channelType = channelType;
		tptInfo.channelId = channelId;
		tptInfo.dltContentId = dltContentId;
		this.tptinfo.add(tptInfo);

		return this;
	}

	@Override
	public String getTitle() {
		return this.desc;
	}

	public String getCategorySubType() {
		return categorySubType;
	}

	public void setCategorySubType(String categorySubType) {
		this.categorySubType = categorySubType;
	}

	public Map<String, Object> getProfileVarMap() {
		return profileVarMap;
	}

	public void setProfileVarMap(Map<String, Object> profileVarMap) {
		this.profileVarMap = profileVarMap;
	}

}
