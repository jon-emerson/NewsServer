package com.janknspank.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import com.google.common.base.Charsets;
import com.janknspank.Asserts;
import com.janknspank.Constants;

/**
 * User's linked in data.
 */
public class LinkedInData {
  public static final String TABLE_NAME_STR = "LinkedInData";
  public static final String USER_ID_STR = "user_id";
  public static final String RAW_DATA_STR = "raw_data";
  private static final String CREATE_TIME_STR = "create_time";
  private static final String LAST_UPDATED_TIME_STR = "last_updated_time";

  private static final String CREATE_TABLE_COMMAND =
      "CREATE TABLE " + TABLE_NAME_STR + " ( " +
      "    " + USER_ID_STR + " VARCHAR(24) PRIMARY KEY, " +
      "    " + RAW_DATA_STR + " BLOB NOT NULL, " +
      "    " + CREATE_TIME_STR + " DATETIME NOT NULL, " +
      "    " + LAST_UPDATED_TIME_STR + " DATETIME NOT NULL )";
  private static final String SELECT_COMMAND =
      "SELECT * FROM " + TABLE_NAME_STR + " WHERE " + USER_ID_STR + " =?";
  private static final String INSERT_COMMAND =
      "INSERT INTO " + TABLE_NAME_STR + " ( " +
      "    " + USER_ID_STR + ", " +
      "    " + RAW_DATA_STR + ", " +
      "    " + CREATE_TIME_STR + ", " +
      "    " + LAST_UPDATED_TIME_STR + ") VALUES (?, ?, ?, ?)";
  private static final String UPDATE_COMMAND =
      "UPDATE " + TABLE_NAME_STR + " " +
      "SET " + RAW_DATA_STR + " =?, " + LAST_UPDATED_TIME_STR + " =? " +
      "WHERE " +
      "    " + USER_ID_STR + " =?";
  private static final String DELETE_COMMAND =
      "DELETE FROM " + TABLE_NAME_STR + " WHERE " + USER_ID_STR + " =?";

  private String userId;
  private String rawData;
  private Date createTime;
  private Date lastUpdatedTime;

  public String getUserId() {
    return userId;
  }

  public String getRawData() {
    return rawData;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public Date getLastUpdatedTime() {
    return lastUpdatedTime;
  }

  public static class Builder {
    private String userId;
    private String rawData;
    private Date createTime;
    private Date lastUpdatedTime;

    public Builder() {
    }

    public Builder setUserId(String userId) {
      this.userId = userId;
      return this;
    }

    public Builder setRawData(String rawData) {
      this.rawData = rawData;
      return this;
    }

    public Builder setCreateTime(Date createTime) {
      this.createTime = createTime;
      return this;
    }

    public Builder setLastUpdatedTime(Date lastUpdatedTime) {
      this.lastUpdatedTime = lastUpdatedTime;
      return this;
    }

    public LinkedInData build() throws ValidationException {
      LinkedInData linkedInData = new LinkedInData();
      linkedInData.userId = userId;
      linkedInData.rawData = rawData;
      linkedInData.createTime = (createTime == null) ? new Date() : createTime;
      linkedInData.lastUpdatedTime = (lastUpdatedTime == null) ? new Date() : lastUpdatedTime;
      linkedInData.assertValid();
      return linkedInData;
    }
  }

  private void assertValid() throws ValidationException {
    Asserts.assertNonEmpty(userId, USER_ID_STR);
    Asserts.assertNonEmpty(rawData, RAW_DATA_STR);
    Asserts.assertNotNull(createTime, CREATE_TIME_STR);
    Asserts.assertNotNull(lastUpdatedTime, LAST_UPDATED_TIME_STR);
  }

  public JSONObject toJSONObject() {
    JSONObject o = new JSONObject();
    o.put(USER_ID_STR, userId);
    o.put(RAW_DATA_STR, rawData);
    o.put(CREATE_TIME_STR, Constants.formatDate(createTime));
    o.put(LAST_UPDATED_TIME_STR, Constants.formatDate(lastUpdatedTime));
    return o;
  }

  private static String createStringFromBlob(Blob blob) throws DataInternalException {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      InputStream in = blob.getBinaryStream();
      byte[] buffer = new byte[1024];
      int readBytes = in.read(buffer, 0, buffer.length);
      while (readBytes >= 0) {
        baos.write(buffer, 0, readBytes);
        readBytes = in.read(buffer, 0, buffer.length);
      }
      return new String(baos.toByteArray());

    } catch (IOException | SQLException e) {
      throw new DataInternalException("Error creating string from blob", e);
    }
  }

  private static LinkedInData createFromResultSet(ResultSet result)
      throws SQLException, DataInternalException {
    if (result.next()) {
      LinkedInData.Builder builder = new LinkedInData.Builder();
      builder.setUserId(result.getString(USER_ID_STR));
      builder.setRawData(createStringFromBlob(result.getBlob(RAW_DATA_STR)));
      builder.setCreateTime(result.getDate(CREATE_TIME_STR));
      builder.setLastUpdatedTime(result.getDate(LAST_UPDATED_TIME_STR));
      try {
        return builder.build();
      } catch (ValidationException e) {
        throw new DataInternalException("Error building LinkedInData: " + e.getMessage(), e);
      }
    }
    return null;
  }

  public static LinkedInData get(String userId) throws DataInternalException {
    try {
      PreparedStatement statement =
          Database.getConnection().prepareStatement(SELECT_COMMAND);
      statement.setString(1, userId);
      return createFromResultSet(statement.executeQuery());
    } catch (SQLException e) {
      throw new DataInternalException("Could not select linked in data: " + e.getMessage(), e);
    }
  }

  public static void put(String userId, String rawData) throws DataInternalException {
    try {
      // See if updating the last found time updates any rows.  If it does,
      // we know we've already discovered this link before.
      java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
      PreparedStatement statement =
          Database.getConnection().prepareStatement(UPDATE_COMMAND);
      statement.setBlob(1, IOUtils.toInputStream(rawData, Charsets.UTF_8));
      statement.setTimestamp(2, now);
      statement.setString(3, userId);
      if (statement.executeUpdate() == 0) {
        // OK, so we didn't update anything.  Let's go ahead and insert.
        LinkedInData newLinkedInData = new LinkedInData.Builder()
            .setUserId(userId)
            .setRawData(rawData)
            .setCreateTime(now)
            .setLastUpdatedTime(now)
            .build();
        newLinkedInData.insert();
      }
    } catch (ValidationException | SQLException e) {
      throw new DataInternalException("Could not insert linked in data: " + e.getMessage(), e);
    }
  }

  /**
   * Deletes the specified user's linked in data.
   */
  public static void delete(String userId) {
    try {
      PreparedStatement statement =
          Database.getConnection().prepareStatement(DELETE_COMMAND);
      statement.setString(1, userId);
      statement.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private void insert() throws SQLException {
    PreparedStatement statement =
        Database.getConnection().prepareStatement(INSERT_COMMAND);
    statement.setString(1, this.userId);
    statement.setBlob(2, IOUtils.toInputStream(this.rawData, Charsets.UTF_8));
    statement.setTimestamp(3, new java.sql.Timestamp(this.createTime.getTime()));
    statement.setTimestamp(4, new java.sql.Timestamp(this.lastUpdatedTime.getTime()));
    statement.execute();
  }

  /** Helper method for creating the Link table. */
  public static void main(String args[]) {
    try {
      Statement statement = Database.getConnection().createStatement();
      statement.executeUpdate(CREATE_TABLE_COMMAND);
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
