package com.janknspank.dom;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.dom.parser.DomBuilder;
import com.janknspank.dom.parser.Node;
import com.janknspank.dom.parser.ParserException;

public class SiteParser {
  private static final Map<String, String[]> DOMAIN_TO_DOM_ADDRESSES = Maps.newHashMap();
  static {
    DOMAIN_TO_DOM_ADDRESSES.put("abc.net.au", new String[] {
        ".article > p"});
    DOMAIN_TO_DOM_ADDRESSES.put("abcnews.go.com", new String[] {
        "#storyText > p"});
    DOMAIN_TO_DOM_ADDRESSES.put("aljazeera.com", new String[] {
        ".text > p"});
    DOMAIN_TO_DOM_ADDRESSES.put("arstechnica.com", new String[] {
        ".article-content > p"});
    DOMAIN_TO_DOM_ADDRESSES.put("bbc.co.uk", new String[] {
        ".story-body > p",
        ".map-body > p"});
    DOMAIN_TO_DOM_ADDRESSES.put("bbc.com", new String[] {
        ".story-body > p",
        ".map-body > p"});
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
    DOMAIN_TO_DOM_ADDRESSES.put("businessweek.com", new String[] {
        "#article_body p"});
    DOMAIN_TO_DOM_ADDRESSES.put("cbc.ca", new String[] {
        ".story-content p"});
    DOMAIN_TO_DOM_ADDRESSES.put("cbsnews.com", new String[] {
        "#article-entry p"});
    DOMAIN_TO_DOM_ADDRESSES.put("channelnewsasia.com", new String[] {
        ".news_detail p"});
    DOMAIN_TO_DOM_ADDRESSES.put("chron.com", new String[] {
        ".article-body p"});
    DOMAIN_TO_DOM_ADDRESSES.put("cleveland.com", new String[] {
        ".entry-content p"});
    DOMAIN_TO_DOM_ADDRESSES.put("cnbc.com", new String[] {
        "#article_body p"});
    DOMAIN_TO_DOM_ADDRESSES.put("cnn.com", new String[] {
        ".cnn_storyarea p"});
    DOMAIN_TO_DOM_ADDRESSES.put("curbed.com", new String[] {
        ".post-body > p",
        ".post-body > .post-more > p"});
    DOMAIN_TO_DOM_ADDRESSES.put("default", new String[] {
        "article > p",
        "article > div > p"});
    DOMAIN_TO_DOM_ADDRESSES.put("latimes.com", new String[] {
        ".trb_article_page > p"});
    DOMAIN_TO_DOM_ADDRESSES.put("markets.cbsnews.com", new String[] {
        ".news-story p"});
    DOMAIN_TO_DOM_ADDRESSES.put("mercurynews.com", new String[] {
        ".articleBody > p"});
    DOMAIN_TO_DOM_ADDRESSES.put("money.cnn.com", new String[] {
        "div#storytext p"});
    DOMAIN_TO_DOM_ADDRESSES.put("nytimes.com", new String[] {
        ".articleBody > p",
        "article > p",
        "article > div > p",
        "nyt_text > p",
        "p.story-body-text",
        "#mod-a-body-first-para > p",
        "#mod-a-body-after-first-para > p"});
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
  }

  /**
   * Returns the best set of DOM addresses we currently know about for the given
   * domain (or subdomain, if one is so configured).  Looks in
   * DOMAIN_TO_DOM_ADDRESSES hierarchically through each subdomain until a match
   * is found, or a default set is returned instead.
   */
  private static String[] getDomAddressesForUrl(String url) {
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
  private static void sortAndDedupe(List<Node> paragraphs) {
    // Sort all the nodes by when they first appeared in the document.
    Collections.sort(paragraphs, new Comparator<Node>() {
      @Override
      public int compare(Node o1, Node o2) {
        return (int) (o1.getStartingOffset() - o2.getStartingOffset());
      }
    });

    // Now that they're all in order, it's easy to remove duplicates - they'd
    // be right next to each other!
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

  private static void printSpacePrefix(int depth) {
    for (int i = 0; i < depth; i ++) {
      System.out.print("  ");
    }
  }

  /**
   * Helper function for pretty printing a site's DOM to System.out.
   */
  @SuppressWarnings("unused")
  private static void printNode(Node node, int depth) {
    // Print {@code node}.
    printSpacePrefix(depth);
    System.out.println(node.toString());

    // Iterate {@code node}'s children, unless they're <script> or <style> text.
    if (!"script".equalsIgnoreCase(node.getTagName()) &&
        !"style".equalsIgnoreCase(node.getTagName())) {
      for (int i = 0; i < node.getChildCount(); i++) {
        if (node.isChildTextNode(i)) {
          printSpacePrefix(depth + 1);
          System.out.println("TEXT: \"" + node.getChildText(i) + "\"");
        } else {
          printNode(node.getChildNode(i), depth + 1);
        }
      }
    }
  }

  /**
   * Returns Nodes for all the paragraph / header / quote / etc content within
   * an article's web page.
   */
  public List<Node> getParagraphNodes(InputStream inputStream, String url) throws DomException {
    DocumentNode documentNode;
    try {
      documentNode = new DomBuilder(inputStream).getDocumentNode();
      // printNode(documentNode, 0);
    } catch (ParserException e) {
      throw new DomException("Could not build DOM for URL " + url + ": " + e.getMessage(), e);
    }

    List<Node> paragraphs = new ArrayList<>();
    for (String domAddress : getDomAddressesForUrl(url)) {
      paragraphs.addAll(documentNode.findAll(domAddress));
    }
    sortAndDedupe(paragraphs);
    return paragraphs;
  }
}
