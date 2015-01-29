package com.janknspank.bizness;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.ArticleKeyword;

/**
 * Helper class that manages storing and retrieving which keywords are
 * associated with which articles.
 */
public class ArticleKeywords {
  public static final int MAX_KEYWORD_LENGTH =
      Database.with(ArticleKeyword.class).getStringLength("keyword");

  /**
   * Returns all of the ArticleKeywords associated with any of the passed-in
   * articles.
   * @throws DatabaseSchemaException 
   */
  public static Iterable<ArticleKeyword> get(Iterable<Article> articleList)
      throws DatabaseSchemaException {
    return Database.with(ArticleKeyword.class).get(
        new QueryOption.WhereEquals("url_id",
            Iterables.transform(articleList, new Function<Article, String>() {
              @Override
              public String apply(Article article) {
                return article.getUrlId();
              }
            })));
  }

  public static int deleteForUrlIds(Iterable<String> urlIds) throws DatabaseSchemaException {
    return Database.with(ArticleKeyword.class).delete(
        new QueryOption.WhereEquals("url_id", urlIds));
  }

  /** Helper method for creating the Article table. */
  public static void main(String args[]) throws Exception {
    Database.with(ArticleKeyword.class).createTable();
  }
}
