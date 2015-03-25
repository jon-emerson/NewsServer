package com.janknspank.server;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.api.client.util.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.janknspank.bizness.Articles;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.Entities;
import com.janknspank.bizness.EntityType;
import com.janknspank.bizness.GuidFactory;
import com.janknspank.bizness.UserInterests;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.Serializer;
import com.janknspank.proto.CoreProto.Entity;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.Interest.InterestSource;
import com.janknspank.proto.UserProto.Interest.InterestType;
import com.janknspank.proto.UserProto.User;
import com.janknspank.rank.NeuralNetworkScorer;

@AuthenticationRequired(requestMethod = "POST")
@ServletMapping(urlPattern = "/v1/set_user_interest")
public class SetUserInterestServlet extends StandardServlet {
  /**
   * Please see the {@code UserProto.Interest} for the required parameters.
   * Depending on the interest type, different fields are required.
   */
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws RequestException, DatabaseSchemaException, DatabaseRequestException, JSONException, BiznessException {
    // Read parameters.
    String interestTypeParam = getRequiredParameter(req, "interest[type]");
    String interestEntityTypeParam = getParameter(req, "interest[entity][type]");
    String interestEntityKeywordParam = getParameter(req, "interest[entity][keyword]");
    String interestEntityIdParam = getParameter(req, "interest[entity][id]");
    String interestIndustryCodeParam = getParameter(req, "interest[industry_code]");
    String interestIntentCodeParam = getParameter(req, "interest[intent_code]");
    boolean followParam = "true".equals(getRequiredParameter(req, "follow"));

    // Parameter validation.
    InterestType interestType = InterestType.valueOf(interestTypeParam);
    if (interestType == null) {
      throw new RequestException("Parameter 'interest[type]' is invalid");
    }

    // Business logic.
    User user = getUser(req);

    // Build the new interest.
    Interest.Builder interestBuilder = Interest.newBuilder()
        .setId(GuidFactory.generate())
        .setType(interestType)
        .setSource(followParam ? InterestSource.USER : InterestSource.TOMBSTONE)
        .setCreateTime(System.currentTimeMillis());

    // Set type-specific properties
    switch (interestType) {
      case ADDRESS_BOOK_CONTACTS:
      case LINKED_IN_CONTACTS:
        // Nothing extra to save
        break;

      case ENTITY:
        Entity entity;
        if (followParam) {
          if (!Strings.isNullOrEmpty(interestEntityIdParam)) {
            entity = Entities.getEntityById(interestEntityIdParam);
            if (entity == null) {
              throw new RequestException("Parameter 'interest[entity][id]' is invalid");
            }
          } else {
            if (interestEntityKeywordParam == null) {
              throw new RequestException("Parameter 'interest[entity][keyword]' is missing");
            }
            entity = Entities.getEntityByKeyword(interestEntityKeywordParam);
            if (entity == null) {
              EntityType entityType = EntityType.fromValue(interestEntityTypeParam);
              if (entityType == null) {
                throw new RequestException("Parameter 'interest[entity][type]' is invalid");
              }

              entity = Entity.newBuilder()
                  .setId(GuidFactory.generate())
                  .setKeyword(interestEntityKeywordParam)
                  .setSource(Entity.Source.USER)
                  .setType(interestEntityTypeParam)
                  .build();
            }
          }
        } else {
          entity = Entity.newBuilder()
              .setId(GuidFactory.generate())
              .setKeyword(interestEntityKeywordParam)
              .setSource(Entity.Source.USER)
              .setType(interestEntityTypeParam)
              .build();
        }
        interestBuilder.setEntity(entity);
        break;

      case INDUSTRY:
        interestBuilder.setIndustryCode(Integer.parseInt(interestIndustryCodeParam));
        break;

      case INTENT:
        if (Strings.isNullOrEmpty(interestIntentCodeParam)) {
          throw new RequestException("Parameter 'interest[intent_code]' is missing");
        }
        interestBuilder.setIntentCode(interestIntentCodeParam);
        break;
    }

    Interest newInterest = interestBuilder.build();

    // Find all interests that are not equivalent to the specified user
    // interest.
    List<Interest> existingInterests = Lists.newArrayList();
    for (Interest interest : user.getInterestList()) {
      if (!UserInterests.equivalent(interest, newInterest)) {
        existingInterests.add(interest);
      }
    }

    // Write the filtered list plus a new Interest that represents the
    // parameters received from the client.
    Database.with(User.class).set(user, "interest",
        Iterables.concat(
            existingInterests,
            ImmutableList.of(newInterest)));

    // Create the response.
    JSONObject response = this.createSuccessResponse();
    response.put("user", new UserHelper(user).getUserJson());

    // To help with client latency, return the articles for the user's home
    // screen in this response.
    response.put("articles", Serializer.toJSON(Articles.getRankedArticles(
        user,
        NeuralNetworkScorer.getInstance(),
        GetArticlesServlet.NUM_RESULTS)));

    return response;
  }
}
