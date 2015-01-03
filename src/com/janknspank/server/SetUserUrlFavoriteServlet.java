package com.janknspank.server;

import java.io.IOException;

import javax.servlet.ServletException;
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
public class SetUserUrlFavoriteServlet extends NewsServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    writeSoyTemplate(resp, ".main", null);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    // Read parameters.
    String urlId = getParameter(req, "urlId");
    Session session = this.getSession(req);

    // Business logic.
    UserUrlFavorite favorite;
    try {
      // Parameter validation.
      if (Urls.getById(urlId) == null) {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        writeJson(resp, getErrorJson(urlId == null ? "Missing urlId" : "URL does not exist"));
        return;
      }

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
    } catch (DataInternalException|ValidationException e) {
      resp.setStatus(e instanceof DataInternalException ?
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR : HttpServletResponse.SC_BAD_REQUEST);
      writeJson(resp, getErrorJson(e.getMessage()));
      return;
    }

    // Write response.
    JSONObject response = new JSONObject();
    response.put("success", true);
    response.put("favorite", Serializer.toJSON(favorite));
    writeJson(resp, response);
  }
}
