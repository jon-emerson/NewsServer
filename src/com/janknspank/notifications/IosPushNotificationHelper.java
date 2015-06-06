package com.janknspank.notifications;

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

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.janknspank.bizness.Articles;
import com.janknspank.bizness.GuidFactory;
import com.janknspank.bizness.Users;
import com.janknspank.common.Host;
import com.janknspank.crawler.SiteManifests;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.CrawlerProto.SiteManifest;
import com.janknspank.proto.NotificationsProto.DeviceRegistration;
import com.janknspank.proto.NotificationsProto.DeviceType;
import com.janknspank.proto.NotificationsProto.Notification;
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

  private KeyStore productionKeyStore;
  private KeyManagerFactory productionKeyManagerFactory;

  private KeyStore betaKeyStore;
  private KeyManagerFactory betaKeyManagerFactory;

  public static synchronized IosPushNotificationHelper getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new IosPushNotificationHelper();
    }
    return INSTANCE;
  }

  private IosPushNotificationHelper() {
    // Load the keystore and key manager factory.
    FileInputStream productionKeyFileInputStream = null;
    FileInputStream betaKeyFileInputStream = null;
    try {
      // Production.
      productionKeyStore = KeyStore.getInstance("PKCS12");
      productionKeyFileInputStream =
          new FileInputStream(new File("WEB-INF/newsserver_production.p12"));
      productionKeyStore.load(
          productionKeyFileInputStream, APNS_PRIVATE_KEY_PASSPHRASE.toCharArray());

      productionKeyManagerFactory = KeyManagerFactory.getInstance("SunX509");
      productionKeyManagerFactory.init(productionKeyStore, APNS_PRIVATE_KEY_PASSPHRASE.toCharArray());

      // Beta (previously called Demo).
      betaKeyStore = KeyStore.getInstance("PKCS12");
      betaKeyFileInputStream = new FileInputStream(
          new File("WEB-INF/beta_newsserver_production.p12"));
      betaKeyStore.load(betaKeyFileInputStream, APNS_PRIVATE_KEY_PASSPHRASE.toCharArray());

      betaKeyManagerFactory = KeyManagerFactory.getInstance("SunX509");
      betaKeyManagerFactory.init(betaKeyStore, APNS_PRIVATE_KEY_PASSPHRASE.toCharArray());

    } catch (GeneralSecurityException|IOException e) {
      throw new Error("Could not load iOS push notification service certs", e);
    } finally {
      IOUtils.closeQuietly(productionKeyFileInputStream);
      IOUtils.closeQuietly(betaKeyFileInputStream);
    }
  }

  /**
   * Returns a byte array containing the request to send to Apple PNS.  This
   * is obviously in a weird binary format, and unfortunately I think it was
   * reversed engineered by someone, so I haven't seen the documentation for
   * it.  But it's widely used and translated into many languages! :)
   */
  private static byte[] getRequestBody(Notification notification)
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
        new QueryOption.WhereEquals("device_type", DeviceType.IOS.name()))) {
      if (!uniqueDeviceIds.containsKey(registration.getDeviceId())) {
        uniqueDeviceIds.put(registration.getDeviceId(), registration);
      }
    }
    return uniqueDeviceIds.values();
  }

  public void sendPushNotification(DeviceRegistration registration, Notification notification)
      throws DatabaseRequestException, DatabaseSchemaException {
    Database.insert(notification);

    SSLSocket socket = null;
    try {
      // Basically do this, but in java:
      // openssl s_client -connect gateway.push.apple.com:2195 \
      //     -cert ProductionCert.pem -key ProductionKey.pem
      SSLContext sslContext = SSLContext.getInstance("TLS");
      if (registration.getIsBeta()) {
        sslContext.init(betaKeyManagerFactory.getKeyManagers(), null, null);
      } else {
        sslContext.init(productionKeyManagerFactory.getKeyManagers(), null, null);
      }
      SSLSocketFactory factory = sslContext.getSocketFactory();
      socket = (SSLSocket) factory.createSocket(SEND_HOSTNAME, SEND_PORT);
      socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());
      socket.startHandshake();

      // Send the request.
      OutputStream out = socket.getOutputStream();
      out.write(getRequestBody(notification));
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
    SiteManifest site = SiteManifests.getForUrl(article.getUrl());
    String origin = article.hasOrigin() ? article.getOrigin()
        : (site == null ? null : site.getShortName());
    String text = (Strings.isNullOrEmpty(origin) ? "" : origin + ": ") + article.getTitle();
    if (text.length() > 110) {
      text = text.substring(0, 107) + "...";
    }
    return text;
  }

  /**
   * Returns the {'aps':{'alert':'{'type':'a','id':'articleId'}'}} JSON object
   * that iOS's server-side SDK expects for APNS requests.  The object contains
   * the IDs of the devices to contact and a data explaining the new sound that
   * was sent.
   */
  private static JSONObject getPayload(Notification notification) {
    // Create JSON object to describe the notification.
    // NOTE(jonemerson): We only have 256 characters to send to APNS, so
    // we can't stuff the whole sound serialization into the request.
    JSONObject alertInner = new JSONObject();
    alertInner.put("type", "a");
    alertInner.put("blob",
        "uid(" + notification.getUrlId() + ")!nid(" + notification.getId() + ")");
    alertInner.put("body", notification.getText());
    if (notification.hasUrlId()) {
      alertInner.put("url_id", notification.getUrlId());
    }

    // Create the aps JSON.
    JSONObject aps = new JSONObject();
    aps.put("alert", alertInner);
    aps.put("badge", 1);
    aps.put("sound", "default");

    // In client v1.1, start doing this again: The client needs to be updated to
    // do a local notification in response to these "silent" notifications in
    // order to keep notifications in the tray.
    // aps.put("content-available", 1);

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

  public static Notification createPushNotification(
      DeviceRegistration registration, Article article) {
    return Notification.newBuilder()
        .setId(GuidFactory.generate())
        .setCreateTime(System.currentTimeMillis())
        .setText(getText(article))
        .setUrlId(article.getUrlId())
        .setUrl(article.getUrl())
        .setDeviceId(registration.getDeviceId())
        .setDeviceType(registration.getDeviceType())
        .setHost(Host.get())
        .setUserId(registration.getUserId())
        .setArticlePublishedTime(Articles.getPublishedTime(article))
        .addAllDedupingStems(article.getDedupingStemsList())
        .build();
  }

  public static final void main(String args[])
      throws DatabaseSchemaException, DatabaseRequestException {
    int count = 0;
    for (DeviceRegistration registration
        : getDeviceRegistrations(Users.getByEmail("jon@jonemerson.net"))) {
      ++count;
      Article article = Database.with(Article.class).getFirst();
      Notification pushNotification = createPushNotification(registration, article);
      IosPushNotificationHelper.getInstance().sendPushNotification(registration, pushNotification);
    }
    System.out.println(count + " notifications sent");
  }
}
