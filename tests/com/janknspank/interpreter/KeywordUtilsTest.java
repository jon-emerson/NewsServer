package com.janknspank.interpreter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class KeywordUtilsTest {
  @Test
  public void testIsValidKeyword() throws Exception {
    assertFalse(KeywordUtils.isValidKeyword("1500"));
    assertFalse(KeywordUtils.isValidKeyword("the"));
    assertFalse(KeywordUtils.isValidKeyword("moo@bloomberg"));
    assertFalse(KeywordUtils.isValidKeyword("celebrity news"));
    assertFalse(KeywordUtils.isValidKeyword("19-year-old Software Bug"));
    assertFalse(KeywordUtils.isValidKeyword("Best of Tech"));
    assertFalse(KeywordUtils.isValidKeyword("Best Actors of 2014"));
    assertTrue(KeywordUtils.isValidKeyword("The New York Times"));
    assertTrue(KeywordUtils.isValidKeyword("US"));
    assertTrue(KeywordUtils.isValidKeyword("Larry Page"));
    assertTrue(KeywordUtils.isValidKeyword("I.B.M."));
  }

  @Test
  public void testCleanKeyword() throws Exception {
    assertEquals("Britney Spears", KeywordUtils.cleanKeyword("britney spears "));
    assertEquals("Jack McCrackin", KeywordUtils.cleanKeyword("Jack McCrackin"));
    assertEquals("Edward R. Murrow", KeywordUtils.cleanKeyword("edward r. murrow"));
    assertEquals("R&D", KeywordUtils.cleanKeyword("R&D."));
    assertEquals("I.B.M.", KeywordUtils.cleanKeyword("I.B.M."));
    assertEquals("Twitter", KeywordUtils.cleanKeyword(" twitter,"));
    assertEquals("Wacom", KeywordUtils.cleanKeyword("Wacom’s Intuos"));
  }
}
