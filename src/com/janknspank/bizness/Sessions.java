package com.janknspank.bizness;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Base64;

import com.google.common.annotations.VisibleForTesting;
import com.janknspank.common.Asserts;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.dom.parser.Node;
import com.janknspank.proto.CoreProto.Session;
import com.janknspank.proto.UserProto.User;
import com.janknspank.server.RequestException;

/**
 * An active user Session.  The existence of a session key for a user, and the
 * presentation of that session key on an authenticated action, authorizes the
 * user to act as the specified user.
 */
public class Sessions {
  static final String SESSION_ENCODER_KEY;
  static {
    SESSION_ENCODER_KEY = System.getenv("SESSION_ENCODER_KEY");
    if (SESSION_ENCODER_KEY == null) {
      throw new Error("$SESSION_ENCODER_KEY is undefined");
    }
  }

  /**
   * Officially sanctioned method for getting a user session from a LinkedIn
   * profile response.
   */
  public static Session createFromLinkedProfile(DocumentNode linkedInProfileDocument, User user)
      throws BiznessException, DatabaseSchemaException {
    // Validate the data looks decent.
    Node emailNode = linkedInProfileDocument.findFirst("email-address");
    if (emailNode == null) {
      throw new BiznessException("Could not get email from LinkedIn profile");
    }

    // Insert a new Session object and return it.
    try {
      Session session = Session.newBuilder()
          .setSessionKey(createSessionKey(user))
          .setUserId(user.getId())
          .setCreateTime(System.currentTimeMillis())
          .build();
      Database.insert(session);
      return session;
    } catch (DatabaseRequestException e) {
      throw new BiznessException("Could not insert session object", e);
    }
  }

  /**
   * Officially sanctioned method for getting a user session from a logged-in
   * session key.
   * @throws RequestException if the session doesn't exist or is invalid
   * @throws BiznessException 
   */
  public static Session getBySessionKey(String sessionKey)
      throws RequestException, DatabaseSchemaException {
    // Make sure that the session key can be decrypted.
    String userId = getUserId(sessionKey);

    // Make sure the session key is in the database.
    Session session = Database.with(Session.class)
        .getFirst(new QueryOption.WhereEquals("session_key", sessionKey));
    if (session == null) {
      throw new RequestException("Session not found in database.");
    }

    // Make sure that the user ID in the decrypted version matches what we
    // have in database storage.
    if (!userId.equals(session.getUserId())) {
      throw new RequestException(
          "Inconsistent data: User ID in session does not match database.");
    }
    return session;
  }

  /**
   * We probably want to delete this before we launch, but it's useful for
   * testing to let people clear out their sessions.
   * @return number of rows deleted
   */
  public static int deleteAllFromUser(User user) throws DatabaseSchemaException {
    return Database.with(Session.class).delete(
        new QueryOption.WhereEquals("user_id", user.getId()));
  }

  /**
   * Returns a base64-encoded String of
   *
   * (X/Y/Z)
   *
   * Where:
   *
   * X = Current time in Milliseconds
   * Y = User ID
   * Z = Salted MD5 hash of the string "X/Y".
   */
  @VisibleForTesting
  static String createSessionKey(User user) {
    try {
      String front = Long.toString(System.currentTimeMillis()) + "/" + user.getId();
      MessageDigest md = MessageDigest.getInstance("SHA1");
      md.update((SESSION_ENCODER_KEY + front).getBytes());
      String oAuthState = front + "/" + new String(md.digest());
      return Base64.encodeBase64URLSafeString(oAuthState.getBytes());
    } catch (NoSuchAlgorithmException e) {
      throw new Error("SHA1 hashing algorithm not found", e);
    }
  }

  /**
   * Verifies that the passed session keys salted SHA1 section matches the
   * milliseconds and user ID at the front of the string, and if verified,
   * returns the validated user ID.
   */
  @VisibleForTesting
  static String getUserId(String sessionKey) throws RequestException {
    try {
      String oAuthState = new String(Base64.decodeBase64(sessionKey));
      String[] components = oAuthState.split("\\/", 3);
      Asserts.assertTrue(components.length == 3, "Session key has format invalid",
          RequestException.class);
      long milliseconds = Long.parseLong(components[0]);
      String userId = components[1];
      String front = Long.toString(milliseconds) + "/" + userId;

      MessageDigest md = MessageDigest.getInstance("SHA1");
      md.update((SESSION_ENCODER_KEY + front).getBytes());
      Asserts.assertTrue(components[2].equals(new String(md.digest())),
          "Session key is not internally consistent", RequestException.class);
      return userId;
    } catch (NoSuchAlgorithmException e) {
      throw new Error("SHA1 hashing algorithm not found", e);
    }
  }

  /** Helper method for creating the Session table. */
  public static void main(String args[]) throws Exception {
    Database.with(Session.class).createTable();
  }
}
