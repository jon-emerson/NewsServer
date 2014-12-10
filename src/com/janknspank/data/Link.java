package com.janknspank.data;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import org.json.JSONObject;

import com.janknspank.Asserts;
import com.janknspank.Constants;
import com.janknspank.ValidationException;

/**
 * Tracks a link from one URL's content to another's.  The primary key is a
 * composite of the origin and destination URL IDs, as defined in the
 * DiscoveredUrl table.
 */
public class Link {
  public static final String TABLE_NAME_STR = "Link";
  public static final String ORIGIN_ID_STR = "origin_id";
  public static final String DESTINATION_ID_STR = "dest_id";
  private static final String DISCOVERY_TIME_STR = "discovery_time";
  private static final String LAST_FOUND_TIME_STR = "last_found_time";

  private static final String CREATE_TABLE_COMMAND =
      "CREATE TABLE " + TABLE_NAME_STR + " ( " +
      "    " + ORIGIN_ID_STR + " VARCHAR(24) NOT NULL, " +
      "    " + DESTINATION_ID_STR + " VARCHAR(24) NOT NULL, " +
      "    " + DISCOVERY_TIME_STR + " DATETIME NOT NULL, " +
      "    " + LAST_FOUND_TIME_STR + " DATETIME NOT NULL, " +
      "    PRIMARY KEY (" + ORIGIN_ID_STR + ", " + DESTINATION_ID_STR + "))";
  private static final String CREATE_ORIGIN_ID_INDEX_COMMAND =
      "CREATE INDEX " + ORIGIN_ID_STR + "_index " +
      "    ON " + TABLE_NAME_STR +
      "    (" + ORIGIN_ID_STR + ") USING HASH";
  private static final String CREATE_DESTINATION_ID_INDEX_COMMAND =
      "CREATE INDEX " + DESTINATION_ID_STR + "_index " +
      "    ON " + TABLE_NAME_STR +
      "    (" + DESTINATION_ID_STR + ") USING HASH";
  private static final String INSERT_COMMAND =
      "INSERT INTO " + TABLE_NAME_STR + " ( " +
      "    " + ORIGIN_ID_STR + ", " +
      "    " + DESTINATION_ID_STR + ", " +
      "    " + DISCOVERY_TIME_STR + ", " +
      "    " + LAST_FOUND_TIME_STR + ") VALUES (?, ?, ?, ?)";
  private static final String UPDATE_LAST_FOUND_TIME_COMMAND =
      "UPDATE " + TABLE_NAME_STR + " SET " + LAST_FOUND_TIME_STR + " =? WHERE " +
      "    " + ORIGIN_ID_STR + " =? AND" +
      "    " + DESTINATION_ID_STR + " =?";
  private static final String DELETE_COMMAND =
      "DELETE FROM " + TABLE_NAME_STR + " WHERE " +
      "    " + ORIGIN_ID_STR + " =? OR" +
      "    " + DESTINATION_ID_STR + " =?";

  private String originId;
  private String destinationId;
  private Date discoveryTime;
  private Date lastFoundTime;

  public String getOriginId() {
    return originId;
  }

  public String getDestinationId() {
    return destinationId;
  }

  public Date getDiscoveryTime() {
    return discoveryTime;
  }

  public Date getLastFoundTime() {
    return lastFoundTime;
  }

  public static class Builder {
    private String originId;
    private String destinationId;
    private Date discoveryTime;
    private Date lastFoundTime;

    public Builder() {
    }

    public Builder setOriginId(String originId) {
      this.originId = originId;
      return this;
    }

    public Builder setDestinationId(String destinationId) {
      this.destinationId = destinationId;
      return this;
    }

    public Builder setDiscoveryTime(Date discoveryTime) {
      this.discoveryTime = discoveryTime;
      return this;
    }

    public Builder setLastFoundTime(Date lastFoundTime) {
      this.lastFoundTime = lastFoundTime;
      return this;
    }

    public Link build() throws ValidationException {
      Link link = new Link();
      link.originId = originId;
      link.destinationId = destinationId;
      link.discoveryTime = (discoveryTime == null) ? new Date() : discoveryTime;
      link.lastFoundTime = (lastFoundTime == null) ? new Date() : lastFoundTime;
      link.assertValid();
      return link;
    }
  }

  private void assertValid() throws ValidationException {
    Asserts.assertNonEmpty(originId, ORIGIN_ID_STR);
    Asserts.assertNonEmpty(destinationId, DESTINATION_ID_STR);
    Asserts.assertNotNull(discoveryTime, DISCOVERY_TIME_STR);
    Asserts.assertNotNull(lastFoundTime, LAST_FOUND_TIME_STR);
  }

  public JSONObject toJSONObject() {
    JSONObject o = new JSONObject();
    o.put(ORIGIN_ID_STR, originId);
    o.put(DESTINATION_ID_STR, destinationId);
    o.put(DISCOVERY_TIME_STR, Constants.DATE_TIME_FORMATTER.format(discoveryTime));
    o.put(LAST_FOUND_TIME_STR, Constants.DATE_TIME_FORMATTER.format(lastFoundTime));
    return o;
  }

  public static void put(String originId, String destinationId, Date now) {
    try {
      // See if updating the last found time updates any rows.  If it does,
      // we know we've already discovered this link before.
      PreparedStatement statement =
          MysqlHelper.getConnection().prepareStatement(UPDATE_LAST_FOUND_TIME_COMMAND);
      statement.setDate(1, new java.sql.Date(System.currentTimeMillis()));
      statement.setString(2, originId);
      statement.setString(3, destinationId);
      if (statement.executeUpdate() == 0) {
        // OK, so we didn't update anything.  Let's go ahead and insert.
        Link newLink = new Link.Builder()
            .setOriginId(originId)
            .setDestinationId(destinationId)
            .setDiscoveryTime(now)
            .setLastFoundTime(now)
            .build();
        newLink.insert();
      }
    } catch (ValidationException | SQLException e) {
      e.printStackTrace();
    }
  }

  /**
   * Deletes any links coming to or from the passed discovered URL ID.
   */
  public static void deleteId(String id) {
    try {
      PreparedStatement statement =
          MysqlHelper.getConnection().prepareStatement(DELETE_COMMAND);
      statement.setString(1, id);
      statement.setString(2, id);
      statement.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private void insert() throws SQLException {
    PreparedStatement statement =
        MysqlHelper.getConnection().prepareStatement(INSERT_COMMAND);
    statement.setString(1, this.originId);
    statement.setString(2, this.destinationId);
    statement.setTimestamp(3, new java.sql.Timestamp(this.discoveryTime.getTime()));
    statement.setTimestamp(4, new java.sql.Timestamp(this.lastFoundTime.getTime()));
    statement.execute();
  }

  /** Helper method for creating the Link table. */
  public static void main(String args[]) {
    try {
      Statement statement = MysqlHelper.getConnection().createStatement();
      statement.executeUpdate(CREATE_TABLE_COMMAND);
      statement.executeUpdate(CREATE_ORIGIN_ID_INDEX_COMMAND);
      statement.executeUpdate(CREATE_DESTINATION_ID_INDEX_COMMAND);
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
