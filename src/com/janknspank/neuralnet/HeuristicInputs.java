package com.janknspank.neuralnet;

import com.janknspank.proto.Core.UserInterest;

public class HeuristicInputs {
  public static int matchedInterestsCount(CompleteUser user, CompleteArticle article) {
    int count = 0;
    for (UserInterest interest : user.getInterests()) {
      if (article.containsInterest(interest)) {
        count++;
      }
    }
    return count;
  }
}