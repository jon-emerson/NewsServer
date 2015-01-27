package com.janknspank.rank;


public interface Scorer {
  public double getScore(CompleteUser completeUser, CompleteArticle article);
}
