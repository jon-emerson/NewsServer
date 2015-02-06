package com.janknspank.rank;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import com.janknspank.bizness.SocialEngagements;
import com.janknspank.bizness.UserInterests;
import com.janknspank.common.TopList;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleIndustry;
import com.janknspank.proto.ArticleProto.ArticleKeyword;
import com.janknspank.proto.ArticleProto.SocialEngagement;
import com.janknspank.proto.ArticleProto.SocialEngagement.Site;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.LinkedInProfile.Employer;
import com.janknspank.proto.UserProto.User;
import com.janknspank.proto.UserProto.UserIndustry;

/**
 * Helper class to generate input node values for the Scorer.
 */
public class InputValuesGenerator {
  private static final int MILLIS_PER_DAY = 86400000;

  public static double relevanceToUserIndustries(User user, Article article) {
    double relevance = 0;
    for (UserIndustry userIndustry : user.getIndustryList()) {
      relevance += getSimilarityToIndustry(article, userIndustry.getIndustryCodeId());
    }
    return Math.min(1.0, relevance);
  }

  public static double relevanceToSocialMedia(User user, Article article) {
    SocialEngagement engagement = SocialEngagements.getForArticle(
        article, SocialEngagement.Site.FACEBOOK);
    if (engagement == null) {
      engagement = SocialEngagement.getDefaultInstance();
    }
    long engagementCount = engagement.getLikeCount() + engagement.getCommentCount()
        + engagement.getShareCount();
    return Math.min(Math.log(engagementCount) / 10, 1.0);
  }

  public static double relevanceToContacts(User user, Article article) {
    Set<String> contactsKeywords = Sets.newHashSet();
    for (Interest interest : user.getInterestList()) {
      if (interest.getType() == UserInterests.TYPE_PERSON) {
        contactsKeywords.add(interest.getKeyword());
      }
    }
    int count = 0;
    for (ArticleKeyword keyword : article.getKeywordList()) {
      if (contactsKeywords.contains(keyword.getKeyword())) {
        count++;
      }
    }
    return count;
  }

  public static double relevanceToCurrentEmployer(User user, Article article) {
    if (user.hasLinkedInProfile()) {
      Employer currentEmployer = user.getLinkedInProfile().getCurrentEmployer();
      if (currentEmployer != null) {
        for (ArticleKeyword keyword : article.getKeywordList()) {
          if (currentEmployer.getName().equals(keyword.getKeyword())) {
            return (double)keyword.getStrength() / 20;
          }
        }
      }
    }
    return 0;
  }

  public static double relevanceToCompaniesTheUserWantsToWorkAt(User user, Article article) {
    return 0;
  }

  public static double relevanceToSkills(User user, Article article) {
    Set<String> skillsKeywords = Sets.newHashSet();
    for (Interest interest : user.getInterestList()) {
      if (interest.getType() == UserInterests.TYPE_SKILL) {
        skillsKeywords.add(interest.getKeyword());
      }
    }
    int count = 0;
    for (ArticleKeyword keyword : article.getKeywordList()) {
      if (skillsKeywords.contains(keyword.getKeyword())) {
        count++;
      }
    }
    return count;
  }

  public static double relevanceToPastEmployers(User user, Article article) {
    double relevance = 0.0;
    if (user.hasLinkedInProfile()) {
      List<Employer> pastEmployers = user.getLinkedInProfile().getPastEmployerList();
      for (Employer employer : pastEmployers) {
        for (ArticleKeyword keyword : article.getKeywordList()) {
          if (employer.getName().equals(keyword.getKeyword())) {
            relevance += (double) keyword.getStrength() / 20;
          }
        }
      }
    }

    return relevance;
  }

  public static double relevanceToCurrentRole(User user, Article article) {
    return 0;
  }

  public static double timeliness(Article article) {
    return timelinessAtTime(article, System.currentTimeMillis());
  }

  public static double timelinessAtTime(Article article, long timeInMillis) {
    // Older is smaller value
    return sigmoid(article.getPublishedTime() - timeInMillis); 
  }
  
  public static double articleTextQualityScore(Article article) {
    // TODO!!!!
    return 0;
  }

  public static double getSimilarityToIndustry(Article article, int industryCode) {
    for (ArticleIndustry classification : article.getIndustryList()) {
      if (classification.getIndustryCodeId() == industryCode) {
        return classification.getSimilarity();
      }
    }
    return 0;
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

  // Normalize any value to [0,1]
  private static double sigmoid(double x) {
    return 1 / (1 + Math.exp(-x));
  }
}