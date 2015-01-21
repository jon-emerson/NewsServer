package com.janknspank.data;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import com.janknspank.proto.Core.TrainedArticleIndustry;

/**
 * Industry codes on articles added by humans
 */
public class TrainedArticleIndustries {
  private static final String SELECT_FOR_ARTICLE_COMMAND =
      "SELECT * FROM " + Database.getTableName(TrainedArticleIndustry.class) + " "
      + "WHERE url_id=?";
  private static final String SELECT_FOR_INDUSTRY_COMMAND =
      "SELECT * FROM " + Database.getTableName(TrainedArticleIndustry.class) + " "
      + "WHERE industry_code_id=?";
  
  public static List<TrainedArticleIndustry> getFromArticle(String urlId) 
      throws DataInternalException {
    try {
      PreparedStatement stmt = Database.getInstance().prepareStatement(SELECT_FOR_ARTICLE_COMMAND);
      stmt.setString(1, urlId);
      return Database.createListFromResultSet(stmt.executeQuery(), TrainedArticleIndustry.class);
    } catch (SQLException e) {
      throw new DataInternalException("Error fetching industry codes for article", e);
    }
  }
  
  public static List<TrainedArticleIndustry> getFromIndustryCode(int industryCode) 
      throws DataInternalException {
    try {
      PreparedStatement stmt = Database.getInstance().prepareStatement(SELECT_FOR_INDUSTRY_COMMAND);
      stmt.setInt(1, industryCode);
      return Database.createListFromResultSet(stmt.executeQuery(), TrainedArticleIndustry.class);
    } catch (SQLException e) {
      throw new DataInternalException("Error fetching articles for industry code", e);
    }
  }
  
  /** Helper method for creating the TrainedArticleIndustry table. */
  public static void main(String args[]) throws Exception {
    Database database = Database.getInstance();
    database.prepareStatement(database.getCreateTableStatement(TrainedArticleIndustry.class)).execute();
	    for (String statement : database.getCreateIndexesStatement(TrainedArticleIndustry.class)) {
	      database.prepareStatement(statement).execute();
	    }
	  }
}