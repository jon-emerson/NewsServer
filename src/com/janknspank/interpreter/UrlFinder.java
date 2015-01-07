package com.janknspank.interpreter;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import com.google.common.collect.Lists;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.dom.parser.Node;

public class UrlFinder {
  /**
   * Resolves a relative URL to its base based on the URL of this article.
   */
  private static String resolveUrl(DocumentNode documentNode, String relativeUrl)
      throws MalformedURLException {
    return new URL(new URL(documentNode.getUrl()), relativeUrl).toString();
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
          !href.startsWith("mailto:")) {
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
}
