package com.janknspank.data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.protobuf.Message;
import com.janknspank.ArticleUrlDetector;
import com.janknspank.DateHelper;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.Url;
import com.janknspank.proto.Validator;

/**
 * Row in the MySQL database: Url.  Represents a URL we discovered.
 * Basically just tracks that a URL exists and gives it an ID that can be
 * used as a foreign key in other tables.
 */
public class Urls {
  private static final String SELECT_NEXT_URL_TO_CRAWL =
      "SELECT * FROM " + Database.getTableName(Url.class) + " " +
      "WHERE crawl_priority > 0 AND " +
      "NOT url LIKE \"https://twitter.com/%\" " +
      "ORDER BY crawl_priority DESC LIMIT 1";
  private static final String UPDATE_CRAWL_PRIORITY_COMMAND =
      "UPDATE " + Database.getTableName(Url.class) + " " +
      "SET crawl_priority=0, proto=? " +
      "WHERE id=? AND crawl_priority > 0";

  /**
   * Returns the crawl priority for the URL, assuming that we don't know
   * anything about it yet (e.g. it hasn't been discovered before and we don't
   * know about any tweets).
   * 
   * New articles are given precedence over older articles.  Non-articles are
   * given very low priority.
   * 
   * TODO(jonemerson): For news sites that use incrementing article numbers,
   * try to generate a priority based on them.
   * @param url the url to get the crawl priority of
   * @param millis the date the article was published, in milliseconds, if known
   */
  public static int getCrawlPriority(String url, Long millis) {
    if (ArticleUrlDetector.isArticle(url)) {
      millis = (millis == null) ?
          DateHelper.getDateFromUrl(url, true /* allowMonth */) : millis;
      if (millis != null) {
        long millisAgo = System.currentTimeMillis() - millis;
        return (int) Math.max(100, 2000 - (millisAgo / (1000 * 60 * 60)));
      }
      return 100;
    }
    return url.contains("//twitter.com/") ? 0 : 10;
  }

  /**
   * Makes sure the passed-in URL is stored in our database.  If it already
   * exists, its tweet count is incremented accordingly, and its crawl priority
   * is increased.
   */
  public static Url put(String url, boolean isTweet) throws DataInternalException {
    Url existing = Database.get(url, Url.class);
    if (existing != null) {
      if (isTweet) {
        Url.Builder updatedUrl = existing.toBuilder();
        updatedUrl.setTweetCount(existing.getTweetCount() + 1);
        if (!existing.hasLastCrawlTime() && existing.getTweetCount() < 500) {
          updatedUrl.setCrawlPriority(existing.getCrawlPriority() + 10);
        }
        try {
          Database.update(updatedUrl.build());
        } catch (ValidationException e) {
          throw new DataInternalException("Could not update discovered URL", e);
        }
      }
      return existing;
    }

    try {
      Url newUrl = Url.newBuilder()
          .setUrl(url)
          .setId(GuidFactory.generate())
          .setTweetCount(isTweet ? 1 : 0)
          .setDiscoveryTime(System.currentTimeMillis())
          .setCrawlPriority(getCrawlPriority(url, null))
          .build();
      Database.insert(newUrl);
      return newUrl;
    } catch (ValidationException e) {
      throw new DataInternalException("Could not insert new discovered URL", e);
    }
  }

  /**
   * Marks this discovered URL as crawled by updating its last crawl time.
   * Note that this is designed to be thread-safe, but it's not yet fault
   * tolerant: If a crawl fails after markAsCrawled has been called, there's
   * no way to realize it yet.
   * @return Url object with an updated last crawl time, or null, if the
   *     given Url has already been crawled by another thread
   */
  public static Url markAsCrawled(Url url) throws DataInternalException {
    try {
      Url discoveredUrl = url.toBuilder()
          .setLastCrawlTime(System.currentTimeMillis())
          .build();
      Validator.assertValid(discoveredUrl);
      PreparedStatement statement =
          Database.getConnection().prepareStatement(UPDATE_CRAWL_PRIORITY_COMMAND);
      statement.setBytes(1, discoveredUrl.toByteArray());
      statement.setString(2, discoveredUrl.getId());
      return (statement.executeUpdate() == 1) ? discoveredUrl : null;
    } catch (SQLException|ValidationException e) {
      throw new DataInternalException("Could not mark URL as crawled", e);
    }
  }

  public static Url getNextUrlToCrawl() throws DataInternalException {
    try {
      Statement stmt = Database.getConnection().createStatement();
      return Database.createFromResultSet(stmt.executeQuery(SELECT_NEXT_URL_TO_CRAWL),
          Url.class);
    } catch (SQLException e) {
      throw new DataInternalException("Could not read next URL to crawl", e);
    }
  }

  /** Helper method for creating the discovered-url table. */
  public static void main(String args[]) throws Exception {
//    Connection connection = Database.getConnection();
//    connection.prepareStatement(Database.getCreateTableStatement(Url.class)).execute();
//    for (String statement : Database.getCreateIndexesStatement(Url.class)) {
//      connection.prepareStatement(statement).execute();
//    }

    // Figure out what articles we've crawled already.
    Set<String> crawledArticleIds = Sets.newHashSet();
    PreparedStatement stmt = Database.getConnection().prepareStatement(
        "SELECT * FROM " + Database.getTableName(Article.class));
    ResultSet result = stmt.executeQuery();
    while (!result.isAfterLast()) {
      Article article = Database.createFromResultSet(result, Article.class);
      if (article != null) {
        crawledArticleIds.add(article.getUrlId());
      }
    }

    stmt = Database.getConnection().prepareStatement(
        "SELECT * FROM " + Database.getTableName(Url.class)
         + " WHERE url > \""
         + "http://www.bloomberg.com/news/2014-11-27/hohn-s-wife-to-receive-530-million-in-london-divorce-case.html\"");
    result = stmt.executeQuery();
    List<Message> urlsToUpdate = Lists.newArrayList();
    while (!result.isAfterLast()) {
      Url url = Database.createFromResultSet(result, Url.class);
      if (url != null) {
        int crawlPriority = crawledArticleIds.contains(url.getId()) ?
            0 : getCrawlPriority(url.getUrl(), null);
        if (Math.abs(url.getCrawlPriority() - crawlPriority) > 5) {
          System.out.println("pri=" + crawlPriority + " for " + url.getUrl());
          urlsToUpdate.add(url.toBuilder()
              .setCrawlPriority(crawlPriority)
              .build());
        }
      }
      if (urlsToUpdate.size() == 100 || url == null) {
        System.out.println(Database.update(urlsToUpdate) + " rows updated");
        urlsToUpdate.clear();
      }
    }
  }
}
