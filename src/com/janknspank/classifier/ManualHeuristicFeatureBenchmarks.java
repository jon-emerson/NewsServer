package com.janknspank.classifier;

import com.janknspank.common.AssertionException;
import com.janknspank.common.Asserts;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.protobuf.TextFormat;
import com.janknspank.bizness.BiznessException;
import com.janknspank.crawler.ArticleCrawler;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ClassifierProto.FeatureBenchmark;

/**
 * Class for testing the quality of each of the manual heuristic features. Each
 * feature has a benchmark of good and bad urls
 */
public class ManualHeuristicFeatureBenchmarks {
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

  /**
   * Scores the articles found within .benchmark files
   * for a manual heuristic feature and outputs
   * success rate of classification
   */
  @VisibleForTesting
  public static void benchmark(FeatureId featureId) throws BiznessException, AssertionException {
    ManualHeuristicFeature feature = new ManualHeuristicFeature(featureId);
    Map<Article, Double> goodArticleScores = new HashMap<>();
    Map<Article, Double> badArticleScores = new HashMap<>();
    FeatureBenchmark benchmark = ManualHeuristicFeatureBenchmarks.getByFeatureId(featureId);

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
    }
  }
  
  /**
   * Called by manualheuristicfeaturebenchmark.sh.
   * @param args an array of feature Ids (ex. 30001 for Launches) to benchmark
   */
  public static void main(String[] args) throws Exception {
    // Args are the industries code ids to benchmark
    if (args.length == 0) {
      System.out.println("ERROR: You must pass feature IDs as parameters.");
      System.out.println("Example: ./manualheuristicfeaturebenchmark.sh 30001");
      System.exit(-1);
    }
    for (String arg : args) {
      benchmark(FeatureId.fromId(Integer.parseInt(arg)));
    }
  }
}
