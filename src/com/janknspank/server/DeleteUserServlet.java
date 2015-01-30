package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.janknspank.bizness.Sessions;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.Core.User;

@AuthenticationRequired(requestMethod = "POST")
public class DeleteUserServlet extends StandardServlet {
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DatabaseSchemaException, DatabaseRequestException {
    User user = Database.with(User.class).get(getSession(req).getUserId());
    Sessions.deleteAllFromUser(user);
    Database.delete(user);

    // Create response.
    JSONObject response = createSuccessResponse();
    response.put("user_id", user.getId());
    return response;
  }
}
