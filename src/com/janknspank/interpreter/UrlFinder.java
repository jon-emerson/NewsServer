package com.janknspank.interpreter;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.janknspank.common.ArticleUrlDetector;
import com.janknspank.dom.parser.DocumentBuilder;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.dom.parser.Node;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.fetch.FetchException;
import com.janknspank.fetch.FetchResponse;
import com.janknspank.fetch.Fetcher;

public class UrlFinder {
  private static final Fetcher FETCHER = new Fetcher();

  /**
   * Simple synchronized cache for <base> tag lookups on documents.
   */
  private static final Map<DocumentNode, String> BASE_URL_MAP = Collections.synchronizedMap(
      new LinkedHashMap<DocumentNode, String>() {
        protected boolean removeEldestEntry(Map.Entry<DocumentNode, String> eldest) {
          return this.size() > 50;
        }
      });

  /**
   * Retrieves the passed URL by making a request to the respective website,
   * and then interprets the returned results.
   */
  public static List<String> findUrls(String url)
      throws FetchException, ParserException, RequiredFieldException {

    FetchResponse response = FETCHER.fetch(url);
    return findUrls(DocumentBuilder.build(url, response.getReader()));
  }

  /**
   * Returns all the URLs from the passed document.  Note: There is no filtering
   * done!!  It's ALL THE URLs!!  We do not want to crawl them all!
   */
  public static List<String> findUrls(DocumentNode documentNode) {
    List<String> urlList = Lists.newArrayList();
    for (Node linkNode : documentNode.findAll("html > body a[href]")) {
      String href = linkNode.getAttributeValue("href");
      String hrefToLowerCase = href.toLowerCase();
      if (!hrefToLowerCase.startsWith("javascript:") &&
          !hrefToLowerCase.startsWith("mailto:") &&
          !hrefToLowerCase.startsWith("whatsapp:")) {
        try {
          String resolvedUrl = resolveUrl(documentNode, href);
          // To save on space, only save links to articles.  Without this,
          // 80% of our data is links to general category pages and the like.
          if (ArticleUrlDetector.isArticle(resolvedUrl)) {
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
   * Resolves a relative URL to its fully-qualified version based on either the
   * <base> tag in the article, or the article URL itself.
   */
  private static String resolveUrl(DocumentNode documentNode, String relativeUrl)
      throws MalformedURLException {
    String baseUrl = BASE_URL_MAP.get(documentNode);
    if (baseUrl == null) {
      Node baseNode = documentNode.findFirst("base");
      baseUrl = (baseNode == null) ? documentNode.getUrl() : baseNode.getAttributeValue("href");
      BASE_URL_MAP.put(documentNode, baseUrl);
    }
    return new URL(new URL(baseUrl), relativeUrl).toString();
  }
}
