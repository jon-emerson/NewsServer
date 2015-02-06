package com.janknspank.rank;

import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.UserProto.User;

public abstract class Scorer {

  public abstract double getScore(User user, Article article);

}
