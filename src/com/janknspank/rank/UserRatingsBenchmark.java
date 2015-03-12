package com.janknspank.rank;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.api.client.util.Sets;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.UrlRatings;
import com.janknspank.bizness.Users;
import com.janknspank.common.Logger;
import com.janknspank.crawler.ArticleCrawler;
import com.janknspank.crawler.ArticleUrlDetector;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.CoreProto.UrlRating;
import com.janknspank.proto.UserProto.User;

public class UserRatingsBenchmark {
  private final static Logger LOG = new Logger(UserRatingsBenchmark.class);

  /**
   * Generates ranking scores for every article-user pair passed
   * in the list of UrlRatings. This is used to determine the
   * error of a scorer against a benchmark.
   */
  public static Map<UrlRating, Double> calculateScoresFor(Iterable<UrlRating> allRatings, 
      Scorer scorer) throws BiznessException, DatabaseSchemaException {
    Map<UrlRating, Double> scoreMap = Maps.newHashMap();

    Set<String> urlStrings = Sets.newHashSet();
    Set<String> userEmails = Sets.newHashSet();
    for (UrlRating urlRating : allRatings) {
      // Double check that we still support the site that was rated.
      // (Sometimes we take down sites that aren't relevant to most people.)
      if (ArticleUrlDetector.isArticle(urlRating.getUrl())) {
        urlStrings.add(urlRating.getUrl());
        userEmails.add(urlRating.getEmail());
      }
    }

    // Load up all users who submitted ratings
    Iterable<User> users = Users.getByEmails(userEmails);
    Map<String, User> emailUserMap = Maps.newHashMap();
    for (User user : users) {
      emailUserMap.put(user.getEmail(), user);
    }

    // Load up all articles that have ratings
    Map<String, Article> urlArticleMap = ArticleCrawler.getArticles(urlStrings, true /* retain */);

    // Compute scores
    for (UrlRating urlRating : allRatings) {
      Article article = urlArticleMap.get(urlRating.getUrl());
      User user = emailUserMap.get(urlRating.getEmail());

      // A user rating may outlive any particular Article or User
      // in our system. So check to see that they exist before scoring
      if (article == null) {
        LOG.warning("Can't find Article to score: " + urlRating.getUrl());
      } else if (user == null) {
        LOG.warning("Can't find User to score: " + urlRating.getEmail());
      } else {
        // Use the holdback for the benchmark. The other 80% are used
        // to train the neural network.
        if (NeuralNetworkTrainer.isInTrainingHoldback(article)) {
          scoreMap.put(urlRating, scorer.getScore(user, article));
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
    int positives = 0; // # of Articles both the scorer and user thought were good
    int userPositives = 0; // Total # of Articles which the user thought were good
    int falseNegatives = 0; // Good articles the scorer thought were bad
    int negatives = 0; // # of Articles both the scorer and user thought were bad
    int userNegatives = 0; // Total # of Articles which the user thought were bad
    int falsePositives = 0; // Bad articles the scorer thought were good
    List<String> falseNegativesUrls = new ArrayList<>();
    for (Map.Entry<UrlRating, Double> entry : scores.entrySet()) {
      double score = entry.getValue();
      UrlRating userUrlRating = entry.getKey();
      if (userUrlRating.getRating() > 0.5) {
        userPositives++;
      } else {
        userNegatives++;
      }

      if (userUrlRating.getRating() > 0.5 && score > 0.5) {
        positives++;
      } else if (userUrlRating.getRating() > 0.5 && score <= 0.5) {
        falseNegatives++;
        falseNegativesUrls.add(userUrlRating.getUrl());
      } else if (userUrlRating.getRating() <= 0.5 && score <= 0.5) {
        negatives++;
      } else if (userUrlRating.getRating() <= 0.5 && score > 0.5) {
        falsePositives++;
      }
    }

    System.out.println("Positives: " + positives
        + " (" + (int) ((double) 100 * positives / userPositives) + "% correct)");
    System.out.println("False negatives: " + falseNegatives);
    System.out.println("False positives: " + falsePositives);
    System.out.println("Negatives: " + negatives
        + " (" + (int) ((double) 100 * negatives / userNegatives) + "% correct)");
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

  private static void printHistogram(Map<UrlRating, Double> scores) {
    List<Double> goodArticleScores = new ArrayList<>();
    List<Double> badArticleScores = new ArrayList<>();

    for (Map.Entry<UrlRating, Double> entry: scores.entrySet()) {
      UrlRating userRating = entry.getKey();
      Double rating = entry.getValue();
      if (userRating.getRating() > 0.5) {
        goodArticleScores.add(rating);
      } else {
        badArticleScores.add(rating);
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
    Map<UrlRating, Double> scores =
        calculateScoresFor(allRatings, NeuralNetworkScorer.getInstance());
    System.out.println("\nNEURAL NETWORK SCORER:");
    printHistogram(scores);
    grade(scores);

    scores =
        calculateScoresFor(allRatings, HeuristicScorer.getInstance());
    System.out.println("\nHEURISTIC SCORER:");
    printHistogram(scores);
    grade(scores);

    System.out.println("\n");
  }
}
