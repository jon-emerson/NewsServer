package com.janknspank.data;

/**
 * This exception is thrown when the call into the 'data' package is invalid,
 * such as trying to log in a user that doesn't exist, trying to create a user
 * that already exists, etc.
 */
public class DataRequestException extends Exception {
  public DataRequestException(String message) {
    super(message);
  }

  public DataRequestException(String message, Exception e) {
    super(message, e);
  }
}
