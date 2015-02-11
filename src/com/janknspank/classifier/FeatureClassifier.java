package com.janknspank.classifier;

import java.util.HashMap;
import java.util.Map;

import com.google.api.client.util.Maps;
import com.janknspank.common.TopList;
import com.janknspank.proto.ArticleProto.ArticleFeature;
import com.janknspank.proto.ArticleProto.ArticleOrBuilder;
import com.janknspank.proto.CoreProto.Distribution;
import com.janknspank.rank.DistributionBuilder;

/**
 * FeatureClassifier scores articles against a number of Features
 * like relevance to startups.
 * @author tomch
 *
 */
public class FeatureClassifier {
  private static FeatureClassifier instance = null;
  // Note not all features have vectors
  static Map<Integer, Vector> featureVectors = new HashMap<>();
  static Map<Integer, Distribution> featureDistributions = Maps.newHashMap();
  
  private FeatureClassifier() {
    for (ArticleFeatureEnum featureEnum : ArticleFeatureEnum.values()) {
      try {
        Feature feature = featureEnum.getFeature();
        if (feature instanceof VectorFeature) {
          VectorFeature vectorFeature = (VectorFeature) feature;
          featureVectors.put(feature.getId(), vectorFeature.getVector());
        }
        featureDistributions.put(feature.getId(), feature.getDistribution());
      } catch (ClassifierException e) {
        // It's OK, just ignore this vector for now.
      }
    }
  }

  public static synchronized FeatureClassifier getInstance() {
    if (instance == null) {
      instance = new FeatureClassifier();
    }
    return instance;
  }
  
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

  public ArticleFeature classifyForFeature(
      ArticleOrBuilder article, Feature feature) throws ClassifierException {
    double rawSimilarity;
    if (feature instanceof VectorFeature) {
      Vector articleVector = new Vector(article);
      Vector cachedFeatureVector = featureVectors.get(feature.getId());
      rawSimilarity = cachedFeatureVector.getCosineSimilarity(
          UniverseVector.getInstance(), articleVector);
    } else {
      rawSimilarity = feature.getScore(article);
    }
    double similarity = DistributionBuilder.projectQuantile(
        featureDistributions.get(feature.getId()), rawSimilarity);
    ArticleFeature classification = ArticleFeature.newBuilder()
        .setFeatureId(feature.getId())
        .setRawSimilarity(rawSimilarity)
        .setSimilarity(similarity)
        .build();
    return classification;
  }
}
