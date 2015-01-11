package com.janknspank.server;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.utils.URIBuilder;
import org.json.JSONObject;

import com.google.common.io.CharStreams;
import com.google.template.soy.data.SoyMapData;
import com.janknspank.data.DataInternalException;
import com.janknspank.data.DataRequestException;
import com.janknspank.data.Sessions;
import com.janknspank.data.ValidationException;
import com.janknspank.fetch.FetchException;
import com.janknspank.fetch.FetchResponse;
import com.janknspank.fetch.Fetcher;

public class ShowLinkedInAccessTokenServlet extends StandardServlet {
  private final Fetcher fetcher = new Fetcher();

  @Override
  protected SoyMapData getSoyMapData(HttpServletRequest req)
      throws DataInternalException, DataRequestException, ValidationException {
    SoyMapData data = new SoyMapData();

    String authorizationCode = req.getParameter("code");
    if (authorizationCode == null) {
      data.put("error", req.getParameter("error"));
      data.put("errorDescription", req.getParameter("error_description"));
      return data;
    }
    Sessions.verifyLinkedInOAuthState(getRequiredParameter(req, "state"));

    // Exchange the authorization code for a access token by bouncing it off
    // of LinkedIn's API.
    FetchResponse response;
    try {
      response = fetcher.fetch(new URIBuilder()
          .setScheme("https")
          .setHost("www.linkedin.com")
          .setPath("/uas/oauth2/accessToken")
          .addParameter("code", authorizationCode)
          .addParameter("redirect_uri", LoginServlet.LINKED_IN_REDIRECT_URL)
          .addParameter("client_id", LoginServlet.LINKED_IN_API_KEY)
          .addParameter("client_secret", LoginServlet.LINKED_IN_SECRET_KEY)
          .build().toString());
    } catch (FetchException|URISyntaxException e) {
      throw new DataInternalException("Could not fetch access token");
    }
    if (response.getStatusCode() != HttpServletResponse.SC_OK) {
      throw new DataRequestException("Access token exchange failed");
    }

    StringWriter sw = new StringWriter();
    try {
      CharStreams.copy(response.getReader(), sw);
    } catch (IOException e) {
      throw new DataInternalException("Could not read accessToken response");
    }
    JSONObject responseObj = new JSONObject(sw.toString());
    data.put("token", responseObj.getString("access_token"));
    data.put("expiresIn", responseObj.getString("expires_in"));

    return data;
  }
}
