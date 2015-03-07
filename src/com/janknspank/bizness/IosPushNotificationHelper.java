package com.janknspank.bizness;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import com.google.common.collect.Maps;
import com.janknspank.common.Host;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.CoreProto.DeviceRegistration;
import com.janknspank.proto.CoreProto.DeviceType;
import com.janknspank.proto.CoreProto.PushNotification;
import com.janknspank.proto.UserProto.User;

/**
 * Helper class for sending push notifications to iOS devices.
 * @see #sendNewArticleViaFuture(Sound, DeviceRegistrationId)
 * @see #sendNewArticleViaFuture(Sound, List<DeviceRegistrationId>)
 *
 * TODO(jonemerson): We probably want to forget about registration IDs that
 * APNS tells us are no good (by deleting them from the database).
 */
public class IosPushNotificationHelper {
  private static IosPushNotificationHelper INSTANCE = null;

  // For development builds, use gateway.sandbox.push.apple.com.
  private static String SEND_HOSTNAME = "gateway.push.apple.com";

  private static int SEND_PORT = 2195;
  private static final String APNS_PRIVATE_KEY_PASSPHRASE;
  static {
    APNS_PRIVATE_KEY_PASSPHRASE = System.getenv("APNS_PRIVATE_KEY_PASSPHRASE");
    if (APNS_PRIVATE_KEY_PASSPHRASE == null) {
      throw new Error("$APNS_PRIVATE_KEY_PASSPHRASE is undefined");
    }
  }

  private KeyStore keyStore;
  private KeyManagerFactory keyManagerFactory;

  public static synchronized IosPushNotificationHelper getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new IosPushNotificationHelper();
    }
    return INSTANCE;
  }

  private IosPushNotificationHelper() {
    // Load the keystore and key manager factory.
    FileInputStream keyFileInputStream = null;
    try {
      File keyFile = new File("WEB-INF/newsserver_production.p12");
      if (!keyFile.exists()) {
        throw new RuntimeException("Could not find key file");
      }

      keyStore = KeyStore.getInstance("PKCS12");
      keyFileInputStream = new FileInputStream(keyFile);
      keyStore.load(keyFileInputStream, APNS_PRIVATE_KEY_PASSPHRASE.toCharArray());

      keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
      keyManagerFactory.init(keyStore, APNS_PRIVATE_KEY_PASSPHRASE.toCharArray());

    } catch (GeneralSecurityException|IOException e) {
      throw new Error("Could not load iOS push notification service certs", e);
    } finally {
      IOUtils.closeQuietly(keyFileInputStream);
    }
  }

  /**
   * Returns a byte array containing the request to send to Apple PNS.  This
   * is obviously in a weird binary format, and unfortunately I think it was
   * reversed engineered by someone, so I haven't seen the documentation for
   * it.  But it's widely used and translated into many languages! :)
   */
  private static byte[] getRequestBody(PushNotification notification)
      throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    baos.write(0); // The command.
    baos.write(0); // The first byte of the deviceId length.
    baos.write(32); // The deviceId length.
    baos.write(hexStringToByteArray(notification.getDeviceId()));
    String payload = getPayload(notification).toString();
    baos.write(0); // First byte of payload length;
    baos.write(payload.length());
    baos.write(payload.getBytes());
    return baos.toByteArray();
  }

  public static Iterable<DeviceRegistration> getDeviceRegistrations(User user)
      throws DatabaseSchemaException {
    Map<String, DeviceRegistration> uniqueDeviceIds = Maps.newHashMap();
    for (DeviceRegistration registration : Database.with(DeviceRegistration.class).get(
        new QueryOption.WhereEquals("user_id", user.getId()),
        new QueryOption.WhereEquals("device_type", DeviceType.IOS.name()),
        new QueryOption.DescendingSort("create_time"),
        new QueryOption.Limit(1))) {
      if (!uniqueDeviceIds.containsKey(registration.getDeviceId())) {
        uniqueDeviceIds.put(registration.getDeviceId(), registration);
      }
    }
    return uniqueDeviceIds.values();
  }

  public void sendPushNotification(PushNotification pushNotification)
      throws DatabaseRequestException, DatabaseSchemaException {
    Database.insert(pushNotification);

    SSLSocket socket = null;
    try {
      // Basically do this, but in java:
      // openssl s_client -connect gateway.push.apple.com:2195 \
      //     -cert ProductionCert.pem -key ProductionKey.pem
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
      SSLSocketFactory factory = sslContext.getSocketFactory();
      socket = (SSLSocket) factory.createSocket(SEND_HOSTNAME, SEND_PORT);
      socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());
      socket.startHandshake();

      // Send the request.
      OutputStream out = socket.getOutputStream();
      out.write(getRequestBody(pushNotification));
      out.flush();

      // Read the response.
      // NOTE(jonemerson): DON'T DO THIS!!!  IT BLOCKS!
//      ByteArrayOutputStream responseOutputStream = new ByteArrayOutputStream();
//      byte[] buffer = new byte[2000];
//      int readBytes = socket.getInputStream().read(buffer, 0, buffer.length);
//      while (readBytes > 0) {
//        responseOutputStream.write(buffer, 0, readBytes);
//        readBytes = socket.getInputStream().read(buffer, 0, buffer.length);
//      }
//      // (BTW it looks like nothing comes back - weird.  How do you know if the
//      // request was valid?)
//      System.err.println("Received response: " +
//          new String(responseOutputStream.toByteArray()));

      // Close the socket.
      socket.getInputStream().close();
      out.close();
    } catch (GeneralSecurityException|IOException e) {
      throw new RuntimeException("IO error sending push notification", e);
    } finally {
      IOUtils.closeQuietly(socket);
    }
  }

  /**
   * Returns the text we should use for a notification about the passed article.
   */
  private static String getText(Article article) {
    String text = article.getTitle();
    if (text.length() > 100) {
      text = text.substring(0, 97) + "...";
    }
    return text;
  }

  /**
   * Returns the {'aps':{'alert':'{'type':'a','id':'articleId'}'}} JSON object
   * that iOS's server-side SDK expects for APNS requests.  The object contains
   * the IDs of the devices to contact and a data explaining the new sound that
   * was sent.
   */
  private static JSONObject getPayload(PushNotification notification) {
    // Create JSON object to describe the notification.
    // NOTE(jonemerson): We only have 256 characters to send to APNS, so
    // we can't stuff the whole sound serialization into the request.
    JSONObject alertInner = new JSONObject();
    alertInner.put("type", "a");
    alertInner.put("blob",
        "uid(" + notification.getUrlId() + ")!nid(" + notification.getId() + ")");
    alertInner.put("body", notification.getText());

    // Create the aps JSON.
    JSONObject aps = new JSONObject();
    aps.put("alert", alertInner);
    aps.put("badge", 1);
    aps.put("sound", "default");

    // Create the top-level JSON.
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("aps", aps);
    return jsonObject;
  }

  private static byte[] hexStringToByteArray(String s) {
    s = s.toUpperCase();
    int len = s.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
          + Character.digit(s.charAt(i+1), 16));
    }
    return data;
  }

  public static PushNotification createPushNotification(
      DeviceRegistration registration, Article article) {
    return PushNotification.newBuilder()
        .setId(GuidFactory.generate())
        .setCreateTime(System.currentTimeMillis())
        .setText(getText(article))
        .setUrlId(article.getUrlId())
        .setDeviceId(registration.getDeviceId())
        .setDeviceType(registration.getDeviceType())
        .setHost(Host.get())
        .setUserId(registration.getUserId())
        .build();
  }

  public static final void main(String args[])
      throws DatabaseSchemaException, DatabaseRequestException {
    int count = 0;
    for (DeviceRegistration registration
        : getDeviceRegistrations(Users.getByEmail("panaceaa@gmail.com"))) {
      ++count;
      Article article = Database.with(Article.class).getFirst();
      PushNotification pushNotification = createPushNotification(registration, article);
      IosPushNotificationHelper.getInstance().sendPushNotification(pushNotification);
    }
    System.out.println(count + " notifications sent");
  }
}
