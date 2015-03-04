package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.UserProto.User;

@AuthenticationRequired
public class GetUserServlet extends StandardServlet {

  @Override
  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DatabaseSchemaException {
    User user = getUser(req);

    // Create the response.
    JSONObject response = this.createSuccessResponse();
    response.put("user", new UserHelper(user).getUserJson());
    return response;
  }
}
