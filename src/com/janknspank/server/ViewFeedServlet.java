package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;

import com.google.common.collect.ImmutableSet;
import com.google.template.soy.data.SoyMapData;
import com.janknspank.bizness.Articles;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.TimeRankingStrategy.IndustryStreamStrategy;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.Interest.InterestType;
import com.janknspank.proto.UserProto.User;
import com.janknspank.rank.DiversificationPass;
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

    final Iterable<Article> articles;
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
      articles = Articles.getStream(
          user,
          new IndustryStreamStrategy(),
          new DiversificationPass.IndustryStreamPass(),
          ImmutableSet.<String>of() /* excludeUrlIds */,
          false /* videoOnly */);
    } else {
      articles = Articles.getMainStream(user);
    }

    return new SoyMapData(
        "sessionKey", this.getSession(req).getSessionKey(),
        "articles", ViewFeedSoy.toSoyListData(articles, user));
  }
}
