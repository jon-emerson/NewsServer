package com.janknspank.bizness;

import java.util.List;

import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.Core.UserUrlRating;

/**
 * Tracks which URLs the current user has rated.
 * Used for training the neural network
 */
public class UserUrlRatings {
  public static Iterable<UserUrlRating> get(String userId) throws DatabaseSchemaException {
    return Database.with(UserUrlRating.class).get(
        new QueryOption.WhereEquals("user_id", userId));
  }

  public static Iterable<UserUrlRating> getAll() throws DatabaseSchemaException {
    return Database.with(UserUrlRating.class).get();
  }

  /**
   * Deletes the passed URLs from the specified user's ratings.
   */
  public static int deleteIds(String userId, List<String> urlIds) throws DatabaseSchemaException {
    return Database.with(UserUrlRating.class).delete(
        new QueryOption.WhereEquals("user_id", userId),
        new QueryOption.WhereEquals("url_id", urlIds));
  }

  /** Helper method for creating the UserUrlRating table. */
  public static void main(String args[]) throws Exception {
    Database.with(UserUrlRating.class).createTable();
  }
}
