package com.janknspank.server;

/**
 * There was an error in the data the user presented to the server.
 */
public class RequestException extends Exception {
  public RequestException(String message) {
    super(message);
  }

  public RequestException(String message, Exception cause) {
    super(message, cause);
  }
}
