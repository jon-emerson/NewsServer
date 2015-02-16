package com.janknspank.rank;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import com.janknspank.bizness.SocialEngagements;
import com.janknspank.bizness.UserInterests;
import com.janknspank.classifier.FeatureId;
import com.janknspank.classifier.IndustryCode;
import com.janknspank.classifier.StartupFeatureHelper;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleFeature;
import com.janknspank.proto.ArticleProto.ArticleKeyword;
import com.janknspank.proto.ArticleProto.SocialEngagement;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.LinkedInProfile.Employer;
import com.janknspank.proto.UserProto.User;
import com.janknspank.proto.UserProto.UserIndustry;

/**
 * Helper class to generate input node values for the Scorer.
 */
public class InputValuesGenerator {
  public static double relevanceToUserIndustries(User user, Article article) {
    double relevance = 0;
    for (UserIndustry userIndustry : user.getIndustryList()) {
      relevance += getSimilarityToIndustry(
          article, IndustryCode.fromId(userIndustry.getIndustryCodeId()));
    }
    return Math.min(1.0, relevance);
  }

  public static double relevanceToSocialMedia(User user, Article article) {
    SocialEngagement engagement = SocialEngagements.getForArticle(
        article, SocialEngagement.Site.FACEBOOK);
    return (engagement == null) ? 0 : engagement.getShareScore();
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
            return (double) keyword.getStrength() / 20;
          }
        }
      }
    }
    return 0;
  }

  public static double relevanceToCompaniesTheUserWantsToWorkAt(User user, Article article) {
    return 0;
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
    return Math.min(1, relevance);
  }

  public static double relevanceToCurrentRole(User user, Article article) {
    return 0;
  }

  public static double timeliness(Article article) {
    // Older is smaller value
    return sigmoid(article.getPublishedTime() - System.currentTimeMillis());
  }

  public static double articleTextQualityScore(Article article) {
    return 0;
  }

  public static double relevanceToStartupIntent(User user, Article article) {
//    for (Intent intent : user.getIntentList()) {
//      if (intent.getCode() == IntentCodes.START_COMPANY.getCode()) {
        for (ArticleFeature articleFeature : article.getFeatureList()) {
          FeatureId featureId = FeatureId.fromId(articleFeature.getFeatureId());
          if (StartupFeatureHelper.isStartupFeature(featureId) &&
              StartupFeatureHelper.isRelatedToIndustries(featureId, user.getIndustryList())) {
            return articleFeature.getSimilarity();
          }
        }
//      }
//    }
    return 0;
  }
  
  public static double relevanceToAcquisitions(User user, Article article) {
    String lowerCaseTitle = article.getTitle().toLowerCase();
    String lowerCaseBody = "";
    for (String paragraph : article.getParagraphList()) {
      lowerCaseBody += paragraph.toLowerCase();
    }

    if (lowerCaseTitle.contains("acquires")) {
      return 1.0;
    } else if (lowerCaseTitle.contains("buys")) {
      return 0.9;
    } else if (lowerCaseBody.contains("acquires")) {
      return 0.8;
    } else if (lowerCaseBody.contains("acquired")) {
      return 0.6;
    } else if (lowerCaseTitle.contains("acquisition")) {
      return 0.5;
    } else if (lowerCaseBody.contains("acquisition")) {
      return 0.2;
    }
    return 0;
  }
  
  public static double relevanceToLaunches(User user, Article article) {
    String lowerCaseTitle = article.getTitle().toLowerCase();
    String lowerCaseBody = "";
    for (String paragraph : article.getParagraphList()) {
      lowerCaseBody += paragraph.toLowerCase();
    }

    if (lowerCaseTitle.contains("launches")) {
      return 1.0;
    } else if (lowerCaseTitle.contains("launched")) {
      return 0.9;
    } else if (lowerCaseBody.contains("launches")) {
      return 0.8;
    } else if (lowerCaseBody.contains("launched")) {
      return 0.6;
    } else if (lowerCaseTitle.contains("releases")) {
      return 0.9;
    } else if (lowerCaseBody.contains("releases")) {
      return 0.4;
    }
    return 0;
  }
  
  public static double relevanceToFundraising(User user, Article article) {
    String lowerCaseTitle = article.getTitle().toLowerCase();
    String lowerCaseBody = "";
    for (String paragraph : article.getParagraphList()) {
      lowerCaseBody += paragraph.toLowerCase();
    }

    if (lowerCaseTitle.contains("raises") || lowerCaseTitle.contains("series a")
        || lowerCaseTitle.contains("series b") || lowerCaseTitle.contains("series c")
        || lowerCaseTitle.contains("angel round") || lowerCaseTitle.contains("series a")
        || lowerCaseTitle.contains("valuation")) {
      return 1.0;
    } else if (lowerCaseBody.contains("raises") || lowerCaseBody.contains("series a")
        || lowerCaseBody.contains("series b") || lowerCaseBody.contains("series c")
        || lowerCaseBody.contains("angel round") || lowerCaseBody.contains("series a")) {
      return 0.9;
    } else if (lowerCaseBody.contains("investors")) {
      return 0.8;
    }
    return 0;
  }

  public static double getSimilarityToIndustry(Article article, IndustryCode industryCode) {
    for (ArticleFeature feature : article.getFeatureList()) {
      if (feature.getFeatureId() == industryCode.getFeatureId().getId()) {
        // Only value relevance greater than 66.7%.
        return Math.max(0, feature.getSimilarity() * 3 - 2);
      }
    }
    return 0;
  }

  // Normalize any value to [0,1]
  private static double sigmoid(double x) {
    return 1 / (1 + Math.exp(-x));
  }
}