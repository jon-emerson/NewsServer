package com.janknspank.data;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ArticleKeywordsTest {
  @Test
  public void testIsValidKeyword() throws Exception {
    assertFalse(ArticleKeywords.isValidKeyword("1500"));
    assertFalse(ArticleKeywords.isValidKeyword("the"));
    assertFalse(ArticleKeywords.isValidKeyword("moo@bloomberg"));
    assertFalse(ArticleKeywords.isValidKeyword("celebrity news"));
    assertFalse(ArticleKeywords.isValidKeyword("19-year-old Software Bug"));
    assertTrue(ArticleKeywords.isValidKeyword("The New York Times"));
    assertTrue(ArticleKeywords.isValidKeyword("US"));
    assertTrue(ArticleKeywords.isValidKeyword("Larry Page"));
    assertTrue(ArticleKeywords.isValidKeyword("I.B.M."));
  }

  @Test
  public void testCleanKeyword() throws Exception {
    assertEquals("Britney Spears", ArticleKeywords.cleanKeyword("britney spears "));
    assertEquals("Jack McCrackin", ArticleKeywords.cleanKeyword("Jack McCrackin"));
    assertEquals("Edward R. Murrow", ArticleKeywords.cleanKeyword("edward r. murrow"));
    assertEquals("R&D", ArticleKeywords.cleanKeyword("R&D."));
    assertEquals("I.B.M.", ArticleKeywords.cleanKeyword("I.B.M."));
    assertEquals("Twitter", ArticleKeywords.cleanKeyword(" twitter,"));
    assertEquals("Wacom", ArticleKeywords.cleanKeyword("Wacom’s Intuos"));
  }
}
