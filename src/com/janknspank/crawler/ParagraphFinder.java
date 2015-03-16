package com.janknspank.crawler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.api.client.util.Lists;
import com.google.common.collect.Iterables;
import com.janknspank.common.Logger;
import com.janknspank.common.StringHelper;
import com.janknspank.database.Database;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.dom.parser.Node;
import com.janknspank.dom.parser.Selector;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.CrawlerProto.SiteManifest;

public class ParagraphFinder {
  private static final int MAX_PARAGRAPH_LENGTH =
      Database.getStringLength(Article.class, "paragraph");
  private static final ParagraphCache PARAGRAPH_CACHE = new ParagraphCache();
  private static final ParagraphNodeCache PARAGRAPH_NODE_CACHE = new ParagraphNodeCache();
  private static final Logger LOG = new Logger(ParagraphFinder.class);

  /**
   * A thread-specific cache of the paragraphs for a specific news article. This
   * fixes the concurrency and memory performance problems we were having with a
   * concurrent hash map that spanned our crawler threads by keeping the caches
   * local to a thread, such that eviction doesn't adversely affect other
   * threads and other threads aren't blocked by writes.  As a consequence of
   * the former, we can reduce the cache size to something that doesn't take
   * a whole bunch of memory.
   */
  private static class ParagraphCache
      extends ThreadLocal<LinkedHashMap<String, Iterable<String>>> {
    private static final int CACHE_SIZE_PER_THREAD = 5;

    @Override
    protected LinkedHashMap<String, Iterable<String>> initialValue() {
      return new LinkedHashMap<String, Iterable<String>>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Iterable<String>> eldest) {
          return size() > CACHE_SIZE_PER_THREAD;
        }
      };
    }

    public boolean containsUrl(String url) {
      return get().containsKey(url);
    }

    public Iterable<String> getParagraphs(String url) {
      return get().get(url);
    }
  };

  private static class ParagraphNodeCache
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

    public boolean containsUrl(String url) {
      return get().containsKey(url);
    }

    public List<Node> getParagraphNodes(String url) {
      return get().get(url);
    }
  };

  private static Iterable<String> getParagraphsInternal(final DocumentNode documentNode)
      throws RequiredFieldException {
    List<String> paragraphs = Lists.newArrayList();
    for (Node paragraphNode : getParagraphNodes(documentNode)) {
      for (String paragraph : paragraphNode.getTextLines()) {
        paragraph = StringHelper.unescape(paragraph).trim();
        if (paragraph.length() > MAX_PARAGRAPH_LENGTH) {
          LOG.warning("Trimming paragraph text on " + documentNode.getUrl());
          paragraph = paragraph.substring(0, MAX_PARAGRAPH_LENGTH - 1) + "\u2026";
        }
        if (paragraph.length() > 0) {
          paragraphs.add(paragraph);
        }
      }
    }
    if (Iterables.isEmpty(paragraphs)) {
      throw new RequiredFieldException("No paragraphs found in " + documentNode.getUrl());
    }
    return paragraphs;
  }

  public static Iterable<String> getParagraphs(final DocumentNode documentNode)
      throws RequiredFieldException {
    if (PARAGRAPH_CACHE.containsUrl(documentNode.getUrl())) {
      return PARAGRAPH_CACHE.getParagraphs(documentNode.getUrl());
    }
    Iterable<String> paragraphs = getParagraphsInternal(documentNode);
    PARAGRAPH_CACHE.get().put(documentNode.getUrl(), paragraphs);
    return paragraphs;
  }

  public static boolean isParagraphNodeOkay(Node node, SiteManifest site, int offset) {
    for (String blacklistSelector : site.getParagraphBlacklistSelectorList()) {
      if (new Selector(blacklistSelector).matches(node)
          || node.findFirst(blacklistSelector) != null) {
        return false;
      }
    }

    // HACK(jonemerson): We should find a more extensible way of removing
    // By-lines and other crap we pick up. This may do for now.
    String text = node.getFlattenedText();
    if (offset == 0
        && text.length() >= 3
        && text.substring(0, 3).equalsIgnoreCase("By ")
        && text.length() < 100) {
      return false;
    }
    return true;
  }

  public static List<Node> getParagraphNodes(final DocumentNode documentNode)
      throws RequiredFieldException {
    if (PARAGRAPH_NODE_CACHE.containsUrl(documentNode.getUrl())) {
      return PARAGRAPH_NODE_CACHE.getParagraphNodes(documentNode.getUrl());
    }

    SiteManifest site = SiteManifests.getForUrl(documentNode.getUrl());
    List<Node> paragraphs = new ArrayList<>();
    for (String paragraphSelector : site.getParagraphSelectorList()) {
      paragraphs.addAll(documentNode.findAll(paragraphSelector));
    }
    sortAndDedupe(paragraphs);

    // Filter through various checks (blacklists, etc).
    List<Node> okayParagraphs = Lists.newArrayList();
    for (Node paragraphNode : paragraphs) {
      if (isParagraphNodeOkay(paragraphNode, site, okayParagraphs.size())) {
        okayParagraphs.add(paragraphNode);
      }
    }
    paragraphs = okayParagraphs;

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

    PARAGRAPH_NODE_CACHE.get().put(documentNode.getUrl(), paragraphs);
    return paragraphs;
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
}
