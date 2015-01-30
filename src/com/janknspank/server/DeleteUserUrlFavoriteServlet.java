package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.common.collect.ImmutableList;
import com.janknspank.bizness.UserUrlFavorites;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.Core.Session;

@AuthenticationRequired(requestMethod = "POST")
public class DeleteUserUrlFavoriteServlet extends StandardServlet {
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws RequestException, DatabaseSchemaException, NotFoundException {
    Session session = this.getSession(req);

    // Get parameters.
    String urlId = getRequiredParameter(req, "urlId");

    // Business logic.
    int numDeleted = UserUrlFavorites.deleteIds(session.getUserId(), ImmutableList.of(urlId));
    if (numDeleted == 0) {
      throw new NotFoundException("Favorite not found");
    }

    // Write response.
    JSONObject response = createSuccessResponse();
    response.put("url_id", urlId);
    return response;
  }
}
