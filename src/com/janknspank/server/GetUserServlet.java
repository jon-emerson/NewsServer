package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.janknspank.data.DataInternalException;
import com.janknspank.data.Database;
import com.janknspank.proto.Core.User;
import com.janknspank.proto.Serializer;

@AuthenticationRequired
public class GetUserServlet extends StandardServlet {

  @Override
  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DataInternalException {
    User user = Database.getInstance().get(getSession(req).getUserId(), User.class);
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
