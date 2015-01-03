package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.common.base.Strings;
import com.janknspank.data.DataInternalException;
import com.janknspank.data.Database;
import com.janknspank.data.Sessions;
import com.janknspank.data.Users;
import com.janknspank.data.ValidationException;
import com.janknspank.proto.Core.Session;

public class LogoutServlet extends StandardServlet {
  @Override
  protected JSONObject doWork(HttpServletRequest req, HttpServletResponse resp)
      throws DataInternalException, ValidationException {
    // Read parameters.
    String email = getParameter(req, "email");
    String sessionKey = getParameter(req, "sessionKey");

    // Parameter validation.
    if (!(Strings.isNullOrEmpty(email) ^ Strings.isNullOrEmpty(sessionKey))) {
      throw new ValidationException("You must only specify email or sessionKey, not both");
    }

    // Business logic.
    int rowsAffected = 0;
    if (!Strings.isNullOrEmpty(email)) {
      rowsAffected = Sessions.deleteAllFromUser(Users.getByEmail(email));
    } else {
      Database.deletePrimaryKey(sessionKey, Session.class);
      rowsAffected++;
    }

    // Create response.
    JSONObject response = createSuccessResponse();
    response.put("rowsAffected", rowsAffected);
    return response;
  }
}
