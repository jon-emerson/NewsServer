package com.janknspank.fetch;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.google.common.io.CharStreams;
import com.janknspank.proto.Core.Url;

/**
 * Central point for fetching Readers of URLs.
 * TODO(jonemerson): This class should enforce robots.txt.
 */
public class Fetcher {
  private CloseableHttpClient httpclient = HttpClients.createDefault();

  public Fetcher() {
  }

  public FetchResponse fetch(Url url) throws FetchException {
    // TODO(jonemerson): Use the file system cache for returning cached responses?

    try {
      CloseableHttpResponse response = httpclient.execute(new HttpGet(url.getUrl()));
      Reader reader = new CharsetDetectingReader(response.getEntity().getContent());
      if (response.getStatusLine().getStatusCode() == HttpServletResponse.SC_OK) {
        try {
          return new FetchResponse(response.getStatusLine().getStatusCode(),
              cacheToFile(url, reader));
        } finally {
          reader.close();
        }
      }
      return new FetchResponse(response.getStatusLine().getStatusCode(), reader);
    } catch (IOException e) {
      throw new FetchException("Error fetching", e);
    }
  }

  public FetchResponse fetch(String urlString) throws FetchException {
    try {
      CloseableHttpResponse response = httpclient.execute(new HttpGet(urlString));
      return new FetchResponse(response.getStatusLine().getStatusCode(),
          new CharsetDetectingReader(response.getEntity().getContent()));
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
