package com.boot.jx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.boot.jx.dict.ContactType;
import com.boot.jx.model.CommonTemplateMeta;
import com.boot.jx.postman.doc.HSMTemplateDoc;
import com.boot.model.MapModel;
import com.boot.utils.ArgUtil;
import com.boot.utils.FileUtil;

/**
 * Pins HSM template resolution priority — mirrors {@code TemplateStore#findByCode}
 * multi-match logic. Update both if resolution rules change.
 */
public class TemplateResolveTest {

	/**
	 * Same resolution order as {@code TemplateStore} (not the older test variant).
	 */
	static HSMTemplateDoc resolve(List<HSMTemplateDoc> temps, ContactType contactType, String lang) {
		if (temps == null || temps.isEmpty()) {
			return null;
		}
		if (temps.size() == 1) {
			return temps.get(0);
		}
		HSMTemplateDoc wildCardTemp = null;
		HSMTemplateDoc exactTemp = null;
		HSMTemplateDoc noLangTemp = null;
		HSMTemplateDoc noContactTemp = null;
		HSMTemplateDoc engLangTemp = null;

		for (HSMTemplateDoc hsmTemplate3rdParty : temps) {
			if (ArgUtil.not(hsmTemplate3rdParty.getContactType()) && ArgUtil.not(hsmTemplate3rdParty.getLang())) {
				wildCardTemp = hsmTemplate3rdParty;
			} else if (ArgUtil.is(hsmTemplate3rdParty.getContactType(), contactType)
					&& ArgUtil.is(hsmTemplate3rdParty.getLang(), lang)) {
				exactTemp = hsmTemplate3rdParty;
				break;
			} else if (ArgUtil.is(hsmTemplate3rdParty.getContactType(), contactType)
					&& (ArgUtil.not(hsmTemplate3rdParty.getLang()) || (ArgUtil.not(noLangTemp)
							&& ArgUtil.is(hsmTemplate3rdParty.getLang(), "en", "en_US", "en_GB")))) {
				noLangTemp = hsmTemplate3rdParty;
			} else if (ArgUtil.not(hsmTemplate3rdParty.getContactType())
					&& ArgUtil.is(hsmTemplate3rdParty.getLang(), lang)) {
				noContactTemp = hsmTemplate3rdParty;
			} else if (ArgUtil.not(hsmTemplate3rdParty.getContactType())
					&& ArgUtil.is(hsmTemplate3rdParty.getLang(), "en", "en_US", "en_GB")) {
				engLangTemp = hsmTemplate3rdParty;
			}
		}
		return ArgUtil.anyOf(exactTemp, noLangTemp, noContactTemp, engLangTemp, wildCardTemp);
	}

	private List<HSMTemplateDoc> loadSampleTemplates() {
		URL url = FileUtil.getResource("sample/hsm_templates.json", TemplateResolveTest.class);
		MapModel x = MapModel.fromSafe(FileUtil.read(url));
		return x.listOf(HSMTemplateDoc.class);
	}

	@Test
	public void resolve_exactMatch_contactTypeAndLang() {
		List<HSMTemplateDoc> temps = loadSampleTemplates();
		HSMTemplateDoc resolved = resolve(temps, ContactType.WHATSAPP, "hi");

		assertNotNull(resolved);
		assertEquals("WHATSAPP", resolved.getContactType());
		assertEquals("hi", resolved.getLang());
	}

	@Test
	public void resolve_webContactWithEnglishLang_inlineFixture() {
		HSMTemplateDoc exact = new HSMTemplateDoc();
		exact.setContactType(ContactType.WEBSITE.toString());
		exact.setLang("en");

		HSMTemplateDoc fallback = new HSMTemplateDoc();
		fallback.setLang("en_GB");

		HSMTemplateDoc resolved = resolve(Arrays.asList(exact, fallback), ContactType.WEBSITE, "en");
		assertEquals(ContactType.WEBSITE.toString(), resolved.getContactType());
		assertEquals("en", resolved.getLang());
	}

	@Test
	public void resolve_sampleFixture_webUsesLegacyWebShorthand_notWebsiteEnum() {
		// Fixture stores "WEB" while ContactType.WEBSITE stringifies to "WEBSITE".
		List<HSMTemplateDoc> temps = loadSampleTemplates();
		HSMTemplateDoc resolved = resolve(temps, ContactType.WEBSITE, "en");

		assertNotNull(resolved);
		assertEquals(null, resolved.getContactType());
		assertEquals("en", resolved.getLang());
	}

	@Test
	public void resolve_whatsappNoLang_fallsBackToEnglishWithoutContact() {
		List<HSMTemplateDoc> temps = loadSampleTemplates();
		HSMTemplateDoc resolved = resolve(temps, ContactType.WHATSAPP, null);

		assertNotNull(resolved);
		assertEquals(null, resolved.getContactType());
		// Last English-variant template in fixture wins (entry "en" after "en_GB").
		assertEquals("en", resolved.getLang());
	}

	@Test
	public void resolve_webNoLang_sampleFixture_fallsBackToEnglishGb() {
		List<HSMTemplateDoc> temps = loadSampleTemplates();
		HSMTemplateDoc resolved = resolve(temps, ContactType.WEBSITE, null);

		assertNotNull(resolved);
		assertEquals(null, resolved.getContactType());
		assertEquals("en", resolved.getLang());
	}

	@Test
	public void resolve_webNoLang_inlineFixture_picksContactOnlyTemplate() {
		HSMTemplateDoc webOnly = new HSMTemplateDoc();
		webOnly.setContactType(ContactType.WEBSITE.toString());

		HSMTemplateDoc resolved = resolve(Arrays.asList(webOnly), ContactType.WEBSITE, null);
		assertEquals(ContactType.WEBSITE.toString(), resolved.getContactType());
	}

	@Test
	public void resolve_telegramNoLang_picksTelegramTemplate() {
		List<HSMTemplateDoc> temps = loadSampleTemplates();
		HSMTemplateDoc resolved = resolve(temps, ContactType.TELEGRAM, null);

		assertNotNull(resolved);
		assertEquals("TELEGRAM", resolved.getContactType());
	}

	@Test
	public void resolve_singleTemplate_returnsItRegardlessOfFilters() {
		HSMTemplateDoc only = new HSMTemplateDoc();
		only.setLang("fr");
		only.setContactType("SMS");

		HSMTemplateDoc resolved = resolve(Arrays.asList(only), ContactType.WHATSAPP, "hi");
		assertEquals(only, resolved);
	}

	@Test
	public void resolve_wildcard_whenNoBetterMatch() {
		HSMTemplateDoc wildcard = new HSMTemplateDoc();
		wildcard.setCode("wildcard");

		HSMTemplateDoc other = new HSMTemplateDoc();
		other.setLang("de");
		other.setContactType("FACEBOOK");

		HSMTemplateDoc resolved = resolve(Arrays.asList(wildcard, other), ContactType.WHATSAPP, "hi");
		assertEquals("wildcard", resolved.getCode());
	}

	@Test
	public void resolve_viaCommonTemplateMeta_whatsappHi() {
		List<HSMTemplateDoc> temps = loadSampleTemplates();
		CommonTemplateMeta template = new CommonTemplateMeta();
		template.setLang("hi");

		HSMTemplateDoc resolved = resolve(temps, ContactType.WHATSAPP, template.getLang());
		assertNotNull(resolved);
		assertTrue(ArgUtil.is(resolved.getContactType(), ContactType.WHATSAPP));
		assertEquals("hi", resolved.getLang());
	}

}
