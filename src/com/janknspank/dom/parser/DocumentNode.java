package com.janknspank.dom.parser;


/**
 * Represents the top-most node in a Document tree.
 */
public class DocumentNode extends Node {
  private final String url;

  public DocumentNode(String url) {
    super(null, null, 0);
    this.url = url;
  }

  @Override
  public String toString() {
    return "DOCUMENT";
  }

  @Override
  public int hashCode() {
    return url.hashCode() + 10;
  }

  public String getUrl() {
    return url;
  }
}
