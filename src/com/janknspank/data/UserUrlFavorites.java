package com.janknspank.data;

import java.util.List;

import com.janknspank.proto.Core.UserUrlFavorite;

/**
 * Tracks which URLs the current user has pinned or favorited.
 */
public class UserUrlFavorites {
  public static List<UserUrlFavorite> get(String userId) throws DataInternalException {
    return Database.with(UserUrlFavorite.class).get(
        new QueryOption.WhereEquals("user_id", userId));
  }

  public static UserUrlFavorite get(String userId, String urlId) throws DataInternalException {
    return Database.with(UserUrlFavorite.class).getFirst(
        new QueryOption.WhereEquals("user_id", userId),
        new QueryOption.WhereEquals("url_id", urlId));
  }

  /**
   * Deletes the passed URLs from the specified user's favorites.
   */
  public static int deleteIds(String userId, List<String> urlIds) throws DataInternalException {
    return Database.with(UserUrlFavorite.class).delete(
        new QueryOption.WhereEquals("user_id", userId),
        new QueryOption.WhereEquals("url_id", urlIds));
  }

  /** Helper method for creating the UserUrlFavorite table. */
  public static void main(String args[]) throws Exception {
    Database.with(UserUrlFavorite.class).createTable();
  }
}
