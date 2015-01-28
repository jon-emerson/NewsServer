package com.janknspank.server;

import java.io.IOException;
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
import com.janknspank.proto.Core.ArticleFacebookEngagement;
import com.janknspank.proto.Core.User;
import com.janknspank.rank.CompleteArticle;
import com.janknspank.rank.HeuristicScorer;

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
        final Map<CompleteArticle, Double> articlesToRankMap = 
            Articles.getCompleteArticlesAndScores(
            getSession(req).getUserId(), HeuristicScorer.getInstance());
        
        // Sort the articles
        TopList<CompleteArticle, Double> articles = new TopList<>(articlesToRankMap.size());
        for (Map.Entry<CompleteArticle, Double> entry : articlesToRankMap.entrySet()) {
          articles.add(entry.getKey(), entry.getValue());
        }
        
        return new SoyMapData(
            "sessionKey", this.getSession(req).getSessionKey(),
            "articles", Iterables.transform(articles.getKeys(),
                new Function<CompleteArticle, SoyMapData>() {
                  @Override
                  public SoyMapData apply(CompleteArticle completeArticle) {
                    Article article = completeArticle.getArticle();
                    ArticleFacebookEngagement engagement = completeArticle.getLatestFacebookEngagement();
                    int likeCount = 0;
                    int shareCount = 0;
                    int commentCount = 0;
                    if (engagement != null) {
                      likeCount = (int) engagement.getLikeCount();
                      shareCount = (int) engagement.getShareCount();
                      commentCount = (int) engagement.getCommentCount();
                    }
                    
                    return new SoyMapData(
                        "title", article.getTitle(),
                        "url", article.getUrl(),
                        "urlId", article.getUrlId(),
                        "description", article.getDescription(),
                        "image_url", article.getImageUrl(),
                        "score", articlesToRankMap.get(completeArticle),
                        "fb_likes", likeCount,
                        "fb_shares", shareCount,
                        "fb_comments" ,commentCount,
                        "industry_classifications", completeArticle
                            .getIndustryClassificationsString());
                  }
                }));
      }
      catch (ParserException | IOException e) {
        return null;
      }
    }
    return null;
  }
}
