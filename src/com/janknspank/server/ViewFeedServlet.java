package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Function;
import com.google.template.soy.data.SoyMapData;
import com.janknspank.bizness.Articles;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.TimeRankingStrategy.AncillaryStreamStrategy;
import com.janknspank.common.TopList;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.Interest.InterestType;
import com.janknspank.proto.UserProto.User;
import com.janknspank.server.soy.ViewFeedSoy;

@AuthenticationRequired
@ServletMapping(urlPattern = "/viewFeed")
public class ViewFeedServlet extends StandardServlet {
  /**
   * Returns any Soy data necessary for rendering the .main template for this
   * servlet's Soy page.
   */
  @Override
  protected SoyMapData getSoyMapData(HttpServletRequest req)
      throws DatabaseSchemaException, RequestException, BiznessException {
    User user = getUser(req);

    final TopList<Article, Double> rankedArticlesAndScores;
    String industryCodeId = this.getParameter(req, "industry_code");
    if (industryCodeId != null) {
      // It's important to override the user, because inside ViewFeedSoy, this
      // user is used to regenerate the inputs to the neural network for display
      // purposes.  If we don't wholly replace the user, what the Soy shows
      // will not match what the neural network actually received.
      user = user.toBuilder()
          .clearInterest()
          .addInterest(Interest.newBuilder()
              .setType(InterestType.INDUSTRY)
              .setIndustryCode(Integer.parseInt(industryCodeId))
              .build())
          .build();
      rankedArticlesAndScores = Articles.getStream(user, new AncillaryStreamStrategy());
    } else {
      rankedArticlesAndScores = Articles.getMainStream(user);
    }

    return new SoyMapData(
        "sessionKey", this.getSession(req).getSessionKey(),
        "articles", ViewFeedSoy.toSoyListData(
            rankedArticlesAndScores,
            user,
            new Function<Article, Double>() {
              @Override
              public Double apply(Article article) {
                return rankedArticlesAndScores.getValue(article);
              }
            }));
  }
}
