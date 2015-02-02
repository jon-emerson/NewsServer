package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
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
    try {
      Article article = Articles.getArticle(getRequiredParameter(req, "id"));
      JSONObject articleJson = Serializer.toJSON(article);

      JSONArray paragraphsArray = new JSONArray();
      for (String paragraph : article.getParagraphList()) {
        paragraphsArray.put(paragraph);
      }
      articleJson.put("paragraphs", paragraphsArray);
      
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
