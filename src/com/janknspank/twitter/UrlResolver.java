package com.janknspank.twitter;

import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.protocol.HttpContext;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Class that converts short URLs to long URLs.
 */
public class UrlResolver {
  private static final UrlResolver SINGLETON = new UrlResolver();

  // Never redirect automatically.  We will handle the redirects.
  CloseableHttpAsyncClient httpClient = HttpAsyncClients.custom()
      .setRedirectStrategy(new RedirectStrategy() {
        @Override
        public HttpUriRequest getRedirect(HttpRequest request, HttpResponse response,
            HttpContext context) throws ProtocolException {
          return null;
        }

        @Override
        public boolean isRedirected(HttpRequest request, HttpResponse response,
            HttpContext context) throws ProtocolException {
          return false;
        }
      }).build();

  private UrlResolver() {
    httpClient.start();
  }

  public static UrlResolver getInstance() {
    return SINGLETON;
  }

  /**
   * Converts a short URL to its long version.
   */
  public ListenableFuture<String> resolve(final String shortUrl) {
    return resolve(shortUrl, 0);
  }

  /**
   * Private implementation of URL longification: Makes sure that we don't run
   * into infinite redirect loops.
   * @param shortUrl the URL to resolve
   * @param depth the number of recursions we've gone through so far
   */
  private ListenableFuture<String> resolve(final String shortUrl, final int depth) {
    String easyLongUrl = getEasyLongUrl(shortUrl);
    if (easyLongUrl != null) {
      return Futures.immediateFuture(easyLongUrl);
    }

    HttpHead httpHead = new HttpHead(shortUrl);
    return Futures.transform(
        JdkFutureAdapters.listenInPoolThread(
            httpClient.execute(httpHead, null)),
        new AsyncFunction<HttpResponse, String>() {
          public ListenableFuture<String> apply(HttpResponse response) throws Exception {
            int statusCode = response.getStatusLine().getStatusCode();
            if (depth < 5 &&
                (statusCode == HttpServletResponse.SC_MOVED_PERMANENTLY ||
                 statusCode == HttpServletResponse.SC_MOVED_TEMPORARILY)) {
              // Resolve the Location URL as a relative URL, because if the host
              // didn't change, oftentimes the response only contains the
              // relative path.
              URL url = new URL(new URL(shortUrl), response.getFirstHeader("Location").getValue());
              return UrlResolver.this.resolve(url.toString(), depth + 1);
            }
            return Futures.immediateFuture(shortUrl);
          }
        });
  }

  private String getEasyLongUrl(String shortUrl) {
    if (shortUrl.startsWith("http://vine.co/v/") ||
        shortUrl.startsWith("http://instagram.com/p/") ||
        shortUrl.startsWith("http://path.com/p/") ||
        shortUrl.startsWith("https://path.com/p/") ||
        shortUrl.startsWith("http://pinterest.com/pin/")) {
      return shortUrl;
    }
    try {
      URL url = new URL(shortUrl);
      if (url.getPath().length() < 2) {
        return shortUrl;
      }
      if (shortUrl.startsWith("http://youtu.be/")) {
        String longUrl = "https://www.youtube.com/watch?v=" + url.getPath().substring(1) + "&feature=youtu.be";
        return (url.getQuery() != null) ? longUrl + "&" + url.getQuery() : longUrl;
      }
    } catch (MalformedURLException e) {
      // Don't care.
    }
    return null;
  }
}
