package com.janknspank.bizness;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

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

  /**
   * Encrypts the passed-in string and returns a base64 representation of the
   * encrypted value.  The base64 is web safe.
   */
  private static String toEncryptedBase64(String rawStr) throws BiznessException {
    try {
      byte[] encryptedBytes = ENCRYPT_CIPHER.doFinal(rawStr.getBytes("UTF-8"));
      return Base64.encodeBase64URLSafeString(encryptedBytes).replaceAll("=", "");
    } catch (UnsupportedEncodingException|IllegalBlockSizeException|BadPaddingException e) {
      throw new BiznessException("Could not encrypt session key: " + e.getMessage(), e);
    }
  }

  /**
   * Returns an expiring LinkedIn OAuth state parameter, that we can use to
   * prevent XSRF attacks.  Contains the encrypted current time.
   */
  public static String getLinkedInOAuthState() throws BiznessException {
    return toEncryptedBase64(Long.toString(System.currentTimeMillis()));
  }

  /**
   * Throws if the passed OAuthState is invalid or expired.
   */
  public static void verifyLinkedInOAuthState(String linkedInOAuthState) throws BiznessException {
    try {
      byte[] encryptedBytes = Base64.decodeBase64(linkedInOAuthState);
      byte[] decryptedBytes = DECRYPT_CIPHER.doFinal(encryptedBytes);
      long millis = Long.parseLong(new String(decryptedBytes));
      if (millis <= System.currentTimeMillis() &&
          (millis >= System.currentTimeMillis() * 60 * 60 * 1000)) {
        throw new BiznessException("State expired");
      }
    } catch (IllegalBlockSizeException e) {
      throw new BiznessException("Could not decrypt state", e);
    } catch (BadPaddingException e) {
      throw new BiznessException("Could not decrypt state", e);
    }
  }

  /**
   * Decrypts a session key and returns the user ID from inside it.
   */
  private static String decrypt(String sessionKey) throws BiznessException, RequestException {
    String rawStr;
    try {
      byte[] encryptedBytes = Base64.decodeBase64(sessionKey);
      byte[] decryptedBytes = DECRYPT_CIPHER.doFinal(encryptedBytes);
      rawStr = new String(decryptedBytes);
    } catch (IllegalBlockSizeException e) {
      throw new BiznessException("Could not decrypt session", e);
    } catch (BadPaddingException e) {
      throw new BiznessException("Could not decrypt session", e);
    }

    Matcher matcher = SESSION_PATTERN.matcher(rawStr);
    if (matcher.find()) {
      return matcher.group(2);
    }
    throw new RequestException("Unrecognized session format: " + rawStr);
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
          .setSessionKey(toEncryptedBase64("v1_" + System.currentTimeMillis()
              + "_" + user.getId()))
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
   */
  public static Session getBySessionKey(String sessionKey)
      throws RequestException, BiznessException, DatabaseSchemaException {
    // Make sure that the session key can be decrypted.
    String userId = decrypt(sessionKey);

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

  /** Helper method for creating the Session table. */
  public static void main(String args[]) throws Exception {
    Database.with(Session.class).createTable();
  }
}