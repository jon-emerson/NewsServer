package com.janknspank.data;

import com.janknspank.data.QueryOption.WhereEquals;
import com.janknspank.proto.Core.TrainedArticleIndustry;

/**
 * Industry codes on articles added by humans
 */
public class TrainedArticleIndustries {
  public static Iterable<TrainedArticleIndustry> getFromArticle(String urlId) 
      throws DataInternalException {
    return Database.with(TrainedArticleIndustry.class).get(
        new WhereEquals("url_id", urlId));
  }
  
  public static Iterable<TrainedArticleIndustry> getFromIndustryCode(int industryCode) 
      throws DataInternalException {
    return Database.with(TrainedArticleIndustry.class).get(
        new WhereEquals("industry_code_id", Integer.toString(industryCode)));
  }

  /** Helper method for creating the TrainedArticleIndustry table. */
  public static void main(String args[]) throws Exception {
    Database.with(TrainedArticleIndustry.class).createTable();
  }
}
