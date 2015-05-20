package com.janknspank.common;

import static org.junit.Assert.assertEquals;

import java.util.Comparator;

import org.junit.Test;

public class VersionStringComparatorTest {
  @Test
  public void test() throws Exception {
    Comparator<String> comparator = new VersionStringComparator();
    assertEquals(0, comparator.compare("0", "0"));
    assertEquals(0, comparator.compare("0.0.0.0", "0.0"));
    assertEquals(0, comparator.compare("0", "0.0.0.0.0"));

    assertEquals(0, comparator.compare("1", "1"));
    assertEquals(0, comparator.compare("1.1.0.0", "1.1"));
    assertEquals(0, comparator.compare("1", "1.0.0.0.0"));

    assertEquals(-1, comparator.compare("1", "2"));
    assertEquals(-1, comparator.compare("1.1.0.0", "1.2"));
    assertEquals(-1, comparator.compare("1", "1.0.0.0.1"));

    assertEquals(1, comparator.compare("3", "2"));
    assertEquals(1, comparator.compare("1.3.0.0", "1.2.1"));
    assertEquals(1, comparator.compare("1.0.1", "1.0.0.0.1"));

    assertEquals(-1, comparator.compare("30", "200"));
    assertEquals(-1, comparator.compare("1.3.0.0", "1.20.1"));
    assertEquals(-1, comparator.compare("1.0.1", "1.0.020.0.1"));

    assertEquals(0, comparator.compare(null, null));
    assertEquals(1, comparator.compare("1", null));
    assertEquals(-1, comparator.compare(null, "1.0.020.0.1"));
  }
}
