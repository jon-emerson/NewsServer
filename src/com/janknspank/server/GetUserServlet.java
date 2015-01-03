package com.janknspank.server;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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
public class GetUserServlet extends StandardServlet {

  @Override
  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DataInternalException {
    Session session = getSession(req);
    User user = Database.get(session.getUserId(), User.class);
    JSONObject userJson = Serializer.toJSON(user);

    // Figure out the IDs of the articles the user has rated or favorited.
    Map<String, Integer> urlIdToRating = Maps.newHashMap();
    for (UserUrlRating rating : UserUrlRatings.get(user.getId())) {
      urlIdToRating.put(rating.getUrlId(), rating.getRating());
    }
    final Map<String, Long> favoriteUrlIdToCreateDate = Maps.newHashMap();
    for (UserUrlFavorite favorite : UserUrlFavorites.get(user.getId())) {
      favoriteUrlIdToCreateDate.put(favorite.getUrlId(), favorite.getCreateTime());
    }

    // Create a map of the articles.
    Iterable<String> articleIds = Iterables.concat(
        urlIdToRating.keySet(), favoriteUrlIdToCreateDate.keySet());
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
    List<Article> favoriteArticles = Lists.newArrayList();
    for (String urlId : favoriteUrlIdToCreateDate.keySet()) {
      if (articles.containsKey(urlId)) {
        favoriteArticles.add(articles.get(urlId));
      }
    }
    Collections.sort(favoriteArticles, new Comparator<Article>() {
      @Override
      public int compare(Article o1, Article o2) {
        // Newest articles first, sorted by when they were favorited.
        return Long.compare(favoriteUrlIdToCreateDate.get(o2.getUrlId()),
            favoriteUrlIdToCreateDate.get(o1.getUrlId()));
      }
    });
    JSONArray favoritesJsonArray = new JSONArray();
    for (Article favoriteArticle : favoriteArticles) {
      JSONObject favoriteJson = Serializer.toJSON(favoriteArticle);
      favoriteJson.put("favorited_time", favoriteUrlIdToCreateDate.get(favoriteArticle.getUrlId()));
      favoritesJsonArray.put(favoriteJson);
    }
    userJson.put("favorites", favoritesJsonArray);

    // Create response.
    JSONObject response = createSuccessResponse();
    response.put("user", userJson);
    return response;
  }
}
