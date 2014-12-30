package com.janknspank.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

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
      "WHERE last_crawl_time IS NULL AND " +
      "NOT url LIKE \"https://twitter.com/%\" " +
      "ORDER BY timestamp DESC LIMIT 1";
  private static final String UPDATE_LAST_CRAWL_TIME_COMMAND =
      "UPDATE " + Database.getTableName(Url.class) + " " +
      "SET last_crawl_time=?, proto=? " +
      "WHERE id=? AND last_crawl_time IS NULL";

  public static Url put(String url, boolean isTweet) throws DataInternalException {
    Url existing = Database.get(url, Url.class);
    if (existing != null) {
      if (isTweet) {
        existing = existing.toBuilder()
            .setTweetCount(existing.getTweetCount() + 1)
            .build();
        try {
          Database.update(existing);
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
          Database.getConnection().prepareStatement(UPDATE_LAST_CRAWL_TIME_COMMAND);
      statement.setLong(1, discoveredUrl.getLastCrawlTime());
      statement.setBytes(2, discoveredUrl.toByteArray());
      statement.setString(3, discoveredUrl.getId());
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
}
