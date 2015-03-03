package com.janknspank.utils;

import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.janknspank.database.Database;
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
 * - Remove all URLs that don't have an Article associated with them.  (URLs
 *     are cheap and easy to find... If we have a URL we haven't crawled, and
 *     it's actually relevant, we'll find it again.)
 * - Keep Article count under 25,000 by removing the oldest articles.  (This
 *     keeps the Article collection at around 250 megabytes.)
 * - Repair the database to reclaim the space we created.
 */
public class PruneMongoDatabase {
  private static final long MAX_ARTICLE_COUNT = 25000;

  /**
   * Deletes any passed URLs that do not have Articles associated with them in
   * the database.
   */
  private static void cleanUrls(List<Url> urls) throws DatabaseSchemaException {
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
    Database.delete(Iterables.filter(urls, new Predicate<Url>() {
      @Override
      public boolean apply(Url url) {
        return !existingArticleUrlIds.contains(url.getId());
      }
    }));
  }

  private static void cleanUrls() throws DatabaseSchemaException {
    Iterable<Url> urls = Database.with(Url.class).get();
    List<Url> urlsToCheck = Lists.newArrayList();
    System.out.print("Cleaning URLs ..");
    for (Url url : urls) {
      urlsToCheck.add(url);
      if (urlsToCheck.size() > 250) {
        cleanUrls(urlsToCheck);
        urlsToCheck.clear();
        System.out.print(".");
      }
    }
    cleanUrls(urlsToCheck);
    System.out.println("done!");
  }

  /**
   * Gets the # of articles in the system reasonably close to MAX_ARTICLE_COUNT
   * by deleting the oldest articles by published_time.
   */
  private static void pruneArticles() throws DatabaseSchemaException {
    // Mongo DB doesn't have a "delete with limit" concept.  So, instead,
    // find the 25,000th oldest article in the system, then delete everything
    // older than it.
    Article oldestArticle = Database.with(Article.class).getFirst(
        new QueryOption.DescendingSort("published_time"),
        new QueryOption.LimitWithOffset(1, (int) MAX_ARTICLE_COUNT));
    if (oldestArticle == null) {
      System.out.println("We're already within the limits for # of articles allowed, "
          + "deleting no articles!");
      return;
    }

    int count = Database.with(Article.class).delete(
        new QueryOption.WhereLessThan("published_time", oldestArticle.getPublishedTime()));
    System.out.println("Deleted " + count + " older articles");
  }

  public static void main(String args[]) throws DatabaseSchemaException {
    pruneArticles();
    cleanUrls();
    MongoConnection.repairDatabase();
    System.out.println("Database pruned successfully.");
  }
}