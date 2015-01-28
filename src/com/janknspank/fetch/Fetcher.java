package com.janknspank.fetch;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;

import javax.servlet.http.HttpServletResponse;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.common.io.CharStreams;
import com.janknspank.proto.Core.Url;

/**
 * Central point for fetching Readers of URLs.
 * TODO(jonemerson): This class should enforce robots.txt.
 */
public class Fetcher {
  private HttpTransport transport = new NetHttpTransport();
  private HttpRequestFactory httpRequestFactory = transport.createRequestFactory();

  public Fetcher() {
  }

  public FetchResponse fetch(Url url) throws FetchException {
    // TODO(jonemerson): Use the file system cache for returning cached responses?

    try {
      HttpRequest request = httpRequestFactory.buildGetRequest(new GenericUrl(url.getUrl()));
      HttpResponse response = request.execute();
      Reader reader = new CharsetDetectingReader(response.getContent());
      if (response.getStatusCode() == HttpServletResponse.SC_OK) {
        try {
          return new FetchResponse(response.getStatusCode(),
              cacheToFile(url, reader));
        } finally {
          reader.close();
        }
      }
      return new FetchResponse(response.getStatusCode(), reader);
    } catch (IOException e) {
      throw new FetchException("Error fetching", e);
    }
  }

  public FetchResponse fetch(String urlString) throws FetchException {
    try {
      HttpRequest request = httpRequestFactory.buildGetRequest(new GenericUrl(urlString));
      HttpResponse response = request.execute();
      return new FetchResponse(response.getStatusCode(),
          new CharsetDetectingReader(response.getContent()));
    } catch (IOException e) {
      throw new FetchException("Error fetching", e);
    }
  }

  private Reader cacheToFile(Url url, Reader reader) throws IOException {
    File file = new File("data/" + url.getId() + ".html");
    FileWriter writer = new FileWriter(file);
    try {
      CharStreams.copy(reader, writer);
    } finally {
      writer.close();
    }
    return new FileReader(file);
  }
}
