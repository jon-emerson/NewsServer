package com.janknspank.crawler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.janknspank.proto.CrawlProto.ContentSite;

/**
 * Utility class for cleaning up news site URLs, so they can be reduced to their
 * canonical forms.
 */
public class UrlCleaner {
  public static final Function<String, String> TRANSFORM_FUNCTION =
      new Function<String, String>() {
        @Override
        public String apply(String dirtyUrl) {
          return clean(dirtyUrl);
        }
      };

  private static Set<String> getWhitelistedQueryParameters(URL url) {
    ContentSite contentSite = UrlWhitelist.getContentSiteForUrl(url);
    return (contentSite == null)
        ? ImmutableSet.<String>of()
        : ImmutableSet.copyOf(contentSite.getWhitelistedQueryParameterList());
  }

  private static List<NameValuePair> getCleanedParameters(URL url) {
    List<NameValuePair> cleanedParameters = Lists.newArrayList();
    Set<String> whitelistedQueryParameters = getWhitelistedQueryParameters(url);
    for (NameValuePair nameValue : URLEncodedUtils.parse(url.getQuery(), Charsets.UTF_8)) {
      if (whitelistedQueryParameters.contains(nameValue.getName())) {
        cleanedParameters.add(nameValue);
      }
    }
    return cleanedParameters;
  }

  public static String clean(String dirtyUrl) {
    URL url;
    try {
      url = new URL(dirtyUrl);
    } catch (MalformedURLException e) {
      // TODO(jonemerson): Handle this more elegantly.  Perhaps the URL
      // whitelist and the URL cleaner should be combined?
      e.printStackTrace();
      return dirtyUrl;
    }

    // Do website-specific query parameter filtering and URL canonicalization.
    String host = url.getHost();
    String path = url.getPath();

    if (host.endsWith(".businessinsider.com") || host.equals("businessinsider.com")) {
      if (path.contains("/")) {
        path = path.substring(0, path.indexOf("/"));
      }
    }
    if (host.endsWith(".cnn.com") || host.equals("cnn.com")) {
      if (path.endsWith("/index.html")) {
        path = path.substring(0, path.length() - "index.html".length());
      }
    }
    if (host.endsWith(".slate.com") || host.equals("slate.com")) {
      // Sometimes Slate paginates its articles - Make sure we always get a
      // single page.
      if (path.endsWith(".html") && !path.endsWith(".single.html")) {
        path = path.substring(0, path.length() - ".html".length()) + ".single.html";
      }
    }
    if (host.endsWith(".theverge.com") || host.equals("theverge.com")) {
      if (path.matches(".*\\/in\\/[0-9]{6,9}")) {
        path = path.substring(0, path.lastIndexOf("/in/"));
      }
    }
    if (host.endsWith(".washingtonpost.com") || host.equals("washingtonpost.com")) {
      if (path.startsWith("/pb/")) {
        path = path.substring("/pb".length());
      }
    }

    // Recreate the URL with alphabetized query parameters, lowercased scheme
    // and domain, and consistentized port, and no fragment.
    StringBuilder b = new StringBuilder();
    b.append(url.getProtocol().toLowerCase());
    b.append("://");
    b.append(host.toLowerCase());
    if (url.getPort() != -1 &&
        ("http".equalsIgnoreCase(url.getProtocol()) && url.getPort() != 80 ||
         "https".equalsIgnoreCase(url.getProtocol()) && url.getPort() != 443)) {
      b.append(":" + url.getPort());
    }
    if (path.length() > 1) {
      b.append(path);
    }
    List<NameValuePair> cleanedParameters = getCleanedParameters(url);
    if (cleanedParameters.size() > 0) {
      b.append("?");
      b.append(URLEncodedUtils.format(cleanedParameters, Charsets.UTF_8));
    }
    return b.toString();
  }
}
