package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.CoreProto.Session;

@AuthenticationRequired(requestMethod = "POST")
public class LogoutServlet extends StandardServlet {
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DatabaseSchemaException, RequestException {
    Database.with(Session.class).delete(
        new QueryOption.WhereEquals("session_key", getRequiredParameter(req, "session_key")));
    return createSuccessResponse();
  }
}
