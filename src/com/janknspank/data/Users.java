package com.janknspank.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

import com.janknspank.proto.Core.User;

/**
 * Tracks a link from one URL's content to another's.
 */
public class Users {
  private static final String SALT = "2s3knlk3lx3";
  private static final String SELECT_BY_EMAIL_COMMAND =
      "SELECT * FROM " + Database.getTableName(User.class) + " WHERE email=?";

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
   * TODO(jonemerson): Make this private again.
   */
  public static User getByEmail(String email) throws DataInternalException {
    try {
      PreparedStatement statement =
          Database.getConnection().prepareStatement(SELECT_BY_EMAIL_COMMAND);
      statement.setString(1, email);
      return Database.createFromResultSet(statement.executeQuery(), User.class);

    } catch (SQLException e) {
      throw new DataInternalException("Could not select user: " + e.getMessage(), e);
    }
  }

  public static User create(String email, String password)
      throws DataRequestException, DataInternalException {
    // Make sure we don't already have a user with this email.
    if (getByEmail(email) != null) {
      throw new DataRequestException("User with this email already exists.");
    }

    // Commit the new user.
    User user = User.newBuilder()
        .setId(GuidFactory.generate())
        .setEmail(email)
        .setPasswordSha256(getPasswordSha256(password))
        .setCreateTime(System.currentTimeMillis())
        .build();
    try {
      Database.insert(user);
    } catch (ValidationException e) {
      throw new DataInternalException("Error creating user", e);
    }
    return user;
  }

  /**
   * Marks a user as logged in and returns the affected User.
   * @see Sessions#create(String, String) to create a session
   */
  public static User login(String email, String password)
      throws DataInternalException, DataRequestException {
    User user = getByEmail(email);
    if (user == null ||
        !getPasswordSha256(password).equals(user.getPasswordSha256())) {
      throw new DataRequestException("User not found - Perhaps the password is wrong?");
    }
    user = user.toBuilder()
        .setLastLoginTime(System.currentTimeMillis())
        .build();
    try {
      Database.update(user);
    } catch (ValidationException e) {
      throw new DataInternalException("Error updating last login time", e);
    }
    return user;
  }

  /** Helper method for creating the User table. */
  public static void main(String args[]) throws Exception {
    Connection connection = Database.getConnection();
    connection.prepareStatement(Database.getCreateTableStatement(User.class)).execute();
    for (String statement : Database.getCreateIndexesStatement(User.class)) {
      connection.prepareStatement(statement).execute();
    }
  }
}
