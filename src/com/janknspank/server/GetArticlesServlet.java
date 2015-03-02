package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;

import com.janknspank.bizness.Articles;
import com.janknspank.bizness.Industry;
import com.janknspank.bizness.Intent;
import com.janknspank.classifier.FeatureId;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.CoreProto.Entity;
import com.janknspank.proto.UserProto.Interest.InterestType;
import com.janknspank.proto.UserProto.User;
import com.janknspank.rank.NeuralNetworkScorer;

@AuthenticationRequired
public class GetArticlesServlet extends AbstractArticlesServlet {
  private static final int NUM_RESULTS = 50;

  @Override
  protected Iterable<Article> getArticles(HttpServletRequest req) throws DatabaseSchemaException {
    String featureId = this.getParameter(req, "feature_id");
    String industryCodeId = this.getParameter(req, "industry_code");
    String contacts = this.getParameter(req, "contacts");
    String entityKeyword = this.getParameter(req, "entity_keyword");
    String entityType = this.getParameter(req, "entity_type");
    String intentCode = this.getParameter(req, "intent_code");

    if (featureId != null) {
      return Articles.getArticlesForFeature(
          FeatureId.fromId(Integer.parseInt(featureId)),
          NUM_RESULTS);
    } else if (industryCodeId != null) {
      return Articles.getArticlesForFeature(
          Industry.fromCode(Integer.parseInt(industryCodeId)).getFeatureId(),
          NUM_RESULTS);
    } else if ("linked_in".equals(contacts)) {
      User user = Database.with(User.class).get(getSession(req).getUserId());
      return Articles.getArticlesForContacts(user, InterestType.LINKED_IN_CONTACTS, NUM_RESULTS);
    } else if ("address_book".equals(contacts)) {
      User user = Database.with(User.class).get(getSession(req).getUserId());
      return Articles.getArticlesForContacts(user, InterestType.ADDRESS_BOOK_CONTACTS, NUM_RESULTS);
    } else if (entityKeyword != null) {
      Entity entity = Entity.newBuilder().setKeyword(entityKeyword).setType(entityType).build();
      return Articles.getArticlesForEntity(entity, NUM_RESULTS);
    } else if (Intent.START_COMPANY.getCode().equals(intentCode)) {
      // TODO: fix this so its the right feature for the user's industries
      return Articles.getArticlesForFeature(
          FeatureId.fromId(20000), NUM_RESULTS);
    } else {
      User user = Database.with(User.class).get(getSession(req).getUserId());
      return Articles.getRankedArticles(
          user,
          NeuralNetworkScorer.getInstance(),
          NUM_RESULTS);
    }
  }
}
