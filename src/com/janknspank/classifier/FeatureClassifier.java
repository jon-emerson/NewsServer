package com.janknspank.classifier;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.common.base.Function;
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
    // For each FeatureType, find the top 10 matches.
    final Map<FeatureType, TopList<ArticleFeature, Double>> topArticleFeaturesMap =
        Maps.newHashMap();
    for (Feature feature : Feature.getAllFeatures()) {
      TopList<ArticleFeature, Double> topArticleFeatures =
          topArticleFeaturesMap.get(feature.getFeatureId().getFeatureType());
      if (topArticleFeatures == null) {
        topArticleFeatures = new TopList<>(10);
        topArticleFeaturesMap.put(feature.getFeatureId().getFeatureType(), topArticleFeatures);
      }

      ArticleFeature classification = classifyForFeature(article, feature);
      topArticleFeatures.add(classification, classification.getSimilarity());
    }

    return Iterables.concat(Iterables.transform(topArticleFeaturesMap.keySet(),
        new Function<FeatureType, Iterable<ArticleFeature>>() {
          @Override
          public Iterable<ArticleFeature> apply(FeatureType featureType) {
            if (featureType == FeatureType.INDUSTRY) {
              // HACK(jonemerson): For Industry vectors, only include >3 matches if
              // the articles have sufficiently high values.
              List<ArticleFeature> goodIndustryFeatures = Lists.newArrayList();
              Iterator<ArticleFeature> iterator =
                  topArticleFeaturesMap.get(FeatureType.INDUSTRY).iterator();
              int i = 0;
              while (iterator.hasNext()) {
                ArticleFeature feature = iterator.next();
                if (i++ < 3 || feature.getSimilarity() >= 0.85) {
                  goodIndustryFeatures.add(feature);
                }
              }
              return goodIndustryFeatures;
            }
            return topArticleFeaturesMap.get(featureType);
          }
    }));
  }
}
