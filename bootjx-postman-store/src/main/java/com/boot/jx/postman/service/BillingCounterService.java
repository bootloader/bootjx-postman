package com.boot.jx.postman.service;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.boot.jx.AppContextUtil;
import com.boot.jx.dict.ContactType;
import com.boot.jx.mongo.CommonMongoTemplate;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.PMEnvironment.PMDomainConfig;
import com.boot.jx.postman.doc.ChatContactDoc;
import com.boot.jx.postman.doc.DAUCounterDoc;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.query.ChatContactQuery;
import com.boot.jx.postman.store.MessageContext;
import com.boot.jx.utils.PostManUtil;
import com.boot.utils.ArgUtil;

@Service
public class BillingCounterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BillingCounterService.class);
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final ZoneId DEFAULT_TIMEZONE = ZoneId.of("Asia/Kolkata");

    @Autowired
    private PMDomainConfig pmDomainConfig;

    @Autowired
    private CommonMongoTemplate commonMongoTemplate;
    
    @Autowired
    private PMEnvironment pmEnvironment;

    /**
     * Optimized method using MessageContext to avoid DB calls
     * Increments both DAU and MAU counters with channel-wise segregation
     * 
     * @param messageContext MessageContext containing cached ChatContactQuery
     * @param messageType "I" for inbound, "O" for outbound
     * @return true if any counter was incremented, false otherwise
     */
    public boolean incrementCounters(MessageContext messageContext, String messageType) {
        if (messageContext == null) {
            LOGGER.warn("DAU Counter: messageContext is null, skipping");
            return false;
        }

        try {
            // OPTIMIZATION: Get ChatContactQuery from MessageContext (cached, no DB call)
            ChatContactQuery contactQuery = messageContext.contact();
            if (contactQuery == null) {
                LOGGER.warn("DAU Counter: No contact query in MessageContext");
                return false;
            }

            ChatContactDoc chatContactDoc = contactQuery.getDoc();
            if (chatContactDoc == null) {
                LOGGER.warn("DAU Counter: No contact doc in ChatContactQuery");
                return false;
            }

            String contactId = chatContactDoc.getContactId();
            String contactType = chatContactDoc.getContactType();
            String lane = chatContactDoc.getLane();  // Extract lane from ChatContactDoc

            // Fallback: Extract from contactId if lane is empty
            if (ArgUtil.isEmpty(lane)) {
                if (contactId != null && contactId.contains("_")) {
                    String[] parts = contactId.split("_");
                    if (parts.length > 1) {
                        lane = parts[1];
                    }
                }
                if (ArgUtil.isEmpty(lane)) {
                    lane = "DEFAULT";
                    LOGGER.warn("DAU Counter: Lane is empty, using DEFAULT fallback for contactId: {}", contactId);
                }
            }

            if (ArgUtil.isEmpty(contactId)) {
                LOGGER.warn("DAU Counter: contactId is empty, skipping");
                return false;
            }
            
            String currentTenant = AppContextUtil.getTenant();
            if (ArgUtil.isEmpty(currentTenant)) {
                LOGGER.warn("DAU Counter: tenant is empty, skipping");
                return false;
            }

            String channelId = PostManUtil.CHANNEL_ID(chatContactDoc);
            ChannelConfig channelConfig = pmEnvironment.config().channel(channelId);
            if (channelConfig == null) {
                LOGGER.warn("DAU Counter: channel not registered in current tenant, skipping. channelId: {}, tenant: {}", channelId, currentTenant);
                return false;
            }

            LOGGER.debug("DAU Counter: Processing contactId: {}, contactType: {}, lane: {}, messageType: {}", 
                    contactId, contactType, lane, messageType);

            // 1. Get current dates in domain timezone
            String todayDAUDate = getCurrentDAUDate();
            String currentMonth = getCurrentMAUMonth();

            // 2. Check if should increment
            boolean shouldIncrementDAU = shouldIncrementCounter(chatContactDoc, todayDAUDate);
            boolean shouldIncrementMAU = shouldIncrementMAU(chatContactDoc, currentMonth);

            if (shouldIncrementDAU || shouldIncrementMAU) {
                // 3. Increment counters atomically
                incrementCounters(todayDAUDate, contactType, lane, messageType, shouldIncrementDAU, shouldIncrementMAU);

                // 4. Update stamps in cached ChatContactQuery (caller will commit later to avoid double commits)
                if (shouldIncrementDAU) {
                    chatContactDoc.setLastDAUStamp(todayDAUDate);
                    contactQuery.setLastDAUStamp(todayDAUDate);
                }
                if (shouldIncrementMAU) {
                    chatContactDoc.setLastMAUStamp(currentMonth);
                    contactQuery.setLastMAUStamp(currentMonth);
                }

                // Note: 
                // - Outbound: ConnectorHandlerFactory commits at line 465
                // - Inbound: MessageEventsImpl commits after billing counter call

                LOGGER.debug("DAU Counter: Successfully incremented counters for contact: {}, date: {}", 
                        contactId, todayDAUDate);
                return true;
            } else {
                LOGGER.debug("DAU Counter: Counters NOT incremented - contact already counted. contactId: {}, lastDAUStamp: {}, lastMAUStamp: {}", 
                        contactId, chatContactDoc.getLastDAUStamp(), chatContactDoc.getLastMAUStamp());
            }
            return false;
        } catch (Exception e) {
            LOGGER.error("DAU Counter: Error incrementing counters for messageType: " + messageType, e);
            return false;
        }
    }

    /**
     * Get current date in domain timezone (YYYYMMDD format)
     */
    private String getCurrentDAUDate() {
        try {
            String tzString = pmDomainConfig.getTimeZoneFromSetup();
            ZoneId zoneId = parseTimeZone(tzString);
            LocalDate today = LocalDate.now(zoneId);
            return today.format(DATE_FORMATTER);
        } catch (Exception e) {
            LOGGER.warn("Error getting timezone, using default", e);
            LocalDate today = LocalDate.now(DEFAULT_TIMEZONE);
            return today.format(DATE_FORMATTER);
        }
    }

    /**
     * Get current month in domain timezone (YYYYMM format)
     */
    private String getCurrentMAUMonth() {
        try {
            String tzString = pmDomainConfig.getTimeZoneFromSetup();
            ZoneId zoneId = parseTimeZone(tzString);
            LocalDate today = LocalDate.now(zoneId);
            return today.format(MONTH_FORMATTER);
        } catch (Exception e) {
            LOGGER.warn("Error getting timezone for MAU, using default", e);
            LocalDate today = LocalDate.now(DEFAULT_TIMEZONE);
            return today.format(MONTH_FORMATTER);
        }
    }

    /**
     * Parse timezone string (reuse pattern from ContactStore)
     */
    private ZoneId parseTimeZone(String tz) {
        if (tz != null && tz.contains("::")) {
            tz = tz.split("::")[0];
        }
        try {
            return ZoneId.of(tz != null ? tz : "Asia/Kolkata");
        } catch (DateTimeException e) {
            LOGGER.warn("Invalid timezone: {}, using default", tz);
            return DEFAULT_TIMEZONE;
        }
    }

    /**
     * Check if counter should be incremented
     */
    private boolean shouldIncrementCounter(ChatContactDoc contactDoc, String todayDAUDate) {
        if (contactDoc == null) {
            return true; // New contact
        }

        String lastDAUStamp = contactDoc.getLastDAUStamp();
        if (ArgUtil.isEmpty(lastDAUStamp)) {
            return true; // Never counted before
        }

        // Compare dates (YYYYMMDD format allows string comparison)
        return lastDAUStamp.compareTo(todayDAUDate) < 0;
    }

    /**
     * Check if MAU counter should be incremented
     */
    private boolean shouldIncrementMAU(ChatContactDoc contactDoc, String currentMonth) {
        if (contactDoc == null) {
            return true; // New contact
        }

        String lastMAUStamp = contactDoc.getLastMAUStamp();
        if (ArgUtil.isEmpty(lastMAUStamp)) {
            return true; // Never counted before
        }

        // Compare months (YYYYMM format allows string comparison)
        return lastMAUStamp.compareTo(currentMonth) < 0;
    }

    /**
     * Map ContactType to channel key for channel-wise counters
     * Returns the ContactType enum name as stored in database
     * Returns null for unknown/invalid contactTypes
     * Tracks ALL channels channel-wise (WHATSAPP, INSTAGRAM, SMS, EMAIL, TELEGRAM, etc.)
     */
    private String getChannelKey(String contactType) {
        if (ArgUtil.isEmpty(contactType)) {
            return null;
        }

        // Parse ContactType enum from string (as stored in database)
        ContactType type = ArgUtil.parseAsEnumT(contactType, ContactType.class);
        if (type == null) {
            LOGGER.warn("DAU Counter: Unknown contactType: {}", contactType);
            return null;
        }

        // Exclude EMPTY type from channel-wise tracking (EMPTY is an invalid placeholder)
        if (type == ContactType.EMPTY) {
            return null;
        }

        // Return enum name for ALL valid channels (e.g., "WHATSAPP", "INSTAGRAM", "SMS", "EMAIL", "TELEGRAM", etc.)
        return type.name();
    }

    /**
     * Generate document _id in format: <channel>:<lane>:<date>
     * @param channelKey Channel name (e.g., "WHATSAPP", "INSTAGRAM")
     * @param lane Lane identifier (e.g., "918828218374")
     * @param dauDate Date in YYYYMMDD format
     * @return _id string (e.g., "WHATSAPP:918828218374:20251218")
     */
    private String generateDocumentId(String channelKey, String lane, String dauDate) {
        if (ArgUtil.isEmpty(channelKey)) {
            channelKey = "UNKNOWN";
        }
        if (ArgUtil.isEmpty(lane)) {
            lane = "DEFAULT";
        }
        // Sanitize: Replace colons in lane (safety check)
        lane = lane.replace(":", "_");
        return channelKey + ":" + lane + ":" + dauDate;
    }

    /**
     * Atomically increment DAU and MAU counters with channel-wise segregation
     * OPTIMIZED: Single atomic operation for all counters
     */
    private void incrementCounters(String dauDate, String contactType, String lane, String messageType,
                                   boolean shouldIncrementDAU, boolean shouldIncrementMAU) {
        String dauMonth = dauDate.substring(0, 6);

        // Get channel key
        String channelKey = getChannelKey(contactType);
        if (channelKey == null) {
            channelKey = "UNKNOWN";
        }

        // Generate document _id with channel:lane:date format
        String documentId = generateDocumentId(channelKey, lane, dauDate);
       
        String tenant = AppContextUtil.getTenant();

        //match _id AND (tenant=us OR tenant=null) - claims pre-existing null-tenant docs
        Criteria idMatch = Criteria.where("_id").is(documentId);
        Criteria tenantMatch = new Criteria().orOperator(
                Criteria.where("tenant").is(tenant),
                Criteria.where("tenant").is(null));
        Query query = new Query(idMatch.andOperator(tenantMatch));

        // Build update with $inc
        Update update = new Update();
        
        // Initialize top-level fields on insert only
        update.setOnInsert("dauMonth", Integer.parseInt(dauMonth));
        update.setOnInsert("dauDate", Integer.parseInt(dauDate));
        update.set("tenant", tenant);  // Set on insert and when claiming null-tenant doc
        // Note: dauWise and mauWise are NOT initialized here - MongoDB will auto-create nested fields
        // when using $inc on nested paths (e.g., "dauWise.WHATSAPP")

        // DAU increments
        if (shouldIncrementDAU) {
            update.inc("dauCounter", 1);
            
            // Direction-wise DAU
            if ("I".equals(messageType)) {
                update.inc("dauWise.INBOUND", 1);
            } else if ("O".equals(messageType)) {
                update.inc("dauWise.OUTBOUND", 1);
            }
        }

        // MAU increments
        if (shouldIncrementMAU) {
            update.inc("mauCounter", 1);
            
            // Direction-wise MAU
            if ("I".equals(messageType)) {
                update.inc("mauWise.INBOUND", 1);
            } else if ("O".equals(messageType)) {
                update.inc("mauWise.OUTBOUND", 1);
            }
        }

        try {
            commonMongoTemplate.upsert(query, update, DAUCounterDoc.class);
            LOGGER.debug("DAU Counter: Upsert completed successfully for documentId: {} in tenant: {}", documentId, tenant);
        } catch (DuplicateKeyException e) {
            LOGGER.warn("DAU Counter: Doc exists for other tenant, skipping. documentId: {}, tenant: {}", documentId, tenant);
        } catch (Exception e) {
            LOGGER.error("DAU Counter: Upsert failed for documentId: " + documentId, e);
            throw e;
        }
    }
}








