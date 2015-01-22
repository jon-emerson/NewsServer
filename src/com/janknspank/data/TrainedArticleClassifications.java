package com.janknspank.data;

import java.util.List;

import com.janknspank.proto.Core.TrainedArticleClassification;

/**
 * ArticleClassification codes on articles added by humans
 */
public class TrainedArticleClassifications {
  public static List<TrainedArticleClassification> getFromArticle(String urlId) 
      throws DataInternalException {
    return Database.getInstance().get(TrainedArticleClassification.class,
        new QueryOption.WhereEquals("url_id", urlId));
  }
  
  public static List<TrainedArticleClassification> getFromClassificationCode(
      String classificationCode) throws DataInternalException {
    return Database.getInstance().get(TrainedArticleClassification.class,
        new QueryOption.WhereEquals("classification_code", classificationCode));
  }
  
  /** Helper method for creating the TrainedArticleIndustry table. */
  public static void main(String args[]) throws Exception {
    Database.getInstance().createTable(TrainedArticleClassification.class);
  }
}
