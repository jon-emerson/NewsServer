package com.janknspank.data;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.janknspank.proto.Core;
import com.janknspank.proto.Core.Article;

/**
 * Helper class that manages storing and retrieving Article objects from the
 * database.
 */
public class Articles {
  private static final String SELECT_ALL_COMMAND =
      "SELECT * FROM " + Database.getTableName(Article.class) + " LIMIT 50";

  public static final int MAX_ARTICLE_LENGTH;
  static {
    int length = 0;
    for (FieldDescriptor field : Article.getDefaultInstance().getDescriptorForType().getFields()) {
      if (JavaType.STRING == field.getJavaType() &&
          "article_body".equals(field.getName())) {
        length = field.getOptions().getExtension(Core.stringLength);
        break;
      }
    }
    if (length > 0) {
      MAX_ARTICLE_LENGTH = length;
    } else {
      throw new Error("No string length defined for article_body");
    }
  }

  /**
   * Officially sanctioned method for getting a user session from a logged-in
   * session key.
   */
  public static List<Article> getArticles()
      throws DataRequestException, DataInternalException {
    ArrayList<Article> articles = new ArrayList<Article>();

    try {
      ResultSet resultSet =
          Database.getConnection().prepareStatement(SELECT_ALL_COMMAND).executeQuery();
      do {
        articles.add(Database.createFromResultSet(resultSet, Article.class));
      } while (!resultSet.isLast());
    } catch (SQLException e) {
      throw new DataInternalException("Could not retrieve articles", e);
    }

    return articles;
  }

  /** Helper method for creating the Article table. */
  public static void main(String args[]) throws Exception {
    Connection connection = Database.getConnection();
    connection.prepareStatement(Database.getCreateTableStatement(Article.class)).execute();

//    Article.Builder builder = Article.newBuilder();
//    String id = "id" + System.currentTimeMillis();
//    builder.setAuthor("author");
//    builder.setArticleBody("body");
//    builder.setCopyright("copyright");
//    builder.setDescription("desc");
//    builder.setId(id);
//    builder.setImageUrl("image urllllz");
//    builder.setModifiedTime(500L);
//    builder.setPublishedTime(7300L);
//    builder.setTitle("title");
//    builder.setType("article");
//    builder.setUrl("http://www.nytimes.com/super/article.html");
//    Article article = builder.build();
//    Database.insert(article);
//
//    Article articleRefetched = Database.get(id, Article.class);
//    Printer.print(articleRefetched);
//
//    Article.Builder articleBuilder = articleRefetched.toBuilder();
//    articleBuilder.setArticleBody("new body");
//    articleBuilder.setDescription("new description");
//    articleBuilder.setTitle("new title");
//    Database.update(articleBuilder.build());
//
//    articleRefetched = Database.get(id, Article.class);
//    Printer.print(articleRefetched);
  }
}
