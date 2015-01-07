package com.janknspank.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.janknspank.common.ArticleUrlDetector;
import com.janknspank.common.DateParser;
import com.janknspank.proto.Core.Url;
import com.janknspank.proto.Validator;

/**
 * Row in the MySQL database: Url.  Represents a URL we discovered.
 * Basically just tracks that a URL exists and gives it an ID that can be
 * used as a foreign key in other tables.
 */
public class Urls {
  private static final String SELECT_BY_ID_COMMAND =
      "SELECT * FROM " + Database.getTableName(Url.class) + " WHERE id=?";
  private static final String SELECT_NEXT_URL_TO_CRAWL =
      "SELECT * FROM " + Database.getTableName(Url.class) + " "
      + "WHERE crawl_priority > 0 AND "
      + "NOT url LIKE \"https://twitter.com/%\" "
      + "ORDER BY crawl_priority DESC LIMIT 1";
  private static final String UPDATE_CRAWL_PRIORITY_COMMAND =
      "UPDATE " + Database.getTableName(Url.class) + " "
      + "SET crawl_priority=0, proto=? "
      + "WHERE id=? AND crawl_priority > 0";

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
      millis = (millis == null)
          ? DateParser.parseDateFromUrl(url, true /* allowMonth */) : millis;
      if (millis != null) {
        long millisAgo = System.currentTimeMillis() - millis;
        return (int) Math.max(100, 2000 - (millisAgo / (1000 * 60 * 60)));
      }
      return 100;
    }
    return url.contains("//twitter.com/") ? 0 : 10;
  }

  private static Url create(String url, boolean isTweet) {
    return Url.newBuilder()
        .setUrl(url)
        .setId(GuidFactory.generate())
        .setTweetCount(isTweet ? 1 : 0)
        .setDiscoveryTime(System.currentTimeMillis())
        .setCrawlPriority(getCrawlPriority(url, null))
        .build();
  }

  /**
   * Makes sure the passed-in URL is stored in our database.  If it exists
   * already, its tweet_count and priority will be updated accordingly.
   * The returned Url is either an updated version of the existing field or
   * a new Url, as necessary.
   */
  public static Url put(String urlString, boolean isTweet)
      throws DataInternalException {
    return Iterables.getFirst(put(ImmutableList.of(urlString), isTweet), null);
  }

  /**
   * Makes sure the passed-in URLs are stored in our database.  Existing URLs
   * have their tweet_count and priority updated accordingly, if isTweet is
   * true.  The return Url objects are in the same order as the URL strings.
   */
  public static Collection<Url> put(Iterable<String> urlStrings, boolean isTweet)
      throws DataInternalException {
    // Create a LinkedHashMap for the return values.  (Doing this now preseves
    // the order of our return values, so they match the order of urlStrings.
    LinkedHashMap<String, Url> urls = Maps.newLinkedHashMap();
    for (String urlString : urlStrings) {
      urls.put(urlString, null);
    }

    List<Url> urlsToUpdate = Lists.newArrayList(); // To increment tweet_count.
    for (Url existingUrl : Database.get(urlStrings, Url.class)) {
      urls.put(existingUrl.getUrl(), existingUrl);

      if (isTweet) {
        Url.Builder updatedUrlBuilder = existingUrl.toBuilder();
        updatedUrlBuilder.setTweetCount(existingUrl.getTweetCount() + 1);
        if (!existingUrl.hasLastCrawlTime() && existingUrl.getTweetCount() < 500) {
          updatedUrlBuilder.setCrawlPriority(existingUrl.getCrawlPriority() + 10);
        }
        urlsToUpdate.add(updatedUrlBuilder.build());
      }
    }

    List<Url> urlsToCreate = Lists.newArrayList();
    for (String urlString : urlStrings) {
      if (urls.get(urlString) == null) {
        Url newUrl = create(urlString, isTweet);
        urlsToCreate.add(newUrl);
        urls.put(urlString, newUrl);
      }
    }

    try {
      Database.insert(urlsToCreate);
      Database.update(urlsToUpdate);
      return urls.values();
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
    Connection connection = Database.getConnection();
    connection.prepareStatement(Database.getCreateTableStatement(Url.class)).execute();
    for (String statement : Database.getCreateIndexesStatement(Url.class)) {
      connection.prepareStatement(statement).execute();
    }
  }

  /**
   * Gets a URL by its ID (since the primary keys for URLs are the URLs
   * themselves).
   */
  public static Url getById(String email) throws DataInternalException {
    try {
      PreparedStatement statement =
          Database.getConnection().prepareStatement(SELECT_BY_ID_COMMAND);
      statement.setString(1, email);
      return Database.createFromResultSet(statement.executeQuery(), Url.class);

    } catch (SQLException e) {
      throw new DataInternalException("Could not select url: " + e.getMessage(), e);
    }
  }
}
