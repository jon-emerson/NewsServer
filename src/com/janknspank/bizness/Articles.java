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
import com.janknspank.bizness.TimeRankingStrategy.AncillaryStreamStrategy;
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
import com.janknspank.proto.CoreProto.Entity.Source;
import com.janknspank.proto.UserProto.AddressBookContact;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.LinkedInContact;
import com.janknspank.proto.UserProto.User;
import com.janknspank.rank.Deduper;
import com.janknspank.rank.NeuralNetworkScorer;
import com.janknspank.rank.Scorer;

/**
 * Helper class that manages storing and retrieving Article objects from the
 * database.
 */
public class Articles {
  /**
   * The number of results to return in user streams.
   */
  public static final int NUM_RESULTS = 50;

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
  private static ListenableFuture<Iterable<Article>> getArticlesForKeywordsFuture(
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
   * Gets articles that contain a set of entity IDs.
   */
  private static ListenableFuture<Iterable<Article>> getArticlesForEntityIdsFuture(
      Iterable<String> entityIds, final Article.Reason reason, int limit)
      throws DatabaseSchemaException {
    return Futures.transform(
        Database.with(Article.class).getFuture(
            new QueryOption.DescendingSort("published_time"),
            new QueryOption.WhereEquals("keyword.entity.id", entityIds),
            new QueryOption.Limit(limit)),
        getFunctionToGiveArticlesReason(reason));
  }

  /**
   * Returns the primary (main, default, whatever) stream for the given user.
   * These are the articles we should show in the initial stream.
   */
  public static TopList<Article, Double> getMainStream(User user)
      throws DatabaseSchemaException, BiznessException {
    return Articles.getRankedArticles(
        user,
        NeuralNetworkScorer.getInstance(),
        new MainStreamStrategy(),
        NUM_RESULTS);
  }

  public static TopList<Article, Double> getStream(User user, TimeRankingStrategy strategy)
      throws DatabaseSchemaException, BiznessException {
    return Articles.getRankedArticles(
        user,
        NeuralNetworkScorer.getInstance(),
        strategy,
        NUM_RESULTS);
  }

  /**
   * Returns the best articles for the current user given the current scorer.
   * NOTE(jonemerson): If you're calling this from a Servlet, be careful!  The
   * default serialization of Articles does not include their keywords.  Instead
   * use {@code ArticleSerializer#serialize(Iterable, User, boolean, boolean)
   * with the results of this method.
   */
  public static TopList<Article, Double> getRankedArticles(
      User user, Scorer scorer, TimeRankingStrategy strategy, int limit)
      throws DatabaseSchemaException, BiznessException {
    long startTime = System.currentTimeMillis();
    TopList<Article, Double> rankedArticles = getRankedArticles(
        user, scorer, strategy, limit,
        getArticlesForInterests(user, UserInterests.getInterests(user), limit * 10));
    System.out.println("getRankedArticles completed in "
        + (System.currentTimeMillis() - startTime) + "ms");
    return rankedArticles;
  }

  /**
   * Helper method for boiling down a set of unranked articles to their best
   * {@code limit} articles, deduped, and prioritized according to the user's
   * interests.  Supports the main stream and the contacts streams.
   */
  private static TopList<Article, Double> getRankedArticles(
      User user, Scorer scorer,
      TimeRankingStrategy strategy,
      int limit,
      Iterable<Article> unrankedArticles)
      throws DatabaseSchemaException, BiznessException {

    // Find the top 150 articles based on neural network rank + time punishment.
    Map<String, Double> scores = Maps.newHashMap();
    TopList<Article, Double> goodArticles = new TopList<>(limit * 3);
    Set<String> urls = Sets.newHashSet();
    for (Article article : unrankedArticles) {
      if (urls.contains(article.getUrl())) {
        continue;
      }
      urls.add(article.getUrl());

      double score = scorer.getScore(user, article) * strategy.getTimeRank(article, user);
      goodArticles.add(article, score);
      scores.put(article.getUrl(), score);
    }

    // Dedupe them - This will knock us down about 10 - 20%.
    List<Article> dedupedArticles = Deduper.filterOutDupes(goodArticles);

    // Keep around only the top 50.  Make decisions based on the score and its
    // social relevance.
    TopList<Article, Double> bestArticles = new TopList<>(limit);
    for (Article article : dedupedArticles) {
      SocialEngagement engagement = SocialEngagements.getForArticle(article, Site.TWITTER);
      bestArticles.add(article, scores.get(article.getUrl()) * (1 + engagement.getShareScore()));
    }

    // Now that we know which articles to keep, sort them how we've historically
    // done: Based on score - time punishment only.
    TopList<Article, Double> sortedArticles = new TopList<>(limit);
    for (Article article : bestArticles) {
      sortedArticles.add(article, scores.get(article.getUrl()));
    }
    return sortedArticles;
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
      User user, Iterable<Interest> interests, int limitPerType)
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

    List<ListenableFuture<Iterable<Article>>> articlesFutures = ImmutableList.of(
        getArticlesForEntityIdsFuture(entityIds, Article.Reason.COMPANY, limitPerType),
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

  public static Iterable<Article> getArticlesForLinkedInContacts(
      User user, int limit) throws DatabaseSchemaException, BiznessException {
    return getArticlesForContacts(user,
        getLinkedInContactNames(user, UserInterests.getTombstones(user)), limit);
  }

  public static Iterable<Article> getArticlesForAddressBookContacts(
      User user, int limit) throws DatabaseSchemaException, BiznessException {
    return getArticlesForContacts(user,
        getAddressBookContactNames(user, UserInterests.getTombstones(user)), limit);
  }

  private static Iterable<Article> getArticlesForContacts(
      User user, List<String> contactNames, int limit) throws DatabaseSchemaException, BiznessException {
    List<Number> featureIdIds = Lists.newArrayList();
    featureIdIds.addAll(UserInterests.getUserIndustryFeatureIdIds(user));
    return getRankedArticles(user,
        NeuralNetworkScorer.getInstance(),
        new AncillaryStreamStrategy(),
        limit,
        Database.with(Article.class).get(
            new QueryOption.WhereEquals("keyword.keyword", contactNames),
            new QueryOption.WhereEqualsNumber("feature.feature_id", featureIdIds),
            new QueryOption.DescendingSort("published_time"),
            new QueryOption.Limit(limit * 3)));
  }

  /** Helper method for creating the Article table. */
  public static void main(String args[]) throws Exception {
    Database.with(Article.class).createTable();
  }
}
