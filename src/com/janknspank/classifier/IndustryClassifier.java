package com.janknspank.classifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.janknspank.data.DataInternalException;
import com.janknspank.data.IndustryCodes;
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
      // TODO: catch errors 
     // if (industryCode.getId() == 6) {
        industryVectors.put(industryCode, new IndustryVector(industryCode));
        
     // }
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
   */
  public List<ArticleIndustryClassification> classify(Article article) 
      throws DataInternalException, IOException {
    DocumentVector articleVector = new DocumentVector(article);

    // TODO: add DB lookup for classifications before trying to recompute
    
    List<ArticleIndustryClassification> classifications = new ArrayList<>();
    ArticleIndustryClassification classification;
    IndustryCode code;
    IndustryVector vector;
    double similarity;
    
    for (Map.Entry<IndustryCode, IndustryVector> industry : industryVectors.entrySet()) {
      code = industry.getKey();
      vector = industry.getValue();
      similarity = articleVector.cosineSimilarityTo(vector);
      if (similarity >= RELEVANCE_THRESHOLD) {
        classification = ArticleIndustryClassification.newBuilder()
            .setUrlId(article.getUrlId())
            .setIndustryCodeId(code.getId())
            .setSimilarity(similarity)
            .build();
        classifications.add(classification);
      }
    }
    
    return classifications;
  }
}
