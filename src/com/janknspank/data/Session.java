package com.janknspank.data;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.janknspank.Asserts;

/**
 * An active user Session.  The existence of a session key for a user, and the
 * presentation of that session key on an authenticated action, authorizes the
 * user to act as the specified user.
 */
public class Session {
  // Database keys.
  public static final String TABLE_NAME_STR = "Session";
  public static final String SESSION_KEY_STR = "session_key";
  public static final String USER_ID_STR = "user_id";
  private static final String CREATE_TIME_STR = "create_time";

  // Encryption classes.
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
  private static final String CREATE_TABLE_COMMAND =
      "CREATE TABLE " + TABLE_NAME_STR + " ( " +
      "    " + SESSION_KEY_STR + " VARCHAR(128) NOT NULL PRIMARY KEY, " +
      "    " + USER_ID_STR + " VARCHAR(24) NOT NULL, " +
      "    " + CREATE_TIME_STR + " DATETIME NOT NULL )";
  private static final String SELECT_BY_SESSION_KEY_COMMAND =
      "SELECT * FROM " + TABLE_NAME_STR + " WHERE " + SESSION_KEY_STR + " =?";
  private static final String CREATE_SESSION_COMMAND =
      "INSERT INTO " + TABLE_NAME_STR + " ( " +
      "    " + SESSION_KEY_STR + ", " +
      "    " + USER_ID_STR + ", " +
      "    " + CREATE_TIME_STR + ") VALUES (?, ?, ?)";
  private static final String DELETE_COMMAND =
      "DELETE FROM " + TABLE_NAME_STR + " WHERE " + SESSION_KEY_STR + " =?";
  private static final String DELETE_BY_USER_ID_COMMAND =
      "DELETE FROM " + TABLE_NAME_STR + " WHERE " + USER_ID_STR + " =?";

  private String sessionKey;
  private String userId;
  private Date createTime;

  public String getSessionKey() {
    return sessionKey;
  }

  public String getUserId() {
    return userId;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public static class Builder {
    private String sessionKey;
    private String userId;
    private Date createTime;

    public Builder() {
    }

    public Builder setSessionKey(String sessionKey) {
      this.sessionKey = sessionKey;
      return this;
    }

    public Builder setUserId(String userId) {
      this.userId = userId;
      return this;
    }

    public Builder setCreateTime(Date createTime) {
      this.createTime = createTime;
      return this;
    }

    public Session build() throws ValidationException {
      Session session = new Session();
      session.sessionKey = sessionKey;
      session.userId = userId;
      session.createTime = (createTime == null) ? new Date() : createTime;
      session.assertValid();
      return session;
    }
  }

  private void assertValid() throws ValidationException {
    Asserts.assertNonEmpty(sessionKey, SESSION_KEY_STR);
    Asserts.assertNonEmpty(userId, USER_ID_STR);
    Asserts.assertNotNull(createTime, CREATE_TIME_STR);
  }

  /**
   * Returns the fields from this object that should be publicly available.
   */
  public JSONObject toJSONObject() {
    JSONObject o = new JSONObject();
    o.put(SESSION_KEY_STR, sessionKey);
    o.put(USER_ID_STR, userId);
    o.put(CREATE_TIME_STR, createTime);
    return o;
  }

  private static Session createFromResultSet(ResultSet result)
      throws SQLException, DataInternalException {
    if (result.next()) {
      Session.Builder builder = new Session.Builder();
      builder.setSessionKey(result.getString(SESSION_KEY_STR));
      builder.setUserId(result.getString(USER_ID_STR));
      builder.setCreateTime(result.getDate(CREATE_TIME_STR));
      try {
        return builder.build();
      } catch (ValidationException e) {
        throw new DataInternalException("Could not create session object: " + e.getMessage(), e);
      }
    }
    return null;
  }

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
   * Officially sanctioned method for creating a user session from email +
   * password credentials.
   */
  public static Session create(String email, String password)
      throws DataRequestException, DataInternalException {
    // Calculate the values we'll be storing, so we can return them
    // later in a new User object.
    User user = User.login(email, password);
    String sessionKey = toEncryptedBase64("v1_" + System.currentTimeMillis() + "_" + user.getId());
    java.sql.Timestamp createTime = new java.sql.Timestamp(System.currentTimeMillis());

    // Commit the new user.
    try {
      PreparedStatement statement =
          MysqlHelper.getConnection().prepareStatement(CREATE_SESSION_COMMAND);
      statement.setString(1, sessionKey);
      statement.setString(2, user.getId());
      statement.setTimestamp(3, createTime);
      statement.execute();
    } catch (SQLException e) {
      throw new DataInternalException("Error creating session: " + e.getMessage(), e);
    }

    // Return the session we just created.
    try {
      return new Session.Builder()
          .setSessionKey(sessionKey)
          .setUserId(user.getId())
          .setCreateTime(createTime)
          .build();
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
    try {
      // Make sure that the session key can be decrypted.
      String userId = decrypt(sessionKey);

      // Make sure the session key is in the database.
      PreparedStatement statement =
          MysqlHelper.getConnection().prepareStatement(SELECT_BY_SESSION_KEY_COMMAND);
      statement.setString(1, sessionKey);
      Session session = createFromResultSet(statement.executeQuery());
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
    } catch (SQLException e) {
      throw new DataInternalException("Could not update last login time", e);
    }
  }

  /**
   * Deletes the session key, preventing future access using that key.
   * @return true, if any users were deleted
   */
  public static boolean deleteSessionKey(String sessionKey) throws DataInternalException {
    try {
      PreparedStatement statement =
          MysqlHelper.getConnection().prepareStatement(DELETE_COMMAND);
      statement.setString(1, sessionKey);
      return statement.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new DataInternalException("Could not delete session: " + e.getMessage(), e);
    }
  }

  /**
   * We probably want to delete this before we launch, but it's useful for
   * testing to let people clear out their sessions.
   * @return number of rows deleted
   */
  public static int deleteAllFromUser(User user) throws DataInternalException {
    try {
      PreparedStatement statement =
          MysqlHelper.getConnection().prepareStatement(DELETE_BY_USER_ID_COMMAND);
      statement.setString(1, user.getId());
      return statement.executeUpdate();
    } catch (SQLException e) {
      throw new DataInternalException("Could not delete session: " + e.getMessage(), e);
    }
  }

  /** Helper method for creating the Session table. */
  public static void main(String args[]) {
    try {
      Statement statement = MysqlHelper.getConnection().createStatement();
      statement.executeUpdate(CREATE_TABLE_COMMAND);
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
