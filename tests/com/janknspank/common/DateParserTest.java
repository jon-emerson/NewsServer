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
    assertNotNull(testDate);
    assertNotNull(wildDate);
    assertEquals(testDate, wildDate);
  }

  @Test
  public void test() {
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
  }
}
