package com.janknspank.bizness;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.janknspank.classifier.FeatureId;
import com.janknspank.classifier.IndustryCode;
import com.janknspank.common.TopList;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleFeature;
import com.janknspank.proto.CoreProto.TrainedArticleIndustry;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.User;
import com.janknspank.proto.UserProto.UserIndustry;
import com.janknspank.rank.Deduper;
import com.janknspank.rank.Scorer;

/**
 * Helper class that manages storing and retrieving Article objects from the
 * database.
 */
public class Articles {
  /**
   * Gets articles that contain a set of keywords.
   */
  public static Iterable<Article> getArticlesForKeywords(Iterable<String> keywords, int limit)
      throws DatabaseSchemaException {
    return Database.with(Article.class).get(
        new QueryOption.DescendingSort("published_time"),
        new QueryOption.WhereEquals("keyword.keyword", keywords),
        new QueryOption.Limit(limit));
  }

  public static TopList<Article, Double> getArticlesForFeature(FeatureId featureId, int limit)
      throws DatabaseSchemaException {
    TopList<Article, Double> goodArticles = new TopList<>(limit * 2);
    for (Article article : Database.with(Article.class).get(
        new QueryOption.DescendingSort("published_time"),
        new QueryOption.WhereEqualsNumber("feature.feature_id", featureId.getId()),
        new QueryOption.Limit(limit * 20))) {
      double similarity = 0;
      for (ArticleFeature feature : article.getFeatureList()) {
        if (feature.getFeatureId() == featureId.getId()) {
          similarity = feature.getSimilarity();
          break;
        }
      }
      goodArticles.add(article, similarity);
    }
    TopList<Article, Double> bestArticles = new TopList<>(limit);
    for (Article article : Deduper.filterOutDupes(goodArticles)) {
      // Include the social score in the ranking here, since we're not using a
      // neural network for ranking, so otherwise it wouldn't be considered.
      bestArticles.add(article, goodArticles.getValue(article));
    }
    return bestArticles;
  }

  /**
   * This is the best method for getting articles relevant to the current user!!
   * This is probably what you're looking for! :)
   */
  public static Iterable<Article> getRankedArticles(User user, Scorer scorer, int limit)
      throws DatabaseSchemaException {
    return getRankedArticlesAndScores(user, scorer, limit);
  }

  public static TopList<Article, Double> getRankedArticlesAndScores(User user, Scorer scorer, int limit)
      throws DatabaseSchemaException {
    TopList<Article, Double> goodArticles = new TopList<>(limit * 2);
    Set<String> urls = Sets.newHashSet();
    for (Article article : Iterables.concat(
        getArticlesByInterest(UserInterests.getCurrentInterests(user), limit * 5),
        getArticlesByIndustries(UserIndustries.getCurrentIndustries(user), limit * 5))) {
      if (urls.contains(article.getUrl())) {
        continue;
      }
      urls.add(article.getUrl());

      double hoursSincePublished =
          ((double) System.currentTimeMillis() - article.getPublishedTime())
              / TimeUnit.HOURS.toMillis(1);
      goodArticles.add(article, scorer.getScore(user, article) / hoursSincePublished);
    }
    TopList<Article, Double> bestArticles = new TopList<>(limit);
    for (Article article : Deduper.filterOutDupes(goodArticles)) {
      bestArticles.add(article, goodArticles.getValue(article));
    }
    return bestArticles;
  }

  /**
   * Used for dupe detection threshold testing.
   */
  private static Iterable<Article> getArticlesByFeatures(Iterable<FeatureId> featureIds, int limit)
      throws DatabaseSchemaException{
    return Database.with(Article.class).get(
        new QueryOption.WhereEqualsNumber("feature.feature_id", Iterables.transform(featureIds,
            new Function<FeatureId, Number>() {
          @Override
          public Number apply(FeatureId featureId) {
            return featureId.getId();
          }
        })),
        new QueryOption.DescendingSort("published_time"),
        new QueryOption.Limit(limit));
  }

  /**
   * Gets a list of articles tailored specifically to the current user's
   * interests.
   */
  public static Iterable<Article> getArticlesByInterest(Iterable<Interest> interests, int limit)
      throws DatabaseSchemaException {
    return getArticlesForKeywords(
        Iterables.transform(interests, new Function<Interest, String>() {
          @Override
          public String apply(Interest interest) {
            return interest.getKeyword();
          }
        }),
        limit);
  }
  
  /**
   * Gets a list of articles containing the user's LinkedIn contacts 
   */
  public static Iterable<Article> getArticlesContainingLinkedInContacts(User user, int limit) 
      throws DatabaseSchemaException {
    return Deduper.filterOutDupes(getArticlesByInterest(
        UserInterests.getCurrentLinkedInContacts(user), limit * 5));
  }
  
  /**
   * Gets a list of articles containing the user's Address Book contacts 
   */
  public static Iterable<Article> getArticlesContainingAddressBookContacts(User user, int limit) 
      throws DatabaseSchemaException {
    return Deduper.filterOutDupes(getArticlesByInterest(
        UserInterests.getCurrentAddressBookContacts(user), limit * 5));
  }
  
  /**
   * Gets a list of articles tailored specifically to the current user's
   * industries.
   */
  public static Iterable<Article> getArticlesByIndustries(Iterable<UserIndustry> industries, int limit)
      throws DatabaseSchemaException {
    return getArticlesByFeatures(Iterables.transform(industries,
            new Function<UserIndustry, FeatureId>() {
              @Override
              public FeatureId apply(UserIndustry industry) {
                return IndustryCode.fromId(industry.getIndustryCodeId()).getFeatureId();
              }
            }), limit);
  }

  /**
   * Returns a random article.
   */
  public static Article getRandomArticle() throws DatabaseSchemaException {
    return Database.with(Article.class).getFirst(new QueryOption.AscendingSort("rand()"));
  }

  /**
   * Returns a random untrained article
   */
  public static Article getRandomUntrainedArticle() throws DatabaseSchemaException {
    Article article;
    Iterable<TrainedArticleIndustry> taggedIndustries;
    do {
      article = Articles.getRandomArticle();
      taggedIndustries = TrainedArticleIndustries.getFromArticle(article.getUrlId());
    } while (!Iterables.isEmpty(taggedIndustries));
    return article;
  }

  /** Helper method for creating the Article table. */
  public static void main(String args[]) throws Exception {
    Database.with(Article.class).createTable();
  }
}
