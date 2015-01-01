package com.janknspank.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import com.janknspank.proto.Core.Link;

/**
 * Tracks a link from one URL's content to another's.  The primary key is a
 * composite of the origin and destination URL IDs, as defined in the
 * DiscoveredUrl table.
 */
public class Links {
  private static final String DELETE_COMMAND =
      "DELETE FROM " + Database.getTableName(Link.class) +
      "    WHERE origin_id =? OR destination_id =?";

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

  /** Helper method for creating the Link table. */
  public static void main(String args[]) throws Exception {
    Connection connection = Database.getConnection();
    connection.prepareStatement(Database.getCreateTableStatement(Link.class)).execute();
    for (String statement : Database.getCreateIndexesStatement(Link.class)) {
      connection.prepareStatement(statement).execute();
    }
  }
}
