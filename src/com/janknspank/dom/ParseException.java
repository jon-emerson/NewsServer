package com.janknspank.dom;

public class ParseException extends Exception {
  public ParseException(String message) {
    super(message);
  }

  public ParseException(String message, Exception e) {
    super(message, e);
  }
}
