package com.janknspank.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.protobuf.Message;
import com.janknspank.dom.InterpretedData;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.ArticleKeyword;

/**
 * Helper class that manages storing and retrieving which keywords are
 * associated with which articles.
 */
public class ArticleKeywords {
  public static void add(Article article, InterpretedData interpretedData)
      throws DataInternalException, ValidationException {
    List<Message> keywords = Lists.newArrayList();
    for (String location : interpretedData.getLocations()) {
      keywords.add(ArticleKeyword.newBuilder()
          .setArticleId(article.getId())
          .setKeyword(location)
          .setStrength(interpretedData.getLocationCount(location))
          .setType("l")
          .build());
    }
    for (String person : interpretedData.getPeople()) {
      keywords.add(ArticleKeyword.newBuilder()
          .setArticleId(article.getId())
          .setKeyword(person)
          .setStrength(interpretedData.getPersonCount(person))
          .setType("p")
          .build());
    }
    for (String organization : interpretedData.getOrganizations()) {
      keywords.add(ArticleKeyword.newBuilder()
          .setArticleId(article.getId())
          .setKeyword(organization)
          .setStrength(interpretedData.getOrganizationCount(organization))
          .setType("o")
          .build());
    }
    Database.insert(keywords);
  }

  /**
   * Returns all of the ArticleKeywords associated with any of the passed-in
   * articles.
   */
  public static List<ArticleKeyword> get(List<Article> articleList)
      throws DataInternalException {
    StringBuilder sql = new StringBuilder();
    sql.append("SELECT * FROM " + Database.getTableName(ArticleKeyword.class) +
        " WHERE article_id IN (");
    for (int i = 0; i < articleList.size(); i++) {
      sql.append(i == articleList.size() - 1 ? "?" : "?, ");
    }
    sql.append(")");

    try {
      PreparedStatement stmt = Database.getConnection().prepareStatement(sql.toString());
      for (int i = 0; i < articleList.size(); i++) {
        stmt.setString(i + 1, articleList.get(i).getId());
      }
      ResultSet result = stmt.executeQuery();
      List<ArticleKeyword> keywordList = Lists.newArrayList();
      while (!result.isAfterLast()) {
        ArticleKeyword keyword = Database.createFromResultSet(result, ArticleKeyword.class);
        if (keyword != null) {
          keywordList.add(keyword);
        }
      }
      return keywordList;
    } catch (SQLException e) {
      throw new DataInternalException("Could not read article keywords: " + e.getMessage(), e);
    }
  }

  /** Helper method for creating the Article table. */
  public static void main(String args[]) throws Exception {
    Connection connection = Database.getConnection();
    connection.prepareStatement(Database.getCreateTableStatement(ArticleKeyword.class)).execute();
  }
}
