package com.janknspank.rank;

import java.util.Set;

import com.google.common.collect.Sets;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleIndustry;
import com.janknspank.proto.ArticleProto.ArticleKeyword;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.LinkedInProfile.Employer;
import com.janknspank.proto.UserProto.User;
import com.janknspank.proto.UserProto.UserIndustry;

/**
 * Helper class to generate input node values for the Scorer.
 */
public class InputValuesGenerator {
  public static boolean isAboutCurrentEmployer(User user, Article article) {
    if (user.hasLinkedInProfile()) {
      Employer currentEmployer = user.getLinkedInProfile().getCurrentEmployer();
      if (currentEmployer != null) {
        for (ArticleKeyword keyword : article.getKeywordList()) {
          if (currentEmployer.getName().equals(keyword.getKeyword())) {
            return true;
          }
        }
      }
    }
    return false;
  }

  public static int matchedInterestsCount(User user, Article article) {
    Set<String> userInterestKeywords = Sets.newHashSet();
    for (Interest interest : user.getInterestList()) {
      userInterestKeywords.add(interest.getKeyword());
    }
    int count = 0;
    for (ArticleKeyword keyword : article.getKeywordList()) {
      if (userInterestKeywords.contains(keyword.getKeyword())) {
        count++;
      }
    }
    return count;
  }

  public static double industryRelevance(User user, Article article) {
    double relevance = 0;
    for (UserIndustry userIndustry : user.getIndustryList()) {
      relevance += getSimilarityToIndustry(article, userIndustry.getIndustryCodeId());
    }
    return relevance;
  }

  public static double getSimilarityToIndustry(Article article, int industryCode) {
    for (ArticleIndustry classification : article.getIndustryList()) {
      if (classification.getIndustryCodeId() == industryCode) {
        return classification.getSimilarity();
      }
    }
    return 0;
  }
}