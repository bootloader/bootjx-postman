package com.boot.jx.postman.wacfb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import org.junit.Test;

/**
 * Pins down {@link WacfbClient#VARIABLES} template placeholder extraction.
 */
public class WacfbClientVariablesTest {

	@Test
	public void variablesPattern_extractsNamedPlaceholders() {
		String input = "Shop now through {{startDate}} {{2}} and use code {{promo123}} to get {{456discount}} off.";
		Matcher matcher = WacfbClient.VARIABLES.matcher(input);
		List<String> placeholders = new ArrayList<>();
		while (matcher.find()) {
			placeholders.add(matcher.group(1));
		}
		assertEquals(4, placeholders.size());
		assertTrue(placeholders.contains("startDate"));
		assertTrue(placeholders.contains("promo123"));
		assertTrue(placeholders.contains("456discount"));
	}

	@Test
	public void variablesPattern_ignoresNonMatchingBraces() {
		String input = "No placeholders here.";
		Matcher matcher = WacfbClient.VARIABLES.matcher(input);
		assertFalse(matcher.find());
	}

}
