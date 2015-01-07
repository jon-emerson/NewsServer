package com.janknspank.interpreter;

public class RequiredFieldException extends Exception {
  public RequiredFieldException(String message) {
    super(message);
  }

  public RequiredFieldException(String message, Exception e) {
    super(message, e);
  }
}
