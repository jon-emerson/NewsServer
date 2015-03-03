package com.janknspank.fetch;

import com.janknspank.dom.parser.DocumentNode;

public class FetchResponse {
  private final int statusCode;
  private final DocumentNode documentNode;

  FetchResponse(int statusCode, DocumentNode documentNode) {
    this.statusCode = statusCode;
    this.documentNode = documentNode;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public DocumentNode getDocumentNode() {
    return documentNode;
  }
}
