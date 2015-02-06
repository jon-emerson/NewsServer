package com.janknspank.server;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.google.api.client.util.Lists;
import com.google.common.base.Strings;
import com.janknspank.bizness.Articles;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.IntentCodes;
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
    User user = Database.with(User.class).get(getSession(req).getUserId());
    if (!Strings.isNullOrEmpty(intentCodesCommaSeparated)) {
      List<String> intentCodes = Arrays.asList(intentCodesCommaSeparated.split(","));
      Iterable<Intent> intents = getIntentsFromCodes(intentCodes);
      user = Intents.setIntents(user, intents);
    }

    try {
      return Articles.getRankedArticles(user, HeuristicScorer.getInstance());
    } catch (DatabaseSchemaException | ParserException e) {
      // Fallback
      System.out.println("Error: couldn't load getRankedArticles: " + e.getMessage());
      return Articles.getArticlesByInterest(user.getInterestList());
    }
  }
  
  public static Iterable<Intent> getIntentsFromCodes(Iterable<String> intentCodes) {
    List<Intent> intents = Lists.newArrayList();
    for (String intentCode : intentCodes) {
      // Validate the intent code strings
      if (IntentCodes.INTENT_CODE_MAP.containsKey(intentCode)) {
        intents.add(Intent.newBuilder()
            .setCode(intentCode)
            .setCreateTime(System.currentTimeMillis())
            .build());
      }
    }
    return intents;
  }
}