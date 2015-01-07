package com.janknspank.fetch;

import java.io.Reader;

public class FetchResponse {
  private final int statusCode;
  private final Reader reader;

  FetchResponse(int statusCode, Reader reader) {
    this.statusCode = statusCode;
    this.reader = reader;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public Reader getReader() {
    return reader;
  }
}
