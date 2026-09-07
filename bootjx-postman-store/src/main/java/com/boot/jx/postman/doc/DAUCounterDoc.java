package com.boot.jx.postman.doc;

import java.io.Serializable;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "DAU_COUNTER")
@TypeAlias("DAUCounterDoc")
public class DAUCounterDoc implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private String id;  // Format: <channel>:<lane>:<date> (e.g., "WHATSAPP:918828218374:20251218")

    private Integer dauDate;  // Format: YYYYMMDD 

    private Integer dauMonth;  // Format: YYYYMM

    private long dauCounter;

    private long mauCounter;  // Monthly Active Users counter

    private Map<String, Long> dauWise;  // Direction-wise DAU: {INBOUND, OUTBOUND} (channel is in _id)

    private Map<String, Long> mauWise;  // Direction-wise MAU: {INBOUND, OUTBOUND} (channel is in _id)
    
    /** Tenant that owns this doc; null for pre-existing docs */
    private String tenant;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getDauDate() {
        return dauDate;
    }

    public void setDauDate(Integer dauDate) {
        this.dauDate = dauDate;
    }

    public Integer getDauMonth() {
        return dauMonth;
    }

    public void setDauMonth(Integer dauMonth) {
        this.dauMonth = dauMonth;
    }

    public long getDauCounter() {
        return dauCounter;
    }

    public void setDauCounter(long dauCounter) {
        this.dauCounter = dauCounter;
    }

    public long getMauCounter() {
        return mauCounter;
    }

    public void setMauCounter(long mauCounter) {
        this.mauCounter = mauCounter;
    }

    public Map<String, Long> getDauWise() {
        return dauWise;
    }

    public void setDauWise(Map<String, Long> dauWise) {
        this.dauWise = dauWise;
    }

    public Map<String, Long> getMauWise() {
        return mauWise;
    }

    public void setMauWise(Map<String, Long> mauWise) {
        this.mauWise = mauWise;
    }
    
    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }
}


