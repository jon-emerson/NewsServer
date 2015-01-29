package com.janknspank.server;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.collect.Maps;
import com.janknspank.bizness.ArticleKeywords;
import com.janknspank.bizness.BiznessException;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.Serializer;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.ArticleKeyword;

public abstract class AbstractArticlesServlet extends StandardServlet {
  protected abstract Iterable<Article> getArticles(HttpServletRequest req)
      throws BiznessException, DatabaseSchemaException, DatabaseRequestException, RequestException;

  @Override
  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DatabaseSchemaException, DatabaseRequestException, RequestException, BiznessException {
    JSONObject response = createSuccessResponse();
    response.put("articles", getArticleJsonArray(getArticles(req)));
    return response;
  }

  /**
   * Returns a JSON array of JSON article objects with their keywords filled in.
   */
  private JSONArray getArticleJsonArray(Iterable<Article> articleList)
      throws DatabaseSchemaException {
    // Create a look-up table for article keywords.
    Map<String, JSONArray> articleKeywordJsonMap = Maps.newHashMap();
    for (ArticleKeyword articleKeyword : ArticleKeywords.get(articleList)) {
      String articleId = articleKeyword.getUrlId();
      JSONArray arrayForArticle = articleKeywordJsonMap.get(articleId);
      if (arrayForArticle == null) {
        arrayForArticle = new JSONArray();
        articleKeywordJsonMap.put(articleId, arrayForArticle);
      }
      arrayForArticle.put(Serializer.toJSON(articleKeyword));
    }

    // Serialize the articles and add their keywords in.
    JSONArray jsonArticles = new JSONArray();
    for (Article article : articleList) {
      String articleId = article.getUrlId();
      JSONObject jsonArticle = Serializer.toJSON(article);
      if (articleKeywordJsonMap.containsKey(articleId)) {
        jsonArticle.put("keywords", articleKeywordJsonMap.get(articleId));
      }
      jsonArticles.put(jsonArticle);
    }
    return jsonArticles;
  }
}
