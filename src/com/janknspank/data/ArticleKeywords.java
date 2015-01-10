package com.janknspank.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.ArticleKeyword;
import com.janknspank.proto.Extensions;

/**
 * Helper class that manages storing and retrieving which keywords are
 * associated with which articles.
 */
public class ArticleKeywords {
  public static final String TYPE_HYPERLINK = "h";
  public static final String TYPE_LOCATION = "l";
  public static final String TYPE_META_TAG = "m";
  public static final String TYPE_PERSON = "p";
  public static final String TYPE_ORGANIZATION = "o";

  public static final int MAX_KEYWORD_LENGTH;
  static {
    int keywordLength = 0;
    for (FieldDescriptor field :
        ArticleKeyword.getDefaultInstance().getDescriptorForType().getFields()) {
      if (JavaType.STRING == field.getJavaType()) {
        if ("keyword".equals(field.getName())) {
          keywordLength = field.getOptions().getExtension(Extensions.stringLength);
        }
      }
    }
    if (keywordLength == 0) {
      throw new IllegalStateException("Could not find length of keyword field");
    }
    MAX_KEYWORD_LENGTH = keywordLength;
  }

  private static final String DELETE_BY_URL_ID_COMMAND =
      "DELETE FROM " + Database.getTableName(ArticleKeyword.class) + " WHERE url_id=?";

  /**
   * Returns all of the ArticleKeywords associated with any of the passed-in
   * articles.
   */
  public static List<ArticleKeyword> get(Iterable<Article> articleList)
      throws DataInternalException {
    StringBuilder sql = new StringBuilder();
    sql.append("SELECT * FROM " + Database.getTableName(ArticleKeyword.class)
        + " WHERE url_id IN (");
    sql.append(Joiner.on(", ").join(
        Iterables.limit(Iterables.cycle("?"), Iterables.size(articleList))));
    sql.append(")");

    try {
      PreparedStatement stmt = Database.getConnection().prepareStatement(sql.toString());
      int i = 0;
      for (Article article : articleList) {
        stmt.setString(++i, article.getUrlId());
      }
      List<ArticleKeyword> keywordList =
          Database.createListFromResultSet(stmt.executeQuery(), ArticleKeyword.class);
      return keywordList;
    } catch (SQLException e) {
      throw new DataInternalException("Could not read article keywords: " + e.getMessage(), e);
    }
  }

  public static int deleteForUrlIds(Iterable<String> urlIds) throws DataInternalException {
    try {
      PreparedStatement statement =
          Database.getConnection().prepareStatement(DELETE_BY_URL_ID_COMMAND);
      for (String urlId : urlIds) {
        statement.setString(1, urlId);
        statement.addBatch();
      }
      return Database.sumIntArray(statement.executeBatch());
    } catch (SQLException e) {
      throw new DataInternalException("Could not delete url IDs: " + e.getMessage(), e);
    }
  }

  /** Helper method for creating the Article table. */
  public static void main(String args[]) throws Exception {
    Connection connection = Database.getConnection();
    connection.prepareStatement(Database.getCreateTableStatement(ArticleKeyword.class)).execute();
    for (String statement : Database.getCreateIndexesStatement(ArticleKeyword.class)) {
      connection.prepareStatement(statement).execute();
    }
  }
}
