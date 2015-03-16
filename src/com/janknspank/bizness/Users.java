package com.janknspank.bizness;

import java.util.Set;

import com.google.common.collect.Sets;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.User;
import com.janknspank.proto.UserProto.Interest.InterestType;

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

  /**
   * Returns a lower-cased Set of all the strings for entities the user's
   * following.
   */
  public static Set<String> getUserKeywordSet(User user) {
    Set<String> userKeywordSet = Sets.newHashSet();
    for (Interest interest : user.getInterestList()) {
      if (interest.getType() == InterestType.ENTITY) {
        userKeywordSet.add(interest.getEntity().getKeyword().toLowerCase());
      }
    }
    return userKeywordSet;
  }

  /** Helper method for creating the User table. */
  public static void main(String args[]) throws Exception {
    Database.with(User.class).createTable();
  }
}
