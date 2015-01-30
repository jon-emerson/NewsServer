package com.janknspank.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.utils.URIBuilder;
import org.json.JSONObject;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.IndustryCodes;
import com.janknspank.bizness.Sessions;
import com.janknspank.bizness.UserIndustries;
import com.janknspank.bizness.UserInterests;
import com.janknspank.bizness.Users;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.Serializer;
import com.janknspank.dom.parser.DocumentBuilder;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.fetch.FetchException;
import com.janknspank.fetch.FetchResponse;
import com.janknspank.fetch.Fetcher;
import com.janknspank.proto.Core.IndustryCode;
import com.janknspank.proto.Core.LinkedInProfile;
import com.janknspank.proto.Core.Session;
import com.janknspank.proto.Core.User;
import com.janknspank.proto.Core.UserIndustry;
import com.janknspank.proto.Core.UserInterest;

public class LoginServlet extends StandardServlet {
  private static final String PROFILE_URL = "https://api.linkedin.com/v1/people/~:("
      + Joiner.on(",").join(ImmutableList.of("id", "first-name", "last-name", "industry",
          "headline", "siteStandardProfileRequest", "location", "num-connections", "summary",
          "specialties", "positions", "picture-url", "api-standard-profile-request",
          "public-profile-url", "email-address", "associations", "interests", "publications",
          "patents", "languages", "skills", "certifications", "educations", "courses", "volunteer",
          "three-current-positions", "three-past-positions", "num-recommenders",
          "recommendations-received", "following", "job-bookmarks", "suggestions",
          "date-of-birth", "member-url-resources", "related-profile-views", "honors-awards"))
      + ")"; // ?oauth2_access_token=%@&format=json
  private static final String CONNECTIONS_URL = "https://api.linkedin.com/v1/people/~/connections";

  private HttpTransport transport = new NetHttpTransport();
  private HttpRequestFactory httpRequestFactory = transport.createRequestFactory();
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

  private DocumentNode getLinkedInResponse(String url, String linkedInAccessToken)
      throws RequestException, ParserException, DatabaseSchemaException {
    HttpResponse response = null;
    try {
      HttpRequest request = httpRequestFactory.buildGetRequest(new GenericUrl(url));
      HttpHeaders headers = new HttpHeaders();
      headers.setAuthorization("Bearer " + linkedInAccessToken);
      request.setHeaders(headers);
      response = request.execute();
      if (response.getStatusCode() != HttpServletResponse.SC_OK) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteStreams.copy(response.getContent(), baos);
        throw new RequestException("Bad access token.  Response code = " +
            response.getStatusCode() + "\n" + new String(baos.toByteArray()));
      }
      return DocumentBuilder.build(url, new InputStreamReader(response.getContent()));
    } catch (IOException e) {
      throw new DatabaseSchemaException("Error reading from LinkedIn: " + e.getMessage(), e);
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
    // Read user's profile from LinkedIn.  If this succeeds, we know the access
    // token is good, and we can proceed to log the user in.
    DocumentNode linkedInProfileDocument;
    try {
      linkedInProfileDocument = getLinkedInResponse(PROFILE_URL, linkedInAccessToken);
    } catch (ParserException e) {
      throw new DatabaseSchemaException("Could not parse linked in profile: " + e.getMessage(), e);
    }

    // Get or create a User and Session object for this user.
    User user = Users.loginFromLinkedIn(linkedInProfileDocument, linkedInAccessToken);
    Session session = Sessions.createFromLinkedProfile(linkedInProfileDocument, user);

    // Try to save the user's profile and update his interests
    // and industries
    Iterable<UserInterest> interests;
    Iterable<UserIndustry> industries;
    try {
     LinkedInProfile linkedInProfile = LinkedInProfile.newBuilder()
          .setUserId(user.getId())
          .setData(linkedInProfileDocument.toLiteralString())
          .setCreateTime(System.currentTimeMillis())
          .build();
      Database.upsert(linkedInProfile);
      interests = UserInterests.updateInterests(user.getId(), linkedInProfileDocument,
          getLinkedInResponse(CONNECTIONS_URL, linkedInAccessToken));
      industries = UserIndustries.updateIndustries(user.getId(), linkedInProfileDocument);
    } catch (ParserException e) {
      System.out.println("Warning: Could not parse linked in profile: " + e.getMessage());
      e.printStackTrace();
      interests = UserInterests.getInterests(user.getId());
      industries = UserIndustries.getIndustries(user.getId());
    }
    
    // Get IndustryCodes so the client has more metadata to show
    Iterable<IndustryCode> industryCodes = IndustryCodes.getFrom(industries);

    // Create the response.
    UserHelper userHelper = new UserHelper(user);
    JSONObject response = this.createSuccessResponse();
    JSONObject userJson = Serializer.toJSON(user);
    userJson.put("ratings", userHelper.getRatingsJsonArray());
    userJson.put("favorites", userHelper.getFavoritesJsonArray());
    userJson.put("interests", Serializer.toJSON(interests));
    userJson.put("industries", Serializer.toJSON(industryCodes));
    response.put("user", userJson);
    response.put("session", Serializer.toJSON(session));
    return response;
  }
}
