package com.janknspank.classifier;

public class ClassifierException extends Exception {
  public ClassifierException(String message) {
    super(message);
  }

  public ClassifierException(String message, Exception cause) {
    super(message, cause);
  }
}
