package com.janknspank.data;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;

import com.janknspank.Asserts;

/**
 * Tracks a link from one URL's content to another's.  The primary key is a
 * composite of the origin and destination URL IDs, as defined in the
 * DiscoveredUrl table.
 */
public class User {
  public static final String TABLE_NAME_STR = "User";
  public static final String ID_STR = "id";
  public static final String NAME_STR = "name";
  public static final String EMAIL_STR = "email";
  public static final String LINKEDIN_ID_STR = "linkedin_id";
  public static final String FACEBOOK_ID_STR = "facebook_id";
  public static final String PASSWORD_SHA256_STR = "password_sha256";
  private static final String CREATE_TIME_STR = "create_time";
  private static final String LAST_LOGIN_TIME_STR = "last_login_time";
  private static final String SALT = "2s3knlk3lx3";

  private static final String CREATE_TABLE_COMMAND =
      "CREATE TABLE " + TABLE_NAME_STR + " ( " +
      "    " + ID_STR + " VARCHAR(24) NOT NULL PRIMARY KEY, " +
      "    " + NAME_STR + " VARCHAR(100), " +
      "    " + EMAIL_STR + " VARCHAR(100) NOT NULL, " +
      "    " + LINKEDIN_ID_STR + " VARCHAR(24), " +
      "    " + FACEBOOK_ID_STR + " VARCHAR(24), " +
      "    " + PASSWORD_SHA256_STR + " VARCHAR(24) NOT NULL, " +
      "    " + CREATE_TIME_STR + " DATETIME NOT NULL, " +
      "    " + LAST_LOGIN_TIME_STR + " DATETIME NOT NULL )";
  private static final String CREATE_EMAIL_INDEX_COMMAND =
      "CREATE UNIQUE INDEX " + EMAIL_STR + "_index " +
      "    ON " + TABLE_NAME_STR +
      "    (" + EMAIL_STR + ") USING HASH";
  private static final String SELECT_BY_ID_COMMAND =
      "SELECT * FROM " + TABLE_NAME_STR + " WHERE " + ID_STR + " =?";
  private static final String CREATE_USER_COMMAND =
      "INSERT INTO " + TABLE_NAME_STR + " ( " +
      "    " + ID_STR + ", " +
      "    " + EMAIL_STR + ", " +
      "    " + PASSWORD_SHA256_STR + ", " +
      "    " + CREATE_TIME_STR + ") VALUES (?, ?, ?, ?)";
  private static final String UPDATE_LAST_LOGIN_TIME_COMMAND =
      "UPDATE " + TABLE_NAME_STR + " SET " + LAST_LOGIN_TIME_STR + " =? WHERE " +
      "    " + EMAIL_STR + " =? AND " +
      "    " + PASSWORD_SHA256_STR + " =?";
  private static final String SET_LINKEDIN_ID_COMMAND =
      "UPDATE " + TABLE_NAME_STR + " SET " + LINKEDIN_ID_STR + " =? WHERE " +
      "    " + ID_STR + " =?";
  private static final String DELETE_COMMAND =
      "DELETE FROM " + TABLE_NAME_STR + " WHERE " + ID_STR + " =?";

  private String id;
  private String name;
  private String email;
  private String linkedinId;
  private String facebookId;
  private String passwordSha256;
  private Date createTime;
  private Date lastLoginTime;

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public String getLinkedinId() {
    return linkedinId;
  }

  public String getFacebookId() {
    return facebookId;
  }

  public String getPasswordSha256() {
    return passwordSha256;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public Date getLastLoginTime() {
    return lastLoginTime;
  }

  public static class Builder {
    private String id;
    private String name;
    private String email;
    private String linkedinId;
    private String facebookId;
    private String passwordSha256;
    private Date createTime;
    private Date lastLoginTime;

    public Builder() {
    }

    public Builder setId(String id) {
      this.id = id;
      return this;
    }

    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    public Builder setEmail(String email) {
      this.email = email;
      return this;
    }

    public Builder setLinkedinId(String linkedinId) {
      this.linkedinId = linkedinId;
      return this;
    }

    public Builder setFacebookId(String facebookId) {
      this.facebookId = facebookId;
      return this;
    }

    public Builder setPasswordSha256(String passwordSha256) {
      this.passwordSha256 = passwordSha256;
      return this;
    }

    public Builder setCreateTime(Date createTime) {
      this.createTime = createTime;
      return this;
    }

    public Builder setLastLoginTime(Date lastLoginTime) {
      this.lastLoginTime = lastLoginTime;
      return this;
    }

    public User build() throws ValidationException {
      User user = new User();
      user.id = id;
      user.email = email;
      user.name = name;
      user.linkedinId = linkedinId;
      user.facebookId = facebookId;
      user.passwordSha256 = passwordSha256;
      user.createTime = (createTime == null) ? new Date() : createTime;
      user.lastLoginTime = lastLoginTime;
      user.assertValid();
      return user;
    }
  }

  private void assertValid() throws ValidationException {
    Asserts.assertNonEmpty(id, ID_STR);
    Asserts.assertNonEmpty(email, EMAIL_STR);
    Asserts.assertNotNull(passwordSha256, PASSWORD_SHA256_STR);
    Asserts.assertNotNull(createTime, CREATE_TIME_STR);
  }

  private static User createFromResultSet(ResultSet result) throws SQLException {
    if (result.next()) {
      User.Builder builder = new User.Builder();
      builder.setId(result.getString(ID_STR));
      builder.setEmail(result.getString(EMAIL_STR));
      builder.setName(result.getString(NAME_STR));
      builder.setLinkedinId(result.getString(LINKEDIN_ID_STR));
      builder.setFacebookId(result.getString(FACEBOOK_ID_STR));
      builder.setPasswordSha256(result.getString(PASSWORD_SHA256_STR));
      builder.setCreateTime(result.getDate(CREATE_TIME_STR));
      builder.setLastLoginTime(result.getDate(LAST_LOGIN_TIME_STR));
      try {
        return builder.build();
      } catch (ValidationException e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  /**
   * Hashes the passed-in plaintext String password and returns the results.
   */
  private static String getPasswordSha256(String password) {
    return Base64.encodeBase64URLSafeString(DigestUtils.sha256(password + SALT))
        .replaceAll("=", "");
  }

  /**
   * This is currently private because its uses should be only internal.
   * When we implement login, that should be through a different method that
   * additionally updates the last login time.
   */
  private static User get(String email, String password) {
    try {
      PreparedStatement statement =
          MysqlHelper.getConnection().prepareStatement(SELECT_BY_ID_COMMAND);
      statement.setString(1, email);
      statement.setString(2, getPasswordSha256(password));
      return createFromResultSet(statement.executeQuery());

    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static User create(String email, String password) throws DataRequestException {
    // Make sure we don't already have a user with this email.
    if (get(email, password) != null) {
      throw new DataRequestException("User with this email already exists.");
    }

    // Calculate the values we'll be storing, so we can return them
    // later in a new User object.
    String userId = GuidFactory.generate();
    String passwordSha256 = getPasswordSha256(password);
    java.sql.Timestamp createTime = new java.sql.Timestamp(System.currentTimeMillis());

    // Commit the new user.
    try {
      PreparedStatement statement =
          MysqlHelper.getConnection().prepareStatement(CREATE_USER_COMMAND);
      statement.setString(1, userId);
      statement.setString(2, email);
      statement.setString(3, passwordSha256);
      statement.setTimestamp(4, createTime);
      statement.execute();
    } catch (SQLException e) {
      throw new DataRequestException("Error creating user", e);
    }

    // Return the user we just created.
    try {
      return new User.Builder()
          .setId(userId)
          .setEmail(email)
          .setPasswordSha256(passwordSha256)
          .setCreateTime(createTime)
          .build();
    } catch (ValidationException e) {
      throw new DataRequestException("Could not construct user object", e);
    }
  }

  /**
   * Marks a user as logged in and returns the affected User.
   * @see Session#create(String, String) to create a session
   */
  public static User login(String email, String password) throws DataRequestException {
    try {
      PreparedStatement statement =
          MysqlHelper.getConnection().prepareStatement(UPDATE_LAST_LOGIN_TIME_COMMAND);
      statement.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
      statement.setString(2, email);
      statement.setString(3, getPasswordSha256(password));
      if (statement.executeUpdate() > 0) {
        return get(email, password);
      } else {
        throw new DataRequestException("User not found - Perhaps the password is wrong?");
      }
    } catch (SQLException e) {
      throw new DataRequestException("Could not update last login time", e);
    }
  }

  /**
   * Updates the database so that the user with the passed userId has the
   * passed LinkedIn user ID associated with his account.
   */
  public static void setLinkedinId(String userId, String linkedinId) throws DataRequestException {
    try {
      PreparedStatement statement =
          MysqlHelper.getConnection().prepareStatement(SET_LINKEDIN_ID_COMMAND);
      statement.setString(1, linkedinId);
      statement.setString(2, userId);
      if (statement.executeUpdate() == 0) {
        throw new DataRequestException("User not found");
      }
    } catch (SQLException e) {
      throw new DataRequestException("Could not update last login time", e);
    }
  }

  /**
   * Deletes the user with the passed ID.
   * @return true, if any users were deleted
   */
  public static boolean deleteId(String id) throws DataRequestException {
    try {
      PreparedStatement statement =
          MysqlHelper.getConnection().prepareStatement(DELETE_COMMAND);
      statement.setString(1, id);
      return statement.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new DataRequestException("Could not delete user", e);
    }
  }

  /** Helper method for creating the User table. */
  public static void main(String args[]) {
    try {
      Statement statement = MysqlHelper.getConnection().createStatement();
      statement.executeUpdate(CREATE_TABLE_COMMAND);
      statement.executeUpdate(CREATE_EMAIL_INDEX_COMMAND);
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
