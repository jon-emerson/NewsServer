package com.janknspank.nlp;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.api.client.util.Lists;
import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.janknspank.bizness.Entities;
import com.janknspank.bizness.EntityType;
import com.janknspank.classifier.FeatureId;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.ArticleProto.ArticleFeature;
import com.janknspank.proto.ArticleProto.ArticleKeyword;
import com.janknspank.proto.CoreProto.Entity;
import com.janknspank.proto.CoreProto.KeywordToEntityId;

public class KeywordCanonicalizer {
  private static final Set<String> PERSON_TITLES = Sets.newHashSet(
      "dr", "mr", "ms", "mrs", "miss", "prof", "rev");

  // This is a Pattern of keywords folks like to put in their articles as an SEO
  // tactic.  For these, we only count them as keywords if they exist in the
  // title of the article.  The thinking goes, if the article's actually about
  // these companies/entities, then they'd put it in the title.
  private static final Pattern KEYWORD_BAIT_ENTITY_PATTERN =
      Pattern.compile("(facebook|google|twitter|tumbr|quora|apple)");

  public static final int STRENGTH_FOR_TITLE_MATCH = 150;
  public static final int STRENGTH_FOR_FIRST_PARAGRAPH_MATCH = 100;

  private static Map<String, KeywordToEntityId> __keywordToEntityIdMap = null;
  private static Map<String, Entity> __entityIdToEntityMap = null;

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
    boolean keyword1IsBetter = keyword1.hasParagraphNumber();
    if (keyword2.hasParagraphNumber()) {
      keyword1IsBetter = keyword1.getParagraphNumber() < keyword2.getParagraphNumber()
          || (keyword1.getParagraphNumber() == keyword2.getParagraphNumber()
              && keyword1.getStrength() > keyword2.getStrength());
    }
    ArticleKeyword.Builder keywordBuilder =
        keyword1IsBetter ? keyword1.toBuilder() : keyword2.toBuilder();
    if (!keywordBuilder.hasEntity()) {
      keywordBuilder.setKeyword(getBestKeywordStr(keyword1.getKeyword(), keyword2.getKeyword()));
    }
    keywordBuilder.setStrength(
        Math.max(keyword1.getStrength(), keyword2.getStrength()) + 1);
    keywordBuilder.setType(
        EntityType.fromValue(keyword1.getType()).getDepth()
            > EntityType.fromValue(keyword2.getType()).getDepth()
            ? keyword1.getType() : keyword2.getType());
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
   * Returns true if the passed features indicate that the article in question
   * is related to the Internet or Software.  In which case, we let clickbait
   * articles (Facebook, Twitter, etc) through.
   * NOTE(jonemerson): We probably want to build this logic to be smarter once
   * we learn of clickbait keywords that are non-tech related.
   */
  private static boolean isInternetArticle(Iterable<ArticleFeature> features) {
    for (ArticleFeature feature : features) {
      if (FeatureId.fromId(feature.getFeatureId()) == FeatureId.INTERNET
          || FeatureId.fromId(feature.getFeatureId()) == FeatureId.SOFTWARE) {
        return true;
      }
    }
    return false;
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
  public static Iterable<ArticleKeyword> canonicalize(
      Iterable<ArticleKeyword> keywords,
      Iterable<ArticleFeature> articleFeatures) {
    final boolean isInternetArticle = isInternetArticle(articleFeatures);

    // Filter illegal + clickbait keywords.
    keywords = Iterables.filter(keywords, new Predicate<ArticleKeyword>() {
      @Override
      public boolean apply(ArticleKeyword keyword) {
        if (!KeywordUtils.isValidKeyword(keyword.getKeyword())) {
          return false;
        }

        // Prevent click-bait: Only allow known clickbait entities through if
        // the keyword if they're in the article's title and the article is
        // topically relevant to their industry.
        Matcher clickbaitMatcher =
            KEYWORD_BAIT_ENTITY_PATTERN.matcher(keyword.getKeyword().toLowerCase());
        if (clickbaitMatcher.matches()
            && (keyword.getParagraphNumber() != 0 || !isInternetArticle)) {
          return false;
        }

        return true;
      }
    });

    // Create a map of unique keyword names, merging any dupes as we find them.
    LinkedHashMap<String, ArticleKeyword> keywordMap = Maps.newLinkedHashMap();
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
        } catch (DatabaseSchemaException e) {
          e.printStackTrace();
        }
      }
    }

    // Further canonicalize remaining keywords to a set of Entities, if we can.
    List<ArticleKeyword> finalKeywords = Lists.newArrayList();
    Set<String> entityIdsSoFar = Sets.newHashSet();
    Map<String, KeywordToEntityId> keywordToEntityIdMap = getKeywordToEntityIdMap();
    Map<String, Entity> entityIdToEntityMap = getEntityIdToEntityMap();

    // Make sure we're dealing with keywords in paragraph-order.  These keywords
    // should already be in proper order, but it doesn't hurt to do it again.
    // (And prevents against future bugs.)
    List<ArticleKeyword> keywordList = Lists.newArrayList(keywordMap.values());
    keywordList.sort(new Comparator<ArticleKeyword>() {
      @Override
      public int compare(ArticleKeyword articleKeyword1, ArticleKeyword articleKeyword2) {
        return Integer.compare(
            articleKeyword1.getParagraphNumber(), articleKeyword2.getParagraphNumber());
      }
    });
    for (ArticleKeyword keyword : keywordList) {
      KeywordToEntityId keywordToEntityId =
          keywordToEntityIdMap.get(keyword.getKeyword().toLowerCase());
      if (keywordToEntityId != null) {
        // Prevent dupes: If we already have canonicalized this keyword to an
        // Entity, we're good... We can stop now :).
        if (entityIdsSoFar.contains(keywordToEntityId.getEntityId())) {
          continue;
        }
        entityIdsSoFar.add(keywordToEntityId.getEntityId());

        // Now you can see why keeping the keywords in paragraph order is
        // important: If we didn't, keywords found at the bottom of the article
        // could prevent title and/or first-paragraph keywords from being
        // properly scored.
        // NOTE(jonemerson): Commented because .getArticleKeywordsFromText
        // already does these promotions through brute-force checks.
        // int strengthAddition = 0;
        // if (keyword.hasParagraphNumber() && keyword.getParagraphNumber() == 0) {
        //   // Title match.
        //   strengthAddition += STRENGTH_FOR_TITLE_MATCH;
        // } else if (keyword.hasParagraphNumber() && keyword.getParagraphNumber() == 1) {
        //   strengthAddition += STRENGTH_FOR_FIRST_PARAGRAPH_MATCH;
        // }

        Entity entity = entityIdToEntityMap.get(keywordToEntityId.getEntityId());
        finalKeywords.add(keyword.toBuilder()
            .setEntity(entity)
            .setKeyword(entity.hasShortName() ? entity.getShortName() : entity.getKeyword())
            // .setStrength(keyword.getStrength() + strengthAddition)
            .build());
      } else {
        finalKeywords.add(keyword);
      }
    }
    return finalKeywords;
  }

  /**
   * If the passed block represents an entity, it is returned.  Else, the left
   * side and the right side of the block are recursively checked, to see what
   * entities we can possibly find anywhere in the block.
   * NOTE(jonemerson): Yaaa this is expensive, which is why we only do it for
   * titles and the first paragraph!! :)
   */
  public static Iterable<ArticleKeyword> getArticleKeywordsFromTextInternal(
      String block,
      int paragraphNumber,
      Map<String, KeywordToEntityId> keywordToEntityIdMap,
      Map<String, Entity> entityIdToEntityMap) {
    KeywordToEntityId keywordToEntityId = keywordToEntityIdMap.get(block.toLowerCase());
    if (keywordToEntityId != null) {
      Entity entity = entityIdToEntityMap.get(keywordToEntityId.getEntityId());
      String entityKeyword = entity.hasShortName() ? entity.getShortName() : entity.getKeyword();

      return ImmutableList.of(ArticleKeyword.newBuilder()
          .setKeyword(entityKeyword)
          .setStrength((paragraphNumber == 0)
              ? STRENGTH_FOR_TITLE_MATCH : STRENGTH_FOR_FIRST_PARAGRAPH_MATCH)
          .setType(entity.getType())
          .setSource(ArticleKeyword.Source.TITLE)
          .setEntity(entity)
          .setParagraphNumber(paragraphNumber)
          .build());
    } else if (block.contains(" ")) {
      return Iterables.concat(
          getArticleKeywordsFromTextInternal(block.substring(0, block.lastIndexOf(" ")),
              paragraphNumber, keywordToEntityIdMap, entityIdToEntityMap),
              getArticleKeywordsFromTextInternal(block.substring(block.indexOf(" ") + 1),
              paragraphNumber, keywordToEntityIdMap, entityIdToEntityMap));
    } else {
      return Collections.emptyList();
    }
  }

  /**
   * Does a brute force search through all our keyword entities to try to find
   * keywords.
   */
  public static List<ArticleKeyword> getArticleKeywordsFromText(
      String text, int paragraphNumber) {
    Map<String, KeywordToEntityId> keywordToEntityIdMap = getKeywordToEntityIdMap();
    Map<String, Entity> entityIdToEntityMap = getEntityIdToEntityMap();

    // Find consecutive capitalized words blocks.  E.g. "Senator Barbara Boxer
    // goes to Google" would create [ "Senator Barbara Boxer", "Google" ].
    List<String> blocks = Lists.newArrayList();
    List<String> blockBuilder = Lists.newArrayList();
    for (String word : text.split("(\\s|\u00A0)")) {
      if (word.isEmpty()) {
        // OK... nothing to do!
      } else if (Character.isUpperCase(word.charAt(0))) {
        blockBuilder.add(KeywordUtils.scrubKeyword(word));
      } else {
        if (blockBuilder.size() > 0) {
          blocks.add(Joiner.on(" ").join(blockBuilder));
          blockBuilder.clear();
        }
      }
    }
    if (blockBuilder.size() > 0) {
      blocks.add(Joiner.on(" ").join(blockBuilder));
      blockBuilder.clear();
    }

    Map<String, ArticleKeyword> entityIdToKeywordMap = Maps.newHashMap();
    for (String block : blocks) {
      for (ArticleKeyword keyword : getArticleKeywordsFromTextInternal(
          block, paragraphNumber, keywordToEntityIdMap, entityIdToEntityMap)) {
        if (!entityIdToKeywordMap.containsKey(keyword.getEntity().getId())) {
          entityIdToKeywordMap.put(keyword.getEntity().getId(), keyword);
        }
      }
    }
    return ImmutableList.copyOf(entityIdToKeywordMap.values());
  }

  private static synchronized Map<String, KeywordToEntityId> getKeywordToEntityIdMap() {
    if (__keywordToEntityIdMap == null) {
      __keywordToEntityIdMap = Maps.newHashMap();
      try {
        System.out.println(
            "WARNING - SLOW QUERY: KeywordCanonicalizer.getKeywordToEntityIdMap() initialization");
        for (KeywordToEntityId keywordToEntityId : Database.with(KeywordToEntityId.class).get(
            new QueryOption.WhereNotNull("entity_id"))) {
          __keywordToEntityIdMap.put(keywordToEntityId.getKeyword(), keywordToEntityId);
        }
      } catch (DatabaseSchemaException e) {
        throw new Error(e);
      }
    }
    return __keywordToEntityIdMap;
  }

  private static synchronized Map<String, Entity> getEntityIdToEntityMap() {
    if (__entityIdToEntityMap == null) {
      Map<String, KeywordToEntityId> keywordToEntityIdMap = getKeywordToEntityIdMap();
      Set<String> entityIds = Sets.newHashSet();
      for (KeywordToEntityId keywordToEntityId : keywordToEntityIdMap.values()) {
        entityIds.add(keywordToEntityId.getEntityId());
      }
      try {
        __entityIdToEntityMap = Maps.newHashMap();
        System.out.println(
            "WARNING - SLOW QUERY: KeywordCanonicalizer.getEntityIdToEntityMap() initialization");
        for (Entity entity : Database.with(Entity.class).get(entityIds)) {
          __entityIdToEntityMap.put(entity.getId(), entity.toBuilder()
              .clearTopic() // These are pretty big and we don't need them here.
              .build());
        }
      } catch (DatabaseSchemaException e) {
        throw new Error(e);
      }
    }
    return __entityIdToEntityMap;
  }

  public static Entity getEntityForKeyword(String keyword) {
    String entityId = getEntityIdForKeyword(keyword);
    return (entityId == null) ? null : getEntityIdToEntityMap().get(entityId);
  }

  public static String getEntityIdForKeyword(String keyword) {
    KeywordToEntityId keywordToEntityId = getKeywordToEntityIdMap().get(keyword.toLowerCase());
    return (keywordToEntityId == null) ? null : keywordToEntityId.getEntityId();
  }
}
