package com.janknspank.classifier;

import com.janknspank.common.TopList;
import com.janknspank.proto.ArticleProto.ArticleFeature;
import com.janknspank.proto.ArticleProto.ArticleOrBuilder;
import com.janknspank.rank.DistributionBuilder;

public class FeatureClassifier {
  
  public Iterable<ArticleFeature> classify(ArticleOrBuilder article) 
      throws ClassifierException {
    // Compute classifications from scratch against Features
    TopList<ArticleFeature, Double> classifications = new TopList<>(20);
    for (ArticleFeatureEnum featureEnum : ArticleFeatureEnum.values()) {
      ArticleFeature classification = classifyForFeature(article, 
          featureEnum.getFeature());
      classifications.add(classification, classification.getSimilarity());
    }
    return classifications;
  }

  public static ArticleFeature classifyForFeature(
      ArticleOrBuilder article, Feature feature) throws ClassifierException {
    double rawSimilarity = feature.getScore(article);
    double similarity = DistributionBuilder.projectQuantile(
        feature.getDistribution(), rawSimilarity);
    ArticleFeature classification = ArticleFeature.newBuilder()
        .setFeatureId(feature.getId())
        .setRawSimilarity(rawSimilarity)
        .setSimilarity(similarity)
        .build();
    return classification;
  }
}
