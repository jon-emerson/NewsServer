package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.Serializer;
import com.janknspank.proto.Core.User;

@AuthenticationRequired
public class GetUserServlet extends StandardServlet {

  @Override
  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DatabaseSchemaException {
    User user = Database.with(User.class).get(getSession(req).getUserId());
    JSONObject userJson = Serializer.toJSON(user);

    UserHelper userHelper = new UserHelper(user);
    userJson.put("ratings", userHelper.getRatingsJsonArray());
    userJson.put("favorites", userHelper.getFavoritesJsonArray());
    userJson.put("interests", userHelper.getInterestsJsonArray());

    // Create response.
    JSONObject response = createSuccessResponse();
    response.put("user", userJson);
    return response;
  }
}
