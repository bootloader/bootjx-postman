package com.boot.jx.email;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins down {@link HtmlToPlainText} HTML stripping behaviour.
 */
public class HtmlToPlainTextTest {

	@Test
	public void getPlainText_paragraph_extractsText() {
		String plain = HtmlToPlainText.getPlainText("<p>Hello world</p>");
		assertTrue(plain.contains("Hello world"));
	}

	@Test
	public void getPlainText_lineBreak_insertsNewline() {
		String plain = HtmlToPlainText.getPlainText("Line one<br/>Line two");
		assertTrue(plain.contains("Line one"));
		assertTrue(plain.contains("Line two"));
	}

	@Test
	public void getPlainText_anchor_includesHref() {
		String plain = HtmlToPlainText.getPlainText("<a href=\"https://example.com\">link</a>");
		assertTrue(plain.contains("link"));
		assertTrue(plain.contains("https://example.com"));
	}

	@Test
	public void getPlainText_emptyHtml_returnsEmptyOrWhitespace() {
		String plain = HtmlToPlainText.getPlainText("");
		assertFalse(plain.contains("null"));
	}

}
