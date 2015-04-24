package com.janknspank.bizness;

import com.janknspank.classifier.FeatureId;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleFeature;

public class ArticleFeatures {
  /**
   * Returns the requested article feature from the given article, if the
   * article has said feature.
   */
  public static ArticleFeature getFeature(Article article, FeatureId featureId) {
    for (ArticleFeature articleFeature : article.getFeatureList()) {
      if (articleFeature.getFeatureId() == featureId.getId()) {
        return articleFeature;
      }
    }
    return null;
  }

  /**
   * Returns the requested article feature from the given article, if the
   * article has said feature.
   */
  public static double getFeatureSimilarity(Article article, FeatureId featureId) {
    for (ArticleFeature articleFeature : article.getFeatureList()) {
      if (articleFeature.getFeatureId() == featureId.getId()) {
        return articleFeature.getSimilarity();
      }
    }
    return 0;
  }
}
