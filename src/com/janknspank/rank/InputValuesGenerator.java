package com.janknspank.rank;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import com.janknspank.bizness.ArticleFeatures;
import com.janknspank.bizness.EntityType;
import com.janknspank.bizness.SocialEngagements;
import com.janknspank.bizness.UserIndustries;
import com.janknspank.bizness.UserInterests;
import com.janknspank.classifier.FeatureId;
import com.janknspank.classifier.StartupFeatureHelper;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleFeature;
import com.janknspank.proto.ArticleProto.ArticleKeyword;
import com.janknspank.proto.ArticleProto.SocialEngagement;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.Interest.InterestType;
import com.janknspank.proto.UserProto.LinkedInProfile.Employer;
import com.janknspank.proto.UserProto.User;

/**
 * Helper class to generate input node values for the Scorer.
 */
public class InputValuesGenerator {
  public static double relevanceToUserIndustries(User user, Article article) {
    double relevance = 0;
    for (FeatureId industryFeatureId : UserIndustries.getIndustryFeatureIds(user)) {
      relevance += getSimilarityToIndustry(article, industryFeatureId);
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
    for (Interest interest : UserInterests.getInterests(user)) {
      if (interest.getType() == InterestType.ENTITY &&
          EntityType.fromValue(interest.getEntity().getType()).isA(EntityType.PERSON)) {
        contactsKeywords.add(interest.getEntity().getKeyword());
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

  public static double articleTextQualityScore(Article article) {
    return 0;
  }

  public static double relevanceToStartupIntent(User user, Article article) {
//    for (Intent intent : user.getIntentList()) {
//      if (intent.getCode() == IntentCodes.START_COMPANY.getCode()) {
        for (ArticleFeature articleFeature : article.getFeatureList()) {
          FeatureId featureId = FeatureId.fromId(articleFeature.getFeatureId());
          if (StartupFeatureHelper.isStartupFeature(featureId) &&
              StartupFeatureHelper.isRelatedToIndustries(
                  featureId, UserIndustries.getIndustryFeatureIds(user))) {
            return articleFeature.getSimilarity();
          }
        }
//      }
//    }
    return 0;
  }

  public static double relevanceToAcquisitions(User user, Article article) {
    ArticleFeature acquisitionFeature =
        ArticleFeatures.getFeature(article, FeatureId.MANUAL_HEURISTIC_ACQUISITIONS);
    return (acquisitionFeature == null) ? 0 : acquisitionFeature.getSimilarity();
  }

  public static double relevanceToLaunches(User user, Article article) {
    ArticleFeature acquisitionFeature =
        ArticleFeatures.getFeature(article, FeatureId.MANUAL_HEURISTIC_LAUNCHES);
    return (acquisitionFeature == null) ? 0 : acquisitionFeature.getSimilarity();
  }

  public static double relevanceToFundraising(User user, Article article) {
    ArticleFeature acquisitionFeature =
        ArticleFeatures.getFeature(article, FeatureId.MANUAL_HEURISTIC_FUNDRAISING);
    return (acquisitionFeature == null) ? 0 : acquisitionFeature.getSimilarity();
  }

  public static double getSimilarityToIndustry(Article article, FeatureId industryFeatureId) {
    ArticleFeature industryFeature =
        ArticleFeatures.getFeature(article, industryFeatureId);
    // Only value relevance greater than 66.7%.
    return (industryFeature == null) ? 0 :
        Math.max(0, industryFeature.getSimilarity() * 3 - 2);
  }

  /**
   * Returns a score for how related this article is to sports, entertainment,
   * or popular politics.
   */
  public static double relevanceToPopCulture(Article article) {
    double score = 0;
    for (ArticleFeature feature : new ArticleFeature[] {
        ArticleFeatures.getFeature(article, FeatureId.TOPIC_ENTERTAINMENT),
        ArticleFeatures.getFeature(article, FeatureId.TOPIC_SPORTS),
        ArticleFeatures.getFeature(article, FeatureId.TOPIC_POLITICS)
    }) {
      if (feature != null) {
        score = Math.max(score, feature.getSimilarity());
      }
    }
    return score;
  }

  // Normalize any value to [0,1]
  // private static double sigmoid(double x) {
  //   return 1 / (1 + Math.exp(-x));
  // }
}