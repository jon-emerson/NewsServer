package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.janknspank.data.DataInternalException;
import com.janknspank.data.DataRequestException;
import com.janknspank.data.Database;
import com.janknspank.data.Sessions;
import com.janknspank.data.Users;
import com.janknspank.data.ValidationException;
import com.janknspank.proto.Core.User;

public class DeleteUserServlet extends StandardServlet {
  @Override
  protected JSONObject doWork(HttpServletRequest req, HttpServletResponse resp)
      throws DataInternalException, DataRequestException, ValidationException {
    // Read parameters.
    String email = getRequiredParameter(req, "email");

    // Business logic.
    User user = Users.getByEmail(email);
    if (user == null) {
      throw new DataRequestException("User not found");
    }
    Sessions.deleteAllFromUser(user);
    Database.delete(user);

    // Create response.
    JSONObject response = createSuccessResponse();
    response.put("user_id", user.getId());
    return response;
  }
}
