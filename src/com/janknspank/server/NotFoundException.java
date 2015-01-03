package com.janknspank.server;

/**
 * Thrown when a request resource does not exist for mutation, deletion, etc.
 */
public class NotFoundException extends Exception {
  public NotFoundException(String message) {
    super(message);
  }

  public NotFoundException(String message, Exception e) {
    super(message, e);
  }
}
