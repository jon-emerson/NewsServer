package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.GuidFactory;
import com.janknspank.bizness.Sessions;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.Serializer;
import com.janknspank.proto.CoreProto.Session;
import com.janknspank.proto.UserProto.User;
import com.janknspank.proto.UserProto.User.AuthenticationService;

@ServletMapping(urlPattern = "/v1/anonymous_login")
public class AnonymousLoginServlet extends StandardServlet {
  @Override
  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws RequestException {
    throw new RequestException("POSTs only");
  }

  private User createAnonymousUser() throws DatabaseRequestException, DatabaseSchemaException {
    User user = User.newBuilder()
        .setId(GuidFactory.generate())
        .setCreateTime(System.currentTimeMillis())
        .setLastLoginTime(System.currentTimeMillis())
        .addLast5AppUseTime(System.currentTimeMillis())
        .setOriginalAuthenticationService(AuthenticationService.ANONYMOUS)
        .build();
    Database.insert(user);
    return user;
  }

  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DatabaseRequestException, DatabaseSchemaException, BiznessException {
    User user = createAnonymousUser();
    Session session = Sessions.createForUser(user);

    // Create the response.
    JSONObject response = this.createSuccessResponse();
    response.put("user", new UserHelper(user).getUserJson());
    response.put("session", Serializer.toJSON(session));
    response.put("articles", new JSONArray());

    return response;
  }
}
