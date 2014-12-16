package com.janknspank.dom;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.google.common.collect.Maps;

public class SiteParser {
  private static final Map<String, String[]> DOMAIN_TO_DOM_ADDRESSES = Maps.newHashMap();
  static {
    DOMAIN_TO_DOM_ADDRESSES.put("bbc.com", new String[] {
        ".story-body > p"});
    DOMAIN_TO_DOM_ADDRESSES.put("curbed.com", new String[] {
        ".post-body > p",
        ".post-body > .post-more > p"});
    DOMAIN_TO_DOM_ADDRESSES.put("default", new String[] {
        "article > p",
        "article > div > p"});
    DOMAIN_TO_DOM_ADDRESSES.put("latimes.com", new String[] {
        ".trb_article_page > p"});
    DOMAIN_TO_DOM_ADDRESSES.put("mercurynews.com", new String[] {
        ".articleBody > p"});
    DOMAIN_TO_DOM_ADDRESSES.put("nytimes.com", new String[] {
        ".articleBody > p",
        "article > p",
        "article > div > p",
        "nyt_text > p",
        "p.story-body-text"});
    DOMAIN_TO_DOM_ADDRESSES.put("sfexaminer.com", new String[] {
        "#storyBody > p"});
    DOMAIN_TO_DOM_ADDRESSES.put("sfgate.com", new String[] {
        ".article-body > p"});
    DOMAIN_TO_DOM_ADDRESSES.put("siliconbeat.com", new String[] {
        ".post-content > p"});
    DOMAIN_TO_DOM_ADDRESSES.put("techcrunch.com", new String[] {
        ".article-entry > p",
        ".article-entry > h2"});
    DOMAIN_TO_DOM_ADDRESSES.put("washingtonpost.com", new String[] {
        ".row p",
        "article > p"});
    DOMAIN_TO_DOM_ADDRESSES.put("abc.net.au", new String[] {
        ".article > p"});
    DOMAIN_TO_DOM_ADDRESSES.put("abcnews.go.com", new String[] {
        "#storyText > p"});
    DOMAIN_TO_DOM_ADDRESSES.put("aljazeera.com", new String[] {
        ".text > p"});
    DOMAIN_TO_DOM_ADDRESSES.put("arstechnica.com", new String[] {
        ".article-content > p"});
    DOMAIN_TO_DOM_ADDRESSES.put("bdnews24.com", new String[] {
        ".body > p"});
    DOMAIN_TO_DOM_ADDRESSES.put("bloomberg.com", new String[] {
        ".article_body > p"});
    DOMAIN_TO_DOM_ADDRESSES.put("boston.com", new String[] {
        ".content-text > p"});
    DOMAIN_TO_DOM_ADDRESSES.put("breitbart.com", new String[] {
        "article > p"});
    DOMAIN_TO_DOM_ADDRESSES.put("buffalonews.com", new String[] {
        ".articleP > p"});
  }

  public static DocumentNode crawl(String url) throws ParseException {
    HttpGet httpget = new HttpGet(url);

    RequestConfig config = RequestConfig.custom()
//        .setCookieSpec(CookieSpecs.IGNORE_COOKIES) // Don't pick up cookies.
        .build();
    CloseableHttpClient httpclient = HttpClients.custom()
        .setDefaultRequestConfig(config)
        .build();

    try {
      CloseableHttpResponse response = httpclient.execute(httpget);
      if (response.getStatusLine().getStatusCode() == 200) {
        return new HtmlHandler(response.getEntity().getContent()).getDocumentNode();
      }
      throw new ParseException("Bad response, status code = " +
          response.getStatusLine().getStatusCode());
    } catch (IOException e) {
      throw new ParseException("Could not read web site", e);
    }
  }

  /**
   * Returns the best set of DOM addresses we currently know about for the given
   * domain (or subdomain, if one is so configured).  Looks in
   * DOMAIN_TO_DOM_ADDRESSES hierarchically through each subdomain until a match
   * is found, or a default set is returned instead.
   */
  private String[] getDomAddressesForUrl(String url) {
    String domain;
    try {
      domain = new URL(url).getHost();
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }

    while (domain.contains(".")) {
      String[] domAddresses = DOMAIN_TO_DOM_ADDRESSES.get(domain);
      if (domAddresses != null) {
        return domAddresses;
      }
      domain = domain.substring(domain.indexOf(".") + 1);
    }
    return DOMAIN_TO_DOM_ADDRESSES.get("default");
  }

  /**
   * Given a list of paragraphs, sorts them and removes all duplicates.
   * Modifications are made in-place on the passed List.
   */
  private void sortAndDedupe(List<Node> paragraphs) {
    Collections.sort(paragraphs, new NodeOffsetComparator());
    Iterator<Node> i = paragraphs.iterator();
    long lastOffset = -1;
    while (i.hasNext()) {
      Node node = i.next();
      if (node.getStartingOffset() == lastOffset) {
        i.remove();
      }
      lastOffset = node.getStartingOffset();
    }
  }

  /**
   * Returns Nodes for all the paragraph / header / quote / etc content within
   * an article's web page.
   */
  public List<Node> getParagraphNodes(String url) throws ParseException {
    DocumentNode documentNode = crawl(url);
    List<Node> paragraphs = new ArrayList<>();
    for (String domAddress : getDomAddressesForUrl(url)) {
      paragraphs.addAll(documentNode.findAll(domAddress));
    }
    sortAndDedupe(paragraphs);
    return paragraphs;
  }
}
