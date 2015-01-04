package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.janknspank.data.DataInternalException;
import com.janknspank.data.Database;
import com.janknspank.data.ValidationException;
import com.janknspank.proto.Core.LinkedInProfile;

@AuthenticationRequired(requestMethod = "POST")
public class SetLinkedInProfileServlet extends StandardServlet {
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DataInternalException, ValidationException {
    LinkedInProfile linkedInProfile = LinkedInProfile.newBuilder()
        .setUserId(this.getSession(req).getUserId())
        .setData(getRequiredParameter(req, "data"))
        .setCreateTime(System.currentTimeMillis())
        .build();
    Database.upsert(linkedInProfile);
    return createSuccessResponse();
  }
}
