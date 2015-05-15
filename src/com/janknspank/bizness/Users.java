package com.janknspank.bizness;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
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
  public static Future<User> updateLast5AppUseTimes(User user, String ipAddress)
      throws DatabaseSchemaException {
    TopList<Long, Long> top = new TopList<>(5);
    for (long time : user.getLast5AppUseTimeList()) {
      top.add(time, time);
    }
    long now = System.currentTimeMillis();
    if ((long) Iterables.getFirst(top, new Long(0)) < (now - TimeUnit.HOURS.toMillis(1))) {
      top.add(now, now);
      return updateLastIpAddress(
          Database.with(User.class).setFuture(user, "last_5_app_use_time", top), ipAddress);
    } else {
      return Futures.immediateFuture(user);
    }
  }

  /**
   * Helper async function for updating the user's IP address stored in MongoDB,
   * if necessary.
   * NOTE(jonemerson): Yep, it'd be cool if we could do multiple setFuture()s in
   * one call rather than doing serial chaining... We can dream, can't we?
   */
  private static Future<User> updateLastIpAddress(
      ListenableFuture<User> userFuture, final String ipAddress) {
    return Futures.transform(userFuture, new AsyncFunction<User, User>() {
      @Override
      public ListenableFuture<User> apply(User user) throws Exception {
        if (!ipAddress.equals(user.getLastIpAddress())) {
          return Database.with(User.class).setFuture(user, "last_ip_address", ipAddress);
        } else {
          return Futures.immediateFuture(user);
        }
      }
    });
  }

  /**
   * Returns the number of minutes ago that the user last used the app,
   * excluding any usages in the last hour.
   */
  public static long getLastAppUsageInMinutes(User user) {
    long lastAppUsageAtLeastOneHourAgo = 0;
    for (long last5AppUseTime : user.getLast5AppUseTimeList()) {
      if (last5AppUseTime < (System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1))
          && last5AppUseTime > lastAppUsageAtLeastOneHourAgo) {
        lastAppUsageAtLeastOneHourAgo = last5AppUseTime;
      }
    }
    return (System.currentTimeMillis() - lastAppUsageAtLeastOneHourAgo)
        / TimeUnit.MINUTES.toMillis(1);
  }

  /** Helper method for creating the User table. */
  public static void main(String args[]) throws Exception {
    Database.with(User.class).createTable();
  }
}
