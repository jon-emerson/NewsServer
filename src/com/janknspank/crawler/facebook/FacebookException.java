package com.janknspank.crawler.facebook;

/**
 * Exception thrown when trying to get social engagement from Facebook.
 */
public class FacebookException extends Exception {
  public FacebookException(String message) {
    super(message);
  }

  public FacebookException(String message, Exception e) {
    super(message, e);
  }
}
