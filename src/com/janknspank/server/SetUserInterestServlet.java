package com.janknspank.server;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.api.client.util.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.janknspank.bizness.Entities;
import com.janknspank.bizness.EntityType;
import com.janknspank.bizness.GuidFactory;
import com.janknspank.bizness.UserInterests;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.CoreProto.Entity;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.Interest.InterestSource;
import com.janknspank.proto.UserProto.Interest.InterestType;
import com.janknspank.proto.UserProto.User;

@AuthenticationRequired(requestMethod = "POST")
@ServletMapping(urlPattern = "/v1/set_user_interest")
public class SetUserInterestServlet extends StandardServlet {
  /**
   * Please see the {@code UserProto.Interest} for the required parameters.
   * Depending on the interest type, different fields are required.
   */
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws RequestException, DatabaseSchemaException, DatabaseRequestException {
    // Read parameters.
    String interestTypeParam = getRequiredParameter(req, "interest[type]");
    String interestEntityTypeParam = getParameter(req, "interest[entity][type]");
    String interestEntityKeywordParam = getParameter(req, "interest[entity][keyword]");
    String interestEntityIdParam = getParameter(req, "interest[entity][id]");
    String interestIndustryCodeParam = getParameter(req, "interest[industry_code]");
    String interestIntentCodeParam = getParameter(req, "interest[intent_code]");
    String followParam = getRequiredParameter(req, "follow");
    User user = getUser(req);

    // Parameter validation.
    InterestType interestType = InterestType.valueOf(interestTypeParam);
    if (interestType == null) {
      throw new RequestException("Parameter 'interest[type]' is invalid");
    }

    InterestSource interestSource;
    if ("true".equals(followParam)) {
      interestSource = InterestSource.USER;
    } else if ("false".equals(followParam)) {
      interestSource = InterestSource.TOMBSTONE;
    } else {
      throw new RequestException("Parameter 'follow' is invalid");
    }

    // Build the new interest
    Interest.Builder interestBuilder = Interest.newBuilder();
    interestBuilder.setId(GuidFactory.generate())
        .setType(interestType)
        .setSource(interestSource)
        .setCreateTime(System.currentTimeMillis());

    // Set type-specific properties
    if (interestType == InterestType.ADDRESS_BOOK_CONTACTS ||
        interestType == InterestType.LINKED_IN_CONTACTS) {
      // Nothing extra to save
    } else if (interestType == InterestType.ENTITY) {
      // Validate entity parameters
      Entity entity;
      if (interestEntityIdParam != null) {
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

          entity = Entity.newBuilder().setId(GuidFactory.generate())
              .setKeyword(interestEntityKeywordParam)
              .setSource(Entity.Source.USER)
              .setType(interestEntityTypeParam)
              .build();
        }
      }
      interestBuilder.setEntity(entity);
    } else if (interestType == InterestType.INDUSTRY) {
      interestBuilder.setIndustryCode(Integer.parseInt(interestIndustryCodeParam));
    } else if (interestType == InterestType.INTENT) {
      if (Strings.isNullOrEmpty(interestIntentCodeParam)) {
        throw new RequestException("Parameter 'interest[intent_code]' is missing");
      }
      interestBuilder.setIntentCode(interestIntentCodeParam);
    }

    Interest newInterest = interestBuilder.build();

    // Business logic.
    // Find all interests that have nothing to do with the specified user
    // interest.
    List<Interest> existingInterests = Lists.newArrayList();
    for (Interest interest : user.getInterestList()) {
      if (!UserInterests.equals(interest, newInterest)) {
        existingInterests.add(interest);
      }
    }

    // Write the filtered list plus a new Interest that represents the
    // parameters received from the client.
    Database.with(User.class).set(user, "interest",
        Iterables.concat(
            existingInterests,
            ImmutableList.of(newInterest)));

    // Write response.
    return createSuccessResponse();
  }
}
