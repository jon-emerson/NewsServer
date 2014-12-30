package com.janknspank.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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
  public static void deleteId(String id) {
    try {
      PreparedStatement statement =
          Database.getConnection().prepareStatement(DELETE_COMMAND);
      statement.setString(1, id);
      statement.setString(2, id);
      statement.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
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
