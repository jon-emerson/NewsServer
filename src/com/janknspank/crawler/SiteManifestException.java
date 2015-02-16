package com.janknspank.crawler;

/**
 * Thrown when a .manifest file for a particular site is invalid.
 */
public class SiteManifestException extends Exception {
  public SiteManifestException(String message) {
    super(message);
  }

  public SiteManifestException(String message, Exception e) {
    super(message, e);
  }
}
