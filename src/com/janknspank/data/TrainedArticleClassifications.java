package com.janknspank.data;

import com.janknspank.data.QueryOption.WhereEquals;
import com.janknspank.proto.Core.TrainedArticleClassification;

/**
 * ArticleClassification codes on articles added by humans
 */
public class TrainedArticleClassifications {
  public static Iterable<TrainedArticleClassification> getFromArticle(String urlId) 
      throws DataInternalException {
    return Database.with(TrainedArticleClassification.class).get(new WhereEquals("url_id", urlId));
  }
  
  public static Iterable<TrainedArticleClassification> getFromClassificationCode(
      String classificationCode) throws DataInternalException {
    return Database.with(TrainedArticleClassification.class).get(
        new WhereEquals("classification_code", classificationCode));
  }
  
  /** Helper method for creating the TrainedArticleIndustry table. */
  public static void main(String args[]) throws Exception {
    Database.with(TrainedArticleClassification.class).createTable();
  }
}
