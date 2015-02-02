package com.janknspank.classifier;

import java.util.HashMap;
import java.util.Map;

import com.janknspank.common.TopList;
import com.janknspank.proto.ArticleProto.ArticleIndustry;
import com.janknspank.proto.ArticleProto.ArticleOrBuilder;
import com.janknspank.proto.EnumsProto.IndustryCode;

public class IndustryClassifier {
  private static IndustryClassifier instance = null;
  static Map<IndustryCode, Vector> industryVectors;

  private IndustryClassifier() {
    industryVectors = new HashMap<>();
    for (IndustryCode industryCode : IndustryCodes.INDUSTRY_CODE_MAP.values()) {
      try {
        Vector industryVector = IndustryVector.get(industryCode);
        if (industryVector.getDocumentCount() == 0) {
          System.out.println("WARNING: Industry vector has no documents: "
              + industryCode.getId() + ", " + industryCode.getDescription());
        } else {
          industryVectors.put(industryCode, industryVector);
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
    double similarity = articleVector.getCosineSimilarity(UniverseVector.getInstance(), vector);
    ArticleIndustry classification = ArticleIndustry.newBuilder()
        .setIndustryCodeId(industryCode.getId())
        .setSimilarity(similarity)
        .build();
    return classification;
  }
}
