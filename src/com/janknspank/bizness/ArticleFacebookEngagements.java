package com.janknspank.bizness;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption.DescendingSort;
import com.janknspank.database.QueryOption.Limit;
import com.janknspank.database.QueryOption.WhereEquals;
import com.janknspank.proto.ArticleProto.SocialEngagement;

public class ArticleFacebookEngagements {
  public static Iterable<SocialEngagement> getLatest(String url, int limit)
      throws BiznessException {
    Iterable<SocialEngagement> engagements;
    try {
      engagements = Database.with(SocialEngagement.class).get(
          new WhereEquals("url", url),
          new Limit(limit),
          new DescendingSort("create_time"));
    } catch (DatabaseSchemaException e) {
      throw new BiznessException("Error reading facebook engagements", e);
    }
    if (Iterables.isEmpty(engagements)) {
      SocialEngagement engagement = FacebookData.getEngagementForURL(url);
      if (engagement != null) {
        return ImmutableList.of(engagement);
      }
    }

    return engagements;
  }

  /** Helper method for creating the SocialEngagement table. */
  public static void main(String args[]) throws Exception {
    Database.with(SocialEngagement.class).createTable();
  }
}