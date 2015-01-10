package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;

import com.janknspank.data.Articles;
import com.janknspank.data.DataInternalException;
import com.janknspank.data.ValidationException;
import com.janknspank.proto.Core.Article;

@AuthenticationRequired
public class GetTopicServlet extends AbstractArticlesServlet {

  @Override
  protected Iterable<Article> getArticles(HttpServletRequest req)
      throws ValidationException, DataInternalException {
    return Articles.getArticlesOnTopic(getRequiredParameter(req, "topic"));
  }
}
