package com.boot.jx.postman.store;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.boot.jx.postman.pbook.PBPhone;
import com.boot.utils.ArgUtil;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

/**
 * Pins phone normalization logic used by {@code ContactStore#parsePhone} — same
 * algorithm as the legacy test helper in {@code ChatStoreTest}.
 */
public class PhoneParseTest {

	private static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();

	public static PBPhone parsePhone(PBPhone pbPhone) {
		String defaultRegion = "IN";
		if (ArgUtil.not(pbPhone.phone)) {
			pbPhone.phone = String.format("+%s%s", pbPhone.countryCallingCode, pbPhone.nationalNumber);
		}
		pbPhone.phone = pbPhone.phone.replace(" ", "").replaceAll("^[\\+0\\s]+(?!$)", "").trim();
		try {
			PhoneNumber phoneNumber = PHONE_NUMBER_UTIL.parse("+" + pbPhone.phone, defaultRegion);
			pbPhone.nationalNumber = ArgUtil.parseAsString(phoneNumber.getNationalNumber());
			pbPhone.countryCallingCode = ArgUtil.parseAsString(phoneNumber.getCountryCode());
			pbPhone.phone = String.format("+%s%s", phoneNumber.getCountryCode(), phoneNumber.getNationalNumber());
			pbPhone.country = PHONE_NUMBER_UTIL.getRegionCodeForCountryCode(phoneNumber.getCountryCode());
		} catch (NumberParseException e) {
			// mirrors production: invalid numbers are left partially normalized
		}
		return pbPhone;
	}

	@Test
	public void parsePhone_indianMobileWithCountryCode() {
		PBPhone result = parsePhone(new PBPhone().phone("91993104050"));

		assertEquals("91", result.getCountryCallingCode());
		assertEquals("993104050", result.getNationalNumber());
		assertEquals("+91993104050", result.getPhone());
		assertEquals("IN", result.getCountry());
	}

	@Test
	public void parsePhone_stripsLeadingPlusAndSpaces() {
		PBPhone result = parsePhone(new PBPhone().phone("+91 99310 4050"));

		assertEquals("91", result.getCountryCallingCode());
		assertEquals("993104050", result.getNationalNumber());
		assertEquals("+91993104050", result.getPhone());
	}

	@Test
	public void parsePhone_fromNationalNumberAndCallingCode() {
		PBPhone input = new PBPhone();
		input.setCountryCallingCode("91");
		input.setNationalNumber("993104050");

		PBPhone result = parsePhone(input);

		assertEquals("91", result.getCountryCallingCode());
		assertEquals("993104050", result.getNationalNumber());
		assertEquals("+91993104050", result.getPhone());
	}

}
