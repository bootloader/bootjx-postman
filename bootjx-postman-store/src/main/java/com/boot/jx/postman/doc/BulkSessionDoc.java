package com.boot.jx.postman.doc;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.dict.ContactType;
import com.boot.jx.logger.LoggerService.LogTimer;
import com.boot.jx.model.AuditCreateEntity;
import com.boot.jx.model.AuditCreateEntity.AuditIdentifier;
import com.boot.jx.model.AuditCreateEntity.AuditTraceEntity;
import com.boot.jx.postman.PMConstants.CAMPAIGN_MODE;
import com.boot.jx.tunnel.ChronoScheduler;
import com.boot.jx.tunnel.task.JobTaskModel.BatchJob;
import com.boot.utils.ArgUtil;

@Document(collection = "BULK_SESSION")
@TypeAlias("BulkSessionDoc")
public class BulkSessionDoc implements AuditCreateEntity, Serializable, AuditTraceEntity, AuditIdentifier {

	public BulkSessionDoc() {
		super();
		this.timer();
	}

	private static final long serialVersionUID = 2126642970366757413L;

	@Id
	private String bulkSessionId;
	
	/** immediate parent campaign id when created via re-send; null on original */
	private String resendBulkSessionId;

	/** root/original campaign id of the resend chain; null on original */
	private String superBulkSessionId;

	/**
	 * Child campaign only: set from re-send body (Admin or XMS/Node).
	 * Audit values: RFMM | RSMM | RAMM.
	 * RFMM=failed, RSMM=successful, RAMM=all.
	 * Also drives recipient filtering on Admin re-send (direct-list):
	 */
	private String resendType;

	private String traceId;

	private String createdBy;
	private Long createdStamp;

	private String templateId;
	private String template;

	private String contactType;
	private String channelId;
	private String lane;
	private String message;

	private String status;

	private Integer messageCount;
	private Integer messageSentCount;
	private Integer messageFailedCount;

	private Map<String, Long> counts;
	private Map<String, Long> stats;
	private Map<String, Long> errors;
	private Long completedStamp;

	/** group key **/
	@Deprecated
	private String groupId;
	private String campaignTitle;
	private String groupName;
	private ChronoScheduler scheduler;

	private BatchJob job;

	private Map<String, Object> campaignSummary;

	private List<String> groups;
	private List<String> filters;
	private CAMPAIGN_MODE campaignMode;
	private LogTimer timer;
	private Map<String, Long> stamps;

	@Override
	public String getTraceId() {
		return traceId;
	}

	@Override
	public void setTraceId(String traceId) {
		this.traceId = traceId;
	}

	@Override
	public String getCreatedBy() {
		return createdBy;
	}

	@Override
	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	@Override
	public Long getCreatedStamp() {
		return createdStamp;
	}

	@Override
	public void setCreatedStamp(Long createdStamp) {
		this.createdStamp = createdStamp;
	}

	public String getTemplateId() {
		return templateId;
	}

	public void setTemplateId(String templateId) {
		this.templateId = templateId;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public void updateStatus(Object status) {
		this.status = ArgUtil.parseAsString(status);
		this.stamps().put(this.status, System.currentTimeMillis());
	}

	public Integer getMessageCount() {
		return messageCount;
	}

	public void setMessageCount(Integer messageCount) {
		this.messageCount = messageCount;
	}

	public Integer getMessageSentCount() {
		return messageSentCount;
	}

	public void setMessageSentCount(Integer messageSentCount) {
		this.messageSentCount = messageSentCount;
	}

	public Integer getMessageFailedCount() {
		return messageFailedCount;
	}

	public void setMessageFailedCount(Integer messageFailedCount) {
		this.messageFailedCount = messageFailedCount;
	}

	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}

	public String getBulkSessionId() {
		return bulkSessionId;
	}
	
	public String getResendBulkSessionId() {
		return resendBulkSessionId;
	}

	public void setResendBulkSessionId(String resendBulkSessionId) {
		this.resendBulkSessionId = resendBulkSessionId;
	}

	public String getSuperBulkSessionId() {
		return superBulkSessionId;
	}

	public void setSuperBulkSessionId(String superBulkSessionId) {
		this.superBulkSessionId = superBulkSessionId;
	}

	public String getResendType() {
		return resendType;
	}

	public void setResendType(String resendType) {
		this.resendType = resendType;
	}
	
	public void setBulkSessionId(String bulkSessionId) {
		this.bulkSessionId = bulkSessionId;
	}

	public String getContactType() {
		return contactType;
	}

	public void setContactType(String contactType) {
		this.contactType = contactType;
	}

	public ContactType contactType() {
		return ArgUtil.parseAsEnumT(contactType, ContactType.class);
	}

	public String getLane() {
		return lane;
	}

	public void setLane(String lane) {
		this.lane = lane;
	}

	public Map<String, Long> getStats() {
		return stats;
	}

	public void setStats(Map<String, Long> stats) {
		this.stats = stats;
	}

	public Map<String, Long> stats() {
		if (this.stats == null) {
			this.stats = new HashMap<String, Long>();
		}
		return this.stats;
	}

	public Long getCompletedStamp() {
		return completedStamp;
	}

	public void setCompletedStamp(Long completedStamp) {
		this.completedStamp = completedStamp;
	}

	public String getChannelId() {
		return channelId;
	}

	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}

	public BatchJob getJob() {
		return job;
	}

	public void setJob(BatchJob job) {
		this.job = job;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public String getCampaignTitle() {
		return campaignTitle;
	}

	public void setCampaignTitle(String campaignTitle) {
		this.campaignTitle = campaignTitle;
	}

	public ChronoScheduler getScheduler() {
		return scheduler;
	}

	public void setScheduler(ChronoScheduler scheduler) {
		this.scheduler = scheduler;
	}

	public Map<String, Object> getCampaignSummary() {
		return campaignSummary;
	}

	public void setCampaignSummary(Map<String, Object> campaignSummary) {
		this.campaignSummary = campaignSummary;
	}

	public List<String> getGroups() {
		return groups;
	}

	public void setGroups(List<String> groups) {
		this.groups = groups;
	}

	public List<String> getFilters() {
		return filters;
	}

	public void setFilters(List<String> filters) {
		this.filters = filters;
	}

	public Map<String, Long> getErrors() {
		return errors;
	}

	public void setErrors(Map<String, Long> errors) {
		this.errors = errors;
	}

	public Map<String, Long> errors() {
		if (this.errors == null) {
			this.errors = new HashMap<String, Long>();
		}
		return this.errors;
	}

	public void setCampaignMode(CAMPAIGN_MODE campaignMode) {
		this.campaignMode = campaignMode;
	}

	public CAMPAIGN_MODE getCampaignMode() {
		return campaignMode;
	}

	public Map<String, Long> getCounts() {
		return counts;
	}

	public void setCounts(Map<String, Long> counts) {
		this.counts = counts;
	}

	@Override
	public String auditIdentifier() {
		return this.getBulkSessionId();
	}

	public LogTimer getTimer() {
		return timer;
	}

	public void setTimer(LogTimer timer) {
		this.timer = timer;
	}

	public LogTimer timer() {
		if (this.timer == null) {
			this.timer = new LogTimer();
		}
		return this.timer;
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
}
