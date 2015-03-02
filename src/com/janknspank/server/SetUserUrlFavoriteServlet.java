package com.janknspank.server;

import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.janknspank.bizness.Urls;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.UserProto.UrlFavorite;
import com.janknspank.proto.UserProto.User;

@AuthenticationRequired(requestMethod = "POST")
public class SetUserUrlFavoriteServlet extends StandardServlet {
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws RequestException, DatabaseSchemaException, DatabaseRequestException {
    // Read parameters.
    String urlId = getRequiredParameter(req, "url_id");
    User user = Database.with(User.class).get(getSession(req).getUserId());

    // Parameter validation.
    if (Urls.getById(urlId) == null) {
      throw new RequestException("URL does not exist");
    }

    // Business logic.
    Set<String> existingFavoriteUrlIds = Sets.newHashSet();
    for (UrlFavorite favorite : user.getUrlFavoriteList()) {
      existingFavoriteUrlIds.add(favorite.getUrlId());
    }
    if (!existingFavoriteUrlIds.contains(urlId)) {
      Database.with(User.class).push(user, "url_favorite", ImmutableList.of(
          UrlFavorite.newBuilder()
              .setUrlId(urlId)
              .setCreateTime(System.currentTimeMillis())
              .build()));
    }

    // Write response.
    return createSuccessResponse();
  }
}
