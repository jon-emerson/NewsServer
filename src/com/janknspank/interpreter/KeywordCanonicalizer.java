package com.janknspank.interpreter;

import java.util.Map;
import java.util.Set;

import com.google.common.base.CharMatcher;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.janknspank.data.DataInternalException;
import com.janknspank.data.Entities;
import com.janknspank.data.EntityType;
import com.janknspank.data.ValidationException;
import com.janknspank.proto.Core.ArticleKeyword;
import com.janknspank.proto.Core.ArticleKeyword.Source;
import com.janknspank.proto.Core.Entity;

public class KeywordCanonicalizer {
  private static final Set<String> PERSON_TITLES = Sets.newHashSet(
      "dr", "mr", "ms", "mrs", "miss", "prof", "rev");

  /**
   * Returns a score for how much we like the passed keyword's type.
   */
  private static int getArticleKeywordScore(ArticleKeyword keyword) {
    EntityType type = EntityType.fromValue(keyword.getType());
    if (type != null) {
      if (type.isA(EntityType.PLACE)) {
        return 20;
      } else if (type.isA(EntityType.ORGANIZATION)) {
        return 30;
      } else if (type.isA(EntityType.PERSON)) {
        return 29;
      }
    }
    return keyword.getSource() == Source.META_TAG ? 5 : 10;
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
        (getArticleKeywordScore(keyword1) > getArticleKeywordScore(keyword2))
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
        (PERSON_TITLES.contains(first.toLowerCase()) ||
            first.endsWith(".") &&
            PERSON_TITLES.contains(first.substring(0, first.length() - 1).toLowerCase()))) {
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
  public static Iterable<ArticleKeyword> canonicalize(Iterable<ArticleKeyword> keywords) {
    // Filter illegal keywords.
    keywords = Iterables.filter(keywords, new Predicate<ArticleKeyword>() {
      @Override
      public boolean apply(ArticleKeyword keyword) {
        try {
          return KeywordUtils.isValidKeyword(keyword.getKeyword());
        } catch (ValidationException e) {
          System.out.print("Error filtering invalid keywords: ");
          e.printStackTrace();
          return false;
        }
      }
    });

    // Create a map of unique keyword names, merging any dupes as we find them.
    Map<String, ArticleKeyword> keywordMap = Maps.newHashMap();
    for (ArticleKeyword keyword : keywords) {
      String keywordStr = keyword.getKeyword().trim();
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

        // Remove honorary titles (e.g. Mr., Mrs.) for people.
        String cleanLittleKey =
            EntityType.PERSON == EntityType.fromValue(keywordMap.get(littleKey).getType()) ?
                removePersonTitle(littleKey) : littleKey;
        try {
          if (bigKey.contains(cleanLittleKey)) {
            Entity entity = Entities.getEntityByKeyword(cleanLittleKey);
            if (entity == null || entity.getType() != keywordMap.get(littleKey).getType()) {
              keywordMap.put(bigKey, merge(keywordMap.get(bigKey), keywordMap.get(littleKey)));
              keywordMap.remove(littleKey);
            } else {
              System.out.println("LITTLE KEY WINS!! " + littleKey);
              // Little key already exists in Wikipedia: Reward it for doing so,
              // and punish the bigger key.
              ArticleKeyword big = keywordMap.get(bigKey);
              keywordMap.put(bigKey,
                  big.toBuilder().setStrength(Math.min(1, big.getStrength() - 1)).build());
              ArticleKeyword little = keywordMap.get(littleKey);
              keywordMap.put(littleKey,
                  little.toBuilder().setStrength(little.getStrength() + 1).build());
            }
          }
        } catch (DataInternalException e) {
          e.printStackTrace();
        }
      }
    }

    return keywordMap.values();
  }
}
