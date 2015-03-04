package com.janknspank.server;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.api.client.util.Lists;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.janknspank.bizness.Articles;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.GuidFactory;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.Interest.InterestSource;
import com.janknspank.proto.UserProto.Interest.InterestType;
import com.janknspank.proto.UserProto.User;
import com.janknspank.rank.HeuristicScorer;

@AuthenticationRequired
@ServletMapping(urlPattern = "/v1/ftue_get_articles")
public class FTUEGetArticlesServlet extends AbstractArticlesServlet {
  private static final int NUM_RESULTS = 50;

  /**
   * Called by the Mobile client right after a user sets their goals / intents
   * It returns the first stream the client will render.
   */
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws RequestException, DatabaseSchemaException, DatabaseRequestException, BiznessException {
    String intentCodesCommaSeparated = getParameter(req, "intents");
    User user = getUser(req);
    if (!Strings.isNullOrEmpty(intentCodesCommaSeparated)) {
      List<String> intentCodes = Arrays.asList(intentCodesCommaSeparated.split(","));
      Iterable<Interest> intentInterests = getInterestsFromIntentCodes(intentCodes);

      // Business logic.
      // Find all interests that have nothing to do with the specified user
      // interest.
      List<Interest> existingInterests = Lists.newArrayList();
      for (Interest interest : user.getInterestList()) {
        if (interest.getType() != InterestType.INTENT) {
          existingInterests.add(interest);
        }
      }

      // Write the filtered list plus a new Interest that represents the
      // parameters received from the client.
      Database.with(User.class).set(user, "interest",
          Iterables.concat(
              existingInterests,
              intentInterests));
    }

    return super.doGetInternal(req, resp);
  }

  @Override
  protected Iterable<Article> getArticles(HttpServletRequest req) throws DatabaseSchemaException {
    // BAD: this makes second call to DB for User after doPostInternal call
    User user = getUser(req);
    return Articles.getRankedArticles(user, HeuristicScorer.getInstance(), NUM_RESULTS);
  }

  public static Iterable<Interest> getInterestsFromIntentCodes(Iterable<String> intentCodes) {
    List<Interest> interests = Lists.newArrayList();
    for (String intentCode : intentCodes) {
      // Validate the intent code strings
      interests.add(Interest.newBuilder().setId(GuidFactory.generate())
          .setCreateTime(System.currentTimeMillis())
          .setType(InterestType.INTENT)
          .setIntentCode(intentCode)
          .setSource(InterestSource.USER)
          .build());
    }
    return interests;
  }
}
