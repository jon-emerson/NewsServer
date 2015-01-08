package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.janknspank.data.DataInternalException;
import com.janknspank.data.DataRequestException;
import com.janknspank.data.Database;
import com.janknspank.data.Sessions;
import com.janknspank.data.ValidationException;
import com.janknspank.proto.Core.User;

@AuthenticationRequired(requestMethod = "POST")
public class DeleteUserServlet extends StandardServlet {
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DataInternalException, DataRequestException, ValidationException {
    User user = Database.get(getSession(req).getUserId(), User.class);
    Sessions.deleteAllFromUser(user);
    Database.delete(user);

    // Create response.
    JSONObject response = createSuccessResponse();
    response.put("user_id", user.getId());
    return response;
  }
}
