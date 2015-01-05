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
import com.janknspank.data.UserInterests;
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

  private Map<String, Integer> getRatedArticleIds(User user) throws DataInternalException {
    Map<String, Integer> ratedArticleIds = Maps.newHashMap();
    for (UserUrlRating rating : UserUrlRatings.get(user.getId())) {
      ratedArticleIds.put(rating.getUrlId(), rating.getRating());
    }
    return ratedArticleIds;
  }

  private Map<String, Long> getFavoriteArticleIds(User user) throws DataInternalException {
    Map<String, Long> favoriteArticleIds = Maps.newHashMap();
    for (UserUrlFavorite favorite : UserUrlFavorites.get(user.getId())) {
      favoriteArticleIds.put(favorite.getUrlId(), favorite.getCreateTime());
    }
    return favoriteArticleIds;
  }

  private Map<String, Article> getArticleMap(Iterable<String> articleIds)
      throws DataInternalException {
    return Maps.uniqueIndex(
        Iterables.isEmpty(articleIds) ?
            Collections.<Article>emptyList() :
            Database.get(articleIds, Article.class),
        new Function<Article, String>() {
          @Override
          public String apply(Article article) {
            return article.getUrlId();
          }
        });
  }

  private JSONArray getRatingsJsonArray(Map<String, Integer> ratedArticleIds,
      Map<String, Article> articleMap) {
    JSONArray ratingsJsonArray = new JSONArray();
    for (String urlId : ratedArticleIds.keySet()) {
      if (articleMap.containsKey(urlId)) {
        JSONObject ratingsJson = Serializer.toJSON(articleMap.get(urlId));
        ratingsJson.put("rating", ratedArticleIds.get(urlId));
        ratingsJsonArray.put(ratingsJson);
      }
    }
    return ratingsJsonArray;
  }

  private JSONArray getFavoritesJsonArray(final Map<String, Long> favoriteArticleIds,
      Map<String, Article> articleMap) {
    List<Article> favoriteArticles = Lists.newArrayList();
    for (String urlId : favoriteArticleIds.keySet()) {
      if (articleMap.containsKey(urlId)) {
        favoriteArticles.add(articleMap.get(urlId));
      }
    }

    Collections.sort(favoriteArticles, new Comparator<Article>() {
      @Override
      public int compare(Article o1, Article o2) {
        // Newest articles first, sorted by when they were favorited.
        return Long.compare(favoriteArticleIds.get(o2.getUrlId()),
            favoriteArticleIds.get(o1.getUrlId()));
      }
    });

    JSONArray favoritesJsonArray = new JSONArray();
    for (Article favoriteArticle : favoriteArticles) {
      JSONObject favoriteJson = Serializer.toJSON(favoriteArticle);
      favoriteJson.put("favorited_time", favoriteArticleIds.get(favoriteArticle.getUrlId()));
      favoritesJsonArray.put(favoriteJson);
    }
    return favoritesJsonArray;
  }

  protected JSONArray getInterestsJsonArray(User user) throws DataInternalException {
    return Serializer.toJSON(UserInterests.getInterests(user.getId()));
  }

  @Override
  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DataInternalException {
    Session session = getSession(req);
    User user = Database.get(session.getUserId(), User.class);
    JSONObject userJson = Serializer.toJSON(user);

    // Figure out the IDs of the articles the user has rated or favorited.
    Map<String, Integer> ratedArticleIds = getRatedArticleIds(user);
    final Map<String, Long> favoriteArticleIds = getFavoriteArticleIds(user);

    // Create a map of the articles.
    final Map<String, Article> articleMap = getArticleMap(Iterables.concat(
        ratedArticleIds.keySet(), favoriteArticleIds.keySet()));

    // Put in ratings.  Be careful about URLs that we may not have crawl data
    // for.
    userJson.put("ratings", getRatingsJsonArray(ratedArticleIds, articleMap));

    // Put in favorites.
    userJson.put("favorites", getFavoritesJsonArray(favoriteArticleIds, articleMap));

    // Put in interests.
    userJson.put("interests", getInterestsJsonArray(user));

    // Create response.
    JSONObject response = createSuccessResponse();
    response.put("user", userJson);
    return response;
  }
}
