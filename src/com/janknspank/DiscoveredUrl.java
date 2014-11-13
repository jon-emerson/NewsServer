package com.janknspank;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import org.json.JSONObject;

/**
 * Row in the MySQL database: DiscoveredUrl.  Represents a URL we discovered.
 * Basically just tracks that a URL exists and gives it an ID that can be
 * used as a foreign key in other tables.
 */
public class DiscoveredUrl {
  public static final String TABLE_NAME_STR = "DiscoveredUrl";
  public static final String URL_STR = "url";
  public static final String ID_STR = "id";
  public static final String TWEET_COUNT_STR = "tweet_count";
  private static final String DISCOVERY_TIME_STR = "discovery_time";
  private static final String LAST_CRAWL_TIME_STR = "last_crawl_time";

  private static final String CREATE_TABLE_COMMAND =
      "CREATE TABLE " + TABLE_NAME_STR + " ( " +
      "    " + URL_STR + " VARCHAR(767) PRIMARY KEY, " +
      "    " + ID_STR + " VARCHAR(24) NOT NULL, " +
      "    " + TWEET_COUNT_STR + " BOOL NOT NULL DEFAULT FALSE, " +
      "    " + DISCOVERY_TIME_STR + " DATETIME NOT NULL, " +
      "    " + LAST_CRAWL_TIME_STR + " DATETIME )";
  private static final String CREATE_ID_INDEX_COMMAND =
      "CREATE UNIQUE INDEX " + ID_STR + "_index " +
      "    ON " + TABLE_NAME_STR + "(" + ID_STR + ") USING HASH";
  private static final String INSERT_COMMAND =
      "INSERT INTO " + TABLE_NAME_STR + " ( " +
      "    " + URL_STR + ", " +
      "    " + ID_STR + ", " +
      "    " + TWEET_COUNT_STR + ", " +
      "    " + DISCOVERY_TIME_STR + ") VALUES (?, ?, ?, ?)";
  private static final String SELECT_BY_ID_COMMAND =
      "SELECT * FROM " + TABLE_NAME_STR + " WHERE " + URL_STR + " =?";
  private static final String SELECT_NEXT_URL_TO_CRAWL =
      "SELECT * FROM " + TABLE_NAME_STR + " WHERE " + LAST_CRAWL_TIME_STR + " IS NULL " +
      "ORDER BY " + TWEET_COUNT_STR + " DESC, " + DISCOVERY_TIME_STR + " DESC LIMIT 1";
  private static final String UPDATE_LAST_CRAWL_TIME_COMMAND =
      "UPDATE " + TABLE_NAME_STR + " SET " + LAST_CRAWL_TIME_STR + " =? " +
      "WHERE " + ID_STR + " =? AND " + LAST_CRAWL_TIME_STR + " IS NULL";
  private static final String INCREMENT_TWEET_COUNT_COMMAND =
      "UPDATE " + TABLE_NAME_STR + " SET " + TWEET_COUNT_STR + "=" +
      TWEET_COUNT_STR + "+1 WHERE " + ID_STR + "=?";

  private String url;
  private String id;
  private int tweetCount;
  private Date discoveryTime;
  private Date lastCrawlTime;

  public String getUrl() {
    return url;
  }

  public String getId() {
    return id;
  }

  public int getTweetCount() {
    return tweetCount;
  }

  public Date getDiscoveryTime() {
    return discoveryTime;
  }

  public Date getLastCrawlTime() {
    return lastCrawlTime;
  }

  public static class Builder {
    private String url;
    private String id;
    private int tweetCount;
    private Date discoveryTime;
    private Date lastCrawlTime;

    public Builder() {
    }

    public Builder setUrl(String url) {
      this.url = url;
      return this;
    }

    public Builder setId(String id) {
      this.id = id;
      return this;
    }

    public Builder setTweetCount(int tweetCount) {
      this.tweetCount = tweetCount;
      return this;
    }

    public Builder setDiscoveryTime(Date discoveryTime) {
      this.discoveryTime = discoveryTime;
      return this;
    }

    public Builder setLastCrawlTime(Date lastCrawlTime) {
      this.lastCrawlTime = lastCrawlTime;
      return this;
    }

    public DiscoveredUrl build() throws ValidationException {
      DiscoveredUrl discoveredUrl = new DiscoveredUrl();
      discoveredUrl.url = url;
      discoveredUrl.id = id;
      discoveredUrl.tweetCount = tweetCount;
      discoveredUrl.discoveryTime = (discoveryTime == null) ? new Date() : discoveryTime;
      discoveredUrl.lastCrawlTime =
          (lastCrawlTime == null) ? new Date() : lastCrawlTime;
      discoveredUrl.assertValid();
      return discoveredUrl;
    }
  }

  private void assertValid() throws ValidationException {
    Asserts.assertNonEmpty(url, URL_STR);
    Asserts.assertNonEmpty(id, ID_STR);
    Asserts.assertNotNull(discoveryTime, DISCOVERY_TIME_STR);
  }

  public JSONObject toJSONObject() {
    JSONObject o = new JSONObject();
    o.put(URL_STR, url);
    o.put(ID_STR, id);
    o.put(TWEET_COUNT_STR, tweetCount);
    o.put(DISCOVERY_TIME_STR, Constants.DATE_TIME_FORMATTER.format(discoveryTime));
    o.put(LAST_CRAWL_TIME_STR,
        Constants.DATE_TIME_FORMATTER.format(lastCrawlTime));
    return o;
  }

  public static DiscoveredUrl put(String url, boolean isTweet) {
    DiscoveredUrl existing = get(url);
    if (existing != null) {
      if (isTweet) {
        existing.incrementTweetCount();
      }
      return existing;
    }
    try {
      DiscoveredUrl newUrl = new DiscoveredUrl.Builder()
          .setUrl(url)
          .setId(GuidFactory.generate())
          .setTweetCount(isTweet ? 1 : 0)
          .setDiscoveryTime(new Date())
          .build();
      newUrl.insert();
      return newUrl;
    } catch (ValidationException | SQLException e) {
      e.printStackTrace();
    }
    return null;
  }

  private void insert() throws SQLException {
    PreparedStatement statement =
        MysqlHelper.getInstance().prepareStatement(INSERT_COMMAND);
    statement.setString(1, this.url);
    statement.setString(2, this.id);
    statement.setInt(3, this.tweetCount);
    statement.setTimestamp(4, new java.sql.Timestamp(this.discoveryTime.getTime()));
    statement.execute();
  }

  public void incrementTweetCount() {
    try {
      PreparedStatement statement =
          MysqlHelper.getInstance().prepareStatement(INCREMENT_TWEET_COUNT_COMMAND);
      statement.setString(1, this.id);
      statement.executeUpdate();
      this.tweetCount += 1;
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  /**
   * Marks this discovered URL as crawled by updating its last crawl time.
   * Note that this is designed to be thread-safe, but it's not yet fault
   * tolerant: If a crawl fails after markAsCrawled has been called, there's
   * no way to realize it yet.
   */
  public boolean markAsCrawled() {
    try {
      Date now = new Date();
      PreparedStatement statement =
          MysqlHelper.getInstance().prepareStatement(UPDATE_LAST_CRAWL_TIME_COMMAND);
      statement.setTimestamp(1, new java.sql.Timestamp(now.getTime()));
      statement.setString(2, this.id);
      return statement.executeUpdate() == 1;
    } catch (SQLException e) {
      return false;
    }
  }

  public static DiscoveredUrl getNextUrlToCrawl() {
    try {
      Statement stmt = MysqlHelper.getInstance().getStatement();
      return createFromResultSet(stmt.executeQuery(SELECT_NEXT_URL_TO_CRAWL));
    } catch (SQLException e) {
      e.printStackTrace();
      return null;
    }
  }

  private static DiscoveredUrl createFromResultSet(ResultSet result) throws SQLException {
    if (result.next()) {
      DiscoveredUrl discoveredUrl = new DiscoveredUrl();
      discoveredUrl.url = result.getString(URL_STR);
      discoveredUrl.id = result.getString(ID_STR);
      discoveredUrl.tweetCount = result.getInt(TWEET_COUNT_STR);
      discoveredUrl.discoveryTime = result.getDate(DISCOVERY_TIME_STR);
      discoveredUrl.lastCrawlTime = result.getDate(LAST_CRAWL_TIME_STR);
      try {
        discoveredUrl.assertValid();
      } catch (ValidationException e) {
        e.printStackTrace();
        return null;
      }
      return discoveredUrl;
    }
    return null;
  }

  /**
   * Returns the DiscoveredUrl from the database, using the passed-in URL
   * primary key.  If the specified URL doesn't exist, returns null.
   */
  public static DiscoveredUrl get(String url) {
    try {
      PreparedStatement statement =
          MysqlHelper.getInstance().prepareStatement(SELECT_BY_ID_COMMAND);
      statement.setString(1, url);
      return createFromResultSet(statement.executeQuery());

    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }
  
  /** Helper method for creating the discovered-url table. */
  public static void main(String args[]) {
    try {
      Statement statement = MysqlHelper.getInstance().getStatement();
      statement.executeUpdate(CREATE_TABLE_COMMAND);
      statement.executeUpdate(CREATE_ID_INDEX_COMMAND);
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
