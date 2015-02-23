package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.common.collect.ImmutableList;
import com.janknspank.classifier.IndustryCode;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.UserProto.User;
import com.janknspank.proto.UserProto.UserIndustry;

public class SetUserIndustriesServlet extends StandardServlet {
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws RequestException, DatabaseSchemaException, DatabaseRequestException {
    // Read parameters.
    int industryCodeId = Integer.parseInt(getRequiredParameter(req, "industry_id"));
    String follow = getRequiredParameter(req, "follow");
    User user = Database.with(User.class).get(getSession(req).getUserId());

    // Parameter validation.
    IndustryCode industryCode = IndustryCode.fromId(industryCodeId);
    if (industryCode == null) {
      throw new RequestException("Industry id is not valid");
    }

    // Business logic.
    UserIndustry.Source source;
    source = ("true".equals(follow)) ? UserIndustry.Source.USER : UserIndustry.Source.TOMBSTONE;

    UserIndustry existingIndustry = null;
    for (UserIndustry userIndustry : user.getIndustryList()) {
      if (userIndustry.getIndustryCodeId() == industryCodeId) {
        existingIndustry = userIndustry;
        break;
      }
    }

    if (existingIndustry == null) {
      Database.with(User.class).push(user, "industry", ImmutableList.of(
          UserIndustry.newBuilder()
              .setIndustryCodeId(industryCodeId)
              .setRelationship(UserIndustry.Relationship.DESIRED_INDUSTRY)
              .setSource(source)
              .setCreateTime(System.currentTimeMillis())
              .build()));
    } else {
      if (existingIndustry.getSource() != source) {
        UserIndustry industryToUpdate = existingIndustry.toBuilder().setSource(source).build();
        Database.update(industryToUpdate);
      }
    }

    // Write response.
    return createSuccessResponse();
  }
}
