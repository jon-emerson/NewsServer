package com.janknspank.bizness;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption.DescendingSort;
import com.janknspank.database.QueryOption.Limit;
import com.janknspank.database.QueryOption.WhereEquals;
import com.janknspank.proto.Core.ArticleFacebookEngagement;

public class ArticleFacebookEngagements {
  public static Iterable<ArticleFacebookEngagement> getLatest(String url, int limit)
      throws BiznessException {
    Iterable<ArticleFacebookEngagement> engagements;
    try {
      engagements = Database.with(ArticleFacebookEngagement.class).get(
          new WhereEquals("url", url),
          new Limit(limit),
          new DescendingSort("create_time"));
    } catch (DatabaseSchemaException e) {
      throw new BiznessException("Error reading facebook engagements", e);
    }
    if (Iterables.isEmpty(engagements)) {
      ArticleFacebookEngagement engagement = FacebookData.getEngagementForURL(url);
      if (engagement != null) {
        return ImmutableList.of(engagement);
      }
    }

    return engagements;
  }

  /** Helper method for creating the ArticleFacebookEngagement table. */
  public static void main(String args[]) throws Exception {
    Database.with(ArticleFacebookEngagement.class).createTable();
  }
}