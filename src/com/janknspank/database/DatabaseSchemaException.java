package com.janknspank.database;

/**
 * Thrown when there's an internal error in the definition of the database's
 * schema.  Basically, the schema definitions are invalid or the database does
 * not match the schema.  These exceptions are not the fault of the caller.
 *
 * TODO(jonemerson): We should validate the schemas at start-up, then change
 * this exception to a RuntimeException, since it will be much more rare.
 */
public class DatabaseSchemaException extends Exception {
  public DatabaseSchemaException(String message, Exception e) {
    super(message, e);
  }

  public DatabaseSchemaException(String message) {
    super(message);
  }
}
