package com.janknspank.bizness;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.CoreProto.DeviceRegistration;
import com.janknspank.proto.CoreProto.DeviceRegistration.DeviceType;
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
  private static ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();
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
      File keyFile = new File("WEB-INF/EnterpriseDistributionCertificates.p12");
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
   * Sends a push notification about a new sound to all the iOS devices
   * registered with the passed phone number.
   */
  public ListenableFuture<Void> sendNewArticleViaFuture(
      final Article article, final User recipient) throws DatabaseSchemaException {
    Iterable<DeviceRegistration> registrations = Database.with(DeviceRegistration.class).get(
        new QueryOption.WhereEquals("user_id", recipient.getId()),
        new QueryOption.WhereEquals("device_type", DeviceType.IOS.name()));
    return sendNewArticleViaFuture(article, registrations);
  }

  public ListenableFuture<Void> sendNewArticleViaFuture(
      final Article article, final Iterable<DeviceRegistration> registrations) {
    List<ListenableFuture<Void>> futures = new ArrayList<>();
    for (DeviceRegistration registration : registrations) {
      futures.add(sendNewArticleViaFuture(article, registration));
    }
    return Futures.transform(
        Futures.allAsList(futures),
        new AsyncFunction<List<Void>, Void>() {
          @Override
          public ListenableFuture<Void> apply(List<Void> result) {
            return Futures.immediateFuture(null);
          }
        });
  }

  public ListenableFuture<Void> sendNewArticleViaFuture(
      final Article article, final DeviceRegistration registration) {
    ListeningExecutorService service = MoreExecutors.listeningDecorator(EXECUTOR_SERVICE);
    return service.submit(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        sendNewArticle(article, registration);
        return null;
      }
    });
  }

  /**
   * Returns a byte array containing the request to send to Apple PNS.  This
   * is obviously in a weird binary format, and unfortunately I think it was
   * reversed engineered by someone, so I haven't seen the documentation for
   * it.  But it's widely used and translated into many languages! :)
   */
  private static byte[] getRequestBody(Article article, DeviceRegistration registration)
      throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    baos.write(0); // The command.
    baos.write(0); // The first byte of the deviceId length.
    baos.write(32); // The deviceId length.
    baos.write(hexStringToByteArray(registration.getDeviceId()));
    String payload = getNewArticleNotificationBody(article).toString();
    baos.write(0); //First byte of payload length;
    baos.write(payload.length());
    baos.write(payload.getBytes());
    return baos.toByteArray();
  }

  private void sendNewArticle(Article article, DeviceRegistration registration) {
    SSLSocket socket = null;
    try {
      // Basically do this, but in java:
      // openssl s_client -connect gateway.sandbox.push.apple.com:2195 \
      //     -cert CookoutCert.pem -key CookoutKey.pem
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
      SSLSocketFactory factory = sslContext.getSocketFactory();
      socket = (SSLSocket) factory.createSocket(SEND_HOSTNAME, SEND_PORT);
      socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());
      socket.startHandshake();

      // Send the request.
      OutputStream out = socket.getOutputStream();
      out.write(getRequestBody(article, registration));
      out.flush();

      // Read the response.
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
   * Retrieves the domain from a URL.
   */
  private static String getDomain(String urlString) {
    urlString = urlString.substring(urlString.indexOf("//") + 2);
    urlString = urlString.substring(0, urlString.indexOf("/"));
    return (urlString.startsWith("www.")) ? urlString.substring("www.".length()) : urlString;
  }

  /**
   * Returns the {'aps':{'alert':'{'type':'a','id':'articleId'}'}} JSON object
   * that iOS's server-side SDK expects for APNS requests.  The object contains
   * the IDs of the devices to contact and a data explaining the new sound that
   * was sent.
   */
  private static JSONObject getNewArticleNotificationBody(Article article) {
    // Create JSON object to describe the notification.
    // NOTE(jonemerson): We only have 256 characters to send to APNS, so
    // we can't stuff the whole sound serialization into the request.
    JSONObject alertInner = new JSONObject();
    alertInner.put("type", "a");
    alertInner.put("id", article.getUrlId());

    String body = getDomain(article.getUrl()) + ": " + article.getTitle();
    if (body.length() > 60) {
      body = body.substring(0, 57) + "...";
    }
    alertInner.put("body", body);

    // Create the aps JSON.
    JSONObject aps = new JSONObject();
    aps.put("alert", alertInner);
    aps.put("content-available", "1");

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

  public static final void main(String args[]) throws DatabaseSchemaException {
    IosPushNotificationHelper.getInstance().sendNewArticleViaFuture(
        Database.with(Article.class).getFirst(), Users.getByEmail("panaceaa@gmail.com"));
  }
}
