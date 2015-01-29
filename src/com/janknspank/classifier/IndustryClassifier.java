package com.janknspank.classifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterables;
import com.janknspank.data.ArticleIndustryClassifications;
import com.janknspank.data.DataInternalException;
import com.janknspank.data.Database;
import com.janknspank.data.IndustryCodes;
import com.janknspank.data.ValidationException;
import com.janknspank.data.QueryOption.WhereEquals;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.ArticleIndustryClassification;
import com.janknspank.proto.Core.IndustryCode;

public class IndustryClassifier {
  private static IndustryClassifier instance = null;
  private static Map<IndustryCode, IndustryVector> industryVectors;
  private static final double RELEVANCE_THRESHOLD = 0.01;
  
  private IndustryClassifier() {
    industryVectors = new HashMap<>();
    for (IndustryCode industryCode : IndustryCodes.INDUSTRY_CODE_MAP.values()) {
      try {
        industryVectors.put(industryCode, new IndustryVector(industryCode));
      } catch (DataInternalException e) {
        // Can't generate industry vector. Ignore it.
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
   * Returns a list of Article Industry Classifications
   * @param article
   * @return
   * @throws DataInternalException
   * @throws IOException 
   * @throws ValidationException 
   */
  public Iterable<ArticleIndustryClassification> classify(Article article) 
      throws DataInternalException, ValidationException {
    // See if the classification has already been computed
    Iterable<ArticleIndustryClassification> classifications =
        ArticleIndustryClassifications.getFor(article);
    if (classifications != null && Iterables.size(classifications) > 0) {
      return classifications;
    }
    
    // Compute classifications from scratch against IndustryVectors
    List<ArticleIndustryClassification> newClassifications = new ArrayList<>();
    for (IndustryCode industryCode : industryVectors.keySet()) {
      ArticleIndustryClassification classification = 
          classifyForIndustry(article, industryCode);
      if (classification.getSimilarity() >= RELEVANCE_THRESHOLD) {
        newClassifications.add(classification);
      }
    }
    saveClassificationsToServer(newClassifications);
    return classifications;
  }
  
  public ArticleIndustryClassification classifyForIndustry(
      Article article, IndustryCode industryCode) 
      throws DataInternalException {
    IndustryVector vector = industryVectors.get(industryCode);
    DocumentVector articleVector = new DocumentVector(article);
    double similarity = articleVector.cosineSimilarityTo(vector);
    // Only save industries that are closely related
    ArticleIndustryClassification classification = 
        ArticleIndustryClassification.newBuilder()
            .setUrlId(article.getUrlId())
            .setIndustryCodeId(industryCode.getId())
            .setSimilarity(similarity)
            .build();
    return classification;
  }
  
  private static void saveClassificationsToServer(
      Iterable<ArticleIndustryClassification> classifications)
      throws ValidationException, DataInternalException {
    ArticleIndustryClassification classification = 
        Iterables.getFirst(classifications, null);
    if (classification != null) {
      String urlId = classification.getUrlId();
      Database.with(ArticleIndustryClassification.class).delete(
          new WhereEquals("url_id", urlId));
      Database.insert(classifications);
    }
  }
}
