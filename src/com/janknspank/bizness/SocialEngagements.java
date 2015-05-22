package com.janknspank.bizness;

import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption.DescendingSort;
import com.janknspank.database.QueryOption.Limit;
import com.janknspank.database.QueryOption.WhereEquals;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.SocialEngagement;

public class SocialEngagements {
  /**
   * Returns the SocialEngagements for a given URL, as cached in the database.
   * All social engagements are returned.
   */
  public static Iterable<SocialEngagement> getForUrl(String url, int limit)
      throws DatabaseSchemaException {
    return Database.with(SocialEngagement.class).get(
        new WhereEquals("url", url),
        new Limit(limit),
        new DescendingSort("create_time"));
  }

  /**
   * Returns the most recent social engagement we have for the given article on
   * a specific social site (facebook, etc).
   */
  public static SocialEngagement getForArticle(Article article, SocialEngagement.Site site) {
    SocialEngagement latest = null;
    for (SocialEngagement engagement : article.getSocialEngagementList()) {
      if (engagement.getSite() == site) {
        if (latest == null || latest.getCreateTime() < engagement.getCreateTime()) {
          latest = engagement;
        }
      }
    }
    return latest;
  }
}