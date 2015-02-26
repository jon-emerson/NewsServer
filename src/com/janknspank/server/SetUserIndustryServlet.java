package com.janknspank.server;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.janknspank.classifier.IndustryCode;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.UserProto.User;
import com.janknspank.proto.UserProto.UserIndustry;

@AuthenticationRequired(requestMethod = "POST")
public class SetUserIndustryServlet extends StandardServlet {
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

    List<UserIndustry> userIndustries = Lists.newArrayList(user.getIndustryList());
    UserIndustry existingIndustry = null;
    boolean industryStateChanged = false;
    int index = 0;
    for (UserIndustry userIndustry : userIndustries) {
      if (userIndustry.getIndustryCodeId() == industryCodeId) {
        existingIndustry = userIndustry;
        if (existingIndustry.getSource() != source) {
          userIndustries.set(index, existingIndustry.toBuilder().setSource(source).build());
          industryStateChanged = true;
        }
      }
      index++;
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
      if (industryStateChanged) {
        Database.with(User.class).set(user, "industry", userIndustries);
      }
    }

    // Write response.
    return createSuccessResponse();
  }
}
