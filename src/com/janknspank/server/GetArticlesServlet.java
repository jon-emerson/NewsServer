package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;

import com.janknspank.bizness.Articles;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.UserProto.User;
import com.janknspank.rank.NeuralNetworkScorer;

@AuthenticationRequired
public class GetArticlesServlet extends AbstractArticlesServlet {
  private static final int NUM_RESULTS = 50;

  @Override
  protected Iterable<Article> getArticles(HttpServletRequest req) throws DatabaseSchemaException {
    User user = Database.with(User.class).get(getSession(req).getUserId());
    return Articles.getRankedArticles(user, NeuralNetworkScorer.getInstance(), NUM_RESULTS);
  }
}
