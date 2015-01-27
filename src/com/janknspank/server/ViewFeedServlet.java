package com.janknspank.server;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.template.soy.data.SoyMapData;
import com.janknspank.common.TopList;
import com.janknspank.data.Articles;
import com.janknspank.data.DataInternalException;
import com.janknspank.data.DataRequestException;
import com.janknspank.data.Database;
import com.janknspank.data.ValidationException;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.User;
import com.janknspank.rank.NeuralNetworkScorer;

public class ViewFeedServlet extends StandardServlet {
  /**
   * Returns any Soy data necessary for rendering the .main template for this
   * servlet's Soy page.
   */
  @Override
  protected SoyMapData getSoyMapData(HttpServletRequest req)
      throws DataInternalException, ValidationException, DataRequestException, NotFoundException {
    User user = Database.with(User.class).get(getSession(req).getUserId());
    if (user.getEmail().equals("tom.charytoniuk@gmail.com") ||
        user.getEmail().equals("panaceaa@gmail.com")) {
      try {
        final Map<Article, Double> articlesToRankMap = Articles.getArticlesAndScores(
            getSession(req).getUserId(), NeuralNetworkScorer.getInstance());
        TopList<Article, Double> articles = new TopList<>(articlesToRankMap.size());
        for (Map.Entry<Article, Double> entry : articlesToRankMap.entrySet()) {
          articles.add(entry.getKey(), entry.getValue());
        }
        return new SoyMapData(
            "sessionKey", this.getSession(req).getSessionKey(),
            "articles", Iterables.transform(articles.getKeys(),
                new Function<Article, SoyMapData>() {
                  @Override
                  public SoyMapData apply(Article article) {
                    return new SoyMapData(
                        "title", article.getTitle(),
                        "url", article.getUrl(),
                        "urlId", article.getUrlId(),
                        "description", article.getDescription(),
                        "image_url", article.getImageUrl(),
                        "rank", articlesToRankMap.get(article));
                  }
                }));
      }
      catch (ParserException e) {
        System.out.println("Error: could not load getArticlesRankedByNeuralNetwork: " + e.getMessage());
        return null;
      }
    }
    return null;
  }
}
