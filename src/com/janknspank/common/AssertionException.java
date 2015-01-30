package com.janknspank.common;

public class AssertionException extends Exception {
  public AssertionException(String message) {
    super(message);
  }

  public AssertionException(String message, Exception cause) {
    super(message, cause);
  }
}
