package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.janknspank.bizness.Articles;
import com.janknspank.bizness.BiznessException;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.Serializer;
import com.janknspank.proto.ArticleProto.Article;

public class GetArticleServlet extends StandardServlet {

  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DatabaseSchemaException, BiznessException,
      DatabaseRequestException, RequestException {
    Article article = Articles.getArticle(getRequiredParameter(req, "id"));
    if (article == null) {
      throw new RequestException("Article doesn't exist");
    }
    JSONObject articleJson = Serializer.toJSON(article);

    // Add full article text to the response
    articleJson.put("paragraphs", AbstractArticlesServlet.toJsonArray(
        article.getParagraphList()));

    // Create response.
    JSONObject response = createSuccessResponse();
    response.put("article", articleJson);
    return response;
  }
}