package com.janknspank.bizness;

import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption.DescendingSort;
import com.janknspank.database.QueryOption.Limit;
import com.janknspank.database.QueryOption.WhereEquals;
import com.janknspank.proto.ArticleProto.SocialEngagement;

public class ArticleFacebookEngagements {
  /**
   * Returns the latest SocialEngagements. Only requests new engagement
   * from FB if there are none.
   * @param url
   * @param limit
   * @return
   * @throws BiznessException
   */
  public static Iterable<SocialEngagement> getLatest(String url, int limit)
      throws DatabaseSchemaException {
    return Database.with(SocialEngagement.class).get(
        new WhereEquals("url", url),
        new Limit(limit),
        new DescendingSort("create_time"));
  }

  /** Helper method for creating the SocialEngagement table. */
  public static void main(String args[]) throws Exception {
    Database.with(SocialEngagement.class).createTable();
  }
}