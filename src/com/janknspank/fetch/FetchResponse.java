package com.janknspank.fetch;

import java.io.IOException;
import java.io.Reader;

import org.apache.http.client.methods.CloseableHttpResponse;

public class FetchResponse {
  private final CloseableHttpResponse closeableHttpResponse;
  private CharsetDetectingReader reader = null;

  FetchResponse(CloseableHttpResponse closeableHttpResponse) {
    this.closeableHttpResponse = closeableHttpResponse;
  }

  public int getStatusCode() {
    return closeableHttpResponse.getStatusLine().getStatusCode();
  }

  public Reader getReader() throws FetchException {
    if (reader != null) {
      throw new FetchException("Reader already fetched");
    }
    try {
      reader = new CharsetDetectingReader(closeableHttpResponse.getEntity().getContent());
    } catch (IOException e) {
      throw new FetchException("Could not read stream: " + e.getMessage(), e);
    }
    return reader;
  }
}
