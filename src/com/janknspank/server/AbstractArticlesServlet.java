package com.janknspank.server;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.janknspank.bizness.BiznessException;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.Serializer;
import com.janknspank.proto.ArticleProto.Article;

public abstract class AbstractArticlesServlet extends StandardServlet {
  protected abstract Iterable<Article> getArticles(HttpServletRequest req)
      throws BiznessException, DatabaseSchemaException, DatabaseRequestException, RequestException;

  @Override
  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DatabaseSchemaException, DatabaseRequestException, RequestException, BiznessException {
    JSONObject response = createSuccessResponse();
    
    Iterable<Article> articles = getArticles(req);
    JSONArray articlesJson = new JSONArray();
    for (Article article : articles) {
      articlesJson.put(serialize(article));
    }
    response.put("articles", articlesJson);
    return response;
  }

  private JSONObject serialize(Article article) {
    JSONObject articleJson = Serializer.toJSON(article);
    List<String> paragraphs = article.getParagraphList();
    articleJson.put("first_3_paragraphs", toJsonArray(
        paragraphs.subList(0, Math.min(3, paragraphs.size()))));

    return articleJson;
  }
  
  public static JSONArray toJsonArray(List<String> strings) {
    JSONArray array = new JSONArray();
    for (String string : strings) {
      array.put(string);
    }
    return array;
  }
}
