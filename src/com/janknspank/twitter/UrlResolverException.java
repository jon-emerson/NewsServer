package com.janknspank.twitter;

public class UrlResolverException extends Exception {
  public UrlResolverException(String message) {
    super(message);
  }

  public UrlResolverException(String message, Exception e) {
    super(message, e);
  }

  public UrlResolverException(Exception e) {
    super(e);
  }
}
