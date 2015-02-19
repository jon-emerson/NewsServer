package com.janknspank.server;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.template.soy.data.SoyMapData;
import com.janknspank.bizness.Articles;
import com.janknspank.bizness.SocialEngagements;
import com.janknspank.classifier.ClassifierException;
import com.janknspank.classifier.Feature;
import com.janknspank.classifier.FeatureId;
import com.janknspank.common.Logger;
import com.janknspank.common.TopList;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleFeature;
import com.janknspank.proto.ArticleProto.SocialEngagement;
import com.janknspank.proto.UserProto.User;
import com.janknspank.rank.NeuralNetworkScorer;

@AuthenticationRequired
public class ViewFeedServlet extends StandardServlet {
  private static final Logger LOG = new Logger(ViewFeedServlet.class);
  private static final int NUM_RESULTS = 50;

  /**
   * Returns any Soy data necessary for rendering the .main template for this
   * servlet's Soy page.
   */
  @Override
  protected SoyMapData getSoyMapData(HttpServletRequest req) throws DatabaseSchemaException {
    User user = Database.with(User.class).get(getSession(req).getUserId());
    if (user.getEmail().equals("tom.charytoniuk@gmail.com") ||
        user.getEmail().equals("panaceaa@gmail.com")) {
      // Debugging for Linkedinprofile document
//      LinkedInProfile profile = user.getLinkedInProfile();
//      DocumentNode linkedInProfileDocument = DocumentBuilder.build(null, 
//          new StringReader(profile.getData()));
//      UserInterests.updateInterests(user, linkedInProfileDocument, null);
      // Uncomment to play with profile data

      final Map<Article, Double> articlesToRankMap =
//          Articles.getArticlesAndScores(user, HeuristicScorer.getInstance());
          Articles.getArticlesAndScores(user, NeuralNetworkScorer.getInstance(), NUM_RESULTS);

      // Sort the articles
      TopList<Article, Double> articles = new TopList<>(articlesToRankMap.size());
      for (Map.Entry<Article, Double> entry : articlesToRankMap.entrySet()) {
        Article article = entry.getKey();
        double daysAgo = Math.floor((System.currentTimeMillis() - article.getPublishedTime())
            / TimeUnit.DAYS.toMillis(1)) + 1;
        articles.add(entry.getKey(), entry.getValue() / daysAgo);
      }

      return new SoyMapData(
          "sessionKey", this.getSession(req).getSessionKey(),
          "articles", Iterables.transform(articles.getKeys(),
              new Function<Article, SoyMapData>() {
                @Override
                public SoyMapData apply(Article article) {
                  SocialEngagement engagement =
                      SocialEngagements.getForArticle(article, SocialEngagement.Site.FACEBOOK);
                  if (engagement == null) {
                    engagement = SocialEngagement.getDefaultInstance();
                  }

                  String industryClassifications = getIndustryString(article);
                  return new SoyMapData(
                      "title", article.getTitle(),
                      "url", article.getUrl(),
                      "urlId", article.getUrlId(),
                      "description", article.getDescription(),
                      "image_url", article.getImageUrl(),
                      "score", articlesToRankMap.get(article),
                      "fb_likes", (int) engagement.getLikeCount(),
                      "fb_shares", (int) engagement.getShareCount(),
                      "fb_comments" ,(int) engagement.getCommentCount(),
                      "industry_classifications", industryClassifications);
                }
              }));
    }
    return null;
  }

  private String getIndustryString(Article article) {
    StringBuilder sb = new StringBuilder();
    for (ArticleFeature articleFeature : article.getFeatureList()) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      if (articleFeature.getType() == ArticleFeature.Type.ABOUT_INDUSTRY) {
        try {
          Feature feature = Feature.getFeature(FeatureId.fromId(articleFeature.getFeatureId()));
          sb.append(feature.getDescription())
              .append(": ")
              .append(articleFeature.getSimilarity());
        } catch (ClassifierException e) {
          LOG.warning("Found invalid article feature: " + articleFeature.getFeatureId());
        }
      }
    }
    return "[" + sb.toString() + "]";
  }
}
