package com.janknspank.server;

/**
 * Thrown when the user isn't properly authenticated.  Basically, this happens
 * if the user has a valid session (somehow) but there's no user associated
 * with it.
 */
public class AuthenticationRequiredException extends RequestException {
  public AuthenticationRequiredException(String message) {
    super(message);
  }

  public AuthenticationRequiredException(String message, Exception cause) {
    super(message, cause);
  }
}
