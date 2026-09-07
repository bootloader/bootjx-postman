package com.boot.jx.postman.doc;

import java.io.Serializable;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.mongo.CommonDocInterfaces.IdNumberSupport;
import com.boot.jx.mongo.CommonDocInterfaces.SimpleDocument;
import com.boot.jx.mongo.CommonDocInterfaces.TimeStampIndex.TimeStampDoc;

@Document(collection = "DICT_KNOW_BASE_QA")
@TypeAlias("KnowBaseQA")
public class KnowBaseQA extends TimeStampDoc implements Serializable, IdNumberSupport, SimpleDocument {
	private static final long serialVersionUID = -5649094988762846983L;

	@Id
	private String id;

	@Indexed
	private String parentId;

	@Indexed
	private String pageId;

	private long idNumber;

	private String question;
	private String answer;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public String getPageId() {
		return pageId;
	}

	public void setPageId(String pageId) {
		this.pageId = pageId;
	}

	public long getIdNumber() {
		return idNumber;
	}

	public void setIdNumber(long idNumber) {
		this.idNumber = idNumber;
	}

	public String getQuestion() {
		return question;
	}

	public void setQuestion(String question) {
		this.question = question;
	}

	public String getAnswer() {
		return answer;
	}

	public void setAnswer(String answer) {
		this.answer = answer;
	}

}
