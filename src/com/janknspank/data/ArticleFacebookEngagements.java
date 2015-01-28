package com.janknspank.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.janknspank.data.QueryOption.DescendingSort;
import com.janknspank.data.QueryOption.Limit;
import com.janknspank.data.QueryOption.WhereEquals;
import com.janknspank.facebook.FacebookData;
import com.janknspank.proto.Core.ArticleFacebookEngagement;

public class ArticleFacebookEngagements {
  public static Iterable<ArticleFacebookEngagement> getLatest(String url, int limit) 
      throws DataInternalException {
    Iterable<ArticleFacebookEngagement> engagements = Database.with(ArticleFacebookEngagement.class).get(
        new WhereEquals("url", url),
        new Limit(limit),
        new DescendingSort("create_time"));
    if (Iterables.isEmpty(engagements)) {
      ArticleFacebookEngagement engagement = FacebookData.getEngagementForURL(url);
      if (engagement != null) {
        return ImmutableList.of(engagement);
      }
      return null;
    }
    
    return engagements;
  }
  
  /** Helper method for creating the ArticleFacebookEngagement table. */
  public static void main(String args[]) throws Exception {
    Database.with(ArticleFacebookEngagement.class).createTable();
  }
}