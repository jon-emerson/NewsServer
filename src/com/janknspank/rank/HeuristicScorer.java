package com.janknspank.rank;

import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.SocialEngagement;
import com.janknspank.proto.UserProto.User;

public class HeuristicScorer extends Scorer {
  private static HeuristicScorer instance = null;

  private HeuristicScorer() {
    // Nothing for now
  }

  public static synchronized HeuristicScorer getInstance() {
    if (instance == null) {
      instance = new HeuristicScorer();
    }
    return instance;
  }

  public double getScore(User user, Article article) {
    SocialEngagement engagement = getLatestFacebookEngagement(article);
    if (engagement == null) {
      engagement = SocialEngagement.getDefaultInstance();
    }
    long engagementCount = engagement.getLikeCount() + engagement.getCommentCount()
        + engagement.getShareCount();

    double score = 0;

    // Max Current workplace value: 0.2
    if (InputValuesGenerator.isAboutCurrentEmployer(user, article)) {
      score += 0.2;
    }

    // Matched interests up to 0.3
    score += Math.min(InputValuesGenerator.matchedInterestsCount(user, article) / 10, 0.3);

    // Article length not super short: 0.1
    if (article.getWordCount() > 300) {
      score += 0.1;
    }

    // FB engagement count up to 0.3
    if (engagementCount > 0) {
      score += Math.min(Math.log(engagementCount) / 10, 0.3);
    }

    // Industry relevance
    score += InputValuesGenerator.industryRelevance(user, article);

    return score;
  }
}
