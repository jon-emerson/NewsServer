package com.janknspank.data;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import com.janknspank.proto.Core.UserUrlFavorite;

/**
 * Tracks which URLs the current user has pinned or favorited.
 */
public class UserUrlFavorites {
  private static final String SELECT_FOR_USER_COMMAND =
      "SELECT * FROM " + Database.getTableName(UserUrlFavorite.class) + " "
      + "WHERE user_id=?";
  private static final String SELECT_COMMAND =
      "SELECT * FROM " + Database.getTableName(UserUrlFavorite.class) + " "
      + "WHERE user_id=? AND url_id=?";
  private static final String DELETE_COMMAND =
      "DELETE FROM " + Database.getTableName(UserUrlFavorite.class) + " "
      + "WHERE user_id=? AND url_id=?";

  public static List<UserUrlFavorite> get(String userId) throws DataInternalException {
    try {
      PreparedStatement stmt = Database.getInstance().prepareStatement(SELECT_FOR_USER_COMMAND);
      stmt.setString(1, userId);
      return Database.createListFromResultSet(stmt.executeQuery(), UserUrlFavorite.class);
    } catch (SQLException e) {
      throw new DataInternalException("Error fetching favorites", e);
    }
  }

  public static UserUrlFavorite get(String userId, String urlId) throws DataInternalException {
    try {
      PreparedStatement stmt = Database.getInstance().prepareStatement(SELECT_COMMAND);
      stmt.setString(1, userId);
      stmt.setString(2, urlId);
      return Database.createFromResultSet(stmt.executeQuery(), UserUrlFavorite.class);
    } catch (SQLException e) {
      throw new DataInternalException("Error fetching favorite", e);
    }
  }

  /**
   * Deletes the passed URLs from the specified user's favorites.
   */
  public static int deleteIds(String userId, List<String> urlIds) throws DataInternalException {
    try {
      PreparedStatement statement =
          Database.getInstance().prepareStatement(DELETE_COMMAND);
      for (int i = 0; i < urlIds.size(); i++) {
        statement.setString(1, userId);
        statement.setString(2, urlIds.get(i));
        statement.addBatch();
      }
      return Database.sumIntArray(statement.executeBatch());

    } catch (SQLException e) {
      throw new DataInternalException("Could not delete favorites: " + e.getMessage(), e);
    }
  }

  /** Helper method for creating the UserUrlFavorite table. */
  public static void main(String args[]) throws Exception {
    Database database = Database.getInstance();
    database.prepareStatement(database.getCreateTableStatement(UserUrlFavorite.class)).execute();
    for (String statement : database.getCreateIndexesStatement(UserUrlFavorite.class)) {
      database.prepareStatement(statement).execute();
    }
  }
}
