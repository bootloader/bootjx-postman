package com.boot.jx.postman.doc;

import java.io.Serializable;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.mongo.CommonDocInterfaces.ADocumentDTO;
import com.boot.model.TimeModels.TimeStampSupportedModel;

@Document(collection = "TIKAT_FEEDBACK_FORM")
@TypeAlias("FeedbackForm")
public class FeedbackFormDoc extends TimeStampSupportedModel
        implements Serializable, ADocumentDTO<FeedbackFormDoc> {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @Indexed(unique = true, sparse = true)
    private String key;

    private String name;

    private String desc;

    private String isActive;

    private Map<String, Object> updateTime;

    private Map<String, Object> createTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getIsActive() {
        return isActive;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }

    public Map<String, Object> getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Map<String, Object> updateTime) {
        this.updateTime = updateTime;
    }

    public Map<String, Object> getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Map<String, Object> createTime) {
        this.createTime = createTime;
    }

    @Override
    public ADocumentDTO<FeedbackFormDoc> newInstance() {
        return new FeedbackFormDoc();
    }
}

