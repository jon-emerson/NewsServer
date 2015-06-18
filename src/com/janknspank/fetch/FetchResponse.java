package com.janknspank.fetch;

import java.util.Collection;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.jsoup.nodes.Document;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

public class FetchResponse {
  private final int statusCode;
  private final Document document;
  private final Multimap<String, String> headers;

  FetchResponse(CloseableHttpResponse response, Document document) {
    this.statusCode = response.getStatusLine().getStatusCode();
    this.document = document;

    ImmutableMultimap.Builder<String, String> headerMapBuilder = ImmutableMultimap.builder();
    for (Header header : response.getAllHeaders()) {
      headerMapBuilder.put(header.getName(), header.getValue());
    }
    headers = headerMapBuilder.build();
  }

  public int getStatusCode() {
    return statusCode;
  }

  public Document getDocument() {
    return document;
  }

  public Collection<String> getHeaders(String headerName) {
    return headers.get(headerName);
  }

  public String getSetCookieValue(String cookieName) {
    for (String cookieHeader : getHeaders("Set-Cookie")) {
      if (cookieHeader.startsWith(cookieName + "=")) {
        return cookieHeader.substring((cookieName + "=").length(), cookieHeader.indexOf(";"));
      }
    }
    return null;
  }
}
