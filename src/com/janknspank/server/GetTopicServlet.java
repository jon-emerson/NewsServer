package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;

import com.google.common.collect.ImmutableList;
import com.janknspank.bizness.Articles;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.ArticleProto.Article;

@AuthenticationRequired
public class GetTopicServlet extends AbstractArticlesServlet {
  private static final int NUM_RESULTS = 50;

  @Override
  protected Iterable<Article> getArticles(HttpServletRequest req)
      throws DatabaseSchemaException, RequestException {
    return Articles.getArticlesForKeywords(
        ImmutableList.of(getRequiredParameter(req, "topic")), NUM_RESULTS);
  }
}
