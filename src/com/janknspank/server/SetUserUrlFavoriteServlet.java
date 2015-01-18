package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.janknspank.data.DataInternalException;
import com.janknspank.data.Database;
import com.janknspank.data.Urls;
import com.janknspank.data.UserUrlFavorites;
import com.janknspank.data.ValidationException;
import com.janknspank.proto.Core.Session;
import com.janknspank.proto.Core.UserUrlFavorite;
import com.janknspank.proto.Serializer;

@AuthenticationRequired(requestMethod = "POST")
public class SetUserUrlFavoriteServlet extends StandardServlet {
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DataInternalException, ValidationException {
    // Read parameters.
    String urlId = getRequiredParameter(req, "urlId");
    Session session = this.getSession(req);

    // Parameter validation.
    if (Urls.getById(urlId) == null) {
      throw new ValidationException("URL does not exist");
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
      Database.getInstance().insert(favorite);
    }

    // Write response.
    JSONObject response = createSuccessResponse();
    response.put("favorite", Serializer.toJSON(favorite));
    return response;
  }
}
