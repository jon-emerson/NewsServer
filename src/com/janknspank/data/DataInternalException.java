package com.janknspank.data;

/**
 * This exception is thrown when an internal error occurred while processing
 * data inside the data package.
 */
public class DataInternalException extends Exception {
  public DataInternalException(String message) {
    super(message);
  }

  public DataInternalException(String message, Exception e) {
    super(message, e);
  }
}
