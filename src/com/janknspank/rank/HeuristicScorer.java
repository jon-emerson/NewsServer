package com.janknspank.rank;

import java.io.IOException;

import com.janknspank.data.DataInternalException;
import com.janknspank.data.ValidationException;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.ArticleFacebookEngagement;

public class HeuristicScorer implements Scorer {
  public double getScore(Article article, CompleteUser completeUser) {
    CompleteArticle completeArticle;
    try {
      completeArticle = new CompleteArticle(article);
    } catch (DataInternalException | IOException | ValidationException e) {
      System.out.println("Can't generate CompleteArticle. return a score of 0.");
      e.printStackTrace();
      return 0;
    }
    
    ArticleFacebookEngagement engagement = completeArticle.getLatestFacebookEngagement();
    long likeCount = 0;
    long commentCount = 0;
    long shareCount = 0;
    if (engagement != null) {
      likeCount = engagement.getLikeCount();
      commentCount = engagement.getCommentCount();
      shareCount = engagement.getShareCount();
    }
    
    double score = 0;
    
    // Max Current workplace value: 0.2
    if (InputValuesGenerator.isAboutCurrentEmployer(completeUser, completeArticle) == 1) {
      score += 0.2;
    }
    
    // Matched interests up to 0.3
    score += Math.min(InputValuesGenerator.matchedInterestsCount(
        completeUser, completeArticle) / 10, 0.3);
    
    // Article length not super short: 0.1
    if (completeArticle.wordCount() > 300) {
      score += 0.1;
    }
    
    // Like count up to 0.3
    if (likeCount > 0) {
      score += Math.min(Math.log(likeCount) / 10, 0.3); 
    }
    
    // TODO: implement heuristic scoring function
    return score;
  }
}
