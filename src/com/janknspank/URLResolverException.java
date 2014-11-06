package com.janknspank;

public class URLResolverException extends Exception {
  public URLResolverException(String message) {
    super(message);
  }

  public URLResolverException(String message, Exception e) {
    super(message, e);
  }

  public URLResolverException(Exception e) {
    super(e);
  }
}
