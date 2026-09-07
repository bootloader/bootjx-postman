package com.boot.jx.postman.model;

import java.io.Serializable;

/**
 * Represents a template status update event from Meta WhatsApp Business API.
 * Triggered when a template is approved, rejected, paused, or disabled by Meta.
 * 
 * This event is created when Meta sends a webhook notification about template status changes.
 * It does not represent a customer message or chat event, but rather an administrative
 * configuration change notification.
 */
public class TemplateStatusEvent implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Channel identifier (e.g., "wacfb/919876543210/+919876543210")
     */
    private String channelId;
    
    /**
     * Template name as registered in Meta (e.g., "welcome_message")
     */
    private String templateName;
    
    /**
     * Template language code (e.g., "en_US", "en", "hi")
     */
    private String language;
    
    /**
     * Status event type from Meta: APPROVED, REJECTED, PAUSED, PENDING, DISABLED
     */
    private String event;
    
    /**
     * Meta's internal template ID
     */
    private String templateId;
    
    /**
     * Rejection or pause reason (if applicable), otherwise "NONE"
     */
    private String reason;
    
    /**
     * Timestamp when this event was received (milliseconds since epoch)
     */
    private Long timestamp;
    
    /**
     * Default constructor - initializes timestamp to current time
     */
    public TemplateStatusEvent() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public String getChannelId() {
        return channelId;
    }
    
    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }
    
    public String getTemplateName() {
        return templateName;
    }
    
    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public String getEvent() {
        return event;
    }
    
    public void setEvent(String event) {
        this.event = event;
    }
    
    public String getTemplateId() {
        return templateId;
    }
    
    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public Long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return String.format("TemplateStatusEvent[name=%s, lang=%s, event=%s]", 
            templateName, language, event);
    }
}

