package com.janknspank.facebook;

public class FacebookDataException extends Exception {
  public FacebookDataException(String message) {
    super(message);
  }

  public FacebookDataException(String message, Exception e) {
    super(message, e);
  }
}
