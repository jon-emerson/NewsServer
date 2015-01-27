package com.janknspank.server;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import com.janknspank.data.Articles;
import com.janknspank.data.DataInternalException;
import com.janknspank.data.UserInterests;
import com.janknspank.data.ValidationException;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.proto.Core.Article;
import com.janknspank.rank.NeuralNetworkScorer;

@AuthenticationRequired
public class GetArticlesServlet extends AbstractArticlesServlet {

  @Override
  protected Iterable<Article> getArticles(HttpServletRequest req) throws DataInternalException {
    try {  
      return Articles.getRankedArticles(getSession(req).getUserId(),
          NeuralNetworkScorer.getInstance());      
    }
    catch (ParserException | ValidationException | IOException e) {
      // Fallback
      System.out.println("Error: couldn't load getArticlesRankedByNeuralNetwork: " + e.getMessage());
      return Articles.getArticles(UserInterests.getInterests(getSession(req).getUserId()));
    }
  }
}
