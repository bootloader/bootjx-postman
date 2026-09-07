package com.boot.jx.postman.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.boot.jx.rest.RestService;
import com.boot.utils.ArgUtil;

@Component
public class ExtUtilService {

	private String googleSecret = "6LeK33AUAAAAANWoO_wM5_3FxJ0DoPjPZp_n7pVz";

	private static final String tinyUrl = "http://tinyurl.com/api-create.php?url=";
	private static final String trimUrl = "http://api.tr.im/v1/trim_simple?url=";

	@Autowired
	RestService restService;

	public Boolean verifyCaptcha(String responseKey, String remoteIP) {
		@SuppressWarnings("unchecked")
		Map<String, Object> resp = restService.ajax("https://www.google.com/recaptcha/api/siteverify").acceptJson()
				.field("secret", googleSecret).field("response", responseKey).field("remoteip", remoteIP).postForm()
				.as(Map.class);
		if (resp != null) {
			return ArgUtil.parseAsBoolean(resp.get("success"));
		}
		return false;

	}

	public String tinyUrl(String url) {
		String tinyUrlLookup = tinyUrl + url;
		BufferedReader reader;
		try {
			reader = new BufferedReader(new InputStreamReader(new URL(tinyUrlLookup).openStream()));
			return reader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			return url;
		}
	}

	public String trimUrl(String url) {
		String tinyUrlLookup = trimUrl + url;
		BufferedReader reader;
		try {
			reader = new BufferedReader(new InputStreamReader(new URL(tinyUrlLookup).openStream()));
			return reader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			return url;
		}
	}
}
