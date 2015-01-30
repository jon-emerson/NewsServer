package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;

import com.janknspank.bizness.Articles;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.Core.Article;

@AuthenticationRequired
public class GetTopicServlet extends AbstractArticlesServlet {

  @Override
  protected Iterable<Article> getArticles(HttpServletRequest req)
      throws DatabaseSchemaException, RequestException {
    return Articles.getArticlesOnTopic(getRequiredParameter(req, "topic"));
  }
}
