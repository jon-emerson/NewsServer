package com.janknspank.server;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.janknspank.data.ArticleKeywords;
import com.janknspank.data.Articles;
import com.janknspank.data.DataInternalException;
import com.janknspank.data.DataRequestException;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.ArticleKeyword;
import com.janknspank.proto.Serializer;

public class GetArticlesServlet extends NewsServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    int statusCode = HttpServletResponse.SC_OK;
    JSONObject response = new JSONObject();

    JSONArray articles = new JSONArray();
    try {
      List<Article> articleList = Articles.getArticles();
      for (Article article : articleList) {
        articles.put(Serializer.toJSON(article));
      }
      response.put("articles", articles);
      response.put("articleKeywords", getArticleKeywordJSONArray(articleList));
      response.put("success", true);
    } catch (DataRequestException | DataInternalException e) {
      statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
      response.put("success", false);
      response.put("error", e.getMessage());
    }

    resp.setStatus(statusCode);
    resp.getOutputStream().write(response.toString().getBytes());
  }

  private JSONArray getArticleKeywordJSONArray(List<Article> articleList)
      throws DataInternalException {
    JSONArray jsonArray = new JSONArray();
    for (ArticleKeyword articleKeyword : ArticleKeywords.get(articleList)) {
      jsonArray.put(Serializer.toJSON(articleKeyword));
    }
    return jsonArray;
  }
}
