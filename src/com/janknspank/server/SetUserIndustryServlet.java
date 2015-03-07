package com.janknspank.server;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableList;
import com.janknspank.bizness.GuidFactory;
import com.janknspank.bizness.Industry;
import com.janknspank.classifier.FeatureId;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.Interest.InterestSource;
import com.janknspank.proto.UserProto.Interest.InterestType;
import com.janknspank.proto.UserProto.User;

/**
 * Toggles whether a user is following a specific industry, based on whether
 * the "follow" parameter is true or false.
 */
@AuthenticationRequired(requestMethod = "POST")
@ServletMapping(urlPattern = "/v1/set_user_industry")
public class SetUserIndustryServlet extends StandardServlet {
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws RequestException, DatabaseSchemaException, DatabaseRequestException {
    // Read parameters.
    String follow = getRequiredParameter(req, "follow");

    // Industry feature ID: Historically this was passed as an industry_code.
    // We've moved it away from being a LinkedIn ID, it's now our own invention.
    // But to keep the client working, we accept either industry_code or id.
    int featureIdValue;
    String industryCodeStr = getParameter(req, "industry_code");
    if (industryCodeStr != null) {
      featureIdValue = Integer.parseInt(industryCodeStr);
    } else {
      featureIdValue = Integer.parseInt(getRequiredParameter(req, "id"));
    }

    User user = getUser(req);

    // Parameter validation.
    FeatureId featureId = FeatureId.fromId(featureIdValue);
    if (featureId == null) {
      // See if it's a LinkedIn industry code.
      Industry industry = Industry.fromCode(featureIdValue);
      if (industry != null) {
        featureId = industry.getFeatureId();
        featureIdValue = featureId.getId();
      } else {
        throw new RequestException("Industry feature ID (id) is not valid");
      }
    }

    // Business logic.
    InterestSource source = ("true".equals(follow))
        ? InterestSource.USER : InterestSource.TOMBSTONE;

    List<Interest> interests = Lists.newArrayList(user.getInterestList());
    Interest existingIndustryInterest = null;
    boolean interestsChanged = false;
    int index = 0;
    for (Interest interest : interests) {
      if (interest.getType() == InterestType.INDUSTRY
          && interest.getIndustryCode() == featureIdValue) {
        existingIndustryInterest = interest;
        if (interest.getSource() != source) {
          interests.set(index, interest.toBuilder().setSource(source).build());
          interestsChanged = true;
        }
      }
      index++;
    }

    if (existingIndustryInterest == null) {
      Database.with(User.class).push(user, "interest", ImmutableList.of(
          Interest.newBuilder()
              .setId(GuidFactory.generate())
              .setType(InterestType.INDUSTRY)
              .setIndustryCode(featureIdValue)
              .setSource(InterestSource.USER)
              .setCreateTime(System.currentTimeMillis())
              .build()));
    } else if (interestsChanged) {
      Database.with(User.class).set(user, "interest", interests);
    }

    // Write response.
    return createSuccessResponse();
  }

  /**
   * Helper method for now: Switches industry_code values on user interests from
   * LinkedIn industry codes to our Industry feature IDs.  Probably never want
   * to run this after March 6th, when I wrote this.
   */
  public static void main(String args[]) throws DatabaseSchemaException, DatabaseRequestException {
    for (User user : Database.with(User.class).get()) {
      boolean userIsDirty = false;
      List<Interest> interests = Lists.newArrayList(user.getInterestList());
      for (int i = 0; i < interests.size(); i++) {
        Interest.Builder interestBuilder = interests.get(i).toBuilder();
        if (interestBuilder.getType() == InterestType.INDUSTRY) {
          Industry industry = Industry.fromCode(interestBuilder.getIndustryCode());
          if (industry != null) {
            interestBuilder.setIndustryCode(industry.getFeatureId().getId());
            interests.set(i, interestBuilder.build());
            userIsDirty = true;
          }
        }
      }
      if (userIsDirty) {
        System.out.println("Updating user...");
        Database.with(User.class).set(user, "interest", interests);
      }
    }
  }
}
