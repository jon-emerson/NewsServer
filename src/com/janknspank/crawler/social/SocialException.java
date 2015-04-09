package com.janknspank.crawler.social;

/**
 * Exception thrown when trying to get social engagement from Facebook.
 */
public class SocialException extends Exception {
  public SocialException(String message) {
    super(message);
  }

  public SocialException(String message, Exception e) {
    super(message, e);
  }
}
