package com.boot.jx.postman.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.boot.jx.dict.ContactType;
import com.boot.jx.model.CommonTemplateMeta;
import com.boot.jx.postman.model.ITemplates.BasicExternalTemplate;
import com.boot.jx.postman.model.ITemplates.ITemplate;
import com.boot.jx.postman.model.MessageDefinitions.Contactable;
import com.boot.jx.postman.pbook.PBVCard;
import com.boot.model.MapModel;
import com.boot.utils.ArgUtil;
import com.boot.utils.CollectionUtil;
import com.boot.utils.JsonUtil;
import com.boot.utils.StringUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Message<T extends Message<T>> implements Serializable, MessageOptions {

	private static final long serialVersionUID = 1363933600245334964L;
	public static final String DATA_KEY = "data";
	public static final String RESULTS_KEY = "results";

	public static enum Status {
		SCHLD, CRTD, INIT, SENT, SENT_ERR, SENT_EXC, SENTX, SENTX_ERR, DLVRD, READ, NSENT, BLCKD, LIMIT, FAILD, DELTD,
		CCWIN,

		// INBOUND STATUS
		RECEIVD, FORWARDED, FORWARD_ERR, STATUS_FORWARD_ERR;
		;
	}

	public static class Priority {
		public static final int HIGHEST = 1;
		public static final int MEDIUM = 2;
		public static final int LOWEST = 3;
	}

	public static interface IChannel {
	}

	protected long timestamp;
	protected int attempt;
	protected String subject;
	protected String message = null;
	protected String footer;

	private String formatType;
	private String formatSubType;

	protected List<String> to = null;
	protected List<ContactMeta> contacts = null;
	private CommonTemplateMeta hsm;
	private BasicExternalTemplate templateExt;
	private String action = null;
	private String type = null;

	private Map<String, Object> model = new HashMap<String, Object>();
	protected Map<String, Object> options = new HashMap<String, Object>();
	protected Map<String, Object> meta;

	private List<PostManFile> files = null;
	private List<Attachment> attachments = null;
	private List<PBVCard> vccards;

	private String id;
	private String messageId;
	private String messageIdExt;
	private String messageIdRef;
	private String replyId;
	private String replyIdExt;

	private String sessionId;
	private Contactable contact;

	private String collapseId;
	public int priority;

	private Status status = null;

	private Map<String, Long> stamps;

	private List<String> lines = new ArrayList<String>();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@JsonIgnore
	public void setIChannel(IChannel channel) {
		this.contact().setChannelType(ArgUtil.parseAsString(channel));
	}

	public Map<String, Object> getModel() {
		return model;
	}

	public void setModel(Map<String, Object> model) {
		this.model = model;
	}

	public Map<String, Object> model() {
		if (this.model == null) {
			this.model = new HashMap<String, Object>();
		}
		return model;
	}

	public MapModel modelMap() {
		return MapModel.from(this.model());
	}

	@SuppressWarnings("unchecked")
	@JsonIgnore
	public void setObject(Object object) {
		this.model = JsonUtil.fromJson(JsonUtil.toJson(object), Map.class);
	}

	@JsonIgnore
	public void setModelData(Object object) {
		this.getModel().put(DATA_KEY, object);
	}

	/**
	 * @return the subject
	 */
	public String getSubject() {
		return subject;
	}

	/**
	 * @param subject the subject to set
	 */
	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String text) {
		this.message = text;
	}

	public Message() {
		this.attempt = 0;
		this.timestamp = System.currentTimeMillis();
		this.status = Status.CRTD;
		this.to = new ArrayList<String>();
		this.contacts = new ArrayList<ContactMeta>();
		this.priority = 0;

	}

	public Message(ContactType contactType) {
		this();
		this.contact().type(contactType);
	}

	public List<String> to() {
		if (this.to == null) {
			this.to = new ArrayList<String>();
		}
		return to;
	}

	/**
	 * @return the to
	 */
	public List<String> getTo() {
		return to;
	}

	/**
	 * @param to the to to set
	 */
	public void setTo(List<String> to) {
		this.to = to;
	}

	/**
	 * @param to the to to set
	 */
	public void addTo(String... recieverIds) {
		for (String recieverId : recieverIds) {
			this.to.add(StringUtils.trim(recieverId));
		}
		this.to = CollectionUtil.distinct(this.to);
	}

	public void addLine(String... lines) {
		for (String line : lines) {
			this.lines.add(line);
		}
	}

	public List<String> getLines() {
		return lines;
	}

	public void setLines(List<String> lines) {
		this.lines = lines;
	}

	public String categoryType() {
		return ArgUtil.parseAsString(meta().get("categoryType"));
	}

	public Status getStatus() {
		return status;
	}

	public void status(Status status) {
		this.status = status;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public int getAttempt() {
		return attempt;
	}

	public void setAttempt(int attempt) {
		this.attempt = attempt;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public List<ContactMeta> getContacts() {
		return contacts;
	}

	public void setContacts(List<ContactMeta> contacts) {
		this.contacts = contacts;
	}

	public void addContact(ContactMeta... contacts) {
		for (ContactMeta contact : contacts) {
			this.contacts.add(contact);
		}
	}

	public List<PostManFile> getFiles() {
		return files;
	}

	public void setFiles(List<PostManFile> files) {
		this.files = files;
	}

	public List<PostManFile> files() {
		if (this.files == null) {
			this.files = new ArrayList<PostManFile>();
		}
		return files;
	}

	public void addFile(PostManFile... files) {
		for (PostManFile file : files) {
			this.files().add(file);
		}
	}

	@SuppressWarnings("unchecked")
	public T to(List<String> to) {
		this.setTo(to);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T model(Map<String, Object> model) {
		this.setModel(model);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T to(String to) {
		this.addTo(to);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T subject(String subject) {
		this.setSubject(subject);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T message(String message) {
		this.setMessage(message);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T lang(Object lang) {
		this.hsm().setLang(ArgUtil.parseAsString(lang));
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T put(String key, Object value) {
		this.model().put(key, value);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T data(Object value) {
		this.getModel().put("data", value);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T data(String key, Object value) {
		this.hsm().data().put(key, value);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T file(PostManFile... files) {
		this.addFile(files);
		return (T) this;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public String getCollapseId() {
		return collapseId;
	}

	public void setCollapseId(String collapseId) {
		this.collapseId = collapseId;
	}

	public Map<String, Object> getOptions() {
		return options;
	}

	public void setOptions(Map<String, Object> options) {
		this.options = options;
	}

	@SuppressWarnings("unchecked")
	public T options(Map<String, Object> options) {
		this.options = new HashMap<String, Object>();
		return (T) this;
	}

	public Map<String, Object> options() {
		if (options == null) {
			this.options = new HashMap<String, Object>();
		}
		return this.options;
	}

	@SuppressWarnings("unchecked")
	public T options(String key, Object value) {
		this.options().put(key, value);
		return (T) this;
	}

	public T buttons(List<TmplElement> buttons) {
		return this.options("buttons", buttons);
	}

	public String getMessageIdExt() {
		return messageIdExt;
	}

	public void setMessageIdExt(String messageIdExt) {
		this.messageIdExt = messageIdExt;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
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

	@SuppressWarnings("unchecked")
	public T attachment(Attachment... attachments) {
		for (Attachment file : attachments) {
			this.attachments().add(file);
		}
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T attachment(List<Attachment> attachments) {
		for (Attachment file : attachments) {
			this.attachments().add(file);
		}
		return (T) this;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getMessageIdRef() {
		return messageIdRef;
	}

	public void setMessageIdRef(String messageIdRef) {
		this.messageIdRef = messageIdRef;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public Map<String, Long> getStamps() {
		return stamps;
	}

	public void setStamps(Map<String, Long> stamps) {
		this.stamps = stamps;
	}

	public Map<String, Long> stamps() {
		if (stamps == null)
			stamps = new HashMap<String, Long>();
		return stamps;
	}

	public void updateStatus(Status status) {
		this.status = status;
		this.stamps().put(ArgUtil.parseAsString(status), System.currentTimeMillis());
	}

	public Contactable getContact() {
		return contact;
	}

	public void setContact(Contactable contact) {
		this.contact = contact;
	}

	public Contactable contact() {
		if (this.contact == null) {
			this.contact = new ContactMeta();
		}
		return this.contact;
	}

	@SuppressWarnings("unchecked")
	public T contact(Contactable contact) {
		this.setContact(contact);
		return (T) this;
	}

	public Map<String, Object> getMeta() {
		return meta;
	}

	public void setMeta(Map<String, Object> meta) {
		this.meta = meta;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public Map<String, Object> meta() {
		if (this.meta == null) {
			this.meta = new HashMap<String, Object>();
		}
		return this.meta;
	}

	public MessageMetaWrapper messageMetaWrapper() {
		if (this.meta == null) {
			this.meta = new HashMap<String, Object>();
		}
		return new MessageMetaWrapper(this.meta);
	}

	@Override
	public String toString() {
		return String.format("[messageId:%s]", this.messageId);
	}

	public String getFooter() {
		return footer;
	}

	public void setFooter(String footer) {
		this.footer = footer;
	}

	public BasicExternalTemplate getTemplateExt() {
		return templateExt;
	}

	public void setTemplateExt(BasicExternalTemplate templateExt) {
		this.templateExt = templateExt;
	}

	public String getFormatType() {
		return formatType;
	}

	public void setFormatType(String formatType) {
		this.formatType = formatType;
	}

	public String getFormatSubType() {
		return formatSubType;
	}

	public void setFormatSubType(String formatSubType) {
		this.formatSubType = formatSubType;
	}

	public CommonTemplateMeta hsm() {
		if (this.hsm == null) {
			this.hsm = new CommonTemplateMeta();
		}
		return this.hsm;
	}

	public String templateId() {
		return this.hsm().getId();
	}

	@SuppressWarnings("unchecked")
	public T templateId(String templateId) {
		this.hsm().setId(templateId);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T hsm(CommonTemplateMeta template) {
		this.hsm = template;
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T template(ITemplate template) {
		this.setITemplate(template);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T template(String template) {
		this.hsm().setCode(template);
		return (T) this;
	}

	public String templateCode() {
		return this.hsm().getCode();
	}

	@JsonIgnore
	public void setITemplate(ITemplate template) {
		this.hsm().setCode(template.toString());
	}

	@JsonIgnore
	public ITemplate getITemplate() {
		return ITemplates.getTemplate(this.hsm().getCode());
	}

	public CommonTemplateMeta getHsm() {
		return hsm;
	}

	public void setHsm(CommonTemplateMeta hsm) {
		this.hsm = hsm;
	}

	public String getReplyId() {
		return replyId;
	}

	public void setReplyId(String replyId) {
		this.replyId = replyId;
	}

	public String getReplyIdExt() {
		return replyIdExt;
	}

	public void setReplyIdExt(String replyIdExt) {
		this.replyIdExt = replyIdExt;
	}

	public List<PBVCard> getVccards() {
		return vccards;
	}

	public void setVccards(List<PBVCard> vccards) {
		this.vccards = vccards;
	}

	public List<PBVCard> vccards() {
		if (vccards == null) {
			this.vccards = new ArrayList<PBVCard>();
		}
		return this.vccards;
	}
}
