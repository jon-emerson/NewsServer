package com.janknspank.data;

import java.util.List;

import com.janknspank.proto.Core.UserUrlRating;

/**
 * Tracks which URLs the current user has rated.
 * Used for training the neural network
 */
public class UserUrlRatings {
  public static List<UserUrlRating> get(String userId) throws DataInternalException {
    return Database.getInstance().get(UserUrlRating.class,
        new QueryOption.WhereEquals("user_id", userId));
  }
  
  public static List<UserUrlRating> getAll() throws DataInternalException {
    return Database.getInstance().get(UserUrlRating.class);
  }

  /**
   * Deletes the passed URLs from the specified user's ratings.
   */
  public static int deleteIds(String userId, List<String> urlIds) throws DataInternalException {
    return Database.getInstance().delete(UserUrlRating.class,
        new QueryOption.WhereEquals("user_id", userId),
        new QueryOption.WhereEquals("url_id", urlIds));
  }

  /** Helper method for creating the UserUrlRating table. */
  public static void main(String args[]) throws Exception {
    Database.getInstance().createTable(UserUrlRating.class);
  }
}
