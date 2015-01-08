package com.janknspank.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import com.google.protobuf.ByteString;
import com.janknspank.data.DataInternalException;
import com.janknspank.data.DataRequestException;
import com.janknspank.data.Database;
import com.janknspank.data.Sessions;
import com.janknspank.data.UserInterests;
import com.janknspank.data.Users;
import com.janknspank.data.ValidationException;
import com.janknspank.dom.parser.DocumentBuilder;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.proto.Core.LinkedInProfile;
import com.janknspank.proto.Core.Session;
import com.janknspank.proto.Core.User;
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

  private CloseableHttpClient httpclient = HttpClients.createDefault();
  private static final String LINKED_IN_API_KEY;
  private static final String LINKED_IN_SECRET_KEY;
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

  private byte[] getProfileResponseBytes(String linkedInAccessToken)
      throws DataInternalException, DataRequestException {
    try {
      HttpGet get = new HttpGet(PROFILE_URL);
      get.setHeader("Authorization", "Bearer " + linkedInAccessToken);
      CloseableHttpResponse response = httpclient.execute(get);
      if (response.getStatusLine().getStatusCode() != HttpServletResponse.SC_OK) {
        throw new DataRequestException("Bad access token");
      }

      ByteArrayOutputStream profileResponseBaos = new ByteArrayOutputStream();
      ByteStreams.copy(response.getEntity().getContent(), profileResponseBaos);
      return profileResponseBaos.toByteArray();
    } catch (IOException e) {
      throw new DataInternalException("Error reading from LinkedIn: " + e.getMessage(), e);
    }
  }

  @Override
  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DataInternalException, ValidationException, DataRequestException, NotFoundException {
    // Get parameters.
    String linkedInAccessToken = getRequiredParameter(req, "linkedInAccessToken");
    // String linkedInExpiresIn = getRequiredParameter(req, "linkedInExpiresIn");

    // Read user's profile from LinkedIn.  If this succeeds, we know the access
    // token is good, and we can proceed to log the user in.
    byte[] profileResponseBytes = getProfileResponseBytes(linkedInAccessToken);
    DocumentNode linkedInProfileDocument;
    try {
      linkedInProfileDocument = DocumentBuilder.build(PROFILE_URL,
          new InputStreamReader(new ByteArrayInputStream(profileResponseBytes)));
    } catch (ParserException e) {
      throw new DataInternalException("Error reading from LinkedIn: " + e.getMessage(), e);
    }

    // Get or create a User and Session object for this user.
    User user = Users.loginFromLinkedIn(linkedInProfileDocument, linkedInAccessToken);
    Session session = Sessions.createFromLinkedProfile(linkedInProfileDocument, user);

    // Save the user's profile and update his interests.
    LinkedInProfile linkedInProfile = LinkedInProfile.newBuilder()
        .setUserId(user.getId())
        .setDataBytes(ByteString.copyFrom(profileResponseBytes))
        .setCreateTime(System.currentTimeMillis())
        .build();
    Database.upsert(linkedInProfile);
    List<UserInterest> interests =
        UserInterests.updateInterests(user.getId(), linkedInProfileDocument);

    // Create the response.
    UserHelper userHelper = new UserHelper(user);
    JSONObject response = this.createSuccessResponse();
    JSONObject userJson = new JSONObject();
    userJson.put("ratings", userHelper.getRatingsJsonArray());
    userJson.put("favorites", userHelper.getFavoritesJsonArray());
    userJson.put("interests", Serializer.toJSON(interests));
    response.put("user", userJson);
    response.put("session", Serializer.toJSON(session));
    return response;
  }

  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DataInternalException, ValidationException, DataRequestException, NotFoundException {
    return doGetInternal(req, resp);
  }
}
