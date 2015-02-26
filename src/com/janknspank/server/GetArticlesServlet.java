package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;

import com.janknspank.bizness.Articles;
import com.janknspank.bizness.IntentCodes;
import com.janknspank.classifier.FeatureId;
import com.janknspank.classifier.IndustryCode;
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
    String featureId = this.getParameter(req, "featureId");
    String industryCodeId = this.getParameter(req, "industryCodeId");
    String contacts = this.getParameter(req, "contacts");

    if (featureId != null) {
      return Articles.getArticlesForFeature(
          FeatureId.fromId(Integer.parseInt(featureId)),
          NUM_RESULTS);
    } else if (industryCodeId != null) {
      return Articles.getArticlesForFeature(
          IndustryCode.fromId(Integer.parseInt(industryCodeId)).getFeatureId(),
          NUM_RESULTS);
    } else if ("linkedIn".equals(contacts)) {
      User user = Database.with(User.class).get(getSession(req).getUserId());
      return Articles.getArticlesContainingLinkedInContacts(user, NUM_RESULTS);
    } else if ("addressBook".equals(contacts)) {
      User user = Database.with(User.class).get(getSession(req).getUserId());
      return Articles.getArticlesContainingAddressBookContacts(user, NUM_RESULTS);
    } else {
      User user = Database.with(User.class).get(getSession(req).getUserId());
      return Articles.getRankedArticles(
          user,
          NeuralNetworkScorer.getInstance(),
          NUM_RESULTS);
    }
  }
}
