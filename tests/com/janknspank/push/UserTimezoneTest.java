package com.janknspank.push;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Test;

public class UserTimezoneTest {
  @Test
  public void testMorningInSanFrancisco() throws Exception {
    UserTimezone userTimezone = new UserTimezone("-7:00") {
      @Override
      protected long getCurrentTime() {
        return ZonedDateTime.now(ZoneId.of("America/Los_Angeles"))
            .withYear(2015)
            .withMonth(4)
            .withDayOfMonth(27)
            .withHour(9)
            .withMinute(43)
            .withSecond(25)
            .toEpochSecond() * 1000;
      }
    };
    assertFalse(userTimezone.isNight());
    assertTrue(userTimezone.isMorning());
  }

  @Test
  public void testAfternoonInSanFrancisco() throws Exception {
    UserTimezone userTimezone = new UserTimezone("-7:00") {
      @Override
      protected long getCurrentTime() {
        return ZonedDateTime.now(ZoneId.of("America/Los_Angeles"))
            .withYear(2015)
            .withMonth(4)
            .withDayOfMonth(27)
            .withHour(17)
            .withMinute(43)
            .withSecond(25)
            .toEpochSecond() * 1000;
      }
    };
    assertFalse(userTimezone.isNight());
    assertFalse(userTimezone.isMorning());
  }

  @Test
  public void testNightInSanFrancisco() throws Exception {
    // Test 10:43pm through 8:43am, inclusive.
    for (int hour = 22; hour < 9; hour = (hour + 2) % 24) {
      final int finalHour = hour;
      UserTimezone userTimezone = new UserTimezone("-7:00") {
        @Override
        protected long getCurrentTime() {
          return ZonedDateTime.now(ZoneId.of("America/Los_Angeles"))
              .withYear(2015)
              .withMonth(4)
              .withDayOfMonth(27)
              .withHour(finalHour)
              .withMinute(43)
              .withSecond(25)
              .toEpochSecond() * 1000;
        }
      };
      assertTrue(finalHour + ":43 should be night", userTimezone.isNight());
      assertFalse(finalHour + ":43 should be night", userTimezone.isMorning());
    }
  }

  @Test
  public void testNightInLondon() throws Exception {
    // Test 10:43pm through 8:43am, inclusive.
    for (int hour = 22; hour < 9; hour = (hour + 2) % 24) {
      final int finalHour = hour;
      UserTimezone userTimezone = new UserTimezone("+01:00") {
        @Override
        protected long getCurrentTime() {
          return ZonedDateTime.now(ZoneId.of("Europe/London"))
              .withYear(2015)
              .withMonth(4)
              .withDayOfMonth(27)
              .withHour(finalHour)
              .withMinute(43)
              .withSecond(25)
              .toEpochSecond() * 1000;
        }
      };
      assertTrue(finalHour + ":43 should be night", userTimezone.isNight());
      assertFalse(finalHour + ":43 should be night", userTimezone.isMorning());
    }
  }
}
