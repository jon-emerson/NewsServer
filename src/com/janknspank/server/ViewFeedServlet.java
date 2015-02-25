package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Function;
import com.google.template.soy.data.SoyMapData;
import com.janknspank.bizness.Articles;
import com.janknspank.common.TopList;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.UserProto.User;
import com.janknspank.rank.NeuralNetworkScorer;
import com.janknspank.server.soy.ArticleSoy;

@AuthenticationRequired
public class ViewFeedServlet extends StandardServlet {
  private static final int NUM_RESULTS = 50;

  /**
   * Returns any Soy data necessary for rendering the .main template for this
   * servlet's Soy page.
   */
  @Override
  protected SoyMapData getSoyMapData(HttpServletRequest req)
      throws DatabaseSchemaException, RequestException {
    User user = Database.with(User.class).get(getSession(req).getUserId());
    if (user.getEmail().equals("tom.charytoniuk@gmail.com") ||
        user.getEmail().equals("panaceaa@gmail.com") ||
        user.getEmail().equals("chrysb@gmail.com")) {
      // Debugging for Linkedinprofile document
//      LinkedInProfile profile = user.getLinkedInProfile();
//      DocumentNode linkedInProfileDocument = DocumentBuilder.build(null, 
//          new StringReader(profile.getData()));
//      UserInterests.updateInterests(user, linkedInProfileDocument, null);
      // Uncomment to play with profile data

      final TopList<Article, Double> rankedArticlesAndScores =
          Articles.getRankedArticlesAndScores(user, NeuralNetworkScorer.getInstance(), NUM_RESULTS);
      return new SoyMapData(
          "sessionKey", this.getSession(req).getSessionKey(),
          "articles", ArticleSoy.toSoyListData(
              rankedArticlesAndScores,
              new Function<Article, Double>() {
                @Override
                public Double apply(Article article) {
                  return rankedArticlesAndScores.getValue(article);
                }
              }));
    } else {
      throw new RequestException("You are not authorized to use this page.");
    }
  }
}
