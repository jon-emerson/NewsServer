package com.janknspank.dom.parser;

public class ParserException extends Exception {
  public ParserException(String message) {
    super(message);
  }

  public ParserException(String message, Exception e) {
    super(message, e);
  }
}
