package com.janknspank.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;

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
import com.google.api.client.util.Lists;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.janknspank.bizness.BiznessException;
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
import com.janknspank.dom.parser.Node;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.fetch.FetchException;
import com.janknspank.fetch.FetchResponse;
import com.janknspank.fetch.Fetcher;
import com.janknspank.proto.CoreProto.Session;
import com.janknspank.proto.UserProto.LinkedInProfile;
import com.janknspank.proto.UserProto.LinkedInProfile.Employer;
import com.janknspank.proto.UserProto.User;

public class LoginServlet extends StandardServlet {
  private static final String PROFILE_URL = "https://api.linkedin.com/v1/people/~:("
      + Joiner.on(",").join(ImmutableList.of("id", "email-address", "first-name", "last-name",
          "maiden-name", "formatted-name", "phonetic-first-name", "phonetic-last-name",
          "formatted-phonetic-name", "headline", "location", "industry", "current-share",
          "num-connections", "num-connections-capped", "summary", "positions", "picture-url",
          "site-standard-profile-request", "api-standard-profile-request", "public-profile-url"))
      + ")";
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

  private Long makeDate(Node year, Node month) {
    if (year == null) {
      return null;
    }
    GregorianCalendar calendar = new GregorianCalendar();
    calendar.set(Calendar.YEAR, Integer.parseInt(year.getFlattenedText()));
    if (month != null) {
      calendar.set(Calendar.MONTH, Integer.parseInt(month.getFlattenedText()) - 1);
    }
    return calendar.getTimeInMillis();
  }

  /**
   * Gets a list of the user's employers, past and present, ordered by end time
   * descending.
   */
  @VisibleForTesting
  List<Employer> getEmployers(DocumentNode linkedInProfileDocument) {
    List<Employer> employers = Lists.newArrayList();
    for (Node node : linkedInProfileDocument.findAll("positions > position")) {
      Employer.Builder builder = Employer.newBuilder();
      builder.setName(node.findFirst("company > name").getFlattenedText());
      builder.setTitle(node.findFirst("title").getFlattenedText());
      Long startTime = makeDate(node.findFirst("startDate > year"),
          node.findFirst("startDate > month"));
      if (startTime != null) {
        builder.setStartTime(startTime);
      }
      Long endTime = makeDate(node.findFirst("endDate > year"),
          node.findFirst("endDate > month"));
      if (endTime != null) {
        builder.setEndTime(endTime);
      }
      employers.add(builder.build());
    }
    Collections.sort(employers, new Comparator<Employer>() {
      @Override
      public int compare(Employer o1, Employer o2) {
        return - Long.compare(
            o1.hasEndTime() ? o1.getEndTime() : Long.MAX_VALUE,
            o2.hasEndTime() ? o2.getEndTime() : Long.MAX_VALUE);
      }
    });
    return employers;
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

    // Update the user's interests and industries based on the LinkedIn profile
    // we just received.
    try {
      LinkedInProfile.Builder linkedInProfileBuilder = LinkedInProfile.newBuilder()
          .setData(linkedInProfileDocument.toLiteralString())
          .setCreateTime(System.currentTimeMillis());
      List<Employer> employers = getEmployers(linkedInProfileDocument);
      if (employers.size() > 0) {
        linkedInProfileBuilder.setCurrentEmployer(employers.get(0));
      }
      if (employers.size() > 1) {
        linkedInProfileBuilder.addAllPastEmployer(employers.subList(1, employers.size()));
      }
      user = Database.set(user, "linked_in_profile", linkedInProfileBuilder.build());
      user = UserInterests.updateInterests(user, linkedInProfileDocument,
          getLinkedInResponse(CONNECTIONS_URL, linkedInAccessToken));
      user = UserIndustries.updateIndustries(user, linkedInProfileDocument);
    } catch (ParserException e) {
      System.out.println("Warning: Could not parse linked in profile: " + e.getMessage());
      e.printStackTrace();
    }

    // Create the response.
    UserHelper userHelper = new UserHelper(user);
    JSONObject response = this.createSuccessResponse();
    JSONObject userJson = Serializer.toJSON(user);
    userJson.put("favorites", userHelper.getFavoritesJsonArray());
    userJson.put("industries", userHelper.getIndustriesJsonArray());
    response.put("user", userJson);
    response.put("session", Serializer.toJSON(session));
    return response;
  }
}
