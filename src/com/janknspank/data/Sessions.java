package com.janknspank.data;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

import com.janknspank.proto.Core.Session;
import com.janknspank.proto.Core.User;

/**
 * An active user Session.  The existence of a session key for a user, and the
 * presentation of that session key on an authenticated action, authorizes the
 * user to act as the specified user.
 */
public class Sessions {
  private static final Cipher ENCRYPT_CIPHER;
  private static final Cipher DECRYPT_CIPHER;
  private static final Pattern SESSION_PATTERN = Pattern.compile("^v1_([0-9]+)_(.*)$");
  static {
    try {
      String sessionAesKey = System.getenv("NEWS_SESSION_AES_KEY");
      if (sessionAesKey == null) {
        throw new Error("$NEWS_SESSION_AES_KEY is undefined");
      }
      String sessionInitVector = System.getenv("NEWS_SESSION_INIT_VECTOR");
      if (sessionInitVector == null) {
        throw new Error("$NEWS_SESSION_INIT_VECTOR is undefined");
      }

      SecretKeySpec key = new SecretKeySpec(sessionAesKey.getBytes("UTF-8"), "AES");

      ENCRYPT_CIPHER = Cipher.getInstance("AES/CBC/PKCS5Padding", "SunJCE");
      ENCRYPT_CIPHER.init(Cipher.ENCRYPT_MODE, key,
              new IvParameterSpec(sessionInitVector.getBytes("UTF-8")));

      DECRYPT_CIPHER = Cipher.getInstance("AES/CBC/PKCS5Padding", "SunJCE");
      DECRYPT_CIPHER.init(Cipher.DECRYPT_MODE, key,
          new IvParameterSpec(sessionInitVector.getBytes("UTF-8")));

    } catch (NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException |
        UnsupportedEncodingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
        throw new Error("Could not intialize session key", e);
    }
  }

  // Database operational commands.
  private static final String DELETE_BY_USER_ID_COMMAND =
      "DELETE FROM " + Database.getTableName(Session.class) + " WHERE user_id=?";

  /**
   * Encrypts the passed-in string and returns a base64 representation of the
   * encrypted value.  The base64 is web safe.
   */
  private static String toEncryptedBase64(String rawStr) throws DataInternalException {
    try {
      byte[] encryptedBytes = ENCRYPT_CIPHER.doFinal(rawStr.getBytes("UTF-8"));
      return Base64.encodeBase64URLSafeString(encryptedBytes).replaceAll("=", "");
    } catch (UnsupportedEncodingException|IllegalBlockSizeException|BadPaddingException e) {
      throw new DataInternalException("Could not encrypt session key: " + e.getMessage(), e);
    }
  }

  /**
   * Decrypts a session key and returns the user ID from inside it.
   */
  private static String decrypt(String sessionKey) throws DataRequestException {
    String rawStr;
    try {
      byte[] encryptedBytes = Base64.decodeBase64(sessionKey);
      byte[] decryptedBytes = DECRYPT_CIPHER.doFinal(encryptedBytes);
      rawStr = new String(decryptedBytes);
    } catch (IllegalBlockSizeException e) {
      throw new DataRequestException("Could not decrypt session", e);
    } catch (BadPaddingException e) {
      throw new DataRequestException("Could not decrypt session", e);
    }

    Matcher matcher = SESSION_PATTERN.matcher(rawStr);
    if (matcher.find()) {
      return matcher.group(2);
    }
    throw new DataRequestException("Unrecognized session format: " + rawStr);
  }

  /**
   * Officially sanctioned method for creating a new session from email
   * password credentials.
   */
  public static Session create(String email, String password)
      throws DataRequestException, DataInternalException {
    // This will throw if the user has invalid credentials.
    User user = Users.login(email, password);

    // Insert a new Session object and return it.
    try {
      Session session = Session.newBuilder()
          .setSessionKey(toEncryptedBase64("v1_" + System.currentTimeMillis()
              + "_" + user.getId()))
          .setUserId(user.getId())
          .setCreateTime(System.currentTimeMillis())
          .build();
      Database.insert(session);
      return session;
    } catch (ValidationException e) {
      throw new DataInternalException("Could not construct session object", e);
    }
  }

  /**
   * Officially sanctioned method for getting a user session from a logged-in
   * session key.
   */
  public static Session get(String sessionKey)
      throws DataRequestException, DataInternalException {
    // Make sure that the session key can be decrypted.
    String userId = decrypt(sessionKey);

    // Make sure the session key is in the database.
    Session session = Database.get(sessionKey, Session.class);
    if (session == null) {
      throw new DataRequestException("Session not found in database.");
    }

    // Make sure that the user ID in the decrypted version matches what we
    // have in database storage.
    if (!userId.equals(session.getUserId())) {
      throw new DataRequestException(
          "Inconsistent data: User ID in session does not match database.");
    }
    return session;
  }

  /**
   * We probably want to delete this before we launch, but it's useful for
   * testing to let people clear out their sessions.
   * @return number of rows deleted
   */
  public static int deleteAllFromUser(User user) throws DataInternalException {
    try {
      PreparedStatement statement =
          Database.getConnection().prepareStatement(DELETE_BY_USER_ID_COMMAND);
      statement.setString(1, user.getId());
      return statement.executeUpdate();
    } catch (SQLException e) {
      throw new DataInternalException("Could not delete session: " + e.getMessage(), e);
    }
  }

  /** Helper method for creating the Session table. */
  public static void main(String args[]) throws Exception {
    Connection connection = Database.getConnection();
    connection.prepareStatement(Database.getCreateTableStatement(Session.class)).execute();
    for (String statement : Database.getCreateIndexesStatement(Session.class)) {
      connection.prepareStatement(statement).execute();
    }
  }
}
