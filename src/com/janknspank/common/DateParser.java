package com.janknspank.common;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Strings;

/**
 * Utility class for parsing dates.  Supports all sorts of RSS date formats,
 * date formats in <meta> tags, as well as pulling dates from URLs.
 */
public class DateParser {
  private static final Pattern[] DATE_IN_URL_PATTERNS = {
      Pattern.compile("\\/((18|19|20)[0-9]{2}\\/[01]?[0-9]\\/[0-3]?[0-9])\\/"),
      Pattern.compile("\\/((18|19|20)[0-9]{2}\\-[01]?[0-9]\\-[0-3]?[0-9])[\\/\\-]"),
      Pattern.compile("\\/((18|19|20)[0-9]{2}[01][0-9][0-3][0-9])\\/"),
      Pattern.compile("\\-((18|19|20)[0-9]{2}[01][0-9][0-3][0-9])$") // buffalonews.com wire stories.
  };
  private static final Pattern MONTH_IN_URL_PATTERN =
      Pattern.compile("\\/20[0-9]{2}\\/(0[0-9]|1[012])\\/");
  private static final DateFormat[] KNOWN_DATE_FORMATS = {
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX"), // ISO 8601, BusinessWeek, CNBC.
      new SimpleDateFormat("MMMM dd, yyyy, hh:mm a"), // CBS News.
      new SimpleDateFormat("MMMM dd, yyyy"), // Chicago Tribune.
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mmX"), // Channelnewsasia.com.
      new SimpleDateFormat("yyyy-MM-dd"), // New York Times, LA Times and PCMag.
      new SimpleDateFormat("yyyyMMddHHmmss"), // New York Times 'pdate'.
      new SimpleDateFormat("yyyyMMdd"), // Washington Post.
      new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"), // BBC.
      new SimpleDateFormat("yyyy/MM/dd"), // CBC, Chron.com.
      new SimpleDateFormat("EEE, dd MMM yyyy HH:mm z"), // Boston.com.
      new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z"), // Boston.com.
      new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss"), // Boston.com
      new SimpleDateFormat("EEE, d MMM yyyy"), // LATimes.com RSS.
      new SimpleDateFormat("MM.dd.yyyy"), // Advice.careerbuilder.com, in content body.
      new SimpleDateFormat("dd MMMM yyyy"), // Ribaj.com, in <date> tag.
      new SimpleDateFormat("EEE dd MMM yyyy 'at' hha z"),
      new SimpleDateFormat("dd MMM yyyy '|' HH:mm z"), // Spectrum.ieee.org.
      new SimpleDateFormat("hh:mm a z  MMMM dd, yyyy"),
      new SimpleDateFormat("EEE MMM d, yyyy   h:mm a"),
      new SimpleDateFormat("MMM d yyyy")
  };
  private static final DateFormat MONTH_IN_URL_DATE_FORMAT =
      new SimpleDateFormat("/yyyy/MM/");

  /**
   * Returns the number of milliseconds since 1970 when the passed article was
   * published, if we can arrive at such a conclusion.
   * @param url the url to inspect
   * @param allowMonth Whether we should look for year+month pairs in the URL.
   *     The default is to only look for complete year+month+day tuples.
   * @return number of milliseconds, or null if a date couldn't be determined
   */
  public static Long parseDateFromUrl(String url, boolean allowMonth) {
    for (Pattern dateInUrlPattern : DATE_IN_URL_PATTERNS) {
      Matcher dateInUrlMatcher = dateInUrlPattern.matcher(url);
      if (dateInUrlMatcher.find()) {
        return parseDateTime(dateInUrlMatcher.group(1));
      }
    }
    if (allowMonth) {
      Matcher dateInUrlMatcher = MONTH_IN_URL_PATTERN.matcher(url);
      if (dateInUrlMatcher.find()) {
        try {
          // Note: SimpleDateFormat is not thread-safe.
          synchronized (MONTH_IN_URL_DATE_FORMAT) {
            return MONTH_IN_URL_DATE_FORMAT.parse(dateInUrlMatcher.group()).getTime();
          }
        } catch (ParseException e) {
          // This is OK - we just don't match.
        }
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
      // Note: SimpleDateFormat is not thread-safe.
      synchronized (format) {
        try {
          return format.parse(dateStr).getTime();
        } catch (ParseException e2) {
          // This is OK - we just don't match.  Try the next one.
        }
      }
    }
    System.err.println("COULD NOT PARSE DATE: " + dateStr);
    return null;
  }
}
