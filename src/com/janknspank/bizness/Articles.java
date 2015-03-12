package com.janknspank.bizness;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.janknspank.classifier.FeatureId;
import com.janknspank.common.TopList;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleFeature;
import com.janknspank.proto.CoreProto.Entity;
import com.janknspank.proto.UserProto.AddressBookContact;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.Interest.InterestType;
import com.janknspank.proto.UserProto.LinkedInContact;
import com.janknspank.proto.UserProto.User;
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

  /**
   * NOTE(jonemerson): This function is crap and shouldn't be used in production.
   */
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

  public static TopList<Article, Double> getRankedArticlesAndScores(
      User user, Scorer scorer, int limit) throws DatabaseSchemaException {
    TopList<Article, Double> goodArticles = new TopList<>(limit * 2);
    Set<String> urls = Sets.newHashSet();
    for (Article article :
        getArticlesForInterests(user, UserInterests.getInterests(user), limit * 5)) {
      if (urls.contains(article.getUrl())) {
        continue;
      }
      urls.add(article.getUrl());

      goodArticles.add(article, scorer.getScore(user, article) /
          Math.sqrt(getHoursSincePublished(article)));
    }
    TopList<Article, Double> bestArticles = new TopList<>(limit);
    for (Article article : Deduper.filterOutDupes(goodArticles)) {
      bestArticles.add(article, goodArticles.getValue(article));
    }
    return bestArticles;
  }

  /**
   * Returns the number of hours since the passed article was published, with
   * a minimum of 15 hours.
   */
  private static double getHoursSincePublished(Article article) {
    return Math.max(18,
        ((double) System.currentTimeMillis() - getPublishedTime(article))
            / TimeUnit.HOURS.toMillis(1)) - 15;
  }

  /**
   * A more intelligent approach to knowing when an article was published: The
   * time we discovered the article, unless the site has self-reported that the
   * article is at least 36 hours older.  This means that sites can't lie and
   * post- or pre-date their publish times... But, if we discover an article
   * well after it was published, we honor its older publish date.
   */
  public static long getPublishedTime(Article article) {
    return (article.getCrawlTime() - TimeUnit.HOURS.toMillis(36) > article.getPublishedTime())
        ? article.getPublishedTime()
        : article.getCrawlTime();
  }

  /**
   * Gets a list of articles tailored specifically to the current user's
   * interests.
   */
  public static Iterable<Article> getArticlesForInterests(
      User user, Iterable<Interest> interests, int limitPerType)
      throws DatabaseSchemaException {
    List<FeatureId> featureIds = Lists.newArrayList();
    List<String> keywords = Lists.newArrayList();
    for (Interest interest : interests) {
      switch (interest.getType()) {
        case ADDRESS_BOOK_CONTACTS:
          for (AddressBookContact contact : user.getAddressBookContactList()) {
            keywords.add(contact.getName());
          }
          break;

        case INDUSTRY:
          featureIds.add(FeatureId.fromId(interest.getIndustryCode()));
          break;

        case LINKED_IN_CONTACTS:
          for (LinkedInContact contact : user.getLinkedInContactList()) {
            keywords.add(contact.getName());
          }
          break;

        case INTENT:
          // INTENT interests are just used for ranking.
          break;

        case ENTITY:
          keywords.add(interest.getEntity().getKeyword());
          break;

        case UNKNONWN:
          break;
      }
    }
    // TODO(jonemerson): Each query should be a future.  The response should be
    // a transforming future that dedupes the results.
    return Iterables.concat(
        getArticlesByFeatureId(featureIds, limitPerType),
        getArticlesForKeywords(keywords, limitPerType));
  }
  
  /**
   * Gets articles containing a specific entity (person, organization, or place)
   */
  public static Iterable<Article> getArticlesForEntity(Entity entity, int limitPerType) 
      throws DatabaseSchemaException {
    return Deduper.filterOutDupes(getArticlesForKeywords(ImmutableList.of(entity.getKeyword()), limitPerType));
  }

  /**
   * Gets a list of articles tailored specifically to the specified
   * industries.
   */
  public static Iterable<Article> getArticlesByFeatureId(
      Iterable<FeatureId> featureIds, int limit) throws DatabaseSchemaException {
    if (Iterables.isEmpty(featureIds)) {
      return ImmutableList.of();
    }
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
   * Returns a random article.
   */
  public static Article getRandomArticle() throws DatabaseSchemaException {
    return Database.with(Article.class).getFirst(new QueryOption.AscendingSort("rand()"));
  }

  public static Iterable<Article> getArticlesForContacts(
      User user, InterestType contactType, int limit) throws DatabaseSchemaException {
    // Kinda hacky but it works and re-uses code.
    return getArticlesForInterests(user,
        ImmutableList.of(Interest.newBuilder().setType(contactType).build()),
        limit);
  }

  /** Helper method for creating the Article table. */
  public static void main(String args[]) throws Exception {
    Database.with(Article.class).createTable();
  }
}
