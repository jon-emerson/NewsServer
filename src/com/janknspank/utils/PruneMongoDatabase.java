package com.janknspank.utils;

import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.MongoConnection;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.CoreProto.Url;

/**
 * Reduces the size of our Mongo DB database so that we can stay within our
 * quota.
 *
 * Steps performed:
 * - Keep Article count under 30,000 by removing the oldest articles.  (This
 *     keeps the Article collection at around 280 megabytes.)
 * - Repair the database to reclaim the space we created.
 *
 * URLs are NOT pruned - We need them so that we know which articles we've seen
 * before.  And they're really not that big.  But we do update the Urls table
 * to remove crawl data from any articles we've pruned above.
 */
public class PruneMongoDatabase {
  private static final long MAX_ARTICLE_COUNT = 30000;

  /**
   * Deletes any passed URLs that do not have Articles associated with them in
   * the database.
   */
  private static int fixUrls(List<Url> urls)
      throws DatabaseSchemaException, DatabaseRequestException {
    final Set<String> existingArticleUrlIds = Sets.newHashSet();
    for (Article article : Database.with(Article.class).get(
        new QueryOption.WhereEquals("url",
            Iterables.transform(urls, new Function<Url, String>() {
              @Override
              public String apply(Url url) {
                return url.getUrl();
              }
            })))) {
      existingArticleUrlIds.add(article.getUrlId());
    }
    List<Url> urlsToUpdate = Lists.newArrayList();
    for (Url url : urls) {
      if (!existingArticleUrlIds.contains(url.getId())
          && (url.hasLastCrawlStartTime() || url.hasLastCrawlFinishTime())) {
        urlsToUpdate.add(url.toBuilder()
            .clearLastCrawlFinishTime()
            .clearLastCrawlStartTime()
            .build());
      }
    }
    Database.update(urlsToUpdate);
    return urlsToUpdate.size();
  }

  /**
   * Updates any URLs that don't have Articles associated with them to not have
   * crawl data, so that they may someday be crawled again.
   */
  private static void fixUrls() throws DatabaseSchemaException, DatabaseRequestException {
    long startTime = System.currentTimeMillis();
    System.out.println("Retrieving URLs ...");

    Iterable<Url> urls = Database.with(Url.class).get();
    System.out.println("Received " + Iterables.size(urls) + " URLs to evaluate in "
        + (System.currentTimeMillis() - startTime) + "ms");

    System.out.print("Fixing ..");
    List<Url> urlsToCheck = Lists.newArrayList();
    int numDeleted = 0;
    for (Url url : urls) {
      urlsToCheck.add(url);
      if (urlsToCheck.size() > 250) {
        numDeleted += fixUrls(urlsToCheck);
        urlsToCheck.clear();
        System.out.print(".");
      }
    }
    numDeleted += fixUrls(urlsToCheck);
    System.out.println(numDeleted + " URLs fixed in "
        + (System.currentTimeMillis() - startTime) + "ms!");
  }

  /**
   * Gets the # of articles in the system reasonably close to MAX_ARTICLE_COUNT
   * by deleting the oldest articles by published_time.
   */
  private static void pruneArticles() throws DatabaseSchemaException {
    long startTime = System.currentTimeMillis();
    System.out.println("Pruning articles to about " + MAX_ARTICLE_COUNT + " ...");

    // Mongo DB doesn't have a "delete with limit" concept.  So, instead,
    // find the 30,000th oldest article in the system, then delete everything
    // older than it.  Complicating this calculation, there's also a set of
    // articles we retain for training the system (vectors, neural network) -
    // so make sure we don't delete those.
    long numArticlesToRetain =
        Database.with(Article.class).getSize(new QueryOption.WhereTrue("retain"));
    Article oldestArticle = Database.with(Article.class).getFirst(
        new QueryOption.DescendingSort("published_time"),
        new QueryOption.LimitWithOffset(1, (int) (MAX_ARTICLE_COUNT - numArticlesToRetain)));
    if (oldestArticle == null) {
      System.out.println("We're already within the limits for # of articles allowed, "
          + "deleting no articles!");
      return;
    }

    int count = Database.with(Article.class).delete(
        new QueryOption.WhereLessThan("published_time", oldestArticle.getPublishedTime()),
        new QueryOption.WhereFalse("retain"));
    System.out.println("Deleted " + count + " older articles in "
        + (System.currentTimeMillis() - startTime) + "ms.  Article count: "
        + Database.with(Article.class).getSize());
  }

  private static void repairDatabase() throws DatabaseSchemaException {
    long startTime = System.currentTimeMillis();
    System.out.println("Repairing database to free up quota from deleted items ...");
    MongoConnection.repairDatabase();
    System.out.println("Database repaired in "
        + (System.currentTimeMillis() - startTime) + "ms");
  }

  public static void main(String args[]) throws DatabaseSchemaException, DatabaseRequestException {
    pruneArticles();
    fixUrls();
    repairDatabase();
    System.out.println("Database pruned successfully.");
  }
}