package com.boot.jx.postman.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TagDocumentTest {

	@Test
	public void addLangUpdatesScoreAndUniqueSortedList() {
		TagDocument tags = new TagDocument();
		tags.addLang("en", "hi", "en", "hi", "hi");

		assertEquals(2, tags.langs().size());
		assertEquals("hi", tags.langs().get(0));
		assertEquals("en", tags.langs().get(1));
		assertEquals(3, tags.langScore("hi"));
		assertEquals(2, tags.langScore("en"));
	}

	@Test
	public void addLangAcceptsList() {
		TagDocument tags = new TagDocument();
		tags.addLang(java.util.Arrays.asList("hi", "en", "hi"));

		assertEquals(2, tags.langs().size());
		assertEquals(2, tags.langScore("hi"));
		assertEquals(1, tags.langScore("en"));
	}

	@Test
	public void mergeTagDocumentCombinesAllTypesUsingWeights() {
		TagDocument sessionTags = new TagDocument();
		sessionTags.addLang("en");

		TagDocument messageTags = new TagDocument();
		messageTags.addLang("hi", "hi");
		messageTags.addCategories("billing");

		sessionTags.merge(messageTags);

		assertEquals(2, sessionTags.langScore("hi"));
		assertEquals(1, sessionTags.langScore("en"));
		assertEquals(1, sessionTags.categoryScore("billing"));
		assertEquals("hi", sessionTags.langs().get(0));
	}

}
