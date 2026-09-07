package com.boot.jx.email;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins down {@link EmailReplyParser} / {@link EmailParser} reply extraction.
 */
public class EmailReplyParserTest {

	@Test
	public void parseReply_nullInput_returnsEmptyString() {
		assertTrue(EmailReplyParser.parseReply(null).isEmpty());
	}

	@Test
	public void parseReply_plainText_returnsFullBody() {
		String body = "Thanks for the update.\n\nLet me know if you need anything else.";
		String visible = EmailReplyParser.parseReply(body);
		assertTrue(visible.contains("Thanks for the update"));
		assertTrue(visible.contains("Let me know"));
	}

	@Test
	public void parseReply_quotedLine_hidesQuotedContent() {
		String body = "Sounds good.\n\n> Previous message line one\n> Previous message line two";
		Email email = EmailReplyParser.read(body);
		String visible = email.getVisibleText();
		String hidden = email.getHiddenText();

		assertTrue(visible.contains("Sounds good"));
		assertTrue(hidden.contains("Previous message"));
	}

	@Test
	public void parseReply_signatureLine_hidesSignature() {
		String body = "Please call me back.\n\n--\nJohn Doe";
		Email email = EmailReplyParser.read(body);
		String visible = email.getVisibleText();
		String hidden = email.getHiddenText();

		assertTrue(visible.contains("Please call me back"));
		assertFalse(visible.contains("John Doe"));
		assertTrue(hidden.contains("John Doe") || hidden.contains("--"));
	}

	@Test
	public void read_returnsEmailWithFragments() {
		Email email = EmailReplyParser.read("Hello there");
		assertNotNull(email);
		assertNotNull(email.getFragments());
		assertFalse(email.getFragments().isEmpty());
	}

}
