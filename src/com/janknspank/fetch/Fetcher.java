package com.janknspank.fetch;

import java.io.IOException;

import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * Central point for fetching Readers of URLs.
 * TODO(jonemerson): This class should enforce robots.txt.
 */
public class Fetcher {
  public Fetcher() {
  }

  public FetchResponse fetch(String url) throws FetchException {
    HttpGet httpget = new HttpGet(url);

    // Don't pick up cookies.
    RequestConfig config = RequestConfig.custom()
        .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
        .build();
    CloseableHttpClient httpclient = HttpClients.custom()
        .setDefaultRequestConfig(config)
        .build();

    try {
      return new FetchResponse(httpclient.execute(httpget));
    } catch (NullPointerException|IOException e) {
      throw new FetchException("Error fetching", e);
    }
  }
}
