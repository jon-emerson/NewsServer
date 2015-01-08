package com.janknspank.interpreter;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.janknspank.data.ArticleKeywords;
import com.janknspank.proto.Core.ArticleKeyword;

public class KeywordCanonicalizer {
  private static final Set<String> PERSON_TITLES = Sets.newHashSet(
      "dr", "mr", "ms", "mrs", "miss", "prof", "rev");

  /**
   * Returns a score for how much we like the passed keyword's type.
   */
  private static int getTypeScore(ArticleKeyword keyword) {
    switch (keyword.getType()) {
      case ArticleKeywords.TYPE_HYPERLINK:
        return 10;
      case ArticleKeywords.TYPE_LOCATION:
        return 20;
      case ArticleKeywords.TYPE_ORGANIZATION:
        return 30;
      case ArticleKeywords.TYPE_PERSON:
        return 29;
    }
    return 5;
  }

  /**
   * Figures out which of the passed keyword strings are better.  Longer
   * keywords are always preferred, but beyond that, we value things like proper
   * capitalization.
   */
  private static String getBestKeywordStr(String keywordStr1, String keywordStr2) {
    keywordStr1 = keywordStr1.trim();
    keywordStr2 = keywordStr2.trim();

    if (keywordStr1.length() > keywordStr2.length()) {
      return keywordStr1;
    } else if (keywordStr2.length() > keywordStr1.length()) {
      return keywordStr2;
    }

    // Simple check to make sure keywordStr1 is capitalized like a proper noun
    // (and not in all caps): Make sure converting it to upper case or to lower
    // case is different than its original value.
    if (!keywordStr1.toUpperCase().equals(keywordStr1) &&
        !keywordStr1.toLowerCase().equals(keywordStr1)) {
      return keywordStr1;
    }

    // Gotta return something...
    return keywordStr2;
  }

  /**
   * Returns a new ArticleKeyword, formed by taking the good things from each
   * of the passed keywords, and making the combined strength just an eensy bit
   * higher.
   */
  private static ArticleKeyword merge(ArticleKeyword keyword1, ArticleKeyword keyword2) {
    ArticleKeyword.Builder keywordBuilder =
        (getTypeScore(keyword1) > getTypeScore(keyword2))
            ? keyword1.toBuilder() : keyword2.toBuilder();
    keywordBuilder.setKeyword(getBestKeywordStr(keyword1.getKeyword(), keyword2.getKeyword()));
    keywordBuilder.setStrength(
        Math.max(keyword1.getStrength(), keyword2.getStrength()) + 1);
    return keywordBuilder.build();
  }

  /**
   * Removes honorifics (E.g. Mr., Mrs.) from a person string.
   */
  private static String removePersonTitle(String person) {
    Iterable<String> components = Splitter.on(CharMatcher.WHITESPACE).limit(2).split(person);
    String first = Iterables.getFirst(components, " ");
    if (Iterables.size(components) > 1 &&
        (PERSON_TITLES.contains(first) ||
            first.endsWith(".") &&
            PERSON_TITLES.contains(first.substring(0, first.length() - 1)))) {
      return Iterables.getLast(components, null);
    }
    return person;
  }

  /**
   * Removes what are essentially dupes in the passed list of ArticleKeywords,
   * by making shorter versions of keywords count as instances of their longer
   * version, then removing said shorter versions.
   *
   * For example, canonicalize("Mr. Smith", "Jorge Smith", "Smith", "I.B.M.")
   * would return only "Jorge Smith" and "I.B.M.", with the strengths adjusted
   * accordingly.
   */
  public static Iterable<ArticleKeyword> canonicalize(List<ArticleKeyword> keywords) {
    // Create a map of unique keyword names, merging any dupes as we find them.
    Map<String, ArticleKeyword> keywordMap = Maps.newHashMap();
    for (ArticleKeyword keyword : keywords) {
      String keywordStr = keyword.getKeyword().trim().toLowerCase();
      if (keywordMap.containsKey(keywordStr)) {
        keywordMap.put(keywordStr, merge(keyword, keywordMap.get(keywordStr)));
      } else {
        keywordMap.put(keywordStr, keyword);
      }
    }

    // Go through all the keywords and see if any are substrings of each other.
    // If they are, merge them, deleting the smaller one.
    Set<String> originalKeys = ImmutableSet.copyOf(keywordMap.keySet());
    for (String bigKey : originalKeys) {
      for (String littleKey : originalKeys) {
        if (bigKey.equals(littleKey) ||
            !keywordMap.containsKey(bigKey) ||
            !keywordMap.containsKey(littleKey)) {
          continue;
        }

        // Look for both straight substrings and substrings of people who have
        // their honorary titles removed (e.g. Mr., Mrs.).
        if (bigKey.contains(littleKey) ||
            (keywordMap.get(littleKey).getType() == ArticleKeywords.TYPE_PERSON &&
                bigKey.contains(removePersonTitle(littleKey)))) {
          keywordMap.put(bigKey, merge(keywordMap.get(bigKey), keywordMap.get(littleKey)));
          keywordMap.remove(littleKey);
        }
      }
    }

    return keywordMap.values();
  }
}
