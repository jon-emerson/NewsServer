package com.janknspank.bizness;

import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption.WhereEquals;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleIndustry;

public class ArticleIndustryClassifications {
  public static Iterable<ArticleIndustry> getFor(Article article)
      throws DatabaseSchemaException {
    return Database.with(ArticleIndustry.class).get(
        new WhereEquals("url_id", article.getUrlId()));
  }

  /** Helper method for creating the Article table. */
  public static void main(String args[]) throws Exception {
    Database.with(ArticleIndustry.class).createTable();
  }
}
