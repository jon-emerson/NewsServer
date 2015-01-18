package com.janknspank.data;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.dom.parser.Node;
import com.janknspank.proto.Core.User;

/**
 * Tracks a link from one URL's content to another's.
 */
public class Users {
  private static final String SELECT_BY_EMAIL_COMMAND =
      "SELECT * FROM " + Database.getTableName(User.class) + " WHERE email=?";

  /**
   * This is currently private because its uses should be only internal.
   * When we implement login, that should be through a different method that
   * additionally updates the last login time.
   */
  private static User getByEmail(String email) throws DataInternalException {
    try {
      PreparedStatement statement =
          Database.getInstance().prepareStatement(SELECT_BY_EMAIL_COMMAND);
      statement.setString(1, email);
      return Database.createFromResultSet(statement.executeQuery(), User.class);

    } catch (SQLException e) {
      throw new DataInternalException("Could not select user: " + e.getMessage(), e);
    }
  }

  public static User loginFromLinkedIn(
      DocumentNode linkedInProfileDocument, String linkedInAccessToken)
      throws DataRequestException, DataInternalException {
    // Get the user's email address from the LinkedIn profile response.
    Node emailNode = linkedInProfileDocument.findFirst("email-address");
    if (emailNode == null) {
      throw new DataRequestException("Could not get email from LinkedIn profile");
    }
    String email = emailNode.getFlattenedText();

    // If we already have a user, great, use him!  Mark him as logged in too!
    User user = getByEmail(email);
    if (user != null) {
      user = user.toBuilder()
          .setLinkedInAccessToken(linkedInAccessToken)
          .setLastLoginTime(System.currentTimeMillis())
          .build();
      try {
        Database.getInstance().update(user);
      } catch (ValidationException e) {
        throw new DataInternalException("Error marking user logged-in", e);
      }
      return user;
    }

    // Create a new user.
    user = User.newBuilder()
        .setId(GuidFactory.generate())
        .setEmail(email)
        .setLinkedInAccessToken(linkedInAccessToken)
        .setCreateTime(System.currentTimeMillis())
        .setLastLoginTime(System.currentTimeMillis())
        .build();
    try {
      Database.getInstance().insert(user);
    } catch (ValidationException e) {
      throw new DataInternalException("Error creating user", e);
    }
    return user;
  }

  /** Helper method for creating the User table. */
  public static void main(String args[]) throws Exception {
    Database database = Database.getInstance();
    database.prepareStatement(database.getCreateTableStatement(User.class)).execute();
    for (String statement : database.getCreateIndexesStatement(User.class)) {
      database.prepareStatement(statement).execute();
    }
  }
}
