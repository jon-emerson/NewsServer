package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.UserProto.UrlFavorite;
import com.janknspank.proto.UserProto.User;

@AuthenticationRequired(requestMethod = "POST")
@ServletMapping(urlPattern = "/v1/delete_user_url_favorite")
public class DeleteUserUrlFavoriteServlet extends StandardServlet {
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DatabaseRequestException, DatabaseSchemaException, RequestException {

    // Get parameters.
    final String urlId = getRequiredParameter(req, "url_id");

    // Business logic.
    User user = getUser(req);
    user = Database.with(User.class).set(user, "url_favorite", Iterables.filter(
        user.getUrlFavoriteList(),
        new Predicate<UrlFavorite>() {
          @Override
          public boolean apply(UrlFavorite urlFavorite) {
            return !urlFavorite.getUrlId().equals(urlId);
          }
        }));

    // Write response.
    JSONObject response = createSuccessResponse();
    response.put("user", user);
    return response;
  }
}
