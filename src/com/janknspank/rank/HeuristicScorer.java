package com.janknspank.rank;

import com.janknspank.classifier.ClassifierException;
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
    score += 0.3 * InputValuesGenerator.relevanceToSocialMedia(user, article);

    // 3. Relevance to contacts
    score += 0.3 * InputValuesGenerator.relevanceToContacts(user, article);

    // 4. Relevance to current employer
    score += 0.2 * InputValuesGenerator.relevanceToCurrentEmployer(user, article);

    // 5. Relevance to companies the user wants to work at
    score += 0.2 * InputValuesGenerator.relevanceToCompaniesTheUserWantsToWorkAt(user, article);

    // 6. Relevance to skills
    score += 0.2 * InputValuesGenerator.relevanceToSkills(user, article);

    // 7. Relevance to current role
    score += 0.1 * InputValuesGenerator.relevanceToCurrentRole(user, article);

    // 8. Timeliness
    score += 0.1 * InputValuesGenerator.timeliness(article);

    // 9. Past employers
    score += 0.1 * InputValuesGenerator.relevanceToPastEmployers(user, article);

    // 10. Article text quality
    score += 0.1 * InputValuesGenerator.articleTextQualityScore(article);

    return score;
  }
}
