package com.janknspank;

import org.junit.Test;
import static org.junit.Assert.*;

public class DateHelperTest {
  /**
   * Tests that two dates parse to be equal.
   * @param testDateStr a string we're using in the test to represent what the
   *     date we've found in the wild should equal
   * @param wildDateStr a date we've seen in the wild
   */
  private void assertSameTime(String testDateStr, String wildDateStr) {
    Long testDate = DateHelper.parseDateTime(testDateStr);
    Long wildDate = DateHelper.parseDateTime(wildDateStr);
    assertNotNull(testDate);
    assertNotNull(wildDate);
    assertEquals(testDate, wildDate);
  }

  @Test
  public void test() {
    // Sanity check that the parser's not just returning 0 for everything.
    assertTrue(DateHelper.parseDateTime("20141231220000") > 500000);
    assertNotEquals(DateHelper.parseDateTime("20141231220000"),
        DateHelper.parseDateTime("20141231220001"));

    // OK, start testing dates we've seen in the wild.
    // NOTE(jonemerson): It smells like these tests are gonna break when day
    // light savings time starts.
    assertSameTime("20141231140000", "2014-12-31T22:00:00Z"); // Curbed.com RSS.
    assertSameTime("20150101154425", "Thu, 01 Jan 2015 23:44:25 GMT"); // Nytimes.com RSS.
    assertSameTime("20150101075748", "Thu, 1 Jan 2015 07:57:48 PST"); // MercuryNews.com RSS.
    assertSameTime("20140325122146", "Tue, 25 Mar 2014 15:21:46 -0400"); // ABCNews.com RSS.
  }
}
