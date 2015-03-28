package com.janknspank.classifier;

import java.util.List;

import com.google.api.client.util.Lists;
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
    List<ArticleFeature> articleFeatures = Lists.newArrayList();

    // For industry features, only keep the top 5.  Otherwise there's some
    // articles that just tend to be classified as being about everything,
    // which sounds nice, but they're actually not super relevant about any
    // thing.
    TopList<ArticleFeature, Double> topIndustryArticleFeatures = new TopList<>(5);

    // Classify and add all the article features.
    for (Feature feature : Feature.getAllFeatures()) {
      ArticleFeature articleFeature = classifyForFeature(article, feature);
      if (feature.getFeatureId().getFeatureType() == FeatureType.INDUSTRY) {
        if (articleFeature.getSimilarity() >= 0.72) {
          topIndustryArticleFeatures.add(articleFeature, articleFeature.getSimilarity());
        }
      } else {
        articleFeatures.add(articleFeature);
      }
    }

    articleFeatures.addAll(topIndustryArticleFeatures.getKeys());
    return articleFeatures;
  }
}
