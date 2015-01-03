package com.janknspank.server;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.collect.Maps;
import com.janknspank.data.ArticleKeywords;
import com.janknspank.data.Articles;
import com.janknspank.data.DataInternalException;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.ArticleKeyword;
import com.janknspank.proto.Serializer;

public class GetArticlesServlet extends StandardServlet {
  @Override
  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DataInternalException {
    JSONObject response = createSuccessResponse();
    response.put("articles", getArticles());
    return response;
  }

  /**
   * Returns a JSON array of JSON article objects with their keywords filled in.
   */
  private JSONArray getArticles() throws DataInternalException {
    List<Article> articleList = Articles.getArticles();

    // Create a look-up table for article keywords.
    Map<String, JSONArray> articleKeywordJsonMap = Maps.newHashMap();
    for (ArticleKeyword articleKeyword : ArticleKeywords.get(articleList)) {
      String articleId = articleKeyword.getArticleId();
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
