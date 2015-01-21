package com.janknspank.data;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import com.janknspank.proto.Core.TrainedArticleClassification;

/**
 * ArticleClassification codes on articles added by humans
 */
public class TrainedArticleClassifications {
  private static final String SELECT_FOR_ARTICLE_COMMAND =
      "SELECT * FROM " + Database.getTableName(TrainedArticleClassification.class) + " "
      + "WHERE url_id=?";
  private static final String SELECT_FOR_CLASSIFICATION_COMMAND =
      "SELECT * FROM " + Database.getTableName(TrainedArticleClassification.class) + " "
      + "WHERE classification_code=?";
  
  public static List<TrainedArticleClassification> getFromArticle(String urlId) 
      throws DataInternalException {
    try {
      PreparedStatement stmt = Database.getInstance().prepareStatement(SELECT_FOR_ARTICLE_COMMAND);
      stmt.setString(1, urlId);
      return Database.createListFromResultSet(stmt.executeQuery(), TrainedArticleClassification.class);
    } catch (SQLException e) {
      throw new DataInternalException("Error fetching classification codes for article", e);
    }
  }
  
  public static List<TrainedArticleClassification> getFromClassificationCode(String classificationCode) 
      throws DataInternalException {
    try {
      PreparedStatement stmt = Database.getInstance().prepareStatement(SELECT_FOR_CLASSIFICATION_COMMAND);
      stmt.setString(1, classificationCode);
      return Database.createListFromResultSet(stmt.executeQuery(), TrainedArticleClassification.class);
    } catch (SQLException e) {
      throw new DataInternalException("Error fetching articles for industry code", e);
    }
  }
  
  /** Helper method for creating the TrainedArticleIndustry table. */
  public static void main(String args[]) throws Exception {
    Database database = Database.getInstance();
    database.prepareStatement(database.getCreateTableStatement(TrainedArticleClassification.class)).execute();
    for (String statement : database.getCreateIndexesStatement(TrainedArticleClassification.class)) {
      database.prepareStatement(statement).execute();
    }
  }
}
