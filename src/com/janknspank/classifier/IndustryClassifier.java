package com.janknspank.classifier;

import java.util.HashMap;
import java.util.Map;

import com.google.api.client.util.Maps;
import com.janknspank.common.TopList;
import com.janknspank.proto.ArticleProto.ArticleIndustry;
import com.janknspank.proto.ArticleProto.ArticleOrBuilder;
import com.janknspank.proto.CoreProto.Distribution;
import com.janknspank.proto.EnumsProto.IndustryCode;
import com.janknspank.rank.DistributionBuilder;

public class IndustryClassifier {
  private static IndustryClassifier instance = null;
  static Map<IndustryCode, Vector> industryVectors = new HashMap<>();
  static Map<IndustryCode, Distribution> industryDistributions = Maps.newHashMap();

  private IndustryClassifier() {
    for (IndustryCode industryCode : IndustryCodes.INDUSTRY_CODE_MAP.values()) {
      try {
        Vector industryVector = IndustryVector.get(industryCode);
        if (industryVector.getDocumentCount() == 0) {
          System.out.println("WARNING: Industry vector has no documents: "
              + industryCode.getId() + ", " + industryCode.getDescription());
        } else {
          Distribution distribution = IndustryVector.getDistribution(industryCode);
          industryVectors.put(industryCode, industryVector);
          industryDistributions.put(industryCode, distribution);
        }
      } catch (ClassifierException e) {
        // It's OK, just ignore this vector for now.
      }
    }
  }

  public static synchronized IndustryClassifier getInstance() {
    if (instance == null) {
      instance = new IndustryClassifier();
    }
    return instance;
  }

  /**
   * Returns a list of article industry classifications.
   */
  public Iterable<ArticleIndustry> classify(ArticleOrBuilder article) throws ClassifierException {
    // Compute classifications from scratch against IndustryVectors
    TopList<ArticleIndustry, Double> classifications = new TopList<>(5);
    for (IndustryCode industryCode : industryVectors.keySet()) {
      ArticleIndustry classification = classifyForIndustry(article, industryCode);
      classifications.add(classification, classification.getSimilarity());
    }
    return classifications;
  }

  public ArticleIndustry classifyForIndustry(
      ArticleOrBuilder article, IndustryCode industryCode) throws ClassifierException {
    Vector vector = IndustryVector.get(industryCode);
    Vector articleVector = new Vector(article);
    double rawSimilarity = articleVector.getCosineSimilarity(UniverseVector.getInstance(), vector);
    double similarity = DistributionBuilder.projectQuantile(
        industryDistributions.get(industryCode), rawSimilarity);
    ArticleIndustry classification = ArticleIndustry.newBuilder()
        .setIndustryCodeId(industryCode.getId())
        .setRawSimilarity(rawSimilarity)
        .setSimilarity(similarity)
        .build();
    return classification;
  }
}
