package com.janknspank.server;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.template.soy.data.SoyMapData;
import com.janknspank.bizness.Articles;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.IndustryCodes;
import com.janknspank.common.TopList;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleIndustry;
import com.janknspank.proto.ArticleProto.SocialEngagement;
import com.janknspank.proto.UserProto.User;
import com.janknspank.rank.HeuristicScorer;
import com.janknspank.rank.Scorer;

public class ViewFeedServlet extends StandardServlet {
  /**
   * Returns any Soy data necessary for rendering the .main template for this
   * servlet's Soy page.
   * @throws DatabaseSchemaException 
   * @throws DatabaseRequestException 
   * @throws BiznessException 
   */
  @Override
  protected SoyMapData getSoyMapData(HttpServletRequest req)
      throws DatabaseSchemaException, BiznessException, DatabaseRequestException {
    User user = Database.with(User.class).get(getSession(req).getUserId());
    if (user.getEmail().equals("tom.charytoniuk@gmail.com") ||
        user.getEmail().equals("panaceaa@gmail.com")) {
      try {
        final Map<Article, Double> articlesToRankMap =
            Articles.getArticlesAndScores(user, HeuristicScorer.getInstance());

        // Sort the articles
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
                    SocialEngagement engagement = Scorer.getLatestFacebookEngagement(article);
                    if (engagement == null) {
                      engagement = SocialEngagement.getDefaultInstance();
                    }

                    return new SoyMapData(
                        "title", article.getTitle(),
                        "url", article.getUrl(),
                        "urlId", article.getUrlId(),
                        "description", article.getDescription(),
                        "image_url", article.getImageUrl(),
                        "score", articlesToRankMap.get(article),
                        "fb_likes", String.valueOf(engagement.getLikeCount()),
                        "fb_shares", String.valueOf(engagement.getShareCount()),
                        "fb_comments" , String.valueOf(engagement.getCommentCount()),
                        "industry_classifications", getIndustryString(article));
                  }
                }));
      } catch (ParserException e) {
        return null;
      }
    }
    return null;
  }

  private String getIndustryString(Article article) {
    String output = null;
    List<ArticleIndustry> classifications = article.getIndustryList();
    if (classifications.size() > 0) {
      output = "[";
      for (ArticleIndustry classification : article.getIndustryList()) {
        output += IndustryCodes.INDUSTRY_CODE_MAP.get(
            classification.getIndustryCodeId()).getDescription();
        output += ": ";
        output += classification.getSimilarity();
        output += ", ";
      }
      output = output.substring(0, output.length() - 2) + "]";
    }
    else {
      output = "[No industry classifications]";
    }

    return output;
  }
}
