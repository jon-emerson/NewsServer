package com.janknspank.server;

import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.common.collect.Lists;
import com.janknspank.bizness.GuidFactory;
import com.janknspank.bizness.Intent;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.Interest.InterestSource;
import com.janknspank.proto.UserProto.Interest.InterestType;
import com.janknspank.proto.UserProto.User;

@AuthenticationRequired(requestMethod = "POST")
@ServletMapping(urlPattern = "/v1/set_user_intent")
public class SetUserIntentServlet extends StandardServlet {
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws RequestException, DatabaseSchemaException, DatabaseRequestException {
    // Read parameters.
    String intentCode = getRequiredParameter(req, "code");
    boolean enabled = Boolean.parseBoolean(getRequiredParameter(req, "enabled"));
    User user = getUser(req);

    // Parameter validation.
    if (Intent.fromCode(intentCode) == null) {
      throw new RequestException("Intent code is not valid");
    }

    // Business logic.
    List<Interest> interests = Lists.newArrayList(user.getInterestList());
    Iterator<Interest> iterator = interests.iterator();
    Interest existingInterest = null;
    boolean intentsChanged = false;
    while (iterator.hasNext()) {
      Interest interest = iterator.next();
      if (interest.getType() == InterestType.INTENT
          && interest.getIntentCode().equals(intentCode)) {
        existingInterest = interest;
        if (!enabled) {
          iterator.remove();
          intentsChanged = true;
        }
      }
    }

    if (existingInterest == null && enabled) {
      interests.add(Interest.newBuilder()
          .setId(GuidFactory.generate())
          .setType(InterestType.INTENT)
          .setIntentCode(intentCode)
          .setSource(InterestSource.USER)
          .setCreateTime(System.currentTimeMillis())
          .build());
      intentsChanged = true;
    }

    if (intentsChanged) {
      Database.with(User.class).set(user, "interest", interests);
    }

    // Write response.
    return createSuccessResponse();
  }
}
