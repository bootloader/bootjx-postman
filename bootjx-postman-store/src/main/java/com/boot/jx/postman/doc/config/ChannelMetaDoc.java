package com.boot.jx.postman.doc.config;

import java.io.Serializable;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.mongo.CommonDocInterfaces.TimeStampIndex;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Document(collection = ChannelMetaDoc.DOCUMENT_NAME)
@TypeAlias("ConfigChannelMeta")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChannelMetaDoc implements Serializable {
    public static final String DOCUMENT_NAME = "CONFIG_CHANNEL_META";
    private static final long serialVersionUID = 1L;

    @Id
    private String id;
    private String channelId;

    // Onboarding section - using Map for flexibility
    private Map<String, Object> onboarding;

    // MMLite section - using Map for flexibility  
    private Map<String, Object> mmLiteInfo;
    
    // Action responses section - stores latest responses for each action
    private Map<String, Object> actionResponses;

    private TimeStampIndex createdAt;
    
    // Messaging limit info section
    private Map<String, Object> messagingLimitInfo;
    
    // Call settings section
    private Map<String, Object> callSettings;
    
    // Webhook settings section
    private Map<String, Object> webhookInfo;

    public Map<String, Object> getMessagingLimitInfo() {
        return messagingLimitInfo;
    }

    public void setMessagingLimitInfo(Map<String, Object> messagingLimitInfo) {
        this.messagingLimitInfo = messagingLimitInfo;
    }


    // Constructors
    public ChannelMetaDoc() {}

    public ChannelMetaDoc(String channelId) {
        this.channelId = channelId;
        this.id = channelId;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public Map<String, Object> getOnboarding() {
        return onboarding;
    }

    public void setOnboarding(Map<String, Object> onboarding) {
        this.onboarding = onboarding;
    }

    public Map<String, Object> getMMLiteInfo() {
        return mmLiteInfo;
    }

    public void setMMLiteInfo(Map<String, Object> mmLiteInfo) {
        this.mmLiteInfo = mmLiteInfo;
    }
    
    public Map<String, Object> getActionResponses() {
    	return actionResponses;
    	}

    public void setActionResponses(Map<String, Object> actionResponses) {
    	this.actionResponses = actionResponses;
    }

    public TimeStampIndex getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(TimeStampIndex createdAt) {
        this.createdAt = createdAt;
    }
    
    public Map<String, Object> getCallSettings() {
        return callSettings;
    }

    public void setCallSettings(Map<String, Object> callSettings) {
        this.callSettings = callSettings;
    }
    
    public Map<String, Object> getWebhookInfo() {
        return webhookInfo;
    }

    public void setWebhookInfo(Map<String, Object> webhookInfo) {
        this.webhookInfo = webhookInfo;
    }
} 