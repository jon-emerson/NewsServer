package com.janknspank.rank;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.api.client.repackaged.com.google.common.base.Joiner;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.janknspank.bizness.BiznessException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.ArticleProto.Article;

public class Benchmark {
  /**
   * Creates an Article -> Scorer's Score map for all articles that are
   * both "good" (aka training score >= 0.5) and in the holdback set.
   */
  private static Map<Article, Double> getGoodScoreMap(
      Scorer scorer, Iterable<TrainingArticle> goodTrainingArticles)
      throws DatabaseSchemaException, BiznessException {
    Map<Article, Double> goodScores = Maps.newHashMap();
    for (TrainingArticle trainingArticle : TrainingArticles.getHoldbackArticles()) {
      if (trainingArticle.getScore() >= 0.5) {
        // This article should be scored as good.
        goodScores.put(trainingArticle.getArticle(),
            scorer.getScore(trainingArticle.getUser(), trainingArticle.getArticle()));
      }
    }
    return goodScores;
  }

  /**
   * Creates an Article -> Scorer's Score map for all articles that are
   * both "bad" (aka training score < 0.5) and in the holdback set.
   */
  private static Map<Article, Double> getBadScoreMap(
      Scorer scorer, Iterable<TrainingArticle> badTrainingArticles)
      throws DatabaseSchemaException, BiznessException {
    Map<Article, Double> goodScores = Maps.newHashMap();
    for (TrainingArticle trainingArticle : TrainingArticles.getHoldbackArticles()) {
      if (trainingArticle.getScore() < 0.5) {
        // This article should be scored as bad.
        goodScores.put(trainingArticle.getArticle(),
            scorer.getScore(trainingArticle.getUser(), trainingArticle.getArticle()));
      }
    }
    return goodScores;
  }

  /**
   * Prints out a performance evaluation for how well the passed Scorer does at
   * creating scores for the passed Article -> Score maps.
   */
  public static double grade(Scorer scorer) throws DatabaseSchemaException, BiznessException {
    Map<Article, Double> goodScoreMap = getGoodScoreMap(scorer, TrainingArticles.getHoldbackArticles());
    Map<Article, Double> badScoreMap = getBadScoreMap(scorer, TrainingArticles.getHoldbackArticles());
    int positives = 0;
    int falseNegatives = 0;
    List<String> falseNegativesTitles = new ArrayList<>();
    for (Map.Entry<Article, Double> entry : goodScoreMap.entrySet()) {
      double score = entry.getValue();
      Article article = entry.getKey();
      if (score > 0.5) {
        positives++;
      } else {
        falseNegatives++;
        falseNegativesTitles.add(article.getTitle());
      }
    }
    int negatives = 0;
    int falsePositives = 0;
    for (Double score : badScoreMap.values()) {
      if (score <= 0.5) {
        negatives++;
      } else {
        falsePositives++;
      }
    }
    System.out.println("Positives: " + positives
        + " (" + (int) ((double) 100 * positives / goodScoreMap.size()) + "% correct)");
    System.out.println("False negatives: " + falseNegatives);
    System.out.println("False positives: " + falsePositives);
    System.out.println("Negatives: " + negatives
        + " (" + (int) ((double) 100 * negatives / badScoreMap.size()) + "% correct)");
    System.out.println("Percent correct: " +
        (100 * (((double) positives + negatives)
            / (goodScoreMap.size() + badScoreMap.size()))) + "%");
    System.out.println("False negative titles:");
    for (int i = 0; i < falseNegativesTitles.size(); i++) {
      System.out.println("  " + falseNegativesTitles.get(i));
    }

    // Return a quality score.
    return ((double) positives + negatives) / (goodScoreMap.size() + badScoreMap.size());
  }

  /**
   * Returns a multiset that counts how many scores are within each 1/10th
   * decimal increment.  Field "0" contains a count of scores between 0 and
   * 0.1, field "1" contains a count of scores between 0.1 and 0.2, etc.
   */
  private static Multiset<Integer> getHistogram(Iterable<Double> scores) {
    Multiset<Integer> histogram = HashMultiset.create();
    for (Double score : scores) {
      int bucket = (int) (score * 10);
      if (bucket < 0 || bucket >= 10) {
        bucket = -100;
      }
      histogram.add(bucket);
    }
    return histogram;
  }

  private static String createStars(int good, int bad) {
    return Joiner.on("").join(Iterables.limit(Iterables.cycle("g"), good)) +
        Joiner.on("").join(Iterables.limit(Iterables.cycle("B"), bad));
  }

  /**
   * Prints (to console) a histogram, showing the actual scores for articles
   * considered good vs. bad in the training holdback.
   */
  private static void printHistogram(Scorer scorer) throws DatabaseSchemaException, BiznessException {
    Multiset<Integer> goodHistogram = getHistogram(
        getGoodScoreMap(scorer, TrainingArticles.getHoldbackArticles()).values());
    Multiset<Integer> badHistogram = getHistogram(
        getBadScoreMap(scorer, TrainingArticles.getHoldbackArticles()).values());
    for (int i = 9; i >= 0; i--) {
      String start = "0." + i;
      String end = (i == 9) ? "1.0" : "0." + (i + 1);
      System.out.println("* " + start + " to " + end + ": "
          + createStars(goodHistogram.count(i), badHistogram.count(i)));
    }
    System.out.println("* SCORE OUT OF RANGE: "
        + createStars(goodHistogram.count(-100), badHistogram.count(-100)));
  }

  public static double printBenchmark(Scorer scorer) throws DatabaseSchemaException, BiznessException {
    System.out.println("\nBENCHMARK:");
    printHistogram(scorer);
    double grade = grade(scorer);

    System.out.println("\n");
    return grade;
  }

  public static void main(String args[]) throws Exception {
    printBenchmark(NeuralNetworkScorer.getInstance());
  }
}
