package com.janknspank.rank;

import com.janknspank.proto.Core.Article;

public interface Scorer {
  public double getScore(Article article, CompleteUser completeUser);
}
