package com.janknspank.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.janknspank.proto.Core.Link;
import com.janknspank.proto.Core.Url;

/**
 * Tracks a link from one URL's content to another's.  The primary key is a
 * composite of the origin and destination URL IDs, as defined in the
 * DiscoveredUrl table.
 */
public class Links {
  private static final String DELETE_COMMAND =
      "DELETE FROM " + Database.getTableName(Link.class)
      + "    WHERE origin_url_id=? OR destination_url_id=?";
  private static final String DELETE_BY_ORIGIN_URL_ID_COMMAND =
      "DELETE FROM " + Database.getTableName(Link.class) + " WHERE origin_url_id=?";

  /**
   * Records that there's a link from {@code sourceUrl} to each of the passed
   * {@code destinationUrls}.
   */
  public static void put(final Url sourceUrl, Iterable<Url> destinationUrls)
     throws DataInternalException{
    try {
      Database.insert(Iterables.transform(destinationUrls,
          new Function<Url, Link>() {
            @Override
            public Link apply(Url destinationUrl) {
              return Link.newBuilder()
                  .setOriginUrlId(sourceUrl.getId())
                  .setDestinationUrlId(destinationUrl.getId())
                  .setDiscoveryTime(destinationUrl.getDiscoveryTime())
                  .setLastFoundTime(destinationUrl.getDiscoveryTime())
                  .build();
            }
          }));
    } catch (ValidationException e) {
      throw new DataInternalException("Could not create link: " + e.getMessage(), e);
    }
  }

  /**
   * Deletes any links coming to or from the passed discovered URL ID.
   */
  public static int deleteIds(List<String> ids) throws DataInternalException {
    try {
      PreparedStatement statement =
          Database.getConnection().prepareStatement(DELETE_COMMAND);
      for (int i = 0; i < ids.size(); i++) {
        statement.setString(1, ids.get(i));
        statement.setString(2, ids.get(i));
        statement.addBatch();
      }
      return Database.sumIntArray(statement.executeBatch());

    } catch (SQLException e) {
      throw new DataInternalException("Could not delete links: " + e.getMessage(), e);
    }
  }

  /**
   * Deletes all recorded links from the passed URLs.  Useful for cleaning
   * up old interpreted link data before a new interpretation / crawl.
   */
  public static int deleteFromOriginUrlId(Iterable<String> urlIds) throws DataInternalException {
    try {
      PreparedStatement statement =
          Database.getConnection().prepareStatement(DELETE_BY_ORIGIN_URL_ID_COMMAND);
      for (String urlId : urlIds) {
        statement.setString(1, urlId);
        statement.addBatch();
      }
      return Database.sumIntArray(statement.executeBatch());
    } catch (SQLException e) {
      throw new DataInternalException("Could not delete links: " + e.getMessage(), e);
    }
  }

  /** Helper method for creating the Link table. */
  public static void main(String args[]) throws Exception {
    Connection connection = Database.getConnection();
    connection.prepareStatement(Database.getCreateTableStatement(Link.class)).execute();
    for (String statement : Database.getCreateIndexesStatement(Link.class)) {
      connection.prepareStatement(statement).execute();
    }
  }
}
