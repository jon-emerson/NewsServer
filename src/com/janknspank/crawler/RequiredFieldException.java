package com.janknspank.crawler;

/**
 * Exception thrown when an article being interpreted doesn't have a field that
 * we consider required on Article objects.  Usually the solution to these
 * exceptions is to upgrade the interpreter to do a better job parsing the
 * article in question - e.g. looking at more meta fields for dates, looking at
 * the right elements for paragraph text, or other heroics.
 *
 * Often times this exception is thrown because the article in question actually
 * is invalid - it's a 404 page, or there actually are no paragraphs.
 */
public class RequiredFieldException extends Exception {
  public RequiredFieldException(String message) {
    super(message);
  }

  public RequiredFieldException(String message, Exception e) {
    super(message, e);
  }
}
