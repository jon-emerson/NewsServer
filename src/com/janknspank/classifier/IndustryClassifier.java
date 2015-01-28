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
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.ArticleIndustryClassification;
import com.janknspank.proto.Core.IndustryCode;

public class IndustryClassifier {
  private static IndustryClassifier instance = null;
  private static Map<IndustryCode, IndustryVector> industryVectors;
  private static final double RELEVANCE_THRESHOLD = 0.01;
  
  private IndustryClassifier() throws IOException, DataInternalException {
    industryVectors = new HashMap<>();
    for (IndustryCode industryCode : IndustryCodes.INDUSTRY_CODE_MAP.values()) {
      try {
        industryVectors.put(industryCode, new IndustryVector(industryCode));
      } catch (IOException | DataInternalException e) {
        // Can't generate industry vector. Ignore it.
      }        
    }
  }
  
  public static synchronized IndustryClassifier getInstance() 
      throws IOException, DataInternalException {
    if(instance == null) {
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
      throws DataInternalException, IOException, ValidationException {
    DocumentVector articleVector = new DocumentVector(article);

    // See if the classification has already been computed
    Iterable<ArticleIndustryClassification> classifications =
        ArticleIndustryClassifications.getFor(article);
    if (classifications != null && Iterables.size(classifications) > 0) {
      return classifications;
    }
    
    // Compute classifications from IndustryVectors
    List<ArticleIndustryClassification> newClassifications = new ArrayList<>();
    for (Map.Entry<IndustryCode, IndustryVector> industry : industryVectors.entrySet()) {
      IndustryCode code = industry.getKey();
      IndustryVector vector = industry.getValue();
      double similarity = articleVector.cosineSimilarityTo(vector);
      // Only save industries that are closely related
      if (similarity >= RELEVANCE_THRESHOLD) {
        ArticleIndustryClassification classification = 
            ArticleIndustryClassification.newBuilder()
            .setUrlId(article.getUrlId())
            .setIndustryCodeId(code.getId())
            .setSimilarity(similarity)
            .build();
        newClassifications.add(classification);
      }
    }
    saveClassificationsToServer(newClassifications);
    return classifications;
  }
  
  private static void saveClassificationsToServer(
      Iterable<ArticleIndustryClassification> classifications)
      throws ValidationException, DataInternalException {
    Database.insert(classifications);
  }
}
