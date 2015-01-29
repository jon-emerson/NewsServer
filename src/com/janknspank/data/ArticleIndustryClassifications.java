package com.janknspank.data;

import com.janknspank.data.QueryOption.WhereEquals;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.ArticleIndustryClassification;

public class ArticleIndustryClassifications {
  public static Iterable<ArticleIndustryClassification> getFor(Article article) 
      throws DataInternalException {
    return Database.with(ArticleIndustryClassification.class).get(
        new WhereEquals("url_id", article.getUrlId()));
  }

  /** Helper method for creating the Article table. */
  public static void main(String args[]) throws Exception {
    Database.with(ArticleIndustryClassification.class).createTable();
  }
}
