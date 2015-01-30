package com.janknspank.rank;

import com.janknspank.common.TopList;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.SocialEngagement;
import com.janknspank.proto.ArticleProto.SocialEngagement.Site;
import com.janknspank.proto.UserProto.User;

public abstract class Scorer {
  private static final int MILLIS_PER_DAY = 86400000;

  public abstract double getScore(User user, Article article);

  public static SocialEngagement getLatestFacebookEngagement(Article article) {
    SocialEngagement facebookEngagement = null;
    for (SocialEngagement socialEngagement : article.getSocialEngagementList()) {
      if (socialEngagement.getSite() == Site.FACEBOOK &&
          (facebookEngagement == null ||
              socialEngagement.getCreateTime() > facebookEngagement.getCreateTime())) {
        facebookEngagement = socialEngagement;
      }
    }
    return facebookEngagement;
  }

  // returns Likes / day
  public static double getLikeVelocity(Article article) {
    TopList<SocialEngagement, Long> q = new TopList<>(2);
    for (SocialEngagement engagement : article.getSocialEngagementList()) {
      if (engagement.getSite() == Site.FACEBOOK) {
        q.add(engagement, engagement.getCreateTime());
      }
    }
    if (q.size() == 0) {
      return 0;
    } else if (q.size() == 1) {
      // Use the published date to get the velocity
      SocialEngagement engagement = q.getKey(0);
      double daysSincePublish = (System.currentTimeMillis() -
          article.getPublishedTime()) / MILLIS_PER_DAY;
      return engagement.getLikeCount() / daysSincePublish;
    } else {
      // Use the time interval between the last two engagement checks
      SocialEngagement mostRecentEng = q.getKey(0);
      SocialEngagement previousEng = q.getKey(1);
      long changeInLikes = mostRecentEng.getLikeCount()
          - previousEng.getLikeCount();
      double daysBetweenChecks = (mostRecentEng.getCreateTime()
          - previousEng.getCreateTime()) / MILLIS_PER_DAY;
      return changeInLikes / daysBetweenChecks;
    }
  }
}
