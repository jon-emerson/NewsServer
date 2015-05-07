package com.janknspank.classifier;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.protobuf.TextFormat;
import com.janknspank.bizness.BiznessException;
import com.janknspank.common.AssertionException;
import com.janknspank.crawler.ArticleCrawler;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ClassifierProto.FeatureBenchmark;

public class ManualHeuristicFeatureTest {
  /**
   * Map from FeatureId to FeatureBenchmark object.
   */
  private static Map<FeatureId, FeatureBenchmark> FEATURE_BENCHMARK_MAP = null;

  /**
   * Builds a Map of FeatureBenchmark object definitions, keyed by FeatureId, from
   * the .benchmark files located in /classifier/manual_heuristic_feature/.
   */
  public static synchronized Map<FeatureId, FeatureBenchmark> getFeatureBenchmarkMap() {
    if (FEATURE_BENCHMARK_MAP == null) {
      ImmutableMap.Builder<FeatureId, FeatureBenchmark> benchmarkListBuilder = ImmutableMap.builder();
      for (File benchmarkFile : new File("classifier/manual_heuristic_feature").listFiles()) {
        if (!benchmarkFile.getName().endsWith(".benchmark")) {
          continue;
        }
        FeatureBenchmark.Builder benchmarkBuilder = FeatureBenchmark.newBuilder();
        Reader reader = null;
        try {
          reader = new FileReader(benchmarkFile);
          try {
            TextFormat.merge(reader, benchmarkBuilder);
          } catch (TextFormat.ParseException e) {
            throw new Error(
                "Error in benchmark " + benchmarkFile.getAbsolutePath() + ": " + e.getMessage(), e);
          }
          benchmarkListBuilder.put(FeatureId.fromId((int) benchmarkBuilder.getFeatureId()), 
              benchmarkBuilder.build());
        } catch (IOException e) {
          throw new Error(e);
        } finally {
          IOUtils.closeQuietly(reader);
        }
      }
      FEATURE_BENCHMARK_MAP = benchmarkListBuilder.build();
    }
    return FEATURE_BENCHMARK_MAP;
  }

  public static FeatureBenchmark getByFeatureId(FeatureId featureId) {
    return getFeatureBenchmarkMap().get(featureId);
  }

  @Test
  public void testGetScore() {
    assertEquals(50.0,
        ManualHeuristicFeature.getScore("Moose drool 500",
            ImmutableMap.<Pattern, Double>builder()
                .put(Pattern.compile("hello"), 200.0)
                .put(Pattern.compile("drool"), 50.0)
                .build()),
        0.000001 /* epsilon */);
    assertEquals(200.0,
        ManualHeuristicFeature.getScore("Moose drool hello 500",
            ImmutableMap.<Pattern, Double>builder()
                .put(Pattern.compile("hello"), 200.0)
                .put(Pattern.compile("drool"), 50.0)
                .build()),
        0.000001 /* epsilon */);
    assertEquals(0.0,
        ManualHeuristicFeature.getScore("Moose drool 500",
            ImmutableMap.<Pattern, Double>builder()
                .put(Pattern.compile("jorge"), 200.0)
                .put(Pattern.compile("pasilda"), 50.0)
                .build()),
        0.000001 /* epsilon */);
  }

//  @Test
  public void testManualHeuristicsAgainstBenchmarks() 
      throws BiznessException, AssertionException, ClassifierException {
    for (FeatureId featureId : getFeatureBenchmarkMap().keySet()) {
      testAgainstBenchmark(featureId);
    }
  }

  /**
   * See if articles found within .benchmark files
   * for manual heuristic features are correctly classified
   */
  private void testAgainstBenchmark(FeatureId featureId) 
      throws BiznessException, AssertionException, ClassifierException {
    if (featureId.getFeatureType() != FeatureType.MANUAL_HEURISTIC) {
      throw new IllegalStateException("The specified feature ID is not a manual heuristic");
    }

    System.out.println("Testing feature: " + featureId.getId());
    ManualHeuristicFeature feature = (ManualHeuristicFeature) Feature.getFeature(featureId);
    Map<Article, Double> goodArticleScores = new HashMap<>();
    Map<Article, Double> badArticleScores = new HashMap<>();
    FeatureBenchmark benchmark = getByFeatureId(featureId);

    Collection<Article> goodArticles =
        ArticleCrawler.getArticles(benchmark.getGoodUrlList(), true /* retain */).values();
    for (Article article : goodArticles) {
      double score = feature.score(article);
      goodArticleScores.put(article, score);
      if (score < 0.5) {
        System.out.println("False negative: \"" + article.getTitle() + "\" (" + score + ")");
        System.out.println(article.getUrl());
        System.out.println("First paragraph: \""
            + Iterables.getFirst(article.getParagraphList(), "") + "\"");
        System.out.println();
      }
      assertEquals("For " + featureId.name() + " (" + featureId.getId() + "), " + article.getUrl(),
          1.0, score, 0.5 /* epsilon */);
    }

    Collection<Article> badArticles =
        ArticleCrawler.getArticles(benchmark.getBadUrlList(), true /* retain */).values();
    for (Article article : badArticles) {
      double score = feature.score(article);
      badArticleScores.put(article, score);
      if (score > 0.3) {
        System.out.println("False positive: \"" + article.getTitle() + "\" (" + score + ")");
        System.out.println(article.getUrl());
        System.out.println("First paragraph: \""
            + Iterables.getFirst(article.getParagraphList(), "") + "\"");
        System.out.println();
      }
      assertEquals("For " + featureId.name() + " (" + featureId.getId() + "), " + article.getUrl(),
          0.0, score, 0.3 /* epsilon */);
    }
  }
}
