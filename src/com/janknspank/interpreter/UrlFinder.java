package com.janknspank.interpreter;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import com.google.common.collect.Lists;
import com.janknspank.dom.parser.DocumentBuilder;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.dom.parser.Node;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.fetch.FetchException;
import com.janknspank.fetch.FetchResponse;
import com.janknspank.fetch.Fetcher;
import com.janknspank.proto.Core.Url;

public class UrlFinder {
  private static final Fetcher FETCHER = new Fetcher();

  /**
   * Retrieves the passed URL by making a request to the respective website,
   * and then interprets the returned results.
   */
  public static List<String> findUrls(Url url)
      throws FetchException, ParserException, RequiredFieldException {

    FetchResponse response = FETCHER.fetch(url);
    return findUrls(DocumentBuilder.build(url.getUrl(), response.getReader()));
  }

  /**
   * Returns all the URLs from the passed document.  Note: There is no filtering
   * done!!  It's ALL THE URLs!!  We do not want to crawl them all!
   */
  public static List<String> findUrls(DocumentNode documentNode) {
    List<String> urlList = Lists.newArrayList();
    for (Node linkNode : documentNode.findAll("html > body a[href]")) {
      String href = linkNode.getAttributeValue("href");
      if (!href.startsWith("javascript:") &&
          !href.startsWith("mailto:") &&
          !href.startsWith("whatsapp:")) {
        try {
          String resolvedUrl = resolveUrl(documentNode, href);
          if (resolvedUrl.startsWith("http://") || resolvedUrl.startsWith("https://")) {
            urlList.add(resolvedUrl);
          }
        } catch (MalformedURLException e) {
          System.out.println("Bad relative URL: " + linkNode.getAttributeValue("href"));
        }
      }
    }
    return urlList;
  }

  /**
   * Resolves a relative URL to its base based on the URL of this article.
   */
  private static String resolveUrl(DocumentNode documentNode, String relativeUrl)
      throws MalformedURLException {
    return new URL(new URL(documentNode.getUrl()), relativeUrl).toString();
  }
}
