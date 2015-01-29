package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import com.janknspank.bizness.Urls;
import com.janknspank.bizness.UserUrlRatings;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.Serializer;
import com.janknspank.proto.Core.Session;
import com.janknspank.proto.Core.UserUrlRating;

@AuthenticationRequired(requestMethod = "POST")
public class SetUserUrlRatingServlet extends StandardServlet {
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
    UserUrlRating rating;
    Integer ratingScore = Ints.tryParse(getParameter(req, "rating"));
    if (ratingScore == null || ratingScore < 0 || ratingScore > 10) {
      throw new RequestException("rating must be between 0 and 10, inclusive");
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

    // Create response.
    JSONObject response = createSuccessResponse();
    response.put("rating", Serializer.toJSON(rating));
    return response;
  }
}
