package com.janknspank.interpreter;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.janknspank.common.DateParser;
import com.janknspank.data.Articles;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.dom.parser.Node;
import com.janknspank.proto.Core.Article;

class ArticleCreator {
  private static final Set<String> IMAGE_URL_BLACKLIST = ImmutableSet.of(
      "http://media.cleveland.com/design/alpha/img/logo_cleve.gif",
      "http://www.chron.com/img/pages/article/opengraph_default.jpg",
      "http://www.sfgate.com/img/pages/article/opengraph_default.png");

  public static Article create(String urlId, DocumentNode documentNode)
      throws RequiredFieldException {
    Article.Builder articleBuilder = Article.newBuilder();
    articleBuilder.setUrlId(urlId);
    articleBuilder.setUrl(documentNode.getUrl());

    // Author.
    String author = getAuthor(documentNode);
    if (author != null) {
      articleBuilder.setAuthor(author);
    }

    // Copyright.
    String copyright = getCopyright(documentNode);
    if (copyright != null) {
      articleBuilder.setCopyright(copyright);
    }

    // Description (required).
    articleBuilder.setDescription(getDescription(documentNode));

    // Image url.
    String imageUrl = getImageUrl(documentNode);
    if (imageUrl != null) {
      articleBuilder.setImageUrl(imageUrl);
    }

    // Modified time.
    Long modifiedTime = getModifiedTime(documentNode);
    if (modifiedTime != null) {
      articleBuilder.setModifiedTime(modifiedTime);
    }

    // Paragraphs (required).
    articleBuilder.addAllParagraph(getParagraphs(documentNode));

    // Published time (required).
    articleBuilder.setPublishedTime(getPublishedTime(documentNode));

    // Title.
    String title = getTitle(documentNode);
    if (title != null) {
      articleBuilder.setTitle(title);
    }

    // Type.
    String type = getType(documentNode);
    if (type != null) {
      articleBuilder.setType(type);
    }

    // Since many sites double-escape their HTML entities (why anyone would
    // do this is beyond me), do another escape pass on everything before we
    // start telling people about this article.
    if (articleBuilder.hasDescription()) {
      articleBuilder.setDescription(unescape(articleBuilder.getDescription()).trim());
    }
    if (articleBuilder.hasAuthor()) {
      articleBuilder.setAuthor(unescape(articleBuilder.getAuthor()));
    }
    if (articleBuilder.hasCopyright()) {
      articleBuilder.setCopyright(unescape(articleBuilder.getCopyright()));
    }
    if (articleBuilder.hasTitle()) {
      articleBuilder.setTitle(unescape(articleBuilder.getTitle()));
    }
    if (articleBuilder.hasImageUrl()) {
      articleBuilder.setImageUrl(unescape(articleBuilder.getImageUrl()));
    }

    // Done!
    return articleBuilder.build();
  }

  public static String getAuthor(DocumentNode documentNode) {
    Node metaNode = documentNode.findFirst("html > head meta[name=\"author\"]");
    return (metaNode != null) ? metaNode.getAttributeValue("content") : null;
  }

  public static String getCopyright(DocumentNode documentNode) {
    Node metaNode = documentNode.findFirst("html > head meta[name=\"copyright\"]");
    return (metaNode != null) ? metaNode.getAttributeValue("content") : null;
  }

  public static String getDescription(DocumentNode documentNode) throws RequiredFieldException {
    Node metaNode = documentNode.findFirst(ImmutableList.of(
        "html > head meta[name=\"sailthru.description\"]", // Best, at least on techcrunch.com.
        "html > head meta[name=\"description\"]",
        "html > head meta[name=\"lp\"]",
        "html > head meta[property=\"rnews:description\"]",
        "html > head meta[property=\"og:description\"]",
        "html > head meta[itemprop=\"description\"]"));
    String description;
    if (metaNode != null) {
      description = metaNode.getAttributeValue("content");
    } else {
      // Fall back to the first significant paragraph.
      Iterable<String> paragraphs = getParagraphs(documentNode);
      description = Iterables.getFirst(paragraphs, null);
      for (String paragraph : paragraphs) {
        if (paragraph.length() >= 50) {
          description = paragraph;
          break;
        }
      }
    }

    if (description.length() > Articles.MAX_DESCRIPTION_LENGTH) {
      System.out.println("Warning: Trimming description for url " + documentNode.getUrl());
      description = description.substring(0, Articles.MAX_DESCRIPTION_LENGTH);
    }

    return description;
  }

  private static String resolveImageUrl(DocumentNode documentNode, Node metaNode) {
    try {
      return new URL(new URL(documentNode.getUrl()), metaNode.getAttributeValue("content")).toString();
    } catch (MalformedURLException e) {
      return "";
    }
  }

  /**
   * Returns a ranking score for how good we think a meta node's thumbnail URL
   * is.  Higher scores are better.  General guideline:
   * - 1000+: Extremely good, probably the best.
   * - 100 - 1000: Pretty good, we'll think about it.
   * - 1 - 100: Probably crap but worth considering.
   * - Below 0: Not a valid URL.
   */
  private static int getImageUrlRank(DocumentNode documentNode, Node metaNode) {
    String imageUrl = resolveImageUrl(documentNode, metaNode);

    // Disallow empty and blacklisted URLs.
    if (imageUrl.isEmpty() ||
        metaNode.getAttributeValue("content").trim().isEmpty() ||
        IMAGE_URL_BLACKLIST.contains(imageUrl)) {
      return -1;
    }

    String documentUrl = documentNode.getUrl();
    if (documentUrl.contains(".nytimes.com/")) {
      if (imageUrl.contains("-facebookJumbo-")) {
        return 1000;
      }
      if (imageUrl.contains("-thumbLarge-")) {
        return 200;
      }
    }

    // Once in a while we're graced with this information - super valuable!
    // TODO(jonemerson): Sometimes the width is specified in the thumbnail URL.
    // E.g. on techcrunch.com, it's done with the "w" parameter.
    int width = NumberUtils.toInt(metaNode.getAttributeValue("width"), 0);
    if (width > 100) {
      return Math.min(1000, width);
    }

    // Sailthru metadata tends to be very high quality.
    if ("sailthru.image.full".equals(metaNode.getAttributeValue("name"))) {
      return 500;
    }

    // Longer URLs are more likely to be specific to the current article
    // (actually I'm totally guessing).
    return Math.min(120, imageUrl.length()) - 20;
  }

  public static String getImageUrl(DocumentNode documentNode) {
    String bestUrl = "";
    int bestUrlRank = 0;
    for (Node metaNode : documentNode.findAll(ImmutableList.of(
        "html > head meta[name=\"thumbnail\"]",
        "html > head meta[name=\"THUMBNAIL_URL\"]",
        "html > head meta[name=\"sailthru.image.full\"]",
        "html > head meta[property=\"og:image\"]",
        "html > head meta[property=\"rnews:thumbnailUrl\"]",
        "html > head meta[itemprop=\"thumbnailUrl\"]"))) {
      int rank = getImageUrlRank(documentNode, metaNode);
      if (rank > bestUrlRank) {
        bestUrl = resolveImageUrl(documentNode, metaNode);
        bestUrlRank = rank;
      }
    }
    return bestUrl.isEmpty() ? null : bestUrl;
  }

  public static Long getModifiedTime(DocumentNode documentNode)
      throws RequiredFieldException {
    Node metaNode = documentNode.findFirst(ImmutableList.of(
        "html > head meta[name=\"utime\"]",
        "html > head meta[itemprop=\"modificationDate\"]",
        "html > head meta[property=\"article:modified_time\"]",
        "html > head meta[itemprop=\"dateModified\"]"));
    return (metaNode != null)
        ? DateParser.parseDateTime(metaNode.getAttributeValue("content")) : null;
  }

  public static Iterable<String> getParagraphs(final DocumentNode documentNode)
      throws RequiredFieldException {
    Iterable<String> paragraphs = Iterables.transform(
        SiteParser.getParagraphNodes(documentNode),
        new Function<Node, String>() {
          @Override
          public String apply(Node paragraphNode) {
            String text = paragraphNode.getFlattenedText();
            if (text.length() > Articles.MAX_PARAGRAPH_LENGTH) {
              System.out.println("Warning: Trimming paragraph text on " +
                  documentNode.getUrl());
              text = text.substring(0, Articles.MAX_PARAGRAPH_LENGTH);
            }
            return text;
          }
    });
    if (Iterables.isEmpty(paragraphs)) {
      throw new RequiredFieldException("No paragraphs found");
    }
    return paragraphs;
  }

  public static long getPublishedTime(DocumentNode documentNode) throws RequiredFieldException {
    Node metaNode = documentNode.findFirst(ImmutableList.of(
        "html > head meta[name=\"ptime\"]", // Usually very precise.
        "html > head meta[name=\"date\"]",
        "html > head meta[name=\"Date\"]", // Abcnews.go.com.
        "html > head meta[name=\"date.release\"]",
        "html > head meta[name=\"pub_date\"]",
        "html > head meta[name=\"publish-date\"]",
        "html > head meta[name=\"OriginalPublicationDate\"]",
        "html > head meta[name=\"sailthru.date\"]",
        "html > head meta[name=\"cXenseParse:recs:publishtime\"]",
        "html > head meta[itemprop=\"datePublished\"]",
        "html > head meta[property=\"pubDate\"]",
        "html > head meta[property=\"article:published_time\"]",
        "html > head meta[property=\"rnews:datePublished\"]",
        "html > head meta[name=\"pdate\"]", // Usually day only.
        "html > head meta[name=\"DC.date.issued\"]", // Abcnews.go.com.
        "html meta[itemprop=\"datePublished\"]")); // Cbsnews.com.
    if (metaNode != null) {
      return DateParser.parseDateTime(metaNode.getAttributeValue("content"));
    }

    // See if we can parse a date out of the URL.
    Long date = DateParser.parseDateFromUrl(documentNode.getUrl(), true /* allowMonth */);
    if (date == null) {
      throw new RequiredFieldException("Could not find published_time");
    }
    return date;
  }

  public static String getTitle(DocumentNode documentNode) throws RequiredFieldException {
    Node metaNode = documentNode.findFirst(ImmutableList.of(
        "html > head meta[name=\"fb_title\"]",
        "html > head meta[name=\"hdl\"]",
        "html > head meta[name=\"Headline\"]",
        "html > head meta[name=\"sailthru.title\"]",
        "html > head meta[property=\"og:title\"]",
        "html > head meta[property=\"rnews:headline\"]",
        "html > head meta[itemprop=\"alternativeHeadline\"]"));
    if (metaNode != null) {
      return metaNode.getAttributeValue("content");
    }
    Node titleNode = documentNode.findFirst("title");
    if (titleNode != null) {
      return titleNode.getFlattenedText();
    }
    throw new RequiredFieldException("Could not find required field: title");
  }

  public static String getType(DocumentNode documentNode) {
    Node metaNode = documentNode.findFirst("html > head meta[property=\"og:type\"]");
    return (metaNode != null) ? metaNode.getAttributeValue("content") : null;
  }

  /**
   * Unescapes the passed string, including &apos;, which was added in XHTML 1.0
   * but for some reason isn't handled by Apache's StringEscapeUtils.  Also,
   * &nbsp; is converted to a standard space, since we don't want to worry about
   * that.
   */
  static String unescape(String escaped) {
    return StringEscapeUtils
        .unescapeHtml4(escaped
            .replaceAll("&nbsp;", " "))
        .replaceAll("&apos;", "'")
        .replaceAll("\u00A0", " ");
  }
}
