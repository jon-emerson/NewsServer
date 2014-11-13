package com.janknspank;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import org.json.JSONObject;

/**
 * Primary metadata about a crawled URL.
 */
public class CrawlData {
  public static final String TABLE_NAME_STR = "CrawlData";
  public static final String ID_STR = "id";
  public static final String TITLE_STR = "title";
  public static final String TYPE_STR = "type";
  public static final String AUTHOR_STR = "author";
  public static final String COPYRIGHT_STR = "copyright";
  public static final String DESCRIPTION_STR = "description";
  public static final String IMAGE_URL_STR = "image_url";
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
      "    " + PUBLISHED_TIME_STR + ", " +
      "    " + MODIFIED_TIME_STR + ", " +
      "    " + LAST_UPDATED_TIME_STR + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

  private String id;
  private String title;
  private String type;
  private String author;
  private String copyright;
  private String description;
  private String imageUrl;
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

    public CrawlData build() throws ValidationException {
      CrawlData data = new CrawlData();
      data.id = id;
      data.title = title;
      data.type = type;
      data.author = author;
      data.copyright = copyright;
      data.description = description;
      data.imageUrl = imageUrl;
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
    Asserts.assertNotNull(lastUpdatedTime, LAST_UPDATED_TIME_STR);
  }

  public JSONObject toJSONObject() {
    JSONObject o = new JSONObject();
    o.put(ID_STR, id);
    o.put(TITLE_STR, title);
    o.put(TYPE_STR, type);
    o.put(AUTHOR_STR, author);
    o.put(COPYRIGHT_STR, copyright);
    o.put(DESCRIPTION_STR, description);
    o.put(IMAGE_URL_STR, imageUrl);
    o.put(PUBLISHED_TIME_STR, Constants.DATE_TIME_FORMATTER.format(publishedTime));
    o.put(MODIFIED_TIME_STR, Constants.DATE_TIME_FORMATTER.format(modifiedTime));
    o.put(LAST_UPDATED_TIME_STR, Constants.DATE_TIME_FORMATTER.format(lastUpdatedTime));
    return o;
  }

  public void insert() {
    try {
      Date now = new Date();
      PreparedStatement statement =
          MysqlHelper.getInstance().prepareStatement(INSERT_COMMAND);
      statement.setString(1, this.id);
      statement.setString(2, this.title.substring(0, Math.min(this.title.length(), 256)));
      statement.setString(3, this.type);
      statement.setString(4, this.author);
      statement.setString(5, this.copyright);
      statement.setString(6, this.description);
      statement.setString(7, this.imageUrl);
      statement.setTimestamp(8, this.publishedTime == null ? null :
          new java.sql.Timestamp(this.publishedTime.getTime()));
      statement.setTimestamp(9, this.modifiedTime == null ? null :
          new java.sql.Timestamp(this.modifiedTime.getTime()));
      statement.setTimestamp(10, new java.sql.Timestamp(now.getTime()));
      statement.execute();
      this.lastUpdatedTime = now;
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  /** Helper method for creating the Link table. */
  public static void main(String args[]) {
    try {
      Statement statement = MysqlHelper.getInstance().getStatement();
      statement.executeUpdate(CREATE_TABLE_COMMAND);
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
