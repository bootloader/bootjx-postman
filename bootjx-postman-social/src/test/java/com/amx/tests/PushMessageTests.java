package com.amx.tests;

import java.math.BigDecimal;
import java.text.ParseException;

import com.boot.jx.model.CommonFile;
import com.boot.jx.postman.PMConfiguration;
import com.boot.jx.postman.pbook.PBName;
import com.boot.jx.postman.plugin.TwitterPlugin.TwitterConfigDetails;
import com.boot.jx.scope.tnt.Tenants;
import com.boot.utils.ArgUtil;
import com.boot.utils.JsonUtil;

public class PushMessageTests { // Noncompliant

	static BigDecimal country = new BigDecimal(30);
	static BigDecimal customer = new BigDecimal(30333);
	static String tnt = Tenants.DEFAULT_STR;
	static String FORMAT = "%10s : %-10s : %10s";

	public static void main(String[] args) throws ParseException {

		PBName name = new PBName();
		name.setFormattedName("lalit nara gsss e   tanwar");
		// name.setFirstName("lalit");
		name.setLastName("tanwar");
		name.setMiddleName("narayan");
		name.fix();

		System.out.println(name.getFirstName());
		System.out.println(name.getMiddleName());
		System.out.println(name.getLastName());
		System.out.println(name.getFormattedName());

	}

	public static void main5(String[] args) throws ParseException {

		CommonFile file = new CommonFile().url(
				"http://meherydata.s3.amazonaws.com/lntinfotech/quickmedia/5f36e371-b7d7-46ad-bc2d-2f631751c3be/WhatsApp Video 2022-06-09 at 8.20.51 PM.mp4");
		System.out.println(file.getFileType());

	}

	public static void main4(String[] args) throws ParseException {
		PMConfiguration config = PMConfiguration.instance();
		String key = "@$test.s";
		TwitterConfigDetails tw = new TwitterConfigDetails();
		tw.setHandler(key);
		String json = JsonUtil.toJson(config);
		System.out.println(json);
		System.out.println(JsonUtil.toJson(JsonUtil.parse(json, PMConfiguration.class)));

	}

	/**
	 * This is just a test method
	 * 
	 * @param args
	 * @throws ParseException
	 */
	public static void main3(String[] args) throws ParseException {
		String id;

		if (ArgUtil.is(id = getNull()))
			System.out.println("WTF " + id);

		if (ArgUtil.is(id = getNoNull()))
			System.out.println("Hmm ok " + id);
	}

	private static String getNull() {
		return null;
	}

	private static String getNoNull() {
		return "OKKKK";
	}

}
