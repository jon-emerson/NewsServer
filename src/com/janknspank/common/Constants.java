package com.janknspank.common;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Constants {
  private static final DateFormat DATE_TIME_FORMATTER =
      new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  static {
    DATE_TIME_FORMATTER.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  public static String formatDate(Date date) {
    return DATE_TIME_FORMATTER.format(date);
  }
}
