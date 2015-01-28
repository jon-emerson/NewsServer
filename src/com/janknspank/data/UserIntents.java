package com.janknspank.data;

import com.janknspank.proto.Core.UserIntent;

public class UserIntents {
  public static Iterable<UserIntent> getIntents(String userId) 
      throws DataInternalException {
    return Database.with(UserIntent.class).get(
        new QueryOption.WhereEquals("user_id", userId));
  }
  
  /** Helper method for creating the UserIntents table. */
  public static void main(String args[]) throws Exception {
    Database.with(UserIntent.class).createTable();
  }
}