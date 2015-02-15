package com.janknspank.bizness;

import com.google.common.collect.ImmutableList;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.CoreProto.UrlRating;
import com.janknspank.proto.UserProto.User;

public class UrlRatings {
  public static Iterable<UrlRating> getAllRatings() throws DatabaseSchemaException {
    return Database.with(UrlRating.class).get();
  }
  
  public static Iterable<UrlRating> getForUser(User user) throws DatabaseSchemaException {
    String email = user.getEmail();
    return Database.with(UrlRating.class).get(new QueryOption.WhereEquals("email", email));
  }
  
  public static void upsertRating(String url, double score, User user)
      throws DatabaseSchemaException, DatabaseRequestException {
    UrlRating existingRating = Database.with(UrlRating.class).getFirst(
        new QueryOption.WhereEquals("email", user.getEmail()),
        new QueryOption.WhereEquals("url", url));
    if (existingRating != null) {
      Database.update(existingRating.toBuilder().setRating(score).build());
    } else {
      Database.with(UrlRating.class).insert(ImmutableList.of(UrlRating.newBuilder()
          .setId(GuidFactory.generate())
          .setUrl(url)
          .setRating(score)
          .setEmail(user.getEmail())
          .setCreateTime(System.currentTimeMillis())
          .build()));
    }
  }
  
  /** Helper method for creating the TrainedArticleIndustry table. */
  public static void main(String args[]) throws Exception {
    Database.with(UrlRating.class).createTable();
  }
}
