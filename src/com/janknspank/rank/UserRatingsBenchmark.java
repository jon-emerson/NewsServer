package com.janknspank.rank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.google.api.client.util.Sets;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.janknspank.ArticleCrawler;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.UrlRatings;
import com.janknspank.bizness.Users;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.UserProto.UrlRating;
import com.janknspank.proto.UserProto.User;

public class UserRatingsBenchmark {
  private final static Logger LOG = Logger.getLogger(UserRatingsBenchmark.class.getName());

  /**
   * Generates ranking scores for every article-user pair passed
   * in the list of UrlRatings. This is used to determine the
   * error of a scorer against a benchmark.
   * @param allRatings
   * @param scorer
   * @return
   * @throws BiznessException
   * @throws DatabaseSchemaException
   */
  public static Map<UrlRating, Double> calculateScoresFor(Iterable<UrlRating> allRatings, 
      Scorer scorer) throws BiznessException, DatabaseSchemaException {
    Map<UrlRating, Double> scoreMap = Maps.newHashMap();

    Set<String> urlStrings = Sets.newHashSet();
    Set<String> userEmails = Sets.newHashSet();
    for (UrlRating urlRating : allRatings) {
      urlStrings.add(urlRating.getUrl());
      userEmails.add(urlRating.getEmail());
    }

    // Load up all users who submitted ratings
    Iterable<User> users = Users.getByEmails(userEmails);
    Map<String, User> emailUserMap = Maps.newHashMap();
    for (User user: users) {
      emailUserMap.put(user.getEmail(), user);
    }

    // Load up all articles that have ratings
    Collection<Article> articles = ArticleCrawler.getArticles(urlStrings).values();
    Map<String, Article> urlArticleMap = Maps.newHashMap();
    for (Article article: articles) {
      urlArticleMap.put(article.getUrl(), article);
    }

    // Compute scores
    for (UrlRating urlRating : allRatings) {
      Article article = urlArticleMap.get(urlRating.getUrl());
      User user = emailUserMap.get(urlRating.getEmail());

      if (article != null && user != null) {
        // Use the holdback for the benchmark. The other 80% are used
        // to train the neural network.
        if (NeuralNetworkTrainer.isInTrainingHoldback(article)) {
          scoreMap.put(urlRating, scorer.getScore(user, article));
        }
      } else {
        if (article == null) {
          LOG.warning("Can't find Article to score: " + urlRating.getUrl());
        }
        if (user == null) {
          LOG.warning("Can't find User to score: " + urlRating.getEmail());
        }
      }
    }
    return scoreMap;
  }

  /**
   * Prints out a performance score (aka a "grade") for how well the Scorer did
   * at creating scores compared to user ratings.
   */
  public static void grade(Map<UrlRating, Double> scores) {
    int positives = 0;
    int falseNegatives = 0;
    int negatives = 0;
    int falsePositives = 0;
    List<String> falseNegativesUrls = new ArrayList<>();
    for (Map.Entry<UrlRating, Double> entry : scores.entrySet()) {
      double spotterScore = entry.getValue();
      UrlRating userUrlRating = entry.getKey();
      
      if (userUrlRating.getRating() > 0.5 && spotterScore > 0.5) {
        positives++;
      } else if (userUrlRating.getRating() > 0.5 && spotterScore <= 0.5) {
        falseNegatives++;
        falseNegativesUrls.add(userUrlRating.getUrl());
      } else if (userUrlRating.getRating() <= 0.5 && spotterScore <= 0.5) {
        negatives++;
      } else if (userUrlRating.getRating() <= 0.5 && spotterScore > 0.5) {
        falsePositives++;
      }
    }

    System.out.println("Positives: " + positives + " (GOOD!)");
    System.out.println("False negatives: " + falseNegatives);
    System.out.println("False positives: " + falsePositives);
    System.out.println("Negatives: " + negatives + " (GOOD!)");
    System.out.println("Percent correct: " +
        (int) (100 * (((double) positives + negatives)
            / (scores.size()))) + "%");
    System.out.println("False negative urls:");
    for (int i = 0; i < falseNegativesUrls.size(); i++) {
      System.out.println("  " + falseNegativesUrls.get(i));
    }
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

  private static void printHistogram(Map<UrlRating, Double> spotterScores) {
    List<Double> goodArticleScores = new ArrayList<>();
    List<Double> badArticleScores = new ArrayList<>();

    for (Map.Entry<UrlRating, Double> entry: spotterScores.entrySet()) {
      UrlRating userRating = entry.getKey();
      Double spotterRating = entry.getValue();
      if (userRating.getRating() > 0.5) {
        goodArticleScores.add(spotterRating);
      } else {
        badArticleScores.add(spotterRating);
      }
    }
    
    Multiset<Integer> goodHistogram = getHistogram(goodArticleScores);
    Multiset<Integer> badHistogram = getHistogram(badArticleScores);
    for (int i = 9; i >= 0; i--) {
      String start = "0." + i;
      String end = (i == 9) ? "1.0" : "0." + (i + 1);
      System.out.println("* " + start + " to " + end + ": "
          + createStars(goodHistogram.count(i), badHistogram.count(i)));
    }
    System.out.println("* SCORE OUT OF RANGE: "
        + createStars(goodHistogram.count(-100), badHistogram.count(-100)));
  }

  public static void main(String args[]) throws Exception {
    Iterable<UrlRating> allRatings = UrlRatings.getAllRatings();
    Map<UrlRating, Double> spotterScores = 
        calculateScoresFor(allRatings, NeuralNetworkScorer.getInstance());
    System.out.println("\nNEURAL NETWORK SCORER:");
    printHistogram(spotterScores);
    grade(spotterScores);

    spotterScores = 
        calculateScoresFor(allRatings, NeuralNetworkScorer.getInstance());
    System.out.println("\nHEURISTIC SCORER:");
    printHistogram(spotterScores);
    grade(spotterScores);

    System.out.println("\n");
  }
}
