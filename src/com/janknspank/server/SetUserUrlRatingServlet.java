package com.janknspank.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import com.janknspank.data.DataInternalException;
import com.janknspank.data.Database;
import com.janknspank.data.Urls;
import com.janknspank.data.UserUrlRatings;
import com.janknspank.data.ValidationException;
import com.janknspank.proto.Core.Session;
import com.janknspank.proto.Core.UserUrlRating;
import com.janknspank.proto.Serializer;

@AuthenticationRequired(requestMethod = "POST")
public class SetUserUrlRatingServlet extends NewsServlet {
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
    UserUrlRating rating;
    try {
      // Parameter validation.
      if (Urls.getById(urlId) == null) {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        writeJson(resp, getErrorJson(urlId == null ? "Missing urlId" : "URL does not exist"));
        return;
      }

      Integer ratingScore = Ints.tryParse(getParameter(req, "rating"));
      if (ratingScore == null || ratingScore < 0 || ratingScore > 10) {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        writeJson(resp, getErrorJson("rating must be between 0 and 10, inclusive"));
        return;
      }

      // Delete any existing rating we have for this userId/urlId tuple.
      UserUrlRatings.deleteIds(session.getUserId(), ImmutableList.of(urlId));

      // Store the rating.
      rating = UserUrlRating.newBuilder()
          .setUserId(session.getUserId())
          .setUrlId(urlId)
          .setCreateTime(System.currentTimeMillis())
          .setRating(ratingScore)
          .build();
      Database.insert(rating);
    } catch (DataInternalException|ValidationException e) {
      resp.setStatus(e instanceof DataInternalException ?
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR : HttpServletResponse.SC_BAD_REQUEST);
      writeJson(resp, getErrorJson(e.getMessage()));
      return;
    }

    // Write response.
    JSONObject response = new JSONObject();
    response.put("success", true);
    response.put("rating", Serializer.toJSON(rating));
    writeJson(resp, response);
  }
}
