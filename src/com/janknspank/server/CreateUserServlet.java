package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.janknspank.data.DataInternalException;
import com.janknspank.data.DataRequestException;
import com.janknspank.data.Sessions;
import com.janknspank.data.Users;
import com.janknspank.data.ValidationException;
import com.janknspank.proto.Core.Session;
import com.janknspank.proto.Core.User;
import com.janknspank.proto.Serializer;

public class CreateUserServlet extends StandardServlet {
  @Override
  protected JSONObject doWork(HttpServletRequest req, HttpServletResponse resp)
      throws DataInternalException, DataRequestException, ValidationException {
    // Read parameters.
    String email = getRequiredParameter(req, "email");
    String password = getRequiredParameter(req, "password");

    // Business logic.
    User user = Users.create(email, password);
    Session session = Sessions.create(email, password);

    // Create response.
    JSONObject response = createSuccessResponse();
    response.put("user", Serializer.toJSON(user));
    response.put("session", Serializer.toJSON(session));
    return response;
  }
}
