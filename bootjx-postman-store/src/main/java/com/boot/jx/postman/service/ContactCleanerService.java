package com.boot.jx.postman.service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.boot.utils.ArgUtil;
import com.boot.utils.FileUtil;
import com.boot.utils.StringUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

@Component
public class ContactCleanerService {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(ContactCleanerService.class);

	int totaltEmails = 0;
	int totaltMobiles = 0;
	int totaltWhatsApp = 0;
	Map<String, String> mapEmails = new HashMap<String, String>();
	Map<String, String> mapMobiles = new HashMap<String, String>();
	Map<String, String> mapWhatsApp = new HashMap<String, String>();

	Map<String, Boolean> blacklistedEmails = new HashMap<String, Boolean>();

	public ContactCleanerService(@Value("${app.test.email}") String[] appTestEmails,
			@Value("${app.test.mobile}") String[] appTestMobile,
			@Value("${app.test.whatsapp}") String[] appTestWhatsApp) {
		totaltEmails = 0;
		for (int e = 0; e < appTestEmails.length; e++) {
			String emailId = appTestEmails[e];
			if (!ArgUtil.isEmpty(emailId)) {
				mapEmails.put(String.valueOf(totaltEmails), emailId);
				mapEmails.put(emailId, emailId);
				totaltEmails++;
			}
		}
		totaltMobiles = 0;
		for (int m = 0; m < appTestMobile.length; m++) {
			String mobileId = appTestMobile[m];
			if (!ArgUtil.isEmpty(mobileId)) {
				mapMobiles.put(String.valueOf(totaltMobiles), mobileId);
				mapMobiles.put(mobileId, mobileId);
				totaltMobiles++;
			}
		}
		totaltWhatsApp = 0;
		for (int w = 0; w < appTestWhatsApp.length; w++) {
			String waId = appTestWhatsApp[w];
			if (!ArgUtil.isEmpty(waId)) {
				mapWhatsApp.put(String.valueOf(totaltWhatsApp), waId);
				mapWhatsApp.put(waId, waId);
				totaltWhatsApp++;
			}
		}

		readEmails();
	}

	public String getMobile(String mobile) {
		if (totaltMobiles > 0) {
			if (mapMobiles.containsKey(mobile)) {
				return mobile;
			}
			String hash = String.valueOf(StringUtils.hash(mobile, totaltMobiles));
			return mapMobiles.get(hash);
		}
		return mobile;
	}

	public String getEmail(String email) {
		if (totaltEmails > 0) {
			if (mapEmails.containsKey(email)) {
				return email;
			}
			String hash = String.valueOf(StringUtils.hash(email, totaltEmails));
			return mapEmails.get(hash);
		}
		return email;
	}

	public String getWhatsApp(String whatsApp) {
		if (totaltWhatsApp > 0) {
			if (mapWhatsApp.containsKey(whatsApp)) {
				return whatsApp;
			}
			String hash = String.valueOf(StringUtils.hash(whatsApp, totaltWhatsApp));
			return mapWhatsApp.get(hash);
		}
		return whatsApp;
	}

	public boolean isWhatsAppTest(String whatsApp) {
		return mapWhatsApp.containsKey(whatsApp);
	}

	public boolean addWhatsAppTest(String whatsApp) {
		mapWhatsApp.put(whatsApp, whatsApp);
		return false;
	}

	public String[] getEmail(List<String> tos) {
		int length = tos.size();
		List<String> list = new ArrayList<String>();

		for (int i = 0; i < length; i++) {
			String x = getEmail(tos.get(i));
			if (!ArgUtil.isEmpty(x) && !isEmailBlackListed(x)) {
				list.add(x);
			}
		}
		return list.toArray(new String[list.size()]);
	}

	public void readEmails() {
		try {
			CsvSchema bootstrapSchema = CsvSchema.emptySchema().withHeader();
			CsvMapper mapper = new CsvMapper();
			File file = FileUtil.getExternalFile("ext-resources/blacklisted_emails.csv");
			MappingIterator<Map<String, String>> readValues = mapper.readerFor(
					new TypeReference<Map<String, String>>() {
					}).with(bootstrapSchema)
					.readValues(file);
			List<Map<String, String>> x = readValues.readAll();

			for (Map<String, String> row : x) {
				blacklistedEmails.put(row.get("email"), true);
			}
		} catch (Exception e) {
			LOGGER.warn("File : ext-resources/blacklisted_emails.csv is missing put it relative to jar {}",
					e.getMessage());
		}
	}

	public boolean isEmailBlackListed(String email) {
		return blacklistedEmails.getOrDefault(email, false);
	}

}
