package com.janknspank.data;

import java.util.List;

import com.janknspank.proto.Core.UserUrlRating;

/**
 * Tracks which URLs the current user has pinned or favorited.
 */
public class UserUrlRatings {
  public static List<UserUrlRating> get(String userId) throws DataInternalException {
    return Database.with(UserUrlRating.class).get(
        new QueryOption.WhereEquals("user_id", userId));
  }

  /**
   * Deletes the passed URLs from the specified user's favorites.
   */
  public static int deleteIds(String userId, List<String> urlIds) throws DataInternalException {
    return Database.with(UserUrlRating.class).delete(
        new QueryOption.WhereEquals("user_id", userId),
        new QueryOption.WhereEquals("url_id", urlIds));
  }

  /** Helper method for creating the UserUrlRating table. */
  public static void main(String args[]) throws Exception {
    Database.with(UserUrlRating.class).createTable();
  }
}
