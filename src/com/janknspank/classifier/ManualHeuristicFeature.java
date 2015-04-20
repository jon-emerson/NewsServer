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
  private static final Map<Pattern, Double> ACQUISITION_TITLE_SCORES =
      ImmutableMap.<Pattern, Double>builder()
          .put(Pattern.compile("acquires"), 1.0)
          .put(Pattern.compile("is acquiring"), 1.0)
          .put(Pattern.compile("buys"), 0.9)
          .put(Pattern.compile("is buying"), 0.8)
          .put(Pattern.compile("buying .+ for"), 1.0)
          .put(Pattern.compile("to acquire .+illion"), 1.0)
          .build();
  private static final Iterable<Pattern> ACQUISITION_TITLE_BLACKLIST =
      Arrays.asList(Pattern.compile("for buying"), 
          Pattern.compile("home-buying"), 
          Pattern.compile("-buying"), 
          Pattern.compile("buying for"), 
          Pattern.compile("buying experience"),
          Pattern.compile("worth buying"),
          Pattern.compile("buying\\?"),
          Pattern.compile("buying opportunity"),
          Pattern.compile("biggest buys"),
          Pattern.compile("who buys"),
          Pattern.compile("is buying what's"));
  private static final Map<Pattern, Double> ACQUISITION_BODY_SCORES =
      ImmutableMap.<Pattern, Double>builder()
          .put(Pattern.compile("acquires"), 0.8)
          .put(Pattern.compile("is acquiring"), 0.8)
          .build();
  private static final Iterable<Pattern> ACQUISITION_BODY_BLACKLIST =
      Arrays.asList();
  private static final Map<Pattern, Double> LAUNCH_TITLE_SCORES =
      ImmutableMap.<Pattern, Double>builder()
          .put(Pattern.compile("launches"), 1.0)
          .put(Pattern.compile("launched"), 0.9)
          .put(Pattern.compile("to launch"), 0.9)
          .put(Pattern.compile("releases"), 0.9)
          .build();
  private static final Iterable<Pattern> LAUNCH_TITLE_BLACKLIST =
      Arrays.asList(Pattern.compile("manifesto"),
          Pattern.compile("report"),
          Pattern.compile("ago"),
          Pattern.compile("court"),
          Pattern.compile("lawsuit"),
          Pattern.compile("vinyl"),
          Pattern.compile("investigation"),
          Pattern.compile("criminal"));
  private static final Map<Pattern, Double> LAUNCH_BODY_SCORES =
      ImmutableMap.<Pattern, Double>builder()
          .put(Pattern.compile("launches"), 0.6)
          .build();
  private static final Iterable<Pattern> LAUNCH_BODY_BLACKLIST =
      Arrays.asList(Pattern.compile("manifesto"),
          Pattern.compile("report"),
          Pattern.compile("ago"),
          Pattern.compile("was launched"),
          Pattern.compile("social media campaign"), 
          Pattern.compile("fell"),
          Pattern.compile("rose"),
          Pattern.compile("as it launches"),
          Pattern.compile("were launched"),
          Pattern.compile("which launches"));
  private static final Map<Pattern, Double> FUNDRAISING_TITLE_SCORES =
      ImmutableMap.<Pattern, Double>builder()
          .put(Pattern.compile("series a "), 1.0)
          .put(Pattern.compile("series a$"), 1.0)
          .put(Pattern.compile("series a,"), 1.0)
          .put(Pattern.compile("series b "), 1.0)
          .put(Pattern.compile("series b$"), 1.0)
          .put(Pattern.compile("series b,"), 1.0)
          .put(Pattern.compile("series c "), 1.0)
          .put(Pattern.compile("series c$"), 1.0)
          .put(Pattern.compile("series c,"), 1.0)
          .put(Pattern.compile("series d "), 1.0)
          .put(Pattern.compile("series d$"), 1.0)
          .put(Pattern.compile("series d,"), 1.0)
          .put(Pattern.compile("series e "), 1.0)
          .put(Pattern.compile("series e$"), 1.0)
          .put(Pattern.compile("series e,"), 1.0)
          .put(Pattern.compile("angel round"), 1.0)
          .put(Pattern.compile("funding round"), 1.0)
          .put(Pattern.compile("raises \\$.+m funding"), 1.0)
          .put(Pattern.compile("raises \\$.+ million"), 1.0)
          .put(Pattern.compile("million of funding"), 1.0)
          .build();
  private static final Iterable<Pattern> FUNDRAISING_TITLE_BLACKLIST =
      Arrays.asList(Pattern.compile("declares dividends"));
  private static final Map<Pattern, Double> FUNDRAISING_BODY_SCORES =
      ImmutableMap.<Pattern, Double>builder()
          .put(Pattern.compile("series a\\."), 0.9)
          .put(Pattern.compile("series a "), 0.9)
          .put(Pattern.compile("series a,"), 0.9)
          .put(Pattern.compile("series b\\."), 0.9)
          .put(Pattern.compile("series b "), 0.9)
          .put(Pattern.compile("series b,"), 0.9)
          .put(Pattern.compile("series c\\."), 0.9)
          .put(Pattern.compile("series c "), 0.9)
          .put(Pattern.compile("series c,"), 0.9)
          .put(Pattern.compile("series d\\."), 0.9)
          .put(Pattern.compile("series d "), 0.9)
          .put(Pattern.compile("series d,"), 0.9)
          .put(Pattern.compile("series e\\."), 0.9)
          .put(Pattern.compile("series e "), 0.9)
          .put(Pattern.compile("series e,"), 0.9)
          .put(Pattern.compile("angel round"), 0.9)
          .build();
  private static final Iterable<Pattern> FUNDRAISING_BODY_BLACKLIST =
      Arrays.asList();
  private static final Map<Pattern, Double> MILITARY_SCORES =
      ImmutableMap.<Pattern, Double>builder()
          .put(Pattern.compile("afghanistan"), 1.0)
          .put(Pattern.compile("attack"), 1.0)
          .put(Pattern.compile("attacks"), 1.0)
          .put(Pattern.compile("crimea"), 1.0)
          .put(Pattern.compile("gaza"), 1.0)
          .put(Pattern.compile("injured"), 1.0)
          .put(Pattern.compile("iran"), 1.0)
          .put(Pattern.compile("iraq"), 1.0)
          .put(Pattern.compile("isil"), 1.0)
          .put(Pattern.compile("isis"), 1.0)
          .put(Pattern.compile("killed"), 1.0)
          .put(Pattern.compile("lebanon"), 1.0)
          .put(Pattern.compile("militant"), 1.0)
          .put(Pattern.compile("militants"), 1.0)
          .put(Pattern.compile("military"), 1.0)
          .put(Pattern.compile("mortar"), 1.0)
          .put(Pattern.compile("mortars"), 1.0)
          .put(Pattern.compile("nasa"), 1.0)
          .put(Pattern.compile("north korea"), 1.0)
          .put(Pattern.compile("pakistan"), 1.0)
          .put(Pattern.compile("paramilitary"), 1.0)
          .put(Pattern.compile("rocket"), 1.0)
          .put(Pattern.compile("wounded"), 1.0)
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
  static double getScore(String text, Map<Pattern, Double> scoreRules) {
    double score = 0;
    for (Pattern pattern : scoreRules.keySet()) {
      Matcher matcher = pattern.matcher(text);
      if (matcher.find()) {
        score = Math.max(score, scoreRules.get(pattern));
      }
    }
    return score;
  }

  @VisibleForTesting
  static double getScore(String text, Map<Pattern, Double> scoreRules, Iterable<Pattern> blacklist) {
    if (Iterables.size(blacklist) > 0) {
      for (Pattern blacklistPattern : blacklist) {
        Matcher blacklistMatcher = blacklistPattern.matcher(text);
        if (blacklistMatcher.find()) {
          return -1;
        }
      }
    }

    return getScore(text, scoreRules);
  }

  private static boolean isAboutMilitary(ArticleOrBuilder article) {
    return 0 !=
        getScore(article.getTitle().toLowerCase(), MILITARY_SCORES)
        + getScore(BODY_CACHE.getBody(article), MILITARY_SCORES);
  }

  public static double relevanceToAcquisitions(ArticleOrBuilder article) {
    double titleScore = getScore(article.getTitle().toLowerCase(), 
        ACQUISITION_TITLE_SCORES, ACQUISITION_TITLE_BLACKLIST);
    if (titleScore < 0) {
      // A blacklist word was found
      return 0;
    }

    String firstParagraph = Iterables.getFirst(article.getParagraphList(), "").toLowerCase();
    double firstParagraphScore = getScore(firstParagraph, ACQUISITION_BODY_SCORES, 
        ACQUISITION_BODY_BLACKLIST);
    if (firstParagraphScore < 0) {
      // A blacklist word was found
      return 0;
    }

    return Math.max(titleScore, firstParagraphScore);
  }

  public static double relevanceToLaunches(ArticleOrBuilder article) {
    double titleScore = getScore(article.getTitle().toLowerCase(), 
        LAUNCH_TITLE_SCORES, LAUNCH_TITLE_BLACKLIST);
    if (titleScore < 0) {
      // A blacklist word was found
      return 0;
    }

    String firstParagraph = Iterables.getFirst(article.getParagraphList(), "").toLowerCase();
    double firstParagraphScore = getScore(firstParagraph, LAUNCH_BODY_SCORES, LAUNCH_BODY_BLACKLIST);
    if (firstParagraphScore < 0) {
      // A blacklist word was found
      return 0;
    }

    return Math.max(titleScore, firstParagraphScore);
  }

  public static double relevanceToFundraising(ArticleOrBuilder article) {
    double titleScore = getScore(article.getTitle().toLowerCase(), 
        FUNDRAISING_TITLE_SCORES, FUNDRAISING_TITLE_BLACKLIST);
    if (titleScore < 0) {
      // A blacklist word was found
      return 0;
    }

    String firstParagraph = Iterables.getFirst(article.getParagraphList(), "").toLowerCase();
    double firstParagraphScore = getScore(firstParagraph, FUNDRAISING_BODY_SCORES, 
        FUNDRAISING_BODY_BLACKLIST);
    if (firstParagraphScore < 0) {
      // A blacklist word was found
      return 0;
    }

    return Math.max(titleScore, firstParagraphScore);
  }
}
