package com.janknspank.classifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
      // TODO: get rid of this clause so as industry seed files are made
      if (industryCode.getId() == 6) {
        try {
          industryVectors.put(industryCode, new IndustryVector(industryCode));
        } catch (IOException | DataInternalException e) {
          System.out.println("Couldn't generate industry vector for industry code: " + 
              industryCode.getId());
          //e.printStackTrace();
        }        
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
  public List<ArticleIndustryClassification> classify(Article article) 
      throws DataInternalException, IOException, ValidationException {
    DocumentVector articleVector = new DocumentVector(article);

    // See if the classifications has already been computed
    List<ArticleIndustryClassification> classifications =
        ArticleIndustryClassifications.getFor(article);
    if (classifications != null && classifications.size() > 0) {
      return classifications;
    }
    
    // Compute classifications from IndustryVectors
    classifications = new ArrayList<>();
    ArticleIndustryClassification classification;
    IndustryCode code;
    IndustryVector vector;
    double similarity;
    
    System.out.println("classifying against " + industryVectors.size() + " industry vectors");
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
    saveClassificationsToServer(classifications);
    
    return classifications;
  }
  
  private static void saveClassificationsToServer(List<ArticleIndustryClassification> classifications)
      throws ValidationException, DataInternalException {
    Database.insert(classifications);
  }
}
