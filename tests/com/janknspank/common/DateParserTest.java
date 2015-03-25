package com.janknspank.common;

import org.junit.Test;

import com.janknspank.common.DateParser;

import static org.junit.Assert.*;

public class DateParserTest {
  /**
   * Tests that two dates parse to be equal.
   * @param testDateStr a string we're using in the test to represent what the
   *     date we've found in the wild should equal
   * @param wildDateStr a date we've seen in the wild
   */
  public static void assertSameTime(String testDateStr, String wildDateStr) {
    assertSameTime(testDateStr, DateParser.parseDateTime(wildDateStr));
  }

  public static void assertSameTime(String testDateStr, Long wildDate) {
    Long testDate = DateParser.parseDateTime(testDateStr);
    assertNotNull("Test date cannot be null", testDate);
    assertNotNull("Wild date cannot be null", wildDate);
    assertEquals(testDate, wildDate);
  }

  @Test
  public void testParseDateTime() {
    // Sanity check that the parser's not just returning 0 for everything.
    assertTrue(DateParser.parseDateTime("20141231220000") > 500000);
    assertNotEquals(DateParser.parseDateTime("20141231220000"),
        DateParser.parseDateTime("20141231220001"));

    // OK, start testing dates we've seen in the wild.
    // NOTE(jonemerson): It smells like these tests are gonna break when day
    // light savings time starts.
    assertSameTime("20141231140000", "2014-12-31T22:00:00Z"); // Curbed.com RSS.
    assertSameTime("20150101154425", "Thu, 01 Jan 2015 23:44:25 GMT"); // Nytimes.com RSS.
    assertSameTime("20150101075748", "Thu, 1 Jan 2015 07:57:48 PST"); // MercuryNews.com RSS.
    assertSameTime("20140325122146", "Tue, 25 Mar 2014 15:21:46 -0400"); // ABCNews.com RSS.
    assertSameTime("20150102232200", "2015-01-03T07:22Z"); // Channelnewsasia.com.
    assertSameTime("20150104000000", "Sun, 4 Jan 2015"); // LATimes.com RSS.
    assertSameTime("20150106224300", "January 6, 2015, 10:43 PM"); // Cbsnews.com.
    assertSameTime("20150227000000", "27 February 2015"); // Ribaj.com.
    assertSameTime("20141119020000", "Wednesday 19 November 2014 at 2am PST");
    assertSameTime("20150320051100", "8:11 AM EDT  March 20, 2015");
  }

  @Test
  public void testParseDateFromUrl() {
    assertSameTime("20150109000000", DateParser.parseDateFromUrl(
        "http://money.cnn.com/2015/01/09/technology/uber-price/index.html", false));
    assertSameTime("20150110000000", DateParser.parseDateFromUrl(
        "http://www.abc.net.au/news/2015-01-10/asian-cup-2015/6009842", false));
    assertSameTime("20150109000000", DateParser.parseDateFromUrl(
        "http://www.buffalonews.com/news-wire-services/french-police-storm-terror-"
        + "sites-in-paris-20150109", false));
    assertSameTime("20120328000000", DateParser.parseDateFromUrl(
        "http://go.bloomberg.com/tech-blog/2012-03-28-facebook-gets-a-new-"
        + "game-not-made-by-zynga-will-it-score/", false));
    assertSameTime("19920108000000", DateParser.parseDateFromUrl(
        "http://articles.latimes.com/1992-01-08/local/me-1330_1_middle-class", false));

    // Months.
    assertNull(DateParser.parseDateFromUrl("http://arstechnica.com/apple/2005/05/305/", false));
    assertSameTime("20050501000000", DateParser.parseDateFromUrl(
        "http://arstechnica.com/apple/2005/05/305/", true));
    assertNull(DateParser.parseDateFromUrl(
        "http://abcnews.go.com/blogs/business/2014/12/2014-its-been-a-brilliant-"
        + "year-for-investors/", false));
    assertSameTime("20141201000000", DateParser.parseDateFromUrl(
        "http://abcnews.go.com/blogs/business/2014/12/2014-its-been-a-brilliant-"
        + "year-for-investors/", true));
    assertNull(DateParser.parseDateFromUrl(
        "http://abcnews.go.com/blogs/politics/2014/10/5-simple-questions-about-the-"
        + "midterm-elections-answered/", false));
    assertSameTime("20141001000000", DateParser.parseDateFromUrl(
        "http://abcnews.go.com/blogs/politics/2014/10/5-simple-questions-about-the-"
        + "midterm-elections-answered/", true));
  }
}
