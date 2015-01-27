package com.janknspank.rank;

import java.util.List;

import com.janknspank.proto.Core.UserIndustry;
import com.janknspank.proto.Core.UserInterest;

public class InputValuesGenerator {
  public static int isAboutCurrentEmployer(CompleteUser user, CompleteArticle article) {
    String currentEmployer = user.getCurrentWorkplace();
    if (article.containsKeyword(currentEmployer)) {
      return 1;
    }
    else {
      return 0;
    }
  }
  
  public static int matchedInterestsCount(CompleteUser user, CompleteArticle article) {
    int count = 0;
    for (UserInterest interest : user.getInterests()) {
      if (article.containsInterest(interest)) {
        count++;
      }
    }
    return count;
  }
  
  public static double industryRelevance(CompleteUser user, CompleteArticle article) {
    double relevance = 0;
    List<UserIndustry> userIndustries = user.getIndustries();
    for (UserIndustry userIndustry : userIndustries) {
      relevance += article.getSimilarityToIndustry(userIndustry.getIndustryCodeId());
    }
    return relevance;
  }
}