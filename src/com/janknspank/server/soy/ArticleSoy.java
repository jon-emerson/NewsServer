package com.janknspank.server.soy;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.template.soy.data.SoyListData;
import com.google.template.soy.data.SoyMapData;
import com.janknspank.bizness.SocialEngagements;
import com.janknspank.classifier.ClassifierException;
import com.janknspank.classifier.Feature;
import com.janknspank.classifier.FeatureId;
import com.janknspank.common.Logger;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleFeature;
import com.janknspank.proto.ArticleProto.SocialEngagement;

public class ArticleSoy {
  private static final Logger LOG = new Logger(ArticleSoy.class);

  public static SoyListData toSoyListData(
      final Iterable<Article> articles, final Function<Article, Double> scoreFunction) {
    return new SoyListData(Iterables.transform(articles,
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
            "score", scoreFunction.apply(article),
            "fb_likes", (int) engagement.getLikeCount(),
            "fb_shares", (int) engagement.getShareCount(),
            "fb_comments" ,(int) engagement.getCommentCount(),
            "industry_classifications", industryClassifications);
      }
    }));
  }

  private static String getIndustryString(Article article) {
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
