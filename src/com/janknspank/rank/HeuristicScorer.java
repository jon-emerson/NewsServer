package com.janknspank.rank;

import com.janknspank.proto.ArticleProto.Article;
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
    double score = 0;

    // 1. Relevance to user's industries
    score += 0.4 * InputValuesGenerator.relevanceToUserIndustries(user, article);

    // 2. Relevance to social media
    score += 0.3 * InputValuesGenerator.relevanceOnFacebook(user, article);

    // 3. Relevance to contacts
    score += 0.3 * InputValuesGenerator.relevanceToContacts(user, article);

    // 4. Past employers
    score += 0.1 * InputValuesGenerator.relevanceToCompanyEntities(user, article);

    return score;
  }
}
