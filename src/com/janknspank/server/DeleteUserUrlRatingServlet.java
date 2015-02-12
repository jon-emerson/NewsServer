package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.janknspank.bizness.Urls;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.CoreProto.Url;
import com.janknspank.proto.UserProto.UrlRating;
import com.janknspank.proto.UserProto.User;

@AuthenticationRequired(requestMethod = "POST")
public class DeleteUserUrlRatingServlet extends StandardServlet {
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DatabaseSchemaException, DatabaseRequestException, RequestException {

    // Get parameters.
    final String urlId = getRequiredParameter(req, "urlId");
    final Url articleUrl = Urls.getById(urlId);
    if (articleUrl == null) {
      throw new RequestException("URL does not exist");
    }
    
    // Business logic.
    User user = Database.with(User.class).get(getSession(req).getUserId());
    user = Database.with(User.class).set(user, "url_rating", Iterables.filter(
        user.getUrlRatingList(),
        new Predicate<UrlRating>() {
          @Override
          public boolean apply(UrlRating urlRating) {
            return !urlRating.getUrl().equals(articleUrl.getUrl());
          }
        }));

    // Write response.
    JSONObject response = createSuccessResponse();
    response.put("user", user);
    return response;
  }
}
