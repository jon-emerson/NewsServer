package com.janknspank.bizness;

import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption.WhereEquals;
import com.janknspank.proto.CoreProto.TrainedArticleIndustry;

/**
 * Industry codes on articles added by humans
 */
public class TrainedArticleIndustries {
  public static Iterable<TrainedArticleIndustry> getFromArticle(String urlId) 
      throws DatabaseSchemaException {
    return Database.with(TrainedArticleIndustry.class).get(
        new WhereEquals("url_id", urlId));
  }

  public static Iterable<TrainedArticleIndustry> getFromIndustryCode(int industryCode) 
      throws DatabaseSchemaException {
    return Database.with(TrainedArticleIndustry.class).get(
        new WhereEquals("industry_code_id", Integer.toString(industryCode)));
  }

  /** Helper method for creating the TrainedArticleIndustry table. */
  public static void main(String args[]) throws Exception {
    Database.with(TrainedArticleIndustry.class).createTable();
  }
}
