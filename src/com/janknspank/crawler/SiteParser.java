package com.janknspank.crawler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterables;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.dom.parser.Node;
import com.janknspank.proto.CrawlerProto.SiteManifest;

public class SiteParser {
  private SiteParser() {}

  private static NodeCache NODE_CACHE = new NodeCache();
  private static class NodeCache
      extends ThreadLocal<LinkedHashMap<String, List<Node>>> {
    private static final int CACHE_SIZE_PER_THREAD = 5;

    @Override
    protected LinkedHashMap<String, List<Node>> initialValue() {
      return new LinkedHashMap<String, List<Node>>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, List<Node>> eldest) {
          return size() > CACHE_SIZE_PER_THREAD;
        }
      };
    }

    public List<Node> getNodes(final DocumentNode documentNode)
        throws RequiredFieldException {
      if (this.get().containsKey(documentNode.getUrl())) {
        return this.get().get(documentNode.getUrl());
      }

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

      this.get().put(documentNode.getUrl(), paragraphs);
      return paragraphs;
    }
  };

  /**
   * Returns the best set of DOM addresses we currently know about for the given
   * domain (or subdomain, if one is so configured).  Looks in
   * DOMAIN_TO_DOM_ADDRESSES hierarchically through each subdomain until a match
   * is found, or a default set is returned instead.
   */
  private static List<String> getDomAddressesForUrl(String url) {
    try {
      SiteManifest site = SiteManifests.getForUrl(new URL(url));
      if (site == null) {
        throw new IllegalStateException("No content site definition found for URL: " + url);
      }
      return site.getParagraphSelectorList();
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
   * @throws RequiredFieldException 
   */
  public static List<Node> getParagraphNodes(DocumentNode documentNode)
      throws RequiredFieldException {
    return NODE_CACHE.getNodes(documentNode);
  }
}
