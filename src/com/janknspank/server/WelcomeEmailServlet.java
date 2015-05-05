package com.janknspank.server;

import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.utils.URIBuilder;

import com.google.template.soy.data.SoyMapData;
import com.janknspank.bizness.Users;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.UserProto.User;

@ServletMapping(urlPattern = "/welcome_email")
public class WelcomeEmailServlet extends StandardServlet {
  private static final String SCHMUTZ_SALT = "schmutzy!";

  /**
   * Returns any Soy data necessary for rendering the .main template for this
   * servlet's Soy page.
   */
  @Override
  protected SoyMapData getSoyMapData(HttpServletRequest req)
      throws DatabaseSchemaException, RequestException {
    User user =
        getValidatedUser(getRequiredParameter(req, "email"), getRequiredParameter(req, "schmutz"));
    return new SoyMapData(
        "title", "Welcome to Spotter",
        "isInBrowser", true,
        "mobileSpotterLogo1ImgSrc", "/resources/img/mobileSpotterLogo1@2x.png",
        "settingsZoomImgSrc", "/resources/img/settingsZoom@2x.png",
        "unsubscribeLink", getUnsubscribeLink(user, false /* relativeUrl */),
        "welcomeEmailLink", getWelcomeEmailLink(user));
  }

  public static User getValidatedUser(String email, String schmutz)
      throws RequestException, DatabaseSchemaException {
    User user = Users.getByEmail(email);
    if (!schmutz.equals(WelcomeEmailServlet.getSchmutz(user))) {
      throw new RequestException("Parameter schmutz does not match user");
    }
    return user;
  }

  public static String getWelcomeEmailLink(User user) {
    try {
      URIBuilder uriBuilder = new URIBuilder("http://www.spotternews.com/welcome_email");
      uriBuilder.setParameter("email", user.getEmail());
      uriBuilder.setParameter("schmutz", getSchmutz(user));
      return uriBuilder.toString();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private static String getSignedLink(String path, User user, boolean relativeUrl) {
    try {
      URIBuilder uriBuilder = new URIBuilder(relativeUrl
          ? path
          : "http://www.spotternews.com" + path);
      uriBuilder.setParameter("email", user.getEmail());
      uriBuilder.setParameter("schmutz", getSchmutz(user));
      return uriBuilder.toString();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getUnsubscribeLink(User user, boolean relativeUrl) {
    return getSignedLink("/unsubscribe", user, relativeUrl);
  }

  public static String getSubscribeLink(User user, boolean relativeUrl) {
    return getSignedLink("/subscribe", user, relativeUrl);
  }

  public static String getSchmutz(User user) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA1");
      md.update((SCHMUTZ_SALT + user.getEmail()).getBytes());
      return Base64.encodeBase64URLSafeString(md.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new Error("SHA1 hashing algorithm not found", e);
    }
  }

  public static void main(String args[]) throws DatabaseSchemaException {
    System.out.println(getUnsubscribeLink(
        Users.getByEmail("panaceaa@gmail.com"), true /* relativeUrl */));
  }
}
