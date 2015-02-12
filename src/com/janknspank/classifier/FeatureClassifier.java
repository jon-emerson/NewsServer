package com.janknspank.classifier;

import java.util.Map;

import com.google.api.client.util.Maps;
import com.google.common.collect.Iterables;
import com.janknspank.common.TopList;
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
    // For each FeatureType, find the top 5 matches.
    Map<FeatureType, TopList<ArticleFeature, Double>> topArticleFeaturesMap = Maps.newHashMap();
    for (Feature feature : Feature.getAllFeatures()) {
      TopList<ArticleFeature, Double> topArticleFeatures =
          topArticleFeaturesMap.get(feature.getFeatureId().getFeatureType());
      if (topArticleFeatures == null) {
        topArticleFeatures = new TopList<>(5);
        topArticleFeaturesMap.put(feature.getFeatureId().getFeatureType(), topArticleFeatures);
      }

      ArticleFeature classification = classifyForFeature(article, feature);
      topArticleFeatures.add(classification, classification.getSimilarity());
    }
    return Iterables.concat(topArticleFeaturesMap.values());
  }
}
