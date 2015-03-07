package com.janknspank.server.soy;

import org.json.JSONObject;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.template.soy.data.SoyListData;
import com.google.template.soy.data.SoyMapData;
import com.janknspank.bizness.SocialEngagements;
import com.janknspank.classifier.FeatureId;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleFeature;
import com.janknspank.proto.ArticleProto.SocialEngagement;

public class ArticleSoy {
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

        return new SoyMapData(
            "title", article.getTitle(),
            "url", article.getUrl(),
            "urlId", article.getUrlId(),
            "description", article.getDescription(),
            "image_url", article.getImageUrl(),
            "score", scoreFunction.apply(article),
            "fb_score", (int) engagement.getShareScore(),
            "fb_likes", (int) engagement.getLikeCount(),
            "fb_shares", (int) engagement.getShareCount(),
            "fb_comments" ,(int) engagement.getCommentCount(),
            "features", getFeaturesString(article));
      }
    }));
  }

  private static String getFeaturesString(Article article) {
    StringBuilder sb = new StringBuilder();
    for (ArticleFeature articleFeature : article.getFeatureList()) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      JSONObject o = new JSONObject();
      o.put("id", articleFeature.getFeatureId());
      FeatureId featureId = FeatureId.fromId(articleFeature.getFeatureId());
      o.put("description", featureId != null
          ? (featureId.getFeatureType().name() + ": " + featureId.getTitle())
          : "UNKNOWN!");
      o.put("similarity", articleFeature.getSimilarity());
      sb.append(o.toString(2));
    }
    return sb.toString();
  }
}
