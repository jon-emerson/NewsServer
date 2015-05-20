package com.janknspank.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.janknspank.common.Version.VersionComparator;

public class VersionTest {
  @Test
  public void testAtLeast() throws Exception {
    Version version = new Version("0.5.4");
    assertTrue(version.atLeast(new Version("0.5.3")));
    assertTrue(version.atLeast(new Version("0.5.4")));
    assertFalse(version.atLeast(new Version("0.5.5")));
    assertTrue(version.atLeast((String) null));
    assertTrue(version.atLeast((Version) null));
  }

  @Test
  public void testComparator() throws Exception {
    VersionComparator comparator = Version.getComparator();
    assertEquals(0, comparator.compare(new Version("0"), new Version("0")));
    assertEquals(0, comparator.compare(new Version("0.0.0.0"), new Version("0.0")));
    assertEquals(0, comparator.compare(new Version("0"), new Version("0.0.0.0.0")));

    assertEquals(0, comparator.compare(new Version("1"), new Version("1")));
    assertEquals(0, comparator.compare(new Version("1.1.0.0"), new Version("1.1")));
    assertEquals(0, comparator.compare(new Version("1"), new Version("1.0.0.0.0")));

    assertEquals(-1, comparator.compare(new Version("1"), new Version("2")));
    assertEquals(-1, comparator.compare(new Version("1.1.0.0"), new Version("1.2")));
    assertEquals(-1, comparator.compare(new Version("1"), new Version("1.0.0.0.1")));

    assertEquals(1, comparator.compare(new Version("3"), new Version("2")));
    assertEquals(1, comparator.compare(new Version("1.3.0.0"), new Version("1.2.1")));
    assertEquals(1, comparator.compare(new Version("1.0.1"), new Version("1.0.0.0.1")));

    assertEquals(-1, comparator.compare(new Version("30"), new Version("200")));
    assertEquals(-1, comparator.compare(new Version("1.3.0.0"), new Version("1.20.1")));
    assertEquals(-1, comparator.compare(new Version("1.0.1"), new Version("1.0.020.0.1")));

    assertEquals(0, comparator.compare(null, null));
    assertEquals(1, comparator.compare(new Version("1"), null));
    assertEquals(-1, comparator.compare(null, new Version("1.0.020.0.1")));
  }
}
