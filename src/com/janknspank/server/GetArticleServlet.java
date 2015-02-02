package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.janknspank.bizness.Articles;
import com.janknspank.bizness.BiznessException;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.ArticleProto.Article;

public class GetArticleServlet extends StandardServlet {

  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DatabaseSchemaException, BiznessException,
      DatabaseRequestException, RequestException {
    try {
      Article article = Articles.getArticle(getRequiredParameter(req, "id"));
      JSONObject articleJson = Articles.serialize(article);

      // Add full article text to the response
      articleJson.put("paragraphs", Articles.getParagraphs(article));

      // Create response.
      JSONObject response = createSuccessResponse();
      response.put("article", articleJson);
      return response;
    } catch (DatabaseSchemaException e) {
      // Fallback
      System.out.println("Error: couldn't load getArticle: " + e.getMessage());
      throw new RequestException("Article ID does not exist");
    }
  }
}
