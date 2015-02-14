package com.janknspank.crawler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.dom.parser.Node;
import com.janknspank.proto.CrawlProto.ContentSite;

public class SiteParser extends CacheLoader<DocumentNode, List<Node>> {
  private static LoadingCache<DocumentNode, List<Node>> CACHE =
      CacheBuilder.newBuilder()
          .maximumSize(10)
          .expireAfterWrite(10, TimeUnit.MINUTES)
          .build(new SiteParser());

  private SiteParser() {}

  /**
   * Returns the best set of DOM addresses we currently know about for the given
   * domain (or subdomain, if one is so configured).  Looks in
   * DOMAIN_TO_DOM_ADDRESSES hierarchically through each subdomain until a match
   * is found, or a default set is returned instead.
   */
  private static List<String> getDomAddressesForUrl(String url) {
    try {
      ContentSite contentSite = UrlWhitelist.getContentSiteForUrl(new URL(url));
      if (contentSite == null) {
        throw new IllegalStateException("No content site definition found for URL: " + url);
      }
      return contentSite.getParagraphSelectorList();
    } catch (MalformedURLException e) {
      throw new IllegalStateException("Bad URL: " + url);
    }
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

  /**
   * Returns Nodes for all the paragraph / header / quote / etc content within
   * an article's web page.
   */
  public static List<Node> getParagraphNodes(DocumentNode documentNode) {
    try {
      return CACHE.get(documentNode);
    } catch (ExecutionException e) {
      Throwables.propagateIfPossible(e.getCause());
      throw new RuntimeException("Could not parse paragraphs: " + e.getMessage(), e);
    }
  }

  /**
   * DO NOT CALL THIS DIRECTLY.
   * @see #getParagraphNodes(DocumentNode)
   */
  @Override
  public List<Node> load(DocumentNode documentNode) {
    List<Node> paragraphs = new ArrayList<>();
    for (String domAddress : getDomAddressesForUrl(documentNode.getUrl())) {
      paragraphs.addAll(documentNode.findAll(domAddress));
    }

    // HACK(jonemerson): We should find a more extensible way of removing
    // By-lines and other crap we pick up. This may do for now.
    while (paragraphs.size() > 0 &&
        paragraphs.get(0).getFlattenedText().toLowerCase().startsWith("by ") &&
        paragraphs.get(0).getFlattenedText().length() < 100) {
      // Remove "By XXX from Washington Post" etc. crap text.
      paragraphs.remove(0);
    }

    // Remove trailing lines.  E.g. the "Have something to add to this story?
    // Share it in the comments." <em> text on mashable.com, or "Chat with me
    // on Twitter @peard33" <strong> text on latimes.com.
    while (paragraphs.size() > 0) {
      // Do allow <em>s and <strong>s if sufficiently embedded inside the
      // paragraph.  (Ya... some sites do use them fairly.)
      long paragraphOffset = paragraphs.get(paragraphs.size() - 1).getStartingOffset();
      Node firstEm = Iterables.getLast(paragraphs, null).findFirst("em");
      Node firstStrong = Iterables.getLast(paragraphs, null).findFirst("strong");
      if ((firstEm != null && (firstEm.getStartingOffset() - paragraphOffset < 10)) ||
          (firstStrong != null && (firstStrong.getStartingOffset() - paragraphOffset < 10))) {
        paragraphs.remove(paragraphs.size() - 1);
      } else {
        break;
      }
    }

    sortAndDedupe(paragraphs);
    return paragraphs;
  }
}
