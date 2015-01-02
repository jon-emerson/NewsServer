package com.janknspank.dom;

public class DomException extends Exception {
  public DomException(String message) {
    super(message);
  }

  public DomException(String message, Exception e) {
    super(message, e);
  }
}
