package com.janknspank;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Strings;

public class DateHelper {
  private static final Pattern[] DATE_IN_URL_PATTERNS = {
      Pattern.compile("\\/[0-9]{4}\\/[01][0-9]\\/[0-3][0-9]\\/"),
      Pattern.compile("\\/[0-9]{4}\\-[01][0-9]\\-[0-3][0-9][\\/\\-]"),
      Pattern.compile("\\/20[0-9]{2}[01][0-9][0-3][0-9]\\/")
  };
  private static final DateFormat[] KNOWN_DATE_FORMATS = {
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX"), // ISO 8601, BusinessWeek, CNBC.
      new SimpleDateFormat("MMMM dd, yyyy, hh:mm a"), // CBS News.
      new SimpleDateFormat("MMMM dd, yyyy"), // Chicago Tribune.
      new SimpleDateFormat("yyyy-MM-dd"), // New York Times and LA Times.
      new SimpleDateFormat("yyyyMMddHHmmss"), // New York Times 'pdate'.
      new SimpleDateFormat("yyyyMMdd"), // Washington Post.
      new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"), // BBC.
      new SimpleDateFormat("EEE, dd MMM yyyy HH:mm z"), // Boston.com.
      new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z"), // Boston.com.
      new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss"), // Boston.com
      new SimpleDateFormat("/yyyy/MM/dd/"), // In URL.
      new SimpleDateFormat("/yyyy-MM-dd/"), // In URL.
      new SimpleDateFormat("/yyyy-MM-dd-"), // In URL.
      new SimpleDateFormat("/yyyyMMdd/") // In URL.
  };

  public static Long getDateFromUrl(String url) {
    for (Pattern dateInUrlPattern : DATE_IN_URL_PATTERNS) {
      Matcher dateInUrlMatcher = dateInUrlPattern.matcher(url);
      if (dateInUrlMatcher.find()) {
        return parseDateTime(dateInUrlMatcher.group());
      }
    }
    return null;
  }

  // TODO(jonemerson): It seems like this is returning long's that have been
  // adjusted for PDT.  E.g. they're bigger than they should be by 7-8 hours.
  public static Long parseDateTime(String dateStr) {
    if (Strings.isNullOrEmpty(dateStr)) {
      return null;
    }
    for (DateFormat format : KNOWN_DATE_FORMATS) {
      try {
        return format.parse(dateStr).getTime();
      } catch (ParseException e2) {
        // This is OK - we just don't match.  Try the next one.
      }
    }
    System.err.println("COULD NOT PARSE DATE: " + dateStr);
    return null;
  }
}
