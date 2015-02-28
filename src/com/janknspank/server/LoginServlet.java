package com.janknspank.server;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.utils.URIBuilder;
import org.json.JSONObject;

import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.LinkedInLoginHandler;
import com.janknspank.bizness.Sessions;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.Serializer;
import com.janknspank.fetch.FetchException;
import com.janknspank.fetch.FetchResponse;
import com.janknspank.fetch.Fetcher;
import com.janknspank.proto.CoreProto.Session;
import com.janknspank.proto.UserProto.User;

public class LoginServlet extends StandardServlet {
  private final Fetcher fetcher = new Fetcher();
  static final String LINKED_IN_API_KEY;
  static final String LINKED_IN_SECRET_KEY;
  static {
    LINKED_IN_API_KEY = System.getenv("LINKED_IN_API_KEY");
    if (LINKED_IN_API_KEY == null) {
      throw new Error("$LINKED_IN_API_KEY is undefined");
    }
    LINKED_IN_SECRET_KEY = System.getenv("LINKED_IN_SECRET_KEY");
    if (LINKED_IN_SECRET_KEY == null) {
      throw new Error("$LINKED_IN_SECRET_KEY is undefined");
    }
  }

  private String getRedirectUrl(HttpServletRequest req) {
    try {
      URIBuilder builder = new URIBuilder()
          .setScheme(req.getScheme())
          .setHost(req.getServerName())
          .setPath("/login");
      int port = req.getServerPort();
      if (!(port == 0 ||
            port == 80 && "http".equals(req.getScheme()) ||
            port == 443 && "https".equals(req.getScheme()))) {
        builder.setPort(port);
      }
      return builder.build().toString();
    } catch (URISyntaxException e) {
      throw new Error(e);
    }
  }

  /**
   * Exchanges an authorization code for a access token by bouncing it off
   * of LinkedIn's API.
   * @throws BiznessException 
   */
  private String getAccessTokenFromAuthorizationCode(
      HttpServletRequest req, String authorizationCode, String state) throws BiznessException {
    Sessions.verifyLinkedInOAuthState(state);

    FetchResponse response;
    try {
      String url = new URIBuilder()
          .setScheme("https")
          .setHost("www.linkedin.com")
          .setPath("/uas/oauth2/accessToken")
          .addParameter("grant_type", "authorization_code")
          .addParameter("code", authorizationCode)
          .addParameter("redirect_uri", getRedirectUrl(req))
          .addParameter("client_id", LoginServlet.LINKED_IN_API_KEY)
          .addParameter("client_secret", LoginServlet.LINKED_IN_SECRET_KEY)
          .build().toString();
      System.out.println("Fetching " + url);
      response = fetcher.fetch(url);
    } catch (FetchException | URISyntaxException e) {
      throw new BiznessException("Could not fetch access token", e);
    }
    StringWriter sw = new StringWriter();
    try {
      CharStreams.copy(response.getReader(), sw);
    } catch (IOException e) {
      throw new BiznessException("Could not read accessToken response", e);
    }
    if (response.getStatusCode() == HttpServletResponse.SC_OK) {
      JSONObject responseObj = new JSONObject(sw.toString());
      return responseObj.getString("access_token");
    } else {
      System.out.println("Error: " + sw.toString());
      throw new BiznessException("Access token exchange failed (" + response.getStatusCode() + ")");
    }
  }

  @Override
  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws BiznessException, RequestException, DatabaseSchemaException, DatabaseRequestException,
          RedirectException {
    String authorizationCode = getParameter(req, "code");
    String state = getParameter(req, "state");
    if (!Strings.isNullOrEmpty(authorizationCode) && !Strings.isNullOrEmpty(state)) {
      String accessToken = getAccessTokenFromAuthorizationCode(req, authorizationCode, state);
      return loginFromLinkedIn(accessToken);
    }

    try {
      throw new RedirectException(new URIBuilder()
          .setScheme("https")
          .setHost("www.linkedin.com")
          .setPath("/uas/oauth2/authorization")
          .addParameter("response_type", "code")
          .addParameter("client_id", LINKED_IN_API_KEY)
          .addParameter("scope", "r_fullprofile r_emailaddress r_network")
          .addParameter("state", Sessions.getLinkedInOAuthState())
          .addParameter("redirect_uri", getRedirectUrl(req))
          .build().toString());
    } catch (URISyntaxException e) {
      throw new DatabaseSchemaException("Error creating LinkedIn OAuth URL", e);
    }
  }

  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DatabaseSchemaException, DatabaseRequestException, NotFoundException, RequestException,
          BiznessException {
    return loginFromLinkedIn(getRequiredParameter(req, "linkedInAccessToken"));
  }

  private JSONObject loginFromLinkedIn(String linkedInAccessToken)
      throws RequestException, DatabaseSchemaException, BiznessException, DatabaseRequestException {
    LinkedInLoginHandler linkedInLoginHandler = new LinkedInLoginHandler(linkedInAccessToken);
    User user = linkedInLoginHandler.getUser();
    Session session =
        Sessions.createFromLinkedProfile(linkedInLoginHandler.getLinkedInProfileDocument(), user);

    // Create the response.
    JSONObject response = this.createSuccessResponse();
    response.put("user", new UserHelper(user).getUserJson());
    response.put("session", Serializer.toJSON(session));
    return response;
  }
}
