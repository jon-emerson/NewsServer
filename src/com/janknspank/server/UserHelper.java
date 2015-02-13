package com.janknspank.server;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.janknspank.bizness.UrlRatings;
import com.janknspank.classifier.IndustryCode;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.Serializer;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.UserProto.UrlFavorite;
import com.janknspank.proto.UserProto.UrlRating;
import com.janknspank.proto.UserProto.User;
import com.janknspank.proto.UserProto.UserIndustry.Relationship;

/**
 * Helper class containing the user's favorite and rated articles.
 */
public class UserHelper {
  private final User user;
  private final Map<String, Long> favoriteArticleIds;
  private final Map<IndustryCode, Relationship> industryCodeRelationships;
  private final Map<String, Article> articleMap;

  public UserHelper(User user) throws DatabaseSchemaException {
    this.user = user;

    // Create a map of the favorite articles.
    favoriteArticleIds = getFavoriteArticleIds();
    industryCodeRelationships = IndustryCode.getFromUserIndustries(user.getIndustryList());
    articleMap = getArticleMap(favoriteArticleIds.keySet());
  }

  private Map<String, Long> getFavoriteArticleIds() throws DatabaseSchemaException {
    Map<String, Long> favoriteArticleIds = Maps.newHashMap();
    for (UrlFavorite favorite : user.getUrlFavoriteList()) {
      favoriteArticleIds.put(favorite.getUrlId(), favorite.getCreateTime());
    }
    return favoriteArticleIds;
  }

  private Map<String, Article> getArticleMap(Iterable<String> articleIds)
      throws DatabaseSchemaException {
    return Maps.uniqueIndex(
        Iterables.isEmpty(articleIds)
            ? Collections.<Article>emptyList()
            : Database.with(Article.class).get(articleIds),
        new Function<Article, String>() {
          @Override
          public String apply(Article article) {
            return article.getUrlId();
          }
        });
  }

  /**
   * @param favoriteArticleIds A map from article URL ID to the time the
   *     current user favorited it.
   */
  public JSONArray getFavoritesJsonArray() {
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

  public JSONArray getInterestsJsonArray() throws DatabaseSchemaException {
    return Serializer.toJSON(user.getInterestList());
  }

  /**
   * Returns a filled-out version of the user's industries, including their
   * titles and groups.  (The industry codes stored in the database are just
   * thin pointer references, which is not enough for the client.)
   */
  public JSONArray getIndustriesJsonArray() {
    JSONArray jsonArray = new JSONArray();
    for (IndustryCode code : industryCodeRelationships.keySet()) {
      JSONObject o = new JSONObject();
      o.put("id", code.getId());
      o.put("group", code.getGroup());
      o.put("description", code.getDescription());
      o.put("relationship", industryCodeRelationships.get(code).toString());
      jsonArray.put(o);
    }
    return jsonArray;
  }
  
  public JSONArray getRatingsJsonArray() throws DatabaseSchemaException {
    JSONArray jsonArray = new JSONArray();
    for (UrlRating rating : UrlRatings.getForUser(user)) {
      JSONObject o = new JSONObject();
      o.put("url", rating.getUrl());
      o.put("rating", rating.getRating());
      jsonArray.put(o);
    }
    return jsonArray;
  }
}