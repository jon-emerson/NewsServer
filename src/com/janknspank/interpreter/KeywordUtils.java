package com.janknspank.interpreter;

import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.janknspank.data.ArticleKeywords;

public class KeywordUtils {
  private static final Pattern NUMBER_PATTERN_1 = Pattern.compile("^[0-9]+$");
  private static final Pattern NUMBER_PATTERN_2 = Pattern.compile("^[0-9]+\\-");
  private static final Pattern BEST_OF_PATTERN = Pattern.compile("^best( [a-z]+)? of ");
  private static final Set<String> BLACKLIST = Sets.newHashSet();
  static {
    for (String keyword : new String[] {
        "and",
        "apartments",
        "blog",
        "business school",
        "business schools",
        "bw business schools page",
        "capitals",
        "ceo",
        "cinema",
        "committee",
        "commodities",
        "company",
        "corrections",
        "crime",
        "department",
        "dialogue",
        "economy",
        "economic",
        "education",
        "films",
        "financial calculator",
        "financial calculators",
        "first",
        "graduate reviews",
        "hedge funds",
        "hello",
        "internet",
        "inequality",
        "insurance",
        "jobs",
        "kids",
        "kidz",
        "leadership",
        "lifestyle",
        "litigation",
        "markets",
        "media",
        "mobile",
        "movies",
        "my",
        "news",
        "opinion",
        "party",
        "pm",
        "profile",
        "restaurants",
        "retail",
        "retailing",
        "shopping",
        "show",
        "society",
        "son",
        "sports",
        "stocks",
        "stock market",
        "story",
        "story-video",
        "teams",
        "tech",
        "technology",
        "that",
        "the",
        "their",
        "there",
        "they",
        "times",
        "top business schools worldwide",
        "trial",
        "trials",
        "try",
        "university",
        "up",
        "video",
        "web",
        "why",
        "with",
        "yet"}) {
      BLACKLIST.add(keyword.toLowerCase());
    }
  }

  @VisibleForTesting
  static boolean isValidKeyword(String keyword) {
    keyword = keyword.trim().toLowerCase();
    if (keyword.length() < 2 ||
        NUMBER_PATTERN_1.matcher(keyword).find() ||
        NUMBER_PATTERN_2.matcher(keyword).find() ||
        BEST_OF_PATTERN.matcher(keyword).find() ||
        keyword.contains("…") ||
        keyword.startsWith("#") ||
        keyword.startsWith("@") ||
        keyword.startsWith("bloomberg ") ||
        keyword.startsWith("mba ") ||
        keyword.endsWith("@bloomberg") ||
        keyword.endsWith(" jobs") ||
        keyword.endsWith(" news") ||
        keyword.endsWith(" profile") ||
        keyword.endsWith(" restaurants") ||
        keyword.endsWith(" trends") ||
        (keyword.contains("&") && keyword.contains(";")) || // XML entities.
        keyword.length() > ArticleKeywords.MAX_KEYWORD_LENGTH) {
      return false;
    }
    return !BLACKLIST.contains(keyword);
  }

  /**
   * Removes possessives and other dirt from strings our parser found (since we
   * trained our parser to include them, so instead of ignoring them as false
   * negatives).
   */
  public static String cleanKeyword(String keyword) {
    keyword = keyword.trim();
    if (keyword.startsWith("‘") || keyword.startsWith("'") ||
        keyword.startsWith("“") || keyword.startsWith("\"")) {
      keyword = keyword.substring(1);
    }
    if (keyword.endsWith("’s") || keyword.endsWith("'s")) {
      return keyword.substring(0, keyword.length() - "’s".length());
    }
    if (keyword.endsWith("’") || keyword.endsWith("'") ||
        keyword.endsWith("”") || keyword.endsWith("\"") ||
        keyword.endsWith(",") || keyword.endsWith(";") ||
        keyword.endsWith("-") || keyword.endsWith("!")) {
      keyword = keyword.substring(0, keyword.length() - 1);
    }
    if (keyword.endsWith(".") && StringUtils.countMatches(keyword, ".") == 1) {
      keyword = keyword.substring(0, keyword.length() - 1);
    }
    if (keyword.endsWith("-based")) {
      keyword = keyword.substring(0, keyword.length() - "-based".length());
    }

    // Since we want to canonicalize as much as we can, and have nice short
    // keywords, for now convert possessive objects to only their posessive
    // part, dropping the rest.  This does mean we lose significant information
    // about the keyword, but perhaps(?) the canonicalization is worth it.
    if (keyword.contains("’")) {
      keyword = keyword.substring(0, keyword.indexOf("’"));
    }
    if (keyword.contains("'")) {
      keyword = keyword.substring(0, keyword.indexOf("'"));
    }

    if (keyword.length() > 0 && !Character.isUpperCase(keyword.charAt(0))) {
      keyword = WordUtils.capitalizeFully(keyword);
      keyword = keyword.replaceAll("Aol", "AOL");
      keyword = keyword.replaceAll("Ios", "iOS");
      keyword = keyword.replaceAll("Ipad", "iPad");
      keyword = keyword.replaceAll("Iphone", "iPhone");
      keyword = keyword.replaceAll("Iwatch", "iWatch");
      keyword = keyword.replaceAll("Ipod", "iPod");
    } if (keyword.length() > 4 && keyword.equals(keyword.toUpperCase())) {
      // For non-abbreviations, don't let folks capitalize everything.
      keyword = WordUtils.capitalizeFully(keyword.toLowerCase());
    }

    return keyword;
  }
}
