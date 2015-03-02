package com.janknspank.fetch;

import java.io.IOException;
import java.io.Reader;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.janknspank.proto.CoreProto.Url;

/**
 * Central point for fetching Readers of URLs.
 * TODO(jonemerson): This class should enforce robots.txt.
 */
public class Fetcher {
  RequestConfig requestConfig = RequestConfig.custom()
      .setConnectionRequestTimeout(10000)
      .setConnectTimeout(10000)
      .setSocketTimeout(10000)
      .build();
  private CloseableHttpClient httpclient = HttpClients.custom()
      .setDefaultRequestConfig(requestConfig)
      .build();

  public Fetcher() {
  }

  public FetchResponse fetch(Url url) throws FetchException {
    // TODO(jonemerson): Use the file system cache for returning cached responses?

    try {
      CloseableHttpResponse response = httpclient.execute(new HttpGet(url.getUrl()));
      Reader reader = new CharsetDetectingReader(response.getEntity().getContent());
      return new FetchResponse(response.getStatusLine().getStatusCode(), reader);
    } catch (IOException e) {
      throw new FetchException("Error fetching " + url.getUrl(), e);
    }
  }

  public FetchResponse fetch(String urlString) throws FetchException {
    try {
      CloseableHttpResponse response = httpclient.execute(new HttpGet(urlString));
      return new FetchResponse(response.getStatusLine().getStatusCode(),
          new CharsetDetectingReader(response.getEntity().getContent()));
    } catch (IOException e) {
      throw new FetchException("Error fetching " + urlString, e);
    }
  }
}