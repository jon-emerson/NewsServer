package com.janknspank.fetch;

import java.io.IOException;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * Central point for fetching Readers of URLs.
 * TODO(jonemerson): This class should enforce robots.txt.
 */
public class Fetcher {
  private CloseableHttpClient httpclient = HttpClients.createDefault();

  public Fetcher() {
  }

  public FetchResponse fetch(String url) throws FetchException {
    HttpGet httpget = new HttpGet(url);

    try {
      return new FetchResponse(httpclient.execute(httpget));
    } catch (NullPointerException|IOException e) {
      throw new FetchException("Error fetching", e);
    }
  }
}
