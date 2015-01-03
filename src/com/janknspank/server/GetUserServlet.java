package com.janknspank.server;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.janknspank.data.DataInternalException;
import com.janknspank.data.Database;
import com.janknspank.data.UserUrlFavorites;
import com.janknspank.data.UserUrlRatings;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.Session;
import com.janknspank.proto.Core.User;
import com.janknspank.proto.Core.UserUrlFavorite;
import com.janknspank.proto.Core.UserUrlRating;
import com.janknspank.proto.Serializer;

@AuthenticationRequired
public class GetUserServlet extends NewsServlet {

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    Session session = getSession(req);

    JSONObject userJson;
    try {
      User user = Database.get(session.getUserId(), User.class);
      userJson = Serializer.toJSON(user);

      // Figure out the IDs of the articles the user has rated or favorited.
      Map<String, Integer> urlIdToRating = Maps.newHashMap();
      for (UserUrlRating rating : UserUrlRatings.get(user.getId())) {
        urlIdToRating.put(rating.getUrlId(), rating.getRating());
      }
      List<String> favoriteUrlIds = Lists.newArrayList();
      for (UserUrlFavorite favorite : UserUrlFavorites.get(user.getId())) {
        favoriteUrlIds.add(favorite.getUrlId());
      }

      // Create a map of the articles.
      Iterable<String> articleIds = Iterables.concat(urlIdToRating.keySet(), favoriteUrlIds);
      final Map<String, Article> articles = Maps.uniqueIndex(
          Iterables.isEmpty(articleIds) ?
              Collections.<Article>emptyList() :
              Database.get(articleIds, Article.class),
          new Function<Article, String>() {
            @Override
            public String apply(Article article) {
              return article.getUrlId();
            }
          });

      // Put in ratings.  Be careful about URLs that we may not have crawl data
      // for.
      JSONArray ratingsJsonArray = new JSONArray();
      for (String urlId : urlIdToRating.keySet()) {
        if (articles.containsKey(urlId)) {
          JSONObject ratingsJson = Serializer.toJSON(articles.get(urlId));
          ratingsJson.put("rating", urlIdToRating.get(urlId));
          ratingsJsonArray.put(ratingsJson);
        }
      }
      userJson.put("ratings", ratingsJsonArray);

      // Put in favorites.
      JSONArray favoritesJsonArray = new JSONArray();
      for (String urlId : favoriteUrlIds) {
        if (articles.containsKey(urlId)) {
          favoritesJsonArray.put(Serializer.toJSON(articles.get(urlId)));
        }
      }
      userJson.put("favorites", favoritesJsonArray);

    } catch (DataInternalException e) {
      e.printStackTrace();
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      writeJson(resp, getErrorJson(e.getMessage()));
      return;
    }

    // Write response.
    JSONObject response = new JSONObject();
    response.put("success", true);
    response.put("user", userJson);
    writeJson(resp, response);
  }
}
