package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;

import com.janknspank.bizness.Articles;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.UserInterests;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.proto.Core.Article;
import com.janknspank.rank.NeuralNetworkScorer;

@AuthenticationRequired
public class GetArticlesServlet extends AbstractArticlesServlet {

  @Override
  protected Iterable<Article> getArticles(HttpServletRequest req)
      throws DatabaseSchemaException, BiznessException, DatabaseRequestException {
    try {
      return Articles.getRankedArticles(getSession(req).getUserId(),
          NeuralNetworkScorer.getInstance());
    } catch (DatabaseSchemaException | ParserException e) {
      // Fallback
      System.out.println("Error: couldn't load getArticlesRankedByNeuralNetwork: " + e.getMessage());
      return Articles.getArticlesByInterest(UserInterests.getInterests(getSession(req).getUserId()));
    }
  }
}
