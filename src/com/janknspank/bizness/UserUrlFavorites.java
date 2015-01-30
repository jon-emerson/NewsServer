package com.janknspank.bizness;

import java.util.List;

import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.Core.UserUrlFavorite;

/**
 * Tracks which URLs the current user has pinned or favorited.
 */
public class UserUrlFavorites {
  public static Iterable<UserUrlFavorite> get(String userId) throws DatabaseSchemaException {
    return Database.with(UserUrlFavorite.class).get(new QueryOption.WhereEquals("user_id", userId));
  }

  public static UserUrlFavorite get(String userId, String urlId) throws DatabaseSchemaException {
    return Database.with(UserUrlFavorite.class).getFirst(
        new QueryOption.WhereEquals("user_id", userId),
        new QueryOption.WhereEquals("url_id", urlId));
  }

  /**
   * Deletes the passed URLs from the specified user's favorites.
   */
  public static int deleteIds(String userId, List<String> urlIds) throws DatabaseSchemaException {
    return Database.with(UserUrlFavorite.class).delete(
        new QueryOption.WhereEquals("user_id", userId),
        new QueryOption.WhereEquals("url_id", urlIds));
  }

  /** Helper method for creating the UserUrlFavorite table. */
  public static void main(String args[]) throws Exception {
    Database.with(UserUrlFavorite.class).createTable();
  }
}
