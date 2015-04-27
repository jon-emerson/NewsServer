package com.janknspank.bizness;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Futures;
import com.janknspank.common.TopList;
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

  /**
   * Asynchronously updates the record of the last 5 times the user's used the
   * app, which we use for notifications and ranking purposes.
   */
  public static Future<User> updateLast5AppUseTimes(User user) throws DatabaseSchemaException {
    TopList<Long, Long> top = new TopList<>(5);
    for (long time : user.getLast5AppUseTimeList()) {
      top.add(time, time);
    }
    long now = System.currentTimeMillis();
    if ((long) Iterables.getFirst(top, new Long(0)) < (now - TimeUnit.HOURS.toMillis(1))) {
      top.add(now, now);
      return Database.with(User.class).setFuture(user, "last_5_app_use_time", top);
    } else {
      return Futures.immediateFuture(user);
    }
  }

  /** Helper method for creating the User table. */
  public static void main(String args[]) throws Exception {
    Database.with(User.class).createTable();
  }
}
