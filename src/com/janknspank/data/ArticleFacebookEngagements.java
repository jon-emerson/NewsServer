package com.janknspank.data;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.janknspank.data.QueryOption.DescendingSort;
import com.janknspank.data.QueryOption.Limit;
import com.janknspank.data.QueryOption.WhereEquals;
import com.janknspank.facebook.FacebookData;
import com.janknspank.proto.Core.ArticleFacebookEngagement;

public class ArticleFacebookEngagements {
  public static List<ArticleFacebookEngagement> getLatest(String url, int limit) 
      throws DataInternalException {
    List<ArticleFacebookEngagement> engagements = Database.with(ArticleFacebookEngagement.class).get(
        new WhereEquals("url", url),
        new Limit(limit),
        new DescendingSort("create_time"));
    if (engagements == null || engagements.isEmpty()) {
      ArticleFacebookEngagement engagement = FacebookData.getEngagementForURL(url);
      if (engagement != null) {
        try {
          Database.insert(engagement);
        } catch (ValidationException e) {
          throw new DataInternalException("Error creating facebook engagement", e);
        }
        return ImmutableList.of(engagement);
      }
      return null;        
    }
    else {
      return engagements;
    }
  }
  
  /** Helper method for creating the ArticleFacebookEngagement table. */
  public static void main(String args[]) throws Exception {
    Database.with(ArticleFacebookEngagement.class).createTable();
  }
}