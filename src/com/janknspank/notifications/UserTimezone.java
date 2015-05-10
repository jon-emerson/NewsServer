package com.janknspank.notifications;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.fetch.FetchException;
import com.janknspank.fetch.Fetcher;
import com.janknspank.proto.UserProto.User;

public class UserTimezone {
  private final int hourOffset;
  private int minuteOffset;

  public UserTimezone(String timezone) {
    hourOffset = Integer.parseInt(timezone.substring(0, timezone.indexOf(":")));
    minuteOffset = Integer.parseInt(timezone.substring(timezone.indexOf(":") + 1));
    if (hourOffset < 0) {
      minuteOffset *= -1;
    }
  }

  // Mocked out for testing.
  protected long getCurrentTime() {
    return System.currentTimeMillis();
  }

  public static UserTimezone getForUser(User user, boolean update)
      throws DatabaseSchemaException {
    if (update
        && user.hasLastIpAddress()
        && (!user.hasTimezoneEstimate()
            || (System.currentTimeMillis() - user.getLastTimezoneEstimateUpdate()
                > TimeUnit.DAYS.toMillis(7)))) {
      try {
        String response = new Fetcher().getResponseBody(
            "http://api.ipinfodb.com/v3/ip-city/?"
            + "key=bfd6cdfcebd81c7b53ddd6f14adcc2444f79cad942ee09b4f78fd4499b8aff02&"
            + "ip=" + user.getLastIpAddress());
        if (response.startsWith("OK;")) {
          String[] components = response.split(";");
          String timezone = components[components.length - 1];
          if (timezone.contains(":")) {
            // OK, we got one!  Let's save it.
            Database.set(user, "timezone_estimate", timezone);
            return new UserTimezone(timezone);
          }
        }
      } catch (FetchException | DatabaseRequestException e) {
        e.printStackTrace();
      }
    }

    // Failover: Last timezone we know about, or default to PDT.
    return new UserTimezone(user.hasTimezoneEstimate() ? user.getTimezoneEstimate() : "-07:00");
  }

  private int getCurrentHour() {
    Instant instant = Instant.ofEpochMilli(getCurrentTime());
    return ZonedDateTime.ofInstant(instant,
        ZoneOffset.ofHoursMinutes(hourOffset, minuteOffset)).getHour();
  }

  private int getCurrentMinute() {
    Instant instant = Instant.ofEpochMilli(getCurrentTime());
    return ZonedDateTime.ofInstant(instant,
        ZoneOffset.ofHoursMinutes(hourOffset, minuteOffset)).getMinute();
  }
  public boolean isNight() {
    int currentHour = getCurrentHour();
    return (currentHour < 8 || currentHour >= 22);
  }

  public boolean isMorning() {
    int currentHour = getCurrentHour();
    return (currentHour >= 9 && currentHour <= 12); // Ya, OK, we let noon/lunch sneak in :).
  }

  public boolean isDaytime() {
    int currentHour = getCurrentHour();
    return (currentHour >= 8 && currentHour <= 19);
  }

  public boolean isAroundNoon() {
    int currentHour = getCurrentHour();
    int currentMinute = getCurrentMinute();
    return (currentHour == 11 && currentMinute > 45)
        || (currentHour == 12 && currentMinute <= 45);
  }

  public boolean isWeekend() {
    Instant instant = Instant.ofEpochMilli(getCurrentTime());
    DayOfWeek dayOfWeek = ZonedDateTime.ofInstant(instant,
        ZoneOffset.ofHoursMinutes(hourOffset, minuteOffset)).getDayOfWeek();
    return (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY);
  }
}
