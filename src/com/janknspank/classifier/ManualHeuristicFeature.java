package com.janknspank.classifier;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.janknspank.proto.ArticleProto.ArticleOrBuilder;

public class ManualHeuristicFeature extends Feature {
  private static class BodyCache extends ThreadLocal<LinkedHashMap<ArticleOrBuilder, String>> {
    private static final int CACHE_SIZE_PER_THREAD = 5;

    @Override
    protected LinkedHashMap<ArticleOrBuilder, String> initialValue() {
      return new LinkedHashMap<ArticleOrBuilder, String>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<ArticleOrBuilder, String> eldest) {
          return size() > CACHE_SIZE_PER_THREAD;
        }
      };
    }

    /** Actually just returns the first 2 paragraphs. */
    public String getBody(final ArticleOrBuilder article) {
      if (this.get().containsKey(article)) {
        return this.get().get(article);
      }
      StringBuilder bodyBuilder = new StringBuilder();
      for (String paragraph : Iterables.limit(article.getParagraphList(), 2)) {
        bodyBuilder.append(paragraph.toLowerCase());
        bodyBuilder.append(" ");
      }
      String body = bodyBuilder.toString();
      this.get().put(article, body);
      return body;
    };
  }
  private static final BodyCache BODY_CACHE = new BodyCache();

  private static final Map<Object, Pattern> PATTERN_CACHE = Maps.newHashMap();

  // Acquisition keywords.
  // NOTE: Do not use the word "Acquisition".  It matches articles like this:
  // http://www.channelnewsasia.com/news/singapore/parliament-passes-changes/1714406.html
  private static final Map<String, Double> ACQUISITION_TITLE_SCORES =
      ImmutableMap.<String, Double>builder()
          .put("acquires", 1.0)
          .put("is acquiring", 1.0)
          .put("buys", 0.9)
          .put("is buying", 0.8)
          .build();
  private static final Iterable<String> ACQUISITION_TITLE_BLACKLIST =
      Arrays.asList("for buying", "buying a", "home-buying", "-buying", "buying for", 
          "buying experience", "worth buying", "buying?", "is buying what's");
  private static final Map<String, Double> ACQUISITION_BODY_SCORES =
      ImmutableMap.<String, Double>builder()
          .put("acquires", 0.8)
          .put("is acquiring", 0.8)
          .build();
  private static final Iterable<String> ACQUISITION_BODY_BLACKLIST =
      Arrays.asList();
  private static final Map<String, Double> LAUNCH_TITLE_SCORES =
      ImmutableMap.<String, Double>builder()
          .put("launches", 1.0)
          .put("launched", 0.9)
          .put("to launch", 0.9)
          .put("releases", 0.9)
          .build();
  private static final Iterable<String> LAUNCH_TITLE_BLACKLIST =
      Arrays.asList("manifesto", "report", "ago", "court", "lawsuit", "vinyl", "investigation", "criminal");
  private static final Map<String, Double> LAUNCH_BODY_SCORES =
      ImmutableMap.<String, Double>builder()
          .put("launches", 0.6)
          .put("releases", 0.4)
          .build();
  private static final Iterable<String> LAUNCH_BODY_BLACKLIST =
      Arrays.asList("manifesto", "report", "ago", "was launched", "social media campaign", 
          "fell", "rose", "as it launches", "were launched", "which launches");
  private static final Map<String, Double> FUNDRAISING_TITLE_SCORES =
      ImmutableMap.<String, Double>builder()
          .put("series a ", 1.0)
          .put("series a,", 1.0)
          .put("series b ", 1.0)
          .put("series b,", 1.0)
          .put("series c ", 1.0)
          .put("series c,", 1.0)
          .put("series d ", 1.0)
          .put("series d,", 1.0)
          .put("series e ", 1.0)
          .put("series e,", 1.0)
          .put("angel round", 1.0)
          .build();
  private static final Iterable<String> FUNDRAISING_TITLE_BLACKLIST =
      Arrays.asList("declares dividends");
  private static final Map<String, Double> FUNDRAISING_BODY_SCORES =
      ImmutableMap.<String, Double>builder()
          .put("series a\\.", 0.9)
          .put("series a ", 0.9)
          .put("series a,", 0.9)
          .put("series b\\.", 0.9)
          .put("series b ", 0.9)
          .put("series b,", 0.9)
          .put("series c\\.", 0.9)
          .put("series c ", 0.9)
          .put("series c,", 0.9)
          .put("series d\\.", 0.9)
          .put("series d ", 0.9)
          .put("series d,", 0.9)
          .put("series e\\.", 0.9)
          .put("series e ", 0.9)
          .put("series e,", 0.9)
          .put("angel round", 0.9)
          .build();
  private static final Iterable<String> FUNDRAISING_BODY_BLACKLIST =
      Arrays.asList();
  private static final Map<String, Double> MILITARY_SCORES =
      ImmutableMap.<String, Double>builder()
          .put("afghanistan", 1.0)
          .put("attack", 1.0)
          .put("attacks", 1.0)
          .put("crimea", 1.0)
          .put("gaza", 1.0)
          .put("injured", 1.0)
          .put("iran", 1.0)
          .put("iraq", 1.0)
          .put("isil", 1.0)
          .put("isis", 1.0)
          .put("killed", 1.0)
          .put("lebanon", 1.0)
          .put("militant", 1.0)
          .put("militants", 1.0)
          .put("military", 1.0)
          .put("mortar", 1.0)
          .put("mortars", 1.0)
          .put("nasa", 1.0)
          .put("north korea", 1.0)
          .put("pakistan", 1.0)
          .put("paramilitary", 1.0)
          .put("rocket", 1.0)
          .put("wounded", 1.0)
          .build();

  public ManualHeuristicFeature(FeatureId featureId) {
    super(featureId);
    if (featureId.getFeatureType() != FeatureType.MANUAL_HEURISTIC) {
      throw new IllegalStateException("The specified feature ID is not a manual heuristic");
    }
  }

  @Override
  public double score(ArticleOrBuilder article) {
    if (isAboutMilitary(article)) {
      return 0;
    }
    switch (featureId) {
      case MANUAL_HEURISTIC_ACQUISITIONS:
        return relevanceToAcquisitions(article);
      case MANUAL_HEURISTIC_LAUNCHES:
        return relevanceToLaunches(article);
      case MANUAL_HEURISTIC_FUNDRAISING:
        return relevanceToFundraising(article);
    }
    throw new IllegalStateException("This scorer does not support the current feature ID");
  }

  @VisibleForTesting
  static double getScore(String text, Map<String, Double> scoreRules) {
    Pattern pattern;
    synchronized (PATTERN_CACHE) {
      if (PATTERN_CACHE.containsKey(scoreRules)) {
        pattern = PATTERN_CACHE.get(scoreRules);
      } else {
        pattern = Pattern.compile(new StringBuilder()
            .append("(")
            .append(Joiner.on("|").join(scoreRules.keySet()))
            .append(")")
            .toString());
        PATTERN_CACHE.put(scoreRules, pattern);
      }
    }
    double score = 0;
    Matcher matcher = pattern.matcher(text);
    while (matcher.find() && scoreRules.containsKey(matcher.group(1))) {
      score = Math.max(score, scoreRules.get(matcher.group(1)));
    }
    return score;
  }
  
  @VisibleForTesting
  static double getScore(String text, Map<String, Double> scoreRules, Iterable<String> blacklist) {
    Pattern pattern;
    Pattern blacklistPattern;

    synchronized (PATTERN_CACHE) {
      if (PATTERN_CACHE.containsKey(blacklist)) {
        blacklistPattern = PATTERN_CACHE.get(blacklist);
      } else {
        blacklistPattern = Pattern.compile(new StringBuilder()
            .append("(")
            .append(Joiner.on("|").join(blacklist))
            .append(")")
            .toString());
        PATTERN_CACHE.put(blacklist, blacklistPattern);
      }
    }

    Matcher blacklistMatcher = blacklistPattern.matcher(text);
    if (blacklistMatcher.find()) {
      return -1;
    }

    synchronized (PATTERN_CACHE) {
      if (PATTERN_CACHE.containsKey(scoreRules)) {
        pattern = PATTERN_CACHE.get(scoreRules);
      } else {
        pattern = Pattern.compile(new StringBuilder()
            .append("(")
            .append(Joiner.on("|").join(scoreRules.keySet()))
            .append(")")
            .toString());
        PATTERN_CACHE.put(scoreRules, pattern);
      }
    }
    double score = 0;
    Matcher matcher = pattern.matcher(text);
    while (matcher.find() && scoreRules.containsKey(matcher.group(1))) {
      score = Math.max(score, scoreRules.get(matcher.group(1)));
    }
    
    return score;
  }

  private static boolean isAboutMilitary(ArticleOrBuilder article) {
    return 0 !=
        getScore(article.getTitle().toLowerCase(), MILITARY_SCORES)
        + getScore(BODY_CACHE.getBody(article), MILITARY_SCORES);
  }

  public static double relevanceToAcquisitions(ArticleOrBuilder article) {
    double titleScore = getScore(article.getTitle().toLowerCase(), ACQUISITION_TITLE_SCORES, ACQUISITION_TITLE_BLACKLIST);
    if (titleScore < 0) {
      // A blacklist word was found
      return 0;
    }

    String firstParagraph = "";
    if (article.getParagraphCount() > 0) {
      firstParagraph = article.getParagraph(0).toLowerCase();
    }

    double bodyScore = getScore(firstParagraph, ACQUISITION_BODY_SCORES, ACQUISITION_BODY_BLACKLIST);
    if (bodyScore < 0) {
      // A blacklist word was found
      return 0;
    }

    return Math.max(titleScore, bodyScore);
  }

  public static double relevanceToLaunches(ArticleOrBuilder article) {
    double titleScore = getScore(article.getTitle().toLowerCase(), LAUNCH_TITLE_SCORES, LAUNCH_TITLE_BLACKLIST);
    if (titleScore < 0) {
      // A blacklist word was found
      return 0;
    }

    String firstParagraph = "";
    if (article.getParagraphCount() > 0) {
      firstParagraph = article.getParagraph(0).toLowerCase();
    }

    double bodyScore = getScore(firstParagraph, LAUNCH_BODY_SCORES, LAUNCH_BODY_BLACKLIST);
    if (bodyScore < 0) {
      // A blacklist word was found
      return 0;
    }

    return Math.max(titleScore, bodyScore);
  }

  public static double relevanceToFundraising(ArticleOrBuilder article) {
    double titleScore = getScore(article.getTitle().toLowerCase(), FUNDRAISING_TITLE_SCORES, FUNDRAISING_TITLE_BLACKLIST);
    if (titleScore < 0) {
      // A blacklist word was found
      return 0;
    }

    String firstParagraph = "";
    if (article.getParagraphCount() > 0) {
      firstParagraph = article.getParagraph(0).toLowerCase();
    }

    double bodyScore = getScore(firstParagraph, FUNDRAISING_BODY_SCORES, FUNDRAISING_BODY_BLACKLIST);
    if (bodyScore < 0) {
      // A blacklist word was found
      return 0;
    }

    return Math.max(titleScore, bodyScore);
  }
}
