package com.janknspank.pinterest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.janknspank.bizness.BiznessException;
import com.janknspank.fetch.FetchException;
import com.janknspank.fetch.FetchResponse;
import com.janknspank.fetch.Fetcher;

/**
 * This class is responsible for finding and caching Pinterest's app version
 * and unauthenticated XSRF tokens.
 */
public class ConstantsRetriever {
  private final Fetcher fetcher;
  private String csrfToken = null;
  private String appVersion = null;

  public ConstantsRetriever(Fetcher fetcher) {
    this.fetcher = fetcher;
  }

  private void init() throws FetchException, BiznessException {
    if (csrfToken != null && appVersion != null) {
      return;
    }

    FetchResponse loginPageResponse = fetcher.get(PinterestPinner.PINTEREST_URL + "/login/");
    String javascriptInit =
        loginPageResponse.getDocument().select("#jsInit").first().text();
    Matcher matcher = Pattern.compile("\"app_version\": \"([0-9a-z]+)\"").matcher(javascriptInit);
    if (matcher.find()) {
      appVersion = matcher.group(1);
    } else {
      throw new BiznessException("Error getting app_version from P.scout.init() JSON data.");
    }

    csrfToken = loginPageResponse.getSetCookieValue("csrftoken");
    if (csrfToken == null) {
      throw new BiznessException("Error getting CSRFToken.");
    }
  }

  /**
   * Returns the CSRF token that should be used for unauthenticated requests to
   * Pinterest.
   */
  public String getCsrfToken() throws FetchException, BiznessException {
    init();
    return csrfToken;
  }

  /**
   * Returns the app version value that should be used on all requests to
   * Pinterest.
   */
  public String getAppVersion() throws FetchException, BiznessException {
    init();
    return appVersion;
  }

  /**
   * For testing.
   */
  public static void main(String args[]) throws Exception {
    ConstantsRetriever constantsRetriever = new ConstantsRetriever(new Fetcher());
    System.out.println("csrf = " + constantsRetriever.getCsrfToken());
    System.out.println("app_version = " + constantsRetriever.getAppVersion());
  }
}
