package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.janknspank.data.DataInternalException;
import com.janknspank.data.Database;
import com.janknspank.data.ValidationException;
import com.janknspank.proto.Core.Session;

@AuthenticationRequired(requestMethod = "POST")
public class LogoutServlet extends StandardServlet {
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws ValidationException, NotFoundException {
    try {
      Database.deletePrimaryKey(getRequiredParameter(req, "sessionKey"), Session.class);
    } catch (DataInternalException e) {
      throw new NotFoundException("Session does not exist");
    }
    return createSuccessResponse();
  }
}
