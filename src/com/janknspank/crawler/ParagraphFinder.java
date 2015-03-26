package com.janknspank.crawler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
import com.janknspank.proto.CrawlerProto.SiteManifest.ParagraphBlacklist;

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
    SiteManifest site = SiteManifests.getForUrl(documentNode.getUrl());
    List<String> paragraphs = Lists.newArrayList();
    for (Node paragraphNode : getParagraphNodes(documentNode)) {
      for (String paragraph : paragraphNode.getTextLines()) {
        paragraph = StringHelper.unescape(paragraph).trim();

        boolean badParagraph = false;
        for (ParagraphBlacklist paragraphBlacklist : site.getParagraphBlacklistList()) {
          // .getTextLines() uncovers additional paragraphs that the paragraph
          // Node objects don't individually address: Namely, <br/>s are used
          // to split paragraphs.  Because of this, we must now re-run our
          // paragraph blacklists (but just the text regex ones!) against all
          // the now-unraveled paragraphs.  If anything's blacklisted, skip it.
          if (!paragraphBlacklist.hasSelector()
              && paragraphBlacklist.hasTextRegex()
              && textMatchesRegex(paragraph, paragraphBlacklist.getTextRegex())) {
            badParagraph = true;
            break;
          }
        }
        if (badParagraph) {
          continue;
        }

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

  private static boolean textMatchesRegex(String text, String textRegex) {
    Pattern pattern = Pattern.compile(textRegex);
    return pattern.matcher(text).find();
  }

  public static boolean isParagraphNodeOkay(Node node, SiteManifest site, int offset) {
    if (node.getChildCount() == 0) {
      return false;
    }

    for (ParagraphBlacklist paragraphBlacklist : site.getParagraphBlacklistList()) {
      // If there's a blacklist paragraph selector, make sure it doesn't match
      // this node or any of its children.
      if (paragraphBlacklist.hasSelector()) {
        if ((!paragraphBlacklist.hasTextRegex()
                || textMatchesRegex(node.getFlattenedText(), paragraphBlacklist.getTextRegex()))
            && (new Selector(paragraphBlacklist.getSelector()).matches(node)    // This node.
                || node.findFirst(paragraphBlacklist.getSelector()) != null)) { // Children.
          return false;
        }
      } else if (!paragraphBlacklist.hasTextRegex()) {
        // NOTE: The case of !selector && !!textRegex is handled inside
        // getParagraphsInternal, after the paragraphs have been broken on their
        // <br>s, etc.
        throw new IllegalStateException(
            "ParagraphBlacklist has neither a selector nor a text_regex");
      }
    }

    // HACK(jonemerson): We should find a more extensible way of removing
    // By-lines and other crap we pick up. This may do for now.
    String text = node.getFlattenedText().toLowerCase();
    if (offset == 0
        && text.length() >= 3
        && text.substring(0, 3).equals("by ")
        && text.length() < 100) {
      return false;
    }

    // HACK(jonemerson): Unfortunately we have to do this...
    // Actually, this was a bad idea.  Witness:
    // - "But WhatsApp has been able to hold its weight against messaging
    //     heavyweights like Twitter."
    // - "Like Instagram, WhatsApp will function as an autonomous unit within
    //     Facebook."
    // if ((text.contains("facebook") || text.contains("twitter"))
    //     && (text.contains("like") || text.contains("follow"))) {
    //   return false;
    // }

    return true;
  }

  public static List<Node> getParagraphNodes(final DocumentNode documentNode)
      throws RequiredFieldException {
    if (PARAGRAPH_NODE_CACHE.containsUrl(documentNode.getUrl())) {
      return PARAGRAPH_NODE_CACHE.getParagraphNodes(documentNode.getUrl());
    }

    SiteManifest site = SiteManifests.getForUrl(documentNode.getUrl());
    if (site == null) {
      throw new IllegalStateException("Site not supported for URL " + documentNode.getUrl());
    }

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
