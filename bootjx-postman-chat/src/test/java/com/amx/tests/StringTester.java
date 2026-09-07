package com.amx.tests;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import javax.mail.MessagingException;

import com.boot.jx.postman.wacfb.WacfbClient;
import com.boot.jx.scope.tnt.Tenants;
import com.boot.utils.ArgUtil;
import com.boot.utils.CryptoUtil;

public class StringTester { // Noncompliant

	static BigDecimal country = new BigDecimal(30);
	static BigDecimal customer = new BigDecimal(30333);
	static String tnt = Tenants.DEFAULT_STR;
	static String FORMAT = "%10s : %-10s : %10s";

	public static final String XAUTH_DELIMITER = CryptoUtil.getEncoder().message("%01").decodeURL().toString();

	public static void main(String[] s) throws Exception {
		StringTester test = new StringTester();
		test.variableFinder();
	}

	private void doTest() throws MessagingException, IOException {
		String str = "wa360:918828218374:w";
		System.out.println(str.replace(":", "="));
		System.out.println(str.replace("=", ":"));
	}

	private void variableFinder() throws MessagingException, IOException {
		String inputString = "Shop now through {{startDate}} {{2}} and use code {{promo123}} to get {{456discount}} off of all merchandise.";
		if (ArgUtil.is(inputString)) {
			Matcher matcher = WacfbClient.VARIABLES.matcher(inputString);
			List<String> placeholders = new ArrayList<>();
			while (matcher.find()) {
				placeholders.add(matcher.group(1)); // Group 1 contains the placeholder value
			}
			// Print the list of placeholders
			System.out.println(placeholders);
		}
	}

}
