package com.janknspank.server;

/**
 * Throw this if you'd like StandardServlet to redirect the user to a different
 * URL.
 */
public class RedirectException extends Exception {
  private final String nextUrl;

  public RedirectException(String nextUrl) {
    super();
    this.nextUrl = nextUrl;
  }

  public String getNextUrl() {
    return nextUrl;
  }
}
