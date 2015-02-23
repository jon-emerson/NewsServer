package com.janknspank.server;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.common.collect.ImmutableList;
import com.janknspank.bizness.GuidFactory;
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
    if (!isValidType(type)) {
      throw new RequestException("Type is not valid");
    }

    // Business logic.
    Interest.Source source;
    if ("true".equals(follow)) {
      source = Interest.Source.USER;
    } else {
      source = Interest.Source.TOMBSTONE;
    }

    boolean isExistingInterest = false;
    List<Interest> interests = user.getInterestList();
    int index = 0;
    for (Interest interest : interests) {
      if (interest.getType().equals(type) && interest.getKeyword().equals(keyword)) {
        if (interest.getSource() != source) {
          interests.set(index, interest.toBuilder().setSource(source).build());
        }
        isExistingInterest = true;
      }
      index++;
    }

    if (!isExistingInterest) {
      Database.with(User.class).push(user, "interest", ImmutableList.of(
          Interest.newBuilder()
              .setId(GuidFactory.generate())
              .setKeyword(keyword)
              .setType(type)
              .setSource(source)
              .setCreateTime(System.currentTimeMillis())
              .build()));
    } else {
      Database.with(User.class).set(user, "interest", interests);
    }

    // Write response.
    return createSuccessResponse();
  }

  private boolean isValidType(String type) {
    // 'o' for organization, 'p' for person, 'l' for location, 's' for skill.
    return (UserInterests.TYPE_LOCATION.equals(type) || UserInterests.TYPE_ORGANIZATION.equals(type) 
        || UserInterests.TYPE_PERSON.equals(type));
  }
}
