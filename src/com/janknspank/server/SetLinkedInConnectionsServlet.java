package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.janknspank.data.DataInternalException;
import com.janknspank.data.Database;
import com.janknspank.data.ValidationException;
import com.janknspank.proto.Core.LinkedInConnections;

@AuthenticationRequired(requestMethod = "POST")
public class SetLinkedInConnectionsServlet extends StandardServlet {
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DataInternalException, ValidationException {
    LinkedInConnections linkedInConnections = LinkedInConnections.newBuilder()
        .setUserId(this.getSession(req).getUserId())
        .setData(getRequiredParameter(req, "data"))
        .setCreateTime(System.currentTimeMillis())
        .build();
    Database.upsert(linkedInConnections);
    return createSuccessResponse();
  }
}
