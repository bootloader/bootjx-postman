package com.boot.jx.postman.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.boot.jx.postman.doc.CustomerProfileDoc;
import com.boot.jx.postman.pbook.PBEmail;
import com.boot.jx.postman.pbook.PBPhone;

/**
 * Pins PII masking applied before returning customer profiles to agents/UI.
 */
public class MaskingUtilTest {

	@Test
	public void mask_nullDoc_returnsNull() {
		assertNull(MaskingUtil.mask(null));
	}

	@Test
	public void mask_masksPhonesAndEmails() {
		CustomerProfileDoc doc = new CustomerProfileDoc();
		Set<PBPhone> phones = new HashSet<>();
		PBPhone phone = new PBPhone();
		phone.setCountryCallingCode("91");
		phone.setNationalNumber("993104050");
		phone.setPhone("+91993104050");
		phones.add(phone);
		doc.setPhones(phones);

		Set<PBEmail> emails = new HashSet<>();
		PBEmail email = new PBEmail();
		email.setEmail("user@example.com");
		emails.add(email);
		doc.setEmails(emails);

		CustomerProfileDoc masked = MaskingUtil.mask(doc);

		assertEquals("***", masked.getPhones().iterator().next().getCountryCallingCode());
		assertEquals("**********", masked.getPhones().iterator().next().getNationalNumber());
		assertEquals("**********", masked.getPhones().iterator().next().getPhone());
		assertEquals("**********", masked.getEmails().iterator().next().getEmail());
	}

	@Test
	public void mask_masksAltPhonesAndEmailsInAdditionalInfo() {
		CustomerProfileDoc doc = new CustomerProfileDoc();
		Map<String, Object> additionalInfo = new HashMap<>();

		PBPhone altPhone = new PBPhone();
		altPhone.setCountryCallingCode("91");
		altPhone.setNationalNumber("9876543210");
		altPhone.setPhone("+919876543210");
		List<PBPhone> altPhones = new ArrayList<>();
		altPhones.add(altPhone);
		additionalInfo.put("alt_phones", altPhones);

		PBEmail altEmail = new PBEmail();
		altEmail.setEmail("alt@test.org");
		List<PBEmail> altEmails = new ArrayList<>();
		altEmails.add(altEmail);
		additionalInfo.put("alt_emails", altEmails);

		doc.setAdditionalInfo(additionalInfo);

		CustomerProfileDoc masked = MaskingUtil.mask(doc);

		@SuppressWarnings("unchecked")
		List<PBPhone> maskedAltPhones = (List<PBPhone>) masked.getAdditionalInfo().get("alt_phones");
		@SuppressWarnings("unchecked")
		List<PBEmail> maskedAltEmails = (List<PBEmail>) masked.getAdditionalInfo().get("alt_emails");

		assertEquals("***", maskedAltPhones.get(0).getCountryCallingCode());
		assertEquals("**********", maskedAltPhones.get(0).getPhone());
		assertEquals("**********", maskedAltEmails.get(0).getEmail());
	}

	@Test
	public void maskList_masksEveryProfile() {
		CustomerProfileDoc doc1 = new CustomerProfileDoc();
		Set<PBPhone> phones = new HashSet<>();
		PBPhone phone = new PBPhone();
		phone.setNationalNumber("111");
		phone.setCountryCallingCode("1");
		phones.add(phone);
		doc1.setPhones(phones);

		List<CustomerProfileDoc> masked = MaskingUtil.maskList(java.util.Arrays.asList(doc1, new CustomerProfileDoc()));
		assertEquals(2, masked.size());
		assertTrue(masked.get(0).getPhones().iterator().next().getPhone().contains("*"));
	}

}
