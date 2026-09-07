package com.boot.jx.postman.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.boot.jx.AppContextUtil;
import com.boot.jx.http.CommonHttpRequest;
import com.boot.jx.mongo.CommonMongoTemplateAbstract;
import com.boot.jx.postman.doc.VisitorActivityDoc;
import com.boot.utils.ArgUtil;

@Component
public class VisitorActivityStore extends CommonMongoTemplateAbstract {

	private static final Logger LOGGER = LoggerFactory.getLogger(VisitorActivityStore.class);

	@Autowired
	private CommonHttpRequest commonHttpRequest;

	public void save(VisitorActivityDoc doc) {
		doc.origin(commonHttpRequest.get("origin")).referer(commonHttpRequest.get("referer"));
		doc.setTraceId(AppContextUtil.getTraceId());

		if (!ArgUtil.is(doc.getVisitorId())) {
			doc.setVisitorId(commonHttpRequest.get("visitorId"));
		}

		if (!ArgUtil.is(doc.getVisitId())) {
			doc.setVisitId(commonHttpRequest.get("visitId"));
		}

		super.save(doc);
	}

}
