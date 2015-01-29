package com.janknspank.bizness;

public class BiznessException extends Exception {
  public BiznessException(String message) {
    super(message);
  }

  public BiznessException(String message, Exception cause) {
    super(message, cause);
  }
}
