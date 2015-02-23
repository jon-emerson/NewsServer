package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.common.collect.ImmutableList;
import com.janknspank.bizness.UserInterests;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.User;

@AuthenticationRequired(requestMethod = "POST")
public class SetUserInterestServlet extends StandardServlet {
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws RequestException, DatabaseSchemaException, DatabaseRequestException {
    // Read parameters.
    String keyword = getRequiredParameter(req, "keyword");
    String type = getRequiredParameter(req, "type");
    String follow = getRequiredParameter(req, "follow");
    User user = Database.with(User.class).get(getSession(req).getUserId());

    // Parameter validation.
    if (isValidType(type)) {
      throw new RequestException("Type is not valid");
    }

    // Business logic.
    Interest.Source source;
    if (follow.equals("true")) {
      source = Interest.Source.USER;
    } else {
      source = Interest.Source.TOMBSTONE;
    }

    Interest existingInterest = null;
    for (Interest interest : user.getInterestList()) {
      if (interest.getType().equals(type) && interest.getKeyword().equals(keyword)) {
        existingInterest = interest;
        break;
      }
    }

    if (existingInterest == null) {
      Database.with(User.class).push(user, "interest", ImmutableList.of(
          Interest.newBuilder()
              .setKeyword(keyword)
              .setType(type)
              .setSource(source)
              .setCreateTime(System.currentTimeMillis())
              .build()));
    } else {
      if (existingInterest.getSource() != source) {
        Interest interestToUpdate = existingInterest.toBuilder().setSource(source).build();
        Database.update(interestToUpdate);
      }
    }

    // Write response.
    return createSuccessResponse();
  }

  private boolean isValidType(String type) {
    // 'o' for organization, 'p' for person, 'l' for location, 's' for skill.
    if (type.equals(UserInterests.TYPE_LOCATION) || type.equals(UserInterests.TYPE_ORGANIZATION) 
        || type.equals(UserInterests.TYPE_PERSON)) {
      return true;
    }
    return false;
  }
}
