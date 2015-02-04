package com.janknspank.server;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.janknspank.bizness.Articles;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.Intents;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.UserProto.Intent;
import com.janknspank.proto.UserProto.User;
import com.janknspank.rank.HeuristicScorer;

public class FTUEGetArticlesServlet extends AbstractArticlesServlet {

  @Override
  protected Iterable<Article> getArticles(HttpServletRequest req)
      throws DatabaseSchemaException, BiznessException, DatabaseRequestException {
    String intentCodesCommaSeparated = getParameter(req, "intents");
    List<String> intentCodes = Arrays.asList(intentCodesCommaSeparated.split(","));
    Iterable<Intent> intents = Intents.getIntentsFromCodes(intentCodes);
    User user = Database.with(User.class).get(getSession(req).getUserId());
    user = Intents.setIntents(user, intents);
    
    try {
      return Articles.getRankedArticles(user, HeuristicScorer.getInstance());
    } catch (DatabaseSchemaException | ParserException e) {
      // Fallback
      System.out.println("Error: couldn't load getRankedArticles: " + e.getMessage());
      return Articles.getArticlesByInterest(user.getInterestList());
    }
  }
}
