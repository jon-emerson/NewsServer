package com.janknspank.bizness;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.api.client.util.Maps;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.janknspank.classifier.FeatureId;
import com.janknspank.common.TopList;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.Article.Reason;
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
   * Returns a function that gives an iterable of articles a specific reason for
   * existing, such as being about companies, people, or industries.
   *
   * Yes it's a little redic that Java's async implementations make us do stupid
   * crap like this... :).
   */
  private static AsyncFunction<Iterable<Article>, Iterable<Article>>
      getFunctionToGiveArticlesReason(final Article.Reason reason) {
    return new AsyncFunction<Iterable<Article>, Iterable<Article>>() {
      @Override
      public ListenableFuture<Iterable<Article>> apply(Iterable<Article> articles) {
        return Futures.immediateFuture(
            Iterables.transform(articles, new Function<Article, Article>() {
              @Override
              public Article apply(Article article) {
                return article.toBuilder().setReason(reason).build();
              }
            }));
      }
    };
  }

  /**
   * Gets articles that contain a set of keywords.
   */
  public static ListenableFuture<Iterable<Article>> getArticlesForKeywordsFuture(
      Iterable<String> keywords, final Article.Reason reason, int limit)
      throws DatabaseSchemaException {
    return Futures.transform(
        Database.with(Article.class).getFuture(
            new QueryOption.DescendingSort("published_time"),
            new QueryOption.WhereEquals("keyword.keyword", keywords),
            new QueryOption.Limit(limit)),
        getFunctionToGiveArticlesReason(reason));
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

  public static TopList<Article, Double> getRankedArticles(
      User user, Scorer scorer, int limit) throws DatabaseSchemaException, BiznessException {
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
    double hoursSincePublished = Math.max(18,
        ((double) System.currentTimeMillis() - getPublishedTime(article))
            / TimeUnit.HOURS.toMillis(1)) - 15;
    return hoursSincePublished;
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
      throws DatabaseSchemaException, BiznessException {
    List<FeatureId> featureIds = Lists.newArrayList();
    List<String> companyNames = Lists.newArrayList();
    List<String> personNames = Lists.newArrayList();
    for (Interest interest : interests) {
      switch (interest.getType()) {
        case ADDRESS_BOOK_CONTACTS:
          for (AddressBookContact contact : user.getAddressBookContactList()) {
            personNames.add(contact.getName());
          }
          break;

        case INDUSTRY:
          featureIds.add(FeatureId.fromId(interest.getIndustryCode()));
          break;

        case LINKED_IN_CONTACTS:
          for (LinkedInContact contact : user.getLinkedInContactList()) {
            personNames.add(contact.getName());
          }
          break;

        case INTENT:
          // INTENT interests are just used for ranking.
          break;

        case ENTITY:
          companyNames.add(interest.getEntity().getKeyword());
          break;

        case UNKNONWN:
          break;
      }
    }

    List<ListenableFuture<Iterable<Article>>> articlesFutures = ImmutableList.of(
        getArticlesForKeywordsFuture(personNames, Article.Reason.PERSON, limitPerType / 4),
        getArticlesForKeywordsFuture(companyNames, Article.Reason.COMPANY, limitPerType),
        getArticlesByFeatureIdFuture(featureIds, limitPerType));
    Map<String, Article> dedupingArticleMap = Maps.newHashMap();
    for (ListenableFuture<Iterable<Article>> articlesFuture : articlesFutures) {
      try {
        for (Article article : articlesFuture.get()) {
          if (!dedupingArticleMap.containsKey(article.getUrlId())) {
            dedupingArticleMap.put(article.getUrlId(), article);
          }
        }
      } catch (InterruptedException | ExecutionException e) {
        throw new BiznessException("Async error: " + e.getMessage(), e);
      }
    }
    return dedupingArticleMap.values();
  }

  /**
   * Gets articles containing a specific entity (person, organization, or place)
   */
  public static Iterable<Article> getArticlesForEntity(Entity entity, int limitPerType) 
      throws DatabaseSchemaException, BiznessException {
    try {
      return Deduper.filterOutDupes(
          getArticlesForKeywordsFuture(
              ImmutableList.of(entity.getKeyword()), Reason.COMPANY, limitPerType).get());
    } catch (InterruptedException | ExecutionException e) {
      throw new BiznessException("Async exception: " + e.getMessage(), e);
    }
  }

  /**
   * Gets a list of articles tailored specifically to the specified
   * industries.
   */
  public static ListenableFuture<Iterable<Article>> getArticlesByFeatureIdFuture(
      Iterable<FeatureId> featureIds, int limit) throws DatabaseSchemaException {
    if (Iterables.isEmpty(featureIds)) {
      Iterable<Article> emptyIterable = Collections.<Article>emptyList();
      return Futures.immediateFuture(emptyIterable);
    }
    return Futures.transform(
        Database.with(Article.class).getFuture(
            new QueryOption.WhereEqualsNumber("feature.feature_id", Iterables.transform(featureIds,
                new Function<FeatureId, Number>() {
              @Override
              public Number apply(FeatureId featureId) {
                return featureId.getId();
              }
            })),
            new QueryOption.DescendingSort("published_time"),
            new QueryOption.Limit(limit)),
        getFunctionToGiveArticlesReason(Reason.INDUSTRY));
  }

  /**
   * Returns a random article.
   */
  public static Article getRandomArticle() throws DatabaseSchemaException {
    return Database.with(Article.class).getFirst(new QueryOption.AscendingSort("rand()"));
  }

  public static Iterable<Article> getArticlesForContacts(
      User user, InterestType contactType, int limit)
          throws DatabaseSchemaException, BiznessException {
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
