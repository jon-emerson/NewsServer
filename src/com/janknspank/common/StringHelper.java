package com.janknspank.common;

import org.apache.commons.lang3.StringEscapeUtils;

public class StringHelper {
  /**
   * Unescapes the passed string, including &apos;, which was added in XHTML 1.0
   * but for some reason isn't handled by Apache's StringEscapeUtils.  Also,
   * &nbsp; is converted to a standard space, since we don't want to worry about
   * that.
   */
  public static String unescape(String escaped) {
    return StringEscapeUtils
        .unescapeHtml4(escaped
            .replaceAll("&nbsp;", " ")
            .replaceAll("&#8203;", "")) // Zero-width space.
        .replaceAll("&apos;", "’")
        .replaceAll("\u00A0", " ") // Non-breaking space.
        .replaceAll("\u200B", ""); // Zero-width space.
  }
}
