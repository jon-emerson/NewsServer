package com.janknspank.dom.parser;


/**
 * Represents the top-most node in a Document tree.
 */
public class DocumentNode extends Node {
  public DocumentNode() {
    super(null, null, 0);
  }

  @Override
  public String toString() {
    return "DOCUMENT";
  }
}
