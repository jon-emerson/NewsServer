package com.janknspank.server;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableList;
import com.janknspank.bizness.GuidFactory;
import com.janknspank.bizness.Industry;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.Interest.InterestSource;
import com.janknspank.proto.UserProto.Interest.InterestType;
import com.janknspank.proto.UserProto.User;

@AuthenticationRequired(requestMethod = "POST")
public class SetUserIndustryServlet extends StandardServlet {
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws RequestException, DatabaseSchemaException, DatabaseRequestException {
    // Read parameters.
    int industryCode = Integer.parseInt(getRequiredParameter(req, "industry_code"));
    String follow = getRequiredParameter(req, "follow");
    User user = Database.with(User.class).get(getSession(req).getUserId());

    // Parameter validation.
    if (Industry.fromCode(industryCode) == null) {
      throw new RequestException("industryCode is not valid");
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
          && interest.getIndustryCode() == industryCode) {
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
              .setIndustryCode(industryCode)
              .setSource(InterestSource.USER)
              .setCreateTime(System.currentTimeMillis())
              .build()));
    } else if (interestsChanged) {
      Database.with(User.class).set(user, "interest", interests);
    }

    // Write response.
    return createSuccessResponse();
  }
}
