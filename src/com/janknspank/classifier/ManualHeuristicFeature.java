package com.janknspank.classifier;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.api.client.util.Throwables;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.janknspank.proto.ArticleProto.ArticleOrBuilder;

public class ManualHeuristicFeature extends Feature {
  private static final LoadingCache<ArticleOrBuilder, String> LOWER_CASE_BODY_CACHE =
      CacheBuilder.newBuilder().maximumSize(1000).build(
          new CacheLoader<ArticleOrBuilder, String>() {
            @Override
            public String load(ArticleOrBuilder article) {
              StringBuilder bodyBuilder = new StringBuilder();
              for (String paragraph : article.getParagraphList()) {
                bodyBuilder.append(paragraph.toLowerCase());
              }
              return bodyBuilder.toString();
            }
          });
  private static final LoadingCache<String, Pattern> PATTERN_CACHE =
      CacheBuilder.newBuilder().maximumSize(1000).build(
          new CacheLoader<String, Pattern>() {
            @Override
            public Pattern load(String regex) {
              return Pattern.compile(regex);
            }
          });

  public ManualHeuristicFeature(FeatureId featureId) {
    super(featureId);
    if (featureId.getFeatureType() != FeatureType.MANUAL_HEURISTIC) {
      throw new IllegalStateException("The specified feature ID is not a manual heuristic");
    }
  }

  @Override
  public double score(ArticleOrBuilder article) throws ClassifierException {
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
    double score = 0;
    try {
      Pattern titlePattern = PATTERN_CACHE.get(
          new StringBuilder()
              .append("(")
              .append(Joiner.on("|").join(scoreRules.keySet()))
              .append(")")
              .toString());
      Matcher titleMatcher = titlePattern.matcher(text);
      while (titleMatcher.find()) {
        score = Math.max(score, scoreRules.get(titleMatcher.group(1)));
      }
    } catch (ExecutionException e) {
      Throwables.propagateIfPossible(e);
      throw new RuntimeException(e);
    }
    return score;
  }

  public static double relevanceToAcquisitions(ArticleOrBuilder article) {
    try {
      return Math.max(
          getScore(article.getTitle().toLowerCase(),
              ImmutableMap.<String, Double>builder()
                  .put("acquires", 1.0)
                  .put("is acquiring", 1.0)
                  .put("buys", 0.9)
                  .put("acquisition", 0.7)
                  .put("buying", 0.4)
                  .build()),
          getScore(LOWER_CASE_BODY_CACHE.get(article),
              ImmutableMap.<String, Double>builder()
                  .put("acquires", 0.8)
                  .put("is acquiring", 0.8)
                  .put("acquisition", 0.2)
                  .build()));
    } catch (ExecutionException e) {
      Throwables.propagateIfPossible(e);
      throw new RuntimeException(e);
    }
  }

  public static double relevanceToLaunches(ArticleOrBuilder article) {
    try {
      return Math.max(
          getScore(article.getTitle().toLowerCase(),
              ImmutableMap.<String, Double>builder()
                  .put("launches", 1.0)
                  .put("launched", 0.9)
                  .put("to launch", 0.9)
                  .put("releases", 0.9)
                  .build()),
          getScore(LOWER_CASE_BODY_CACHE.get(article),
              ImmutableMap.<String, Double>builder()
                  .put("launches", 0.8)
                  .put("launched", 0.6)
                  .put("releases", 0.4)
                  .build()));
    } catch (ExecutionException e) {
      Throwables.propagateIfPossible(e);
      throw new RuntimeException(e);
    }
  }

  public static double relevanceToFundraising(ArticleOrBuilder article) {
    try {
      return Math.max(
          getScore(article.getTitle().toLowerCase(),
              ImmutableMap.<String, Double>builder()
                  .put("raises", 1.0)
                  .put("series a", 1.0)
                  .put("series b", 1.0)
                  .put("series c", 1.0)
                  .put("series d", 1.0)
                  .put("series e", 1.0)
                  .put("angel round", 1.0)
                  .put("valuation", 1.0)
                  .build()),
          getScore(LOWER_CASE_BODY_CACHE.get(article),
              ImmutableMap.<String, Double>builder()
                  .put("raises", 0.9)
                  .put("series a", 0.9)
                  .put("series b", 0.9)
                  .put("series c", 0.9)
                  .put("series d", 0.9)
                  .put("series e", 0.9)
                  .put("angel round", 0.9)
                  .put("investors", 0.8)
                  .build()));
    } catch (ExecutionException e) {
      Throwables.propagateIfPossible(e);
      throw new RuntimeException(e);
    }
  }
}
