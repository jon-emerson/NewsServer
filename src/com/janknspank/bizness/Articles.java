package com.janknspank.bizness;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import com.google.api.client.util.Maps;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.janknspank.bizness.TimeRankingStrategy.EntityStreamStrategy;
import com.janknspank.bizness.TimeRankingStrategy.MainStreamStrategy;
import com.janknspank.classifier.FeatureId;
import com.janknspank.common.TopList;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.Article.Reason;
import com.janknspank.proto.ArticleProto.ArticleOrBuilder;
import com.janknspank.proto.ArticleProto.SocialEngagement;
import com.janknspank.proto.ArticleProto.SocialEngagement.Site;
import com.janknspank.proto.CoreProto.Entity;
import com.janknspank.proto.CoreProto.Entity.Source;
import com.janknspank.proto.CoreProto.EntityIdToIndustryRelevance;
import com.janknspank.proto.UserProto.AddressBookContact;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.Interest.InterestType;
import com.janknspank.proto.UserProto.LinkedInContact;
import com.janknspank.proto.UserProto.User;
import com.janknspank.rank.Deduper;
import com.janknspank.rank.DiversificationPass;
import com.janknspank.rank.NeuralNetworkScorer;
import com.janknspank.rank.Scorer;

/**
 * Helper class that manages storing and retrieving Article objects from the
 * database.
 */
public class Articles {
  /**
   * The number of results to return in user streams.  One more thrown in to
   * support pagination.
   */
  public static final int NUM_RESULTS = 51;

  /**
   * Returns a function that gives an iterable of articles a specific reason for
   * existing, such as being about companies, people, or industries.
   *
   * Yes it's a little redic that Java's async implementations make us do stupid
   * crap like this... :).
   */
  private static AsyncFunction<Iterable<Article>, Iterable<Article>>
      getFunctionToGiveArticlesReason(
          final Article.Reason reason,
          final @Nullable FeatureId industryFeatureId) {
    return new AsyncFunction<Iterable<Article>, Iterable<Article>>() {
      @Override
      public ListenableFuture<Iterable<Article>> apply(Iterable<Article> articles) {
        return Futures.immediateFuture(
            Iterables.transform(articles, new Function<Article, Article>() {
              @Override
              public Article apply(Article article) {
                Article.Builder builder = article.toBuilder();
                builder.setReason(reason);
                if (industryFeatureId != null) {
                  builder.setReasonIndustryCode(industryFeatureId.getId());
                }
                return builder.build();
              }
            }));
      }
    };
  }

  /**
   * Gets articles that contain a set of keywords.
   */
  private static ListenableFuture<Iterable<Article>> getArticlesForKeywordsFuture(
      Iterable<String> keywords, final Article.Reason reason, int limit,
      Set<String> excludeUrlIds, boolean videoOnly)
      throws DatabaseSchemaException {
    return Futures.transform(
        Database.with(Article.class).getFuture(
            new QueryOption.DescendingSort("published_time"),
            new QueryOption.WhereEquals("keyword.keyword", keywords),
            new QueryOption.WhereNotEquals("url_id", excludeUrlIds),
            videoOnly ? new QueryOption.WhereNotNull("video") : null,
            new QueryOption.Limit(limit)),
        getFunctionToGiveArticlesReason(reason, null));
  }

  /**
   * Gets articles that contain a set of entity IDs.
   */
  private static ListenableFuture<Iterable<Article>> getArticlesForEntityIdsFuture(
      Iterable<String> entityIds, final Article.Reason reason, int limit,
      Set<String> excludeUrlIds, boolean videoOnly)
      throws DatabaseSchemaException {
    return Futures.transform(
        Database.with(Article.class).getFuture(
            new QueryOption.DescendingSort("published_time"),
            new QueryOption.WhereEquals("keyword.entity.id", entityIds),
            new QueryOption.WhereNotEquals("url_id", excludeUrlIds),
            videoOnly ? new QueryOption.WhereNotNull("video") : null,
            new QueryOption.Limit(limit)),
        getFunctionToGiveArticlesReason(reason, null));
  }

  /**
   * Returns the primary (main, default, whatever) stream for the given user.
   * These are the articles we should show in the initial stream.
   */
  public static Iterable<Article> getMainStream(User user)
      throws DatabaseSchemaException, BiznessException {
    return getMainStream(user, ImmutableSet.<String>of(), false /* videoOnly */);
  }

  public static Iterable<Article> getMainStream(
      User user, Set<String> excludeUrlIds, boolean videoOnly)
      throws DatabaseSchemaException, BiznessException {
    return Articles.getRankedArticles(
        user,
        NeuralNetworkScorer.getInstance(),
        new MainStreamStrategy(),
        new DiversificationPass.MainStreamPass(),
        NUM_RESULTS,
        excludeUrlIds,
        videoOnly);
  }

  public static Iterable<Article> getStream(
      User user,
      TimeRankingStrategy strategy,
      DiversificationPass diversificationPass,
      Set<String> excludeUrlIds,
      boolean videoOnly)
      throws DatabaseSchemaException, BiznessException {
    return Articles.getRankedArticles(
        user,
        NeuralNetworkScorer.getInstance(),
        strategy,
        diversificationPass,
        NUM_RESULTS,
        excludeUrlIds,
        videoOnly);
  }

  /**
   * Returns the best articles for the current user given the current scorer.
   * NOTE(jonemerson): If you're calling this from a Servlet, be careful!  The
   * default serialization of Articles does not include their keywords.  Instead
   * use {@code ArticleSerializer#serialize(Iterable, User, boolean, boolean)
   * with the results of this method.
   */
  public static Iterable<Article> getRankedArticles(
      User user,
      Scorer scorer,
      TimeRankingStrategy strategy,
      DiversificationPass diversificationPass,
      int limit,
      Set<String> excludeUrlIds,
      boolean videoOnly)
      throws DatabaseSchemaException, BiznessException {
    return getRankedArticles(
        user, scorer, strategy, diversificationPass, limit,
        getArticlesForInterests(
            user, UserInterests.getInterests(user), limit * 3, excludeUrlIds, videoOnly));
  }

  public static Iterable<Article> getEntityStream(
      Entity entity, User user, Set<String> excludeUrlIds, boolean videoOnly)
      throws DatabaseSchemaException, BiznessException {
    // As a "hack" to encourage on-topic results for entity queries, so that
    // Barack Obama gets articles mostly about government, find the top
    // industries per entity and tell the ranker that the user is interested
    // in those industries.
    TopList<Integer, Long> topIndustryCodes = new TopList<>(3);
    for (EntityIdToIndustryRelevance relevance :
        Database.with(EntityIdToIndustryRelevance.class).get(
            new QueryOption.WhereEquals("entity_id", entity.getId()))) {
      topIndustryCodes.add(relevance.getIndustryId(), relevance.getCount());
    }
    List<Interest> topIndustryInterests = Lists.newArrayList();
    for (Integer industryCode : topIndustryCodes) {
      topIndustryInterests.add(Interest.newBuilder()
          .setType(InterestType.INDUSTRY)
          .setIndustryCode(industryCode)
          .build());
    }

    try {
      return getRankedArticles(
          user.toBuilder()
              .clearInterest()
              .addInterest(Interest.newBuilder()
                  .setType(InterestType.ENTITY)
                  .setEntity(entity)
                  .build())
              .addAllInterest(topIndustryInterests)
              .build(),
          NeuralNetworkScorer.getInstance(),
          new TimeRankingStrategy.EntityStreamStrategy(),
          new DiversificationPass.NoOpPass(),
          NUM_RESULTS,
          getArticlesForEntityIdsFuture(
              ImmutableList.of(entity.getId()),
              Reason.COMPANY,
              NUM_RESULTS * 5,
              excludeUrlIds,
              videoOnly).get());
    } catch (InterruptedException | ExecutionException e) {
      throw new BiznessException("Async error: " + e.getMessage(), e);
    }
  }

  /**
   * Helper method for boiling down a set of unranked articles to their best
   * {@code limit} articles, deduped, and prioritized according to the user's
   * interests.  Supports the main stream and the contacts streams.
   */
  private static Iterable<Article> getRankedArticles(
      User user,
      Scorer scorer,
      TimeRankingStrategy strategy,
      DiversificationPass diversificationPass,
      int limit,
      Iterable<Article> unrankedArticles)
      throws DatabaseSchemaException, BiznessException {

    // Find the top 150 articles based on neural network rank + time punishment.
    TopList<Article, Double> goodArticles = new TopList<>(limit * 3);
    Set<String> urls = Sets.newHashSet();
    for (Article article : unrankedArticles) {
      if (urls.contains(article.getUrl())) {
        continue;
      }
      urls.add(article.getUrl());

      double score = scorer.getScore(user, article) * strategy.getTimeRank(article, user);
      goodArticles.add(article.toBuilder().setScore(score).build(), score);
    }

    // Dedupe them - This will knock us down about 10 - 20%.
    List<Article> dedupedArticles = Deduper.filterOutDupes(goodArticles);

    // Keep around only the top 55.  Make decisions based on the score and its
    // social relevance.
    TopList<Article, Double> bestArticles = new TopList<>(limit + 5);
    for (Article article : dedupedArticles) {
      SocialEngagement engagement = SocialEngagements.getForArticle(article, Site.TWITTER);
      double shareScore = (engagement == null) ? 0.5 : engagement.getShareScore();
      bestArticles.add(article, article.getScore() * (1 + shareScore));
    }

    // Now that we know which articles to keep, sort them how we've historically
    // done: Based on score - time punishment only.
    TopList<Article, Double> sortedArticles = new TopList<>(limit + 5);
    for (Article article : bestArticles) {
      sortedArticles.add(article, article.getScore());
    }

    // Distribute them, letting the least-different articles to fall off the
    // bottom.
    return Iterables.limit(diversificationPass.diversify(sortedArticles), limit);
  }

  /**
   * A more intelligent approach to knowing when an article was published: The
   * time we discovered the article, unless the site has self-reported that the
   * article is at least 36 hours older.  This means that sites can't lie and
   * post- or pre-date their publish times... But, if we discover an article
   * well after it was published, we honor its older publish date.
   */
  public static long getPublishedTime(ArticleOrBuilder article) {
    return
        article.hasCrawlTime()
            && article.getCrawlTime() - TimeUnit.HOURS.toMillis(36) > article.getPublishedTime()
        ? article.getPublishedTime()
        : article.getCrawlTime();
  }

  private static List<String> getLinkedInContactNames(User user, Set<String> tombstones) {
    List<String> personNames = Lists.newArrayList();
    for (LinkedInContact contact : user.getLinkedInContactList()) {
      if (!tombstones.contains(contact.getName())) {
        personNames.add(contact.getName());
      }
    }
    return personNames;
  }

  private static List<String> getAddressBookContactNames(User user, Set<String> tombstones) {
    List<String> personNames = Lists.newArrayList();
    for (AddressBookContact contact : user.getAddressBookContactList()) {
      if (!tombstones.contains(contact.getName())) {
        personNames.add(contact.getName());
      }
    }
    return personNames;
  }

  /**
   * Gets a list of articles tailored specifically to the current user's
   * interests.
   */
  public static Iterable<Article> getArticlesForInterests(
      User user, Iterable<Interest> interests, int limitPerType,
      Set<String> excludeUrlIds, boolean videoOnly)
      throws DatabaseSchemaException, BiznessException {
    Set<String> tombstones = UserInterests.getTombstones(user);

    List<FeatureId> featureIds = Lists.newArrayList();
    List<String> entityIds = Lists.newArrayList();
    List<String> companyNames = Lists.newArrayList();
    List<String> personNames = Lists.newArrayList();
    for (Interest interest : interests) {
      switch (interest.getType()) {
        case ADDRESS_BOOK_CONTACTS:
          personNames.addAll(getAddressBookContactNames(user, tombstones));
          break;

        case INDUSTRY:
          FeatureId featureId = FeatureId.fromId(interest.getIndustryCode());
          if (featureId != null) {
            featureIds.add(featureId);
          }
          break;

        case LINKED_IN_CONTACTS:
          personNames.addAll(getLinkedInContactNames(user, tombstones));
          break;

        case ENTITY:
          if (interest.getEntity().hasId() && interest.getEntity().getSource() != Source.USER) {
            entityIds.add(interest.getEntity().getId());
          } else {
            companyNames.add(interest.getEntity().getKeyword());
          }
          break;

        case UNKNONWN:
          break;
      }
    }

    List<ListenableFuture<Iterable<Article>>> articlesFutures = Lists.newArrayList();
    articlesFutures.add(getArticlesForEntityIdsFuture(
        entityIds, Article.Reason.COMPANY, limitPerType, excludeUrlIds, videoOnly));
    articlesFutures.add(getArticlesForKeywordsFuture(
        personNames, Article.Reason.PERSON, limitPerType / 4, excludeUrlIds, videoOnly));
    articlesFutures.add(getArticlesForKeywordsFuture(
        companyNames, Article.Reason.COMPANY, limitPerType, excludeUrlIds, videoOnly));
    for (FeatureId featureId : featureIds) {
      articlesFutures.add(getArticlesByFeatureIdFuture(
          featureId, limitPerType, excludeUrlIds, videoOnly));
    }
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
   * Gets a list of articles tailored specifically to the specified
   * industries.
   */
  public static ListenableFuture<Iterable<Article>> getArticlesByFeatureIdFuture(
      FeatureId featureId, int limit, Set<String> excludeUrlIds, boolean videoOnly)
      throws DatabaseSchemaException {
    return Futures.transform(
        Database.with(Article.class).getFuture(
            new QueryOption.WhereEqualsNumber("feature.feature_id", featureId.getId()),
            new QueryOption.DescendingSort("published_time"),
            new QueryOption.WhereNotEquals("url_id", excludeUrlIds),
            videoOnly ? new QueryOption.WhereNotNull("video") : null,
            new QueryOption.Limit(limit)),
        getFunctionToGiveArticlesReason(Reason.INDUSTRY, featureId));
  }

  public static Iterable<Article> getArticlesForLinkedInContacts(
      User user, Set<String> excludeUrlIds, boolean videoOnly)
      throws DatabaseSchemaException, BiznessException {
    return getArticlesForContacts(user,
        getLinkedInContactNames(user, UserInterests.getTombstones(user)),
        NUM_RESULTS, excludeUrlIds, videoOnly);
  }

  public static Iterable<Article> getArticlesForAddressBookContacts(
      User user, Set<String> excludeUrlIds, boolean videoOnly)
      throws DatabaseSchemaException, BiznessException {
    return getArticlesForContacts(user,
        getAddressBookContactNames(user, UserInterests.getTombstones(user)),
        NUM_RESULTS, excludeUrlIds, videoOnly);
  }

  private static Iterable<Article> getArticlesForContacts(
      User user, List<String> contactNames, int limit, Set<String> excludeUrlIds, boolean videoOnly)
      throws DatabaseSchemaException, BiznessException {
    List<Number> featureIdIds = Lists.newArrayList();
    featureIdIds.addAll(UserInterests.getUserIndustryFeatureIdIds(user));
    return getRankedArticles(user,
        NeuralNetworkScorer.getInstance(),
        new EntityStreamStrategy(),
        new DiversificationPass.MainStreamPass(),
        limit,
        Database.with(Article.class).get(
            new QueryOption.WhereEquals("keyword.keyword", contactNames),
            new QueryOption.WhereEqualsNumber("feature.feature_id", featureIdIds),
            new QueryOption.DescendingSort("published_time"),
            new QueryOption.WhereNotEquals("url_id", excludeUrlIds),
            videoOnly ? new QueryOption.WhereNotNull("video") : null,
            new QueryOption.Limit(limit * 3)));
  }

  /** Helper method for creating the Article table. */
  public static void main(String args[]) throws Exception {
    Database.with(Article.class).createTable();
  }
}
