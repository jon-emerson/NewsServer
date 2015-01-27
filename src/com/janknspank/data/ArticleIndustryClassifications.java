package com.janknspank.data;

import java.util.List;

import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.ArticleIndustryClassification;
import com.janknspank.data.QueryOption.WhereEquals;

public class ArticleIndustryClassifications {
  public static List<ArticleIndustryClassification> getFor(Article article) 
      throws DataInternalException {
    return Database.with(ArticleIndustryClassification.class).get(
        new WhereEquals("url_id", article.getUrlId()));
  }
  
  /** Helper method for creating the Article table. */
  public static void main(String args[]) throws Exception {
    Database.with(ArticleIndustryClassification.class).createTable();
  }
}
