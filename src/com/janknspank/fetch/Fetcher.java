package com.janknspank.fetch;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.google.common.io.CharStreams;
import com.janknspank.dom.parser.DocumentBuilder;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.proto.CoreProto.Url;

/**
 * Central point for fetching Readers of URLs.
 * TODO(jonemerson): This class should enforce robots.txt.
 */
public class Fetcher {
  RequestConfig requestConfig = RequestConfig.custom()
      .setConnectionRequestTimeout(10000)
      .setConnectTimeout(10000)
      .setSocketTimeout(10000)
      .build();
  private CloseableHttpClient httpclient = HttpClients.custom()
      .setDefaultRequestConfig(requestConfig)
      .setUserAgent("Mozilla/5.0 (compatible; Spotterbot/1.0; +http://spotternews.com/)")
      .build();

  public Fetcher() {
  }

  public FetchResponse fetch(Url url) throws FetchException, ParserException {
    return fetch(url.getUrl());
  }

  public FetchResponse fetch(String urlString) throws FetchException, ParserException {
    CloseableHttpResponse response = null;
    Reader reader = null;
    try {
      response = httpclient.execute(new HttpGet(urlString));
      reader = new CharsetDetectingReader(response.getEntity().getContent());
      return new FetchResponse(response.getStatusLine().getStatusCode(),
          DocumentBuilder.build(urlString, reader));
    } catch (IOException e) {
      throw new FetchException("Error fetching " + urlString, e);
    } finally {
      IOUtils.closeQuietly(response);
      IOUtils.closeQuietly(reader);
    }
  }

  public String getResponseBody(String urlString) throws FetchException {
    CloseableHttpResponse response = null;
    Reader reader = null;
    StringWriter sw = null;
    try {
      response = httpclient.execute(new HttpGet(urlString));
      if (response.getStatusLine().getStatusCode() != HttpServletResponse.SC_OK) {
        throw new FetchException("Error fetching " + urlString + ": "
            + response.getStatusLine().getStatusCode());
      }
      reader = new CharsetDetectingReader(response.getEntity().getContent());
      sw = new StringWriter();
      CharStreams.copy(reader, sw);
      return sw.toString();
    } catch (IOException e) {
      throw new FetchException("Error fetching " + urlString, e);
    } finally {
      IOUtils.closeQuietly(response);
      IOUtils.closeQuietly(reader);
      IOUtils.closeQuietly(sw);
    }
  }
}