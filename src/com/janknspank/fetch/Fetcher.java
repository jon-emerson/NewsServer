package com.janknspank.fetch;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.CharStreams;
import com.janknspank.dom.parser.DocumentBuilder;
import com.janknspank.dom.parser.ParserException;

/**
 * Central point for fetching Readers of URLs.
 * TODO(jonemerson): This class should enforce robots.txt.
 */
public class Fetcher {
  private static final PoolingHttpClientConnectionManager CONNECTION_MANAGER =
      new PoolingHttpClientConnectionManager();
  static {
    // Increase max total connection to 200.
    CONNECTION_MANAGER.setMaxTotal(200);

    // Increase default max connection per route to 20.
    CONNECTION_MANAGER.setDefaultMaxPerRoute(20);
  }

  private final CloseableHttpClient httpClient;

  public Fetcher() {
    RequestConfig requestConfig = RequestConfig.custom()
        .setConnectionRequestTimeout(10000)
        .setConnectTimeout(10000)
        .setSocketTimeout(10000)
        .build();
    httpClient = HttpClients.custom()
        .setDefaultRequestConfig(requestConfig)
        .setConnectionManager(CONNECTION_MANAGER)
        .setUserAgent("Mozilla/5.0 (compatible; Spotterbot/1.0; +http://spotternews.com/)")
        .build();
  }

  public FetchResponse get(String urlString) throws FetchException {
    return get(urlString, ImmutableMultimap.<String, String>of());
  }

  public FetchResponse get(String urlString, Multimap<String, String> headers)
      throws FetchException {
    CloseableHttpResponse response = null;
    Reader reader = null;
    try {
      HttpGet httpGet = new HttpGet(urlString);
      for (Map.Entry<String, String> header : headers.entries()) {
        httpGet.addHeader(header.getKey(), header.getValue());
      }
      response = httpClient.execute(httpGet);
      reader = new CharsetDetectingReader(response.getEntity().getContent());
      return new FetchResponse(response, DocumentBuilder.build(urlString, reader));
    } catch (IOException e) {
      throw new FetchException("Error fetching " + urlString + ": " + e.getMessage(), e);
    } catch (ParserException e) {
      throw new FetchException("Error parsing URL " + urlString + ": " + e.getMessage(), e);
    } finally {
      IOUtils.closeQuietly(response);
      IOUtils.closeQuietly(reader);
    }
  }

  public FetchResponse post(
      String urlString, List<NameValuePair> postParameters, Multimap<String, String> headers)
      throws FetchException {
    CloseableHttpResponse response = null;
    Reader reader = null;
    try {
      HttpPost httpPost = new HttpPost(urlString);
      httpPost.setEntity(new UrlEncodedFormEntity(postParameters, Charsets.UTF_8));
      for (Map.Entry<String, String> header : headers.entries()) {
        httpPost.addHeader(header.getKey(), header.getValue());
      }
      response = httpClient.execute(httpPost);
      reader = new CharsetDetectingReader(response.getEntity().getContent());
      return new FetchResponse(response,
          DocumentBuilder.build(urlString, reader));
    } catch (IOException e) {
      throw new FetchException("Error fetching " + urlString + ": " + e.getMessage(), e);
    } catch (ParserException e) {
      throw new FetchException("Error parsing URL " + urlString + ": " + e.getMessage(), e);
    } finally {
      IOUtils.closeQuietly(response);
      IOUtils.closeQuietly(reader);
    }
  }

  public String postResponseBody(
      String urlString, List<NameValuePair> postParameters, Multimap<String, String> headers)
      throws FetchException {
    CloseableHttpResponse response = null;
    Reader reader = null;
    StringWriter sw = null;
    try {
      HttpPost httpPost = new HttpPost(urlString);
      httpPost.setEntity(new UrlEncodedFormEntity(postParameters, Charsets.UTF_8));
      for (Map.Entry<String, String> header : headers.entries()) {
        httpPost.addHeader(header.getKey(), header.getValue());
      }
      response = httpClient.execute(httpPost);
      reader = new CharsetDetectingReader(response.getEntity().getContent());
      sw = new StringWriter();
      CharStreams.copy(reader, sw);
      return sw.toString();
    } catch (IOException e) {
      throw new FetchException("Error fetching " + urlString + ": " + e.getMessage(), e);
    } finally {
      IOUtils.closeQuietly(response);
      IOUtils.closeQuietly(reader);
      IOUtils.closeQuietly(sw);
    }
  }

  public String getResponseBody(String urlString) throws FetchException {
    return getResponseBody(urlString, ImmutableMultimap.<String, String>of());
  }

  public String getResponseBody(String urlString, Multimap<String, String> headers)
      throws FetchException {
    CloseableHttpResponse response = null;
    Reader reader = null;
    StringWriter sw = null;
    try {
      HttpGet httpGet = new HttpGet(urlString);
      for (Map.Entry<String, String> header : headers.entries()) {
        httpGet.addHeader(header.getKey(), header.getValue());
      }
      response = httpClient.execute(httpGet);
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