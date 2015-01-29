package com.janknspank.database;

/**
 * Thrown if the data provided by the caller is invalid and cannot be used
 * to successfully execute a query against the database.
 */
public class DatabaseRequestException extends Exception {
  public DatabaseRequestException(String message, Exception e) {
    super(message, e);
  }

  public DatabaseRequestException(String message) {
    super(message);
  }
}
