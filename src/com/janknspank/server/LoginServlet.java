package com.janknspank.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.janknspank.data.DataInternalException;
import com.janknspank.data.DataRequestException;
import com.janknspank.data.Database;
import com.janknspank.data.IndustryCodes;
import com.janknspank.data.Sessions;
import com.janknspank.data.UserIndustries;
import com.janknspank.data.UserInterests;
import com.janknspank.data.Users;
import com.janknspank.data.ValidationException;
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
import com.janknspank.proto.Serializer;

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

  private CloseableHttpClient httpclient = HttpClients.createDefault();
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
      return new URIBuilder()
          .setScheme("http")
          .setHost(req.getServerName())
          .setPort(req.getLocalPort())
          .setPath("/login")
          .build().toString();
    } catch (URISyntaxException e) {
      throw new Error(e);
    }
  }

  private DocumentNode getLinkedInResponse(String url, String linkedInAccessToken)
      throws DataRequestException, DataInternalException, IllegalStateException, ParserException {
    CloseableHttpResponse response = null;
    try {
      HttpGet get = new HttpGet(url);
      get.setHeader("Authorization", "Bearer " + linkedInAccessToken);
      response = httpclient.execute(get);
      if (response.getStatusLine().getStatusCode() != HttpServletResponse.SC_OK) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteStreams.copy(response.getEntity().getContent(), baos);
        throw new DataRequestException("Bad access token.  Response code = " +
            response.getStatusLine().getStatusCode() + "\n" + new String(baos.toByteArray()));
      }
      return DocumentBuilder.build(url, new InputStreamReader(response.getEntity().getContent()));
    } catch (IOException e) {
      throw new DataInternalException("Error reading from LinkedIn: " + e.getMessage(), e);
    } finally {
      if (response != null) {
        try {
          response.close();
        } catch (IOException e) {}
      }
    }
  }

  /**
   * Exchanges an authorization code for a access token by bouncing it off
   * of LinkedIn's API.
   */
  private String getAccessTokenFromAuthorizationCode(
      HttpServletRequest req, String authorizationCode, String state)
      throws DataInternalException, DataRequestException {
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
    } catch (FetchException|URISyntaxException e) {
      throw new DataInternalException("Could not fetch access token");
    }
    StringWriter sw = new StringWriter();
    try {
      CharStreams.copy(response.getReader(), sw);
    } catch (IOException e) {
      throw new DataInternalException("Could not read accessToken response");
    }
    if (response.getStatusCode() == HttpServletResponse.SC_OK) {
      JSONObject responseObj = new JSONObject(sw.toString());
      return responseObj.getString("access_token");
    } else {
      System.out.println("Error: " + sw.toString());
      throw new DataRequestException("Access token exchange failed ("
          + response.getStatusCode() + ")");
    }
  }

  @Override
  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DataInternalException, ValidationException, DataRequestException, NotFoundException {
    String authorizationCode = getParameter(req, "code");
    String state = getParameter(req, "state");
    if (!Strings.isNullOrEmpty(authorizationCode) && !Strings.isNullOrEmpty(state)) {
      String accessToken = getAccessTokenFromAuthorizationCode(req, authorizationCode, state);
      return loginFromLinkedIn(accessToken);
    }

    resp.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
    try {
      resp.setHeader("Location", new URIBuilder()
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
      throw new DataInternalException("Error creating LinkedIn OAuth URL", e);
    }
    return null;
  }

  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DataInternalException, ValidationException, DataRequestException, NotFoundException {
    return loginFromLinkedIn(getRequiredParameter(req, "linkedInAccessToken"));
  }

  private JSONObject loginFromLinkedIn(String linkedInAccessToken)
      throws DataInternalException, DataRequestException, ValidationException {
    // Read user's profile from LinkedIn.  If this succeeds, we know the access
    // token is good, and we can proceed to log the user in.
    DocumentNode linkedInProfileDocument;
    try {
      linkedInProfileDocument = getLinkedInResponse(PROFILE_URL, linkedInAccessToken);
    } catch (ParserException e) {
      throw new DataInternalException("Could not parse linked in profile: " + e.getMessage(), e);
    }

    // Get or create a User and Session object for this user.
    User user = Users.loginFromLinkedIn(linkedInProfileDocument, linkedInAccessToken);
    Session session = Sessions.createFromLinkedProfile(linkedInProfileDocument, user);

    // Try to save the user's profile and update his interests
    // and industries
    List<UserInterest> interests;
    List<UserIndustry> industries;
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
    List<IndustryCode> industryCodes = IndustryCodes.getFrom(industries);

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
