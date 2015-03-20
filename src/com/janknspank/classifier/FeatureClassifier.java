package com.janknspank.classifier;

import java.util.List;

import com.google.api.client.util.Lists;
import com.janknspank.proto.ArticleProto.ArticleFeature;
import com.janknspank.proto.ArticleProto.ArticleOrBuilder;

/**
 * FeatureClassifier scores articles against a number of Features
 * like relevance to startups.
 */
public class FeatureClassifier {
  public static ArticleFeature classifyForFeature(ArticleOrBuilder article, Feature feature)
      throws ClassifierException {
    return ArticleFeature.newBuilder()
        .setFeatureId(feature.getId().getId())
        .setSimilarity(feature.score(article))
        .build();
  }

  public static Iterable<ArticleFeature> classify(ArticleOrBuilder article)
      throws ClassifierException {
    List<ArticleFeature> articleFeatures = Lists.newArrayList();
    for (Feature feature : Feature.getAllFeatures()) {
      ArticleFeature articleFeature = classifyForFeature(article, feature);
      if (feature.getFeatureId().getFeatureType() == FeatureType.INDUSTRY) {
        if (articleFeature.getSimilarity() >= 0.80) {
          articleFeatures.add(articleFeature);
        }
      } else {
        articleFeatures.add(articleFeature);
      }
    }
    return articleFeatures;
  }
}
