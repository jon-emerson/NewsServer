package com.janknspank.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;

public class UrlsTest {
  @Test
  public void testGetCrawlPriority() {
    assertEquals(10, Urls.getCrawlPriority(
        "http://www.bloomberg.com/", null));

    // Test fully-qualified dates.
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    assertEquals(100, Urls.getCrawlPriority(
        "http://www.bloomberg.com/news/" + dateFormat.format(new Date(0))
        + "/obama-delivers-support-for-myanmar-opposition.html", null));
    assertTrue(500 < Urls.getCrawlPriority(
        "http://www.bloomberg.com/news/" + dateFormat.format(new Date())
        + "/obama-delivers-support-for-myanmar-opposition.html", null));

    // Test month-only.
    dateFormat = new SimpleDateFormat("yyyy/MM");
    assertEquals(100, Urls.getCrawlPriority(
        "http://www.boston.com/business/innovation/state-of-play/"
        + "2005/02/inside_demiurge_albert_reed_ce.html", null));
    assertTrue(500 < Urls.getCrawlPriority(
        "http://www.boston.com/business/innovation/state-of-play/"
        + dateFormat.format(new Date()) + "/inside_demiurge_albert_reed_ce.html", null));
  }
}
