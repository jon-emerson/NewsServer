package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.janknspank.data.DataInternalException;
import com.janknspank.data.Database;
import com.janknspank.data.UserInterests;
import com.janknspank.data.ValidationException;
import com.janknspank.proto.Core.LinkedInProfile;

@AuthenticationRequired(requestMethod = "POST")
public class SetLinkedInProfileServlet extends StandardServlet {
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DataInternalException, ValidationException {
    String userId = this.getSession(req).getUserId();
    LinkedInProfile linkedInProfile = LinkedInProfile.newBuilder()
        .setUserId(userId)
        .setData(getRequiredParameter(req, "data"))
        .setCreateTime(System.currentTimeMillis())
        .build();
    Database.upsert(linkedInProfile);
    UserInterests.updateInterests(userId, linkedInProfile);
    return createSuccessResponse();
  }

  public static void main(String args[]) throws Exception {
    for (String userId : new String[] {
        "vWxNTAAKB-KYAEofUGJL4A",
        "o0Sr9HzgxZMUVcUi09NIhg"}) {
      LinkedInProfile linkedInProfile = Database.get(userId, LinkedInProfile.class);
      UserInterests.updateInterests(userId, linkedInProfile);
    }
  }
}
