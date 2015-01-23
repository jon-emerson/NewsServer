package com.janknspank.data;

import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.dom.parser.Node;
import com.janknspank.proto.Core.User;

/**
 * Tracks a link from one URL's content to another's.
 */
public class Users {
  /**
   * This is currently private because its uses should be only internal.
   * When we implement login, that should be through a different method that
   * additionally updates the last login time.
   */
  private static User getByEmail(String email) throws DataInternalException {
    return Database.with(User.class).getFirst(
        new QueryOption.WhereEquals("email", email));
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
        Database.update(user);
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
      Database.insert(user);
    } catch (ValidationException e) {
      throw new DataInternalException("Error creating user", e);
    }
    return user;
  }

  /** Helper method for creating the User table. */
  public static void main(String args[]) throws Exception {
    Database.with(User.class).createTable();
  }
}
