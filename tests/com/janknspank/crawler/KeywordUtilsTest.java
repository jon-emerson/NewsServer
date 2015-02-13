package com.janknspank.crawler;

import org.junit.Test;

import com.janknspank.nlp.KeywordUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class KeywordUtilsTest {
  @Test
  public void testIsValidKeyword() throws Exception {
    assertFalse(KeywordUtils.isValidKeyword("1500"));
    assertFalse(KeywordUtils.isValidKeyword("The"));
    assertFalse(KeywordUtils.isValidKeyword("Moo@bloomberg"));
    assertFalse(KeywordUtils.isValidKeyword("Celebrity News"));
    assertFalse(KeywordUtils.isValidKeyword("19-year-old Software Bug"));
    assertFalse(KeywordUtils.isValidKeyword("Best of Tech"));
    assertFalse(KeywordUtils.isValidKeyword("Best Actors of 2014"));
    assertTrue(KeywordUtils.isValidKeyword("The New York Times"));
    assertTrue(KeywordUtils.isValidKeyword("US"));
    assertTrue(KeywordUtils.isValidKeyword("Larry Page"));
    assertTrue(KeywordUtils.isValidKeyword("IBM"));
  }

  @Test
  public void testCleanKeyword() throws Exception {
    assertEquals("Britney Spears", KeywordUtils.cleanKeyword("britney spears "));
    assertEquals("Jack McCrackin", KeywordUtils.cleanKeyword("Jack McCrackin"));
    assertEquals("Edward R. Murrow", KeywordUtils.cleanKeyword("edward r. murrow"));
    assertEquals("R&D", KeywordUtils.cleanKeyword("R&D."));
    assertEquals("IBM", KeywordUtils.cleanKeyword("I.B.M."));
    assertEquals("Twitter", KeywordUtils.cleanKeyword(" twitter,"));
    assertEquals("Wacom", KeywordUtils.cleanKeyword("Wacom’s Intuos"));
    assertEquals("iOS", KeywordUtils.cleanKeyword("iOS"));
  }
}
