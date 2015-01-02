package com.janknspank.fetch;

public class FetchException extends Exception {
  public FetchException(String message) {
    super(message);
  }

  public FetchException(String message, Exception e) {
    super(message, e);
  }
}
