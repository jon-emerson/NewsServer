package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.janknspank.bizness.Articles;
import com.janknspank.bizness.BiznessException;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.Serializer;
import com.janknspank.proto.ArticleProto.Article;

@AuthenticationRequired
@ServletMapping(urlPattern = "/v1/get_article")
public class GetArticleServlet extends StandardServlet {
  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DatabaseSchemaException, BiznessException,
      DatabaseRequestException, RequestException {
    Article article = Database.with(Article.class).get(getRequiredParameter(req, "id"));
    if (article == null) {
      throw new RequestException("Article doesn't exist");
    }
    JSONObject articleJson = Serializer.toJSON(article);

    // If we're allowed, add full article text to the response.
    articleJson.put("native_reader_enabled", ArticleSerializer.isNativeReaderEnabled(article));
    articleJson.put("native_reader_v1.1_enabled", true);
    articleJson.put("paragraphs", ArticleSerializer.toJsonArray(
        article.getParagraphList()));

    // Replace the published time with the crawl time, since people often just
    // give a date for a publish time, so without this, the clients are showing
    // midnight as most articles' ages.
    articleJson.put("published_time", Long.toString(Articles.getPublishedTime(article)));

    // Create response.
    JSONObject response = createSuccessResponse();
    response.put("article", articleJson);
    return response;
  }
}
