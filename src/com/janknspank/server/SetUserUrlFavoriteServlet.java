package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.janknspank.bizness.Urls;
import com.janknspank.bizness.UserUrlFavorites;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.Serializer;
import com.janknspank.proto.Core.Session;
import com.janknspank.proto.Core.UserUrlFavorite;

@AuthenticationRequired(requestMethod = "POST")
public class SetUserUrlFavoriteServlet extends StandardServlet {
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws RequestException, DatabaseSchemaException, DatabaseRequestException {
    // Read parameters.
    String urlId = getRequiredParameter(req, "urlId");
    Session session = this.getSession(req);

    // Parameter validation.
    if (Urls.getById(urlId) == null) {
      throw new RequestException("URL does not exist");
    }

    // Business logic.
    UserUrlFavorite favorite;

    // Make sure that the user hasn't already favorited this.
    favorite = UserUrlFavorites.get(session.getUserId(), urlId);
    if (favorite == null) {
      favorite = UserUrlFavorite.newBuilder()
          .setUserId(session.getUserId())
          .setUrlId(urlId)
          .setCreateTime(System.currentTimeMillis())
          .build();
      Database.insert(favorite);
    }

    // Write response.
    JSONObject response = createSuccessResponse();
    response.put("favorite", Serializer.toJSON(favorite));
    return response;
  }
}
