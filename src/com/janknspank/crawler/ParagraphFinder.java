package com.janknspank.crawler;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.janknspank.common.Logger;
import com.janknspank.common.StringHelper;
import com.janknspank.database.Database;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.CrawlerProto.SiteManifest;
import com.janknspank.proto.CrawlerProto.SiteManifest.ParagraphBlacklist;

import de.jetwick.snacktory.ArticleTextExtractor;

public class ParagraphFinder {
  private static final Extractor EXTRACTOR = new Extractor();
  private static class Extractor extends ArticleTextExtractor {
    public Elements extractParagraphs(Document doc) {
      prepareDocument(doc);

      // Initialize elements.
      Collection<Element> nodes = getNodes(doc);
      int maxWeight = 0;
      Element bestMatchElement = null;
      for (Element entry : nodes) {
        int currentWeight = getWeight(entry);
        if (currentWeight > maxWeight) {
          maxWeight = currentWeight;
          bestMatchElement = entry;
          if (maxWeight > 200) {
            break;
          }
        }
      }

      Elements elements = new Elements();
      if (bestMatchElement != null) {
        for (Element element : bestMatchElement.select("p")) {
          if (element.hasText()) {
            elements.add(element);
          }
        }
      }
      return elements;
    }
  }

  private static final int MAX_PARAGRAPH_LENGTH =
      Database.getStringLength(Article.class, "paragraph");
  private static final ParagraphCache PARAGRAPH_CACHE = new ParagraphCache();
  private static final ParagraphElCache PARAGRAPH_NODE_CACHE = new ParagraphElCache();
  private static final Logger LOG = new Logger(ParagraphFinder.class);
  private static final Set<String> LINE_BREAK_TAG_NAMES =
      ImmutableSet.of("br", "p", "h1", "h2", "h3");

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

  private static class ParagraphElCache
      extends ThreadLocal<LinkedHashMap<String, Elements>> {
    private static final int CACHE_SIZE_PER_THREAD = 5;

    @Override
    protected LinkedHashMap<String, Elements> initialValue() {
      return new LinkedHashMap<String, Elements>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Elements> eldest) {
          return size() > CACHE_SIZE_PER_THREAD;
        }
      };
    }

    public boolean containsUrl(String url) {
      return get().containsKey(url);
    }

    public Elements getParagraphEls(String url) {
      return get().get(url);
    }
  };

  private static Iterable<String> getTextLines(Element element) {
    final StringBuilder currentBuilder = new StringBuilder();
    final List<String> textLines = Lists.newArrayList();
    new NodeTraversor(new NodeVisitor() {
      @Override
      public void head(Node node, int depth) {
        if (node instanceof TextNode) {
          currentBuilder.append(((TextNode) node).text());
        } else if (node instanceof Element
            && LINE_BREAK_TAG_NAMES.contains(((Element) node).tagName().toLowerCase())
            && currentBuilder.length() > 0) {
          textLines.add(currentBuilder.toString());
          currentBuilder.setLength(0);
        }
      }

      @Override
      public void tail(Node node, int depth) {
      }
    }).traverse(element);
    if (currentBuilder.length() > 0) {
      textLines.add(currentBuilder.toString());
    }
    return textLines;
  }

  private static Iterable<String> getParagraphsInternal(Document document)
      throws RequiredFieldException {
    SiteManifest site = SiteManifests.getForUrl(document.baseUri());

    List<String> paragraphs = Lists.newArrayList();
    for (Element paragraphEl : getParagraphEls(document)) {
      for (String paragraph : getTextLines(paragraphEl)) {
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
          LOG.warning("Trimming paragraph text on " + document.baseUri());
          paragraph = paragraph.substring(0, MAX_PARAGRAPH_LENGTH - 1) + "\u2026";
        }
        if (paragraph.length() > 0) {
          paragraphs.add(paragraph);
        }
      }
    }
    if (Iterables.isEmpty(paragraphs)) {
      throw new RequiredFieldException("No paragraphs found in " + document.baseUri());
    }
    return paragraphs;
  }

  public static Iterable<String> getParagraphs(final Document document)
      throws RequiredFieldException {
    if (PARAGRAPH_CACHE.containsUrl(document.baseUri())) {
      return PARAGRAPH_CACHE.getParagraphs(document.baseUri());
    }
    Iterable<String> paragraphs = getParagraphsInternal(document);
    PARAGRAPH_CACHE.get().put(document.baseUri(), paragraphs);
    return paragraphs;
  }

  private static boolean textMatchesRegex(String text, String textRegex) {
    Pattern pattern = Pattern.compile(textRegex);
    return pattern.matcher(text).find();
  }

  public static boolean isParagraphElOkay(Element element, SiteManifest site, int offset) {
    if (site == null) {
      return true;
    }
    if (element.childNodeSize() == 0) {
      return false;
    }

    for (ParagraphBlacklist paragraphBlacklist : site.getParagraphBlacklistList()) {
      // If there's a blacklist paragraph selector, make sure it doesn't match
      // this node or any of its children.
      if (paragraphBlacklist.hasSelector()) {
        if (!paragraphBlacklist.hasTextRegex()
            || textMatchesRegex(element.text(), paragraphBlacklist.getTextRegex())) {
          // In Jsoup, select() includes the root element if it matches the selector.
          if (!element.select(paragraphBlacklist.getSelector()).isEmpty()) {
            return false;
          }
        }
      } else if (paragraphBlacklist.hasTextRegex()) {
        // Check textMatchesRegex here, as well as in getParagraphsInternal,
        // because sometimes callers get the Nodes (e.g. when finding hypertext
        // keywords), other time callers get the Strings (e.g. when writing to
        // the database), and both should have bad paragraphs removed.
        if (textMatchesRegex(element.text(), paragraphBlacklist.getTextRegex())) {
          return false;
        }
      } else {
        // NOTE: The case of !selector && !!textRegex is handled inside
        // getParagraphsInternal, after the paragraphs have been broken on their
        // <br>s, etc.
        throw new IllegalStateException(
            "ParagraphBlacklist has neither a selector nor a text_regex");
      }
    }

    // HACK(jonemerson): We should find a more extensible way of removing
    // By-lines and other crap we pick up. This may do for now.
    String text = element.text().toLowerCase();
    if (offset == 0
        && text.length() >= 3
        && text.substring(0, 3).equals("by ")
        && text.length() < 100) {
      return false;
    }

    // HACK(jonemerson): If people are glaringly touting that people should
    // follow them on Facebook and/or Twitter, don't include it in our indexing.
    // NOTE TO FUTURE SELVES: BE CAREFUL HERE.  Many sentences use the word
    // "like" in completely fair ways.  Don't filter on the word "like".  E.g.:
    // - "But WhatsApp has been able to hold its weight against messaging
    //     heavyweights like Twitter."
    // - "Like Instagram, WhatsApp will function as an autonomous unit within
    //     Facebook."
    if ((text.contains("facebook") || text.contains("twitter"))
         && text.contains("follow ")) { // The trailing space is deliberate.
       return false;
    }

    return true;
  }

  public static Elements getParagraphEls(final Document document)
      throws RequiredFieldException {
    if (PARAGRAPH_NODE_CACHE.containsUrl(document.baseUri())) {
      return PARAGRAPH_NODE_CACHE.getParagraphEls(document.baseUri());
    }

    Elements paragraphEls = new Elements();
    SiteManifest site = SiteManifests.getForUrl(document.baseUri());
    if (site != null) {
      for (String paragraphSelector : site.getParagraphSelectorList()) {
        paragraphEls.addAll(document.select(paragraphSelector));
      }
    }
    if (paragraphEls.isEmpty()) {
      // Fail-over to Snacktory's paragraph detection algorithm, which tries to
      // find non-trivial text nodes and then identify them as paragraphs, if
      // the manifest has no paragraph selectors or the paragraph selectors from
      // the manifest identified no paragraphs.
      paragraphEls.addAll(EXTRACTOR.extractParagraphs(document));
    } else {
      paragraphEls = sortAndDedupe(paragraphEls);
    }

    // Filter through various checks (blacklists, etc).
    Elements okayParagraphEls = new Elements();
    for (Element paragraphEl : paragraphEls) {
      if (isParagraphElOkay(paragraphEl, site, okayParagraphEls.size())) {
        okayParagraphEls.add(paragraphEl);
      }
    }
    paragraphEls = okayParagraphEls;

    PARAGRAPH_NODE_CACHE.get().put(document.baseUri(), paragraphEls);
    return paragraphEls;
  }

  /**
   * Given a list of paragraphs, sorts them and removes all duplicates.
   */
  private static Elements sortAndDedupe(Elements elements) {
    final Elements sortedDedupedElements = new Elements();
    final Set<Element> elementSet = Sets.newHashSet(elements);
    new NodeTraversor(new NodeVisitor() {
      @Override
      public void head(Node node, int depth) {
        if (elementSet.contains(node)) {
          elementSet.remove(node);
          sortedDedupedElements.add((Element) node);
        }
      }

      @Override
      public void tail(Node node, int depth) {
      }
    }).traverse(elements.first().parents().last());
    return sortedDedupedElements;
  }
}
