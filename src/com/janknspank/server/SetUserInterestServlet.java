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
import com.janknspank.bizness.Industry;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.CoreProto.Entity;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.Interest.InterestSource;
import com.janknspank.proto.UserProto.Interest.InterestType;
import com.janknspank.proto.UserProto.User;

@AuthenticationRequired(requestMethod = "POST")
public class SetUserInterestServlet extends StandardServlet {
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws RequestException, DatabaseSchemaException, DatabaseRequestException {
    // Read parameters.
    String interestTypeString = getRequiredParameter(req, "interest[type]");
    String entityTypeString = getParameter(req, "interest[entity][type]");
    String entityKeyword = getParameter(req, "interest[entity][keyword]");
    String entityId = getParameter(req, "interest[entity][id]");
    String entitySourceString = getParameter(req, "interest[entity][source]");
    String industryCode = getParameter(req, "interest[industry_code]");
    String intentCode = getParameter(req, "interest[intent_code]");
    String following = getRequiredParameter(req, "follow");
    User user = Database.with(User.class).get(getSession(req).getUserId());

    // Parameter validation.
    InterestType interestType = InterestType.valueOf(interestTypeString);
    if (interestType == null) {
      throw new RequestException("Parameter 'interest[type]' is invalid");
    }

    InterestSource interestSource;
    if ("true".equals(following)) {
      interestSource = InterestSource.USER;
    } else if ("false".equals(following)) {
      interestSource = InterestSource.TOMBSTONE;
    } else {
      throw new RequestException("Parameter 'following' is invalid");
    }
    
    // Build the new interest
    Interest.Builder interestBuilder = Interest.newBuilder();
    interestBuilder.setType(interestType)
        .setSource(interestSource)
        .setCreateTime(System.currentTimeMillis());
    
    // Set type-specific properties
    if (interestType == InterestType.ADDRESS_BOOK_CONTACTS ||
        interestType == InterestType.LINKED_IN_CONTACTS) {
      // Nothing extra to save
    } else if (interestType == InterestType.ENTITY) {
      // Validate entity parameters
      Entity entity;
      if (entityId != null) {
        entity = Entities.getEntityById(entityId);
        if (entity == null) {
          throw new RequestException("Parameter 'interest[entity][id]' is invalid");
        }
      } else {
        if (entityKeyword == null) {
          throw new RequestException("Parameter 'interest[entity][keyword]' is missing");
        }
        entity = Entities.getEntityByKeyword(entityKeyword);
        if (entity == null) {
          EntityType entityType = EntityType.valueOf(entityTypeString);
          if (entityType == null) {
            throw new RequestException("Parameter 'interest[entity][type]' is invalid");
          }

          Entity.Source entitySource = Entity.Source.valueOf(entitySourceString);
          // TODO: Create a new entity if possible
          System.out.println("TODO: create a new entity from the setUserInterest parameters");
        }
      }
      interestBuilder.setEntity(entity);
    } else if (interestType == InterestType.INDUSTRY) {
      interestBuilder.setIndustryCode(Integer.parseInt(industryCode));
    } else if (interestType == InterestType.INTENT) {
      if (Strings.isNullOrEmpty(intentCode)) {
        throw new RequestException("Parameter 'interest[intent_code]' is missing");
      }
      interestBuilder.setIntentCode(intentCode);
    }

    Interest newInterest = interestBuilder.build();

    // Business logic.
    // Find all interests that have nothing to do with the specified user
    // interest.
    List<Interest> existingInterests = Lists.newArrayList();
    for (Interest interest : user.getInterestList()) {
      if (equals(interest, newInterest)) {
        continue;
      }
      existingInterests.add(interest);
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
  
  /**
   * Returns if the two interests are about the same thing, but not if they are from
   * the same source
   */
  private boolean equals(Interest interest1, Interest interest2) {
    if (interest1.getType() == interest2.getType()) {
      if (interest1.getType() == InterestType.ADDRESS_BOOK_CONTACTS ||
          interest1.getType() == InterestType.LINKED_IN_CONTACTS) {
        return true;
      } else if (interest1.getType() == InterestType.ENTITY) {
        return interest1.getEntity().equals(interest2.getEntity());
      } else if (interest1.getType() == InterestType.INDUSTRY) {
        return interest1.getIndustryCode() == interest2.getIndustryCode();
      } else if (interest1.getType() == InterestType.INTENT) {
        return interest1.getIntentCode() == interest2.getIntentCode();
      }
    }
    return false;
  }
}
