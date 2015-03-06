package com.janknspank.bizness;

import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.UserProto.User;

/**
 * Tracks a link from one URL's content to another's.
 */
public class Users {
  public static User getByEmail(String email) throws DatabaseSchemaException {
    return Database.with(User.class).getFirst(new QueryOption.WhereEquals("email", email));
  }

  public static Iterable<User> getByEmails(Iterable<String> emails) 
      throws DatabaseSchemaException {
    return Database.with(User.class).get(new QueryOption.WhereEquals("email", emails));
  }

  public static User getByUserId(String userId) throws DatabaseSchemaException {
    return Database.with(User.class).get(userId);
  }
  
  public static Iterable<User> getByUserIds(Iterable<String> userIds) 
      throws DatabaseSchemaException {
    return Database.with(User.class).get(userIds);
  }

  /** Helper method for creating the User table. */
  public static void main(String args[]) throws Exception {
    Database.with(User.class).createTable();
  }
}
