package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;

import com.janknspank.bizness.Articles;
import com.janknspank.bizness.BiznessException;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.UserProto.User;
import com.janknspank.rank.NeuralNetworkScorer;

@AuthenticationRequired
public class GetArticlesServlet extends AbstractArticlesServlet {

  @Override
  protected Iterable<Article> getArticles(HttpServletRequest req)
      throws DatabaseSchemaException, BiznessException, DatabaseRequestException {
    User user = Database.with(User.class).get(getSession(req).getUserId());
    try {
      return Articles.getRankedArticles(user, NeuralNetworkScorer.getInstance());
    } catch (DatabaseSchemaException | ParserException e) {
      // Fallback
      System.out.println("Error: couldn't load getRankedArticles: " + e.getMessage());
      return Articles.getArticlesByInterest(user.getInterestList());
    }
  }
}
