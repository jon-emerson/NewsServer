package com.janknspank.data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONObject;

import com.janknspank.Asserts;
import com.janknspank.Constants;

/**
 * Metadata about a news article.
 */
public class Article {
  public static final String TABLE_NAME_STR = "CrawlData";
  public static final String ID_STR = "id";
  public static final String TITLE_STR = "title";
  public static final String TYPE_STR = "type";
  public static final String AUTHOR_STR = "author";
  public static final String COPYRIGHT_STR = "copyright";
  public static final String DESCRIPTION_STR = "description";
  public static final String IMAGE_URL_STR = "image_url";
  public static final String ARTICLE_BODY_STR = "article_body";
  public static final String PUBLISHED_TIME_STR = "published_time";
  public static final String MODIFIED_TIME_STR = "modified_time";
  public static final String LAST_UPDATED_TIME_STR = "last_updated_time";

  private static final String CREATE_TABLE_COMMAND =
      "CREATE TABLE " + TABLE_NAME_STR + " ( " +
      "    " + ID_STR + " VARCHAR(24) PRIMARY KEY, " +
      "    " + TITLE_STR + " VARCHAR(256) NOT NULL, " +
      "    " + TYPE_STR + " VARCHAR(30), " +
      "    " + AUTHOR_STR + " VARCHAR(100), " +
      "    " + COPYRIGHT_STR + " VARCHAR(100), " +
      "    " + DESCRIPTION_STR + " BLOB, " +
      "    " + IMAGE_URL_STR + " BLOB, " +
      "    " + ARTICLE_BODY_STR + " BLOB, " +
      "    " + PUBLISHED_TIME_STR + " DATETIME, " +
      "    " + MODIFIED_TIME_STR + " DATETIME, " +
      "    " + LAST_UPDATED_TIME_STR + " DATETIME NOT NULL)";
  private static final String INSERT_COMMAND =
      "INSERT INTO " + TABLE_NAME_STR + " ( " +
      "    " + ID_STR + ", " +
      "    " + TITLE_STR + ", " +
      "    " + TYPE_STR + ", " +
      "    " + AUTHOR_STR + ", " +
      "    " + COPYRIGHT_STR + ", " +
      "    " + DESCRIPTION_STR + ", " +
      "    " + IMAGE_URL_STR + ", " +
      "    " + ARTICLE_BODY_STR + ", " +
      "    " + PUBLISHED_TIME_STR + ", " +
      "    " + MODIFIED_TIME_STR + ", " +
      "    " + LAST_UPDATED_TIME_STR + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
  private static final String SELECT_ALL_COMMAND =
      "SELECT * FROM " + TABLE_NAME_STR + " LIMIT 50";

  private String id;
  private String title;
  private String type;
  private String author;
  private String copyright;
  private String description;
  private String imageUrl;
  private String articleBody;
  private Date publishedTime;
  private Date modifiedTime;
  private Date lastUpdatedTime;

  public String getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getType() {
    return type;
  }

  public String getAuthor() {
    return author;
  }

  public String getCopyright() {
    return copyright;
  }

  public String getDescription() {
    return description;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public String getArticleBody() {
    return articleBody;
  }

  public Date getPublishedTime() {
    return publishedTime;
  }

  public Date getModifiedTime() {
    return modifiedTime;
  }

  public Date getLastUpdatedTime() {
    return lastUpdatedTime;
  }

  public static class Builder {
    private String id;
    private String title = null;
    private String type;
    private String author;
    private String copyright;
    private String description;
    private String imageUrl;
    private String articleBody;
    private Date publishedTime;
    private Date modifiedTime;
    private Date lastUpdatedTime;

    public Builder() {
    }

    public Builder setId(String id) {
      this.id = id;
      return this;
    }

    public Builder setTitle(String title) {
      this.title = title;
      return this;
    }

    public boolean hasTitle() {
      return title != null;
    }

    public Builder setType(String type) {
      this.type = type;
      return this;
    }

    public Builder setAuthor(String author) {
      this.author = author;
      return this;
    }

    public Builder setCopyright(String copyright) {
      this.copyright = copyright;
      return this;
    }

    public Builder setDescription(String description) {
      this.description = description;
      return this;
    }

    public Builder setImageUrl(String imageUrl) {
      this.imageUrl = imageUrl;
      return this;
    }

    public Builder setArticleBody(String articleBody) {
      this.articleBody = articleBody;
      return this;
    }

    public Builder setPublishedTime(Date publishedTime) {
      this.publishedTime = publishedTime;
      return this;
    }

    public Builder setModifiedTime(Date modifiedTime) {
      this.modifiedTime = modifiedTime;
      return this;
    }

    public Builder setLastUpdatedTime(Date lastUpdatedTime) {
      this.lastUpdatedTime = lastUpdatedTime;
      return this;
    }

    public Article build() throws ValidationException {
      Article data = new Article();
      data.id = id;
      data.title = title;
      data.type = type;
      data.author = author;
      data.copyright = copyright;
      data.description = description;
      data.imageUrl = imageUrl;
      data.articleBody = articleBody;
      data.publishedTime = publishedTime;
      data.modifiedTime = modifiedTime;
      data.lastUpdatedTime = (lastUpdatedTime == null) ? new Date() : lastUpdatedTime;
      data.assertValid();
      return data;
    }
  }

  private void assertValid() throws ValidationException {
    Asserts.assertNonEmpty(id, ID_STR);
    Asserts.assertNonEmpty(title, TITLE_STR);
    Asserts.assertNonEmpty(articleBody, ARTICLE_BODY_STR);
    Asserts.assertNotNull(lastUpdatedTime, LAST_UPDATED_TIME_STR);
  }

  public JSONObject toJSONObject() {
    // Deliberately omitting articleBody since it's really big and the client
    // doesn't actually need it.

    JSONObject o = new JSONObject();
    o.put(ID_STR, id);
    o.put(TITLE_STR, title);
    if (type != null) {
      o.put(TYPE_STR, type);
    }
    if (author != null) {
      o.put(AUTHOR_STR, author);
    }
    if (copyright != null) {
      o.put(COPYRIGHT_STR, copyright);
    }
    if (description != null) {
      o.put(DESCRIPTION_STR, description);
    }
    if (imageUrl != null) {
      o.put(IMAGE_URL_STR, imageUrl);
    }
    if (publishedTime != null) {
      o.put(PUBLISHED_TIME_STR, Constants.formatDate(publishedTime));
    }
    if (modifiedTime != null) {
      o.put(MODIFIED_TIME_STR, Constants.formatDate(modifiedTime));
    }
    o.put(LAST_UPDATED_TIME_STR, Constants.formatDate(lastUpdatedTime));
    return o;
  }

  public void insert() {
    try {
      Date now = new Date();
      PreparedStatement statement =
          MysqlHelper.getConnection().prepareStatement(INSERT_COMMAND);
      statement.setString(1, this.id);
      statement.setString(2, this.title.substring(0, Math.min(this.title.length(), 256)));
      statement.setString(3, this.type);
      statement.setString(4, this.author);
      statement.setString(5, this.copyright);
      statement.setString(6, this.description);
      statement.setString(7, this.imageUrl);
      statement.setString(8, this.articleBody);
      statement.setTimestamp(9, this.publishedTime == null ? null :
          new java.sql.Timestamp(this.publishedTime.getTime()));
      statement.setTimestamp(10, this.modifiedTime == null ? null :
          new java.sql.Timestamp(this.modifiedTime.getTime()));
      statement.setTimestamp(11, new java.sql.Timestamp(now.getTime()));
      statement.execute();
      this.lastUpdatedTime = now;
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private static Article createFromResultSet(ResultSet result)
      throws SQLException, DataInternalException {
    if (result.next()) {
      Article.Builder builder = new Article.Builder();
      builder.setId(result.getString(ID_STR));
      builder.setTitle(result.getString(TITLE_STR));
      builder.setType(result.getString(TYPE_STR));
      builder.setAuthor(result.getString(AUTHOR_STR));
      builder.setCopyright(result.getString(COPYRIGHT_STR));
      builder.setDescription(result.getString(DESCRIPTION_STR));
      builder.setImageUrl(result.getString(IMAGE_URL_STR));
      builder.setArticleBody(result.getString(ARTICLE_BODY_STR));
      builder.setPublishedTime(result.getDate(PUBLISHED_TIME_STR));
      builder.setModifiedTime(result.getDate(MODIFIED_TIME_STR));
      builder.setLastUpdatedTime(result.getDate(LAST_UPDATED_TIME_STR));
      try {
        return builder.build();
      } catch (ValidationException e) {
        throw new DataInternalException("Could not create article object: " + e.getMessage(), e);
      }
    }
    return null;
  }

  /**
   * Officially sanctioned method for getting a user session from a logged-in
   * session key.
   */
  public static List<Article> getArticles()
      throws DataRequestException, DataInternalException {
    try {
      ArrayList<Article> articles = new ArrayList<Article>();
      PreparedStatement statement =
          MysqlHelper.getConnection().prepareStatement(SELECT_ALL_COMMAND);
      ResultSet resultSet = statement.executeQuery();
      Article article = createFromResultSet(resultSet);
      while (article != null) {
        articles.add(article);
        article = createFromResultSet(resultSet);
      }
      return articles;
    } catch (SQLException e) {
      throw new DataInternalException("Could not retrieve articles", e);
    }
  }

  /** Helper method for creating the Link table. */
  public static void main(String args[]) {
    try {
      Statement statement = MysqlHelper.getConnection().createStatement();
      statement.executeUpdate(CREATE_TABLE_COMMAND);
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
