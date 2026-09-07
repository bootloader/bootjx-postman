package com.boot.jx.postman.dms;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.boot.utils.CryptoUtil;
import com.boot.utils.CryptoUtil.HashBuilder;
import com.boot.utils.UniqueID;

@Component
public class DMSClient {

	@Value("${postman.dms.api.server}")
	private String dmsApiServer;

	@Value("${postman.dms.api.callback}")
	private String dmsApiCallback;

	@Value("${postman.dms.api.key}")
	private String dmsApiKey;

	@Value("${postman.dms.api.secret}")
	private String dmsApiSecret;

	public DMSObject generateUrl(String uniqueKey) {
		return generateUrl(uniqueKey, new HashMap<String, Object>());
	}

	public DMSObject generateUrl(String uniqueKey, Map<String, Object> params) {
		DMSObject dmsObject = new DMSObject();

		dmsObject.setTimestamp(System.currentTimeMillis());
		dmsObject.setPublicId(UniqueID.generateString());
		dmsObject.setSignature(
				new HashBuilder().secret(dmsApiSecret).message(dmsObject.getPublicId()).toHMAC().output());

		params.put("callback", dmsApiCallback);

		dmsObject.setParams(params);
		String paramsd = CryptoUtil.getEncoder().obzect(dmsObject.getParams()).encodeBase64().toString();

		dmsObject.setUploadPageUrl(String.format("%s/ext/upload/frame/%d/%s/%s?params", dmsApiServer,
				dmsObject.getTimestamp(), dmsObject.getPublicId(), dmsObject.getSignature(), paramsd));

		return dmsObject;
	}

}
