package com.janknspank.crawler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.math.NumberUtils;

import com.google.api.client.util.Lists;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.janknspank.classifier.ClassifierException;
import com.janknspank.classifier.FeatureClassifier;
import com.janknspank.common.DateParser;
import com.janknspank.common.Logger;
import com.janknspank.common.StringHelper;
import com.janknspank.crawler.facebook.FacebookData;
import com.janknspank.crawler.facebook.FacebookException;
import com.janknspank.database.Database;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.dom.parser.Node;
import com.janknspank.nlp.KeywordFinder;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.SocialEngagement;

/**
 * ArticleCreator is a very high-level object in the crawling system: It's
 * responsible for creating Article objects from yet-uninterpreted
 * DocumentNodes.  ArticleCreator digs through the DocumentNode to find all
 * the Article's attributes, such as entities, industry and other
 * classification features, plus Facebook / other social engagement scores.
 */
class ArticleCreator extends CacheLoader<DocumentNode, Iterable<String>> {
  private static final Logger LOG = new Logger(ArticleCreator.class);
  private static LoadingCache<DocumentNode, Iterable<String>> PARAGRAPH_CACHE =
      CacheBuilder.newBuilder()
          .maximumSize(10)
          .expireAfterWrite(10, TimeUnit.MINUTES)
          .build(new ArticleCreator());
  private static final int MAX_TITLE_LENGTH =
      Database.getStringLength(Article.class, "title");
  private static final int MAX_PARAGRAPH_LENGTH =
      Database.getStringLength(Article.class, "paragraph");
  private static final int MAX_DESCRIPTION_LENGTH =
      Database.getStringLength(Article.class, "description");
  private static final Set<String> IMAGE_URL_BLACKLIST = ImmutableSet.of(
      "http://media.cleveland.com/design/alpha/img/logo_cleve.gif",
      "http://www.chron.com/img/pages/article/opengraph_default.jpg",
      "http://www.sfgate.com/img/pages/article/opengraph_default.png");
  private static final Pattern TEXT_TO_REMOVE_FROM_TITLE_ENDS[] = new Pattern[] {
      Pattern.compile("\\s\\([A-Za-z]{2,15}(\\s[A-Za-z]{2,15})?\\)$"),
      Pattern.compile("\\s*(\\||\\-\\-|\\-|—)\\s+([A-Z][A-Za-z]+\\.com)$"),
      Pattern.compile("\\s*(\\||\\-\\-|\\-|—)\\s+[A-Z][A-Za-z\\s'']{2,25}$")};

  public static Article create(String urlId, DocumentNode documentNode)
      throws RequiredFieldException {
    Article.Builder articleBuilder = Article.newBuilder();
    articleBuilder.setUrlId(urlId);
    articleBuilder.setUrl(documentNode.getUrl());

    // Paragraphs (required).
    articleBuilder.addAllParagraph(getParagraphs(documentNode));

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

    // Word count (required).
    articleBuilder.setWordCount(getWordCount(documentNode));

    // Keywords.
    articleBuilder.addAllKeyword(KeywordFinder.getInstance().findKeywords(urlId, documentNode));

    // Since many sites double-escape their HTML entities (why anyone would
    // do this is beyond me), do another escape pass on everything before we
    // start telling people about this article.
    if (articleBuilder.hasDescription()) {
      articleBuilder.setDescription(StringHelper.unescape(articleBuilder.getDescription()).trim());
    }
    if (articleBuilder.hasAuthor()) {
      articleBuilder.setAuthor(StringHelper.unescape(articleBuilder.getAuthor()));
    }
    if (articleBuilder.hasCopyright()) {
      articleBuilder.setCopyright(StringHelper.unescape(articleBuilder.getCopyright()));
    }
    if (articleBuilder.hasImageUrl()) {
      articleBuilder.setImageUrl(StringHelper.unescape(articleBuilder.getImageUrl()));
    }
    try {
      articleBuilder.addAllFeature(FeatureClassifier.classify(articleBuilder));
    } catch (ClassifierException e) {
      e.printStackTrace();
    }
    try {
      SocialEngagement engagement = FacebookData.getEngagementForURL(articleBuilder);
      if (engagement != null) {
        articleBuilder.addSocialEngagement(engagement);
      }
    } catch (FacebookException e) {
      e.printStackTrace();
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

    if (description.length() > MAX_DESCRIPTION_LENGTH) {
      System.out.println("Warning: Trimming description for url " + documentNode.getUrl());
      description = description.substring(0, MAX_DESCRIPTION_LENGTH - 1) + "\u2026";
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

    // Disallow these URLs, e.g.
    // http://cdn.gotraffic.net/politics/20150107201907/public/images/logos/
    //     FB-Sharing.73b07052.png
    // Which was found on http://www.bloomberg.com/politics/articles/2014-12-30/
    //     the-new-york-times-joins-the-nypd-funeral-protest-backlash
    // And is a text image.
    if (imageUrl.contains("//cdn.gotraffic.net/") && imageUrl.contains("FB-Sharing")) {
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
    try {
      return PARAGRAPH_CACHE.get(documentNode);
    } catch (ExecutionException e) {
      if (e.getCause() instanceof RequiredFieldException) {
        throw (RequiredFieldException) e.getCause();
      }
      throw new RequiredFieldException("Could not get paragraphs: " + e.getMessage(), e);
    }
  }

  public static int getWordCount(final DocumentNode documentNode) throws RequiredFieldException {
    Pattern pattern = Pattern.compile("[\\s]+");
    int words = 0;
    for (String paragraph : getParagraphs(documentNode)) {
      Matcher matcher = pattern.matcher(paragraph);
      while (matcher.find()) {
        words++;
      }
      words++;
    }
    return words;
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
        "html meta[itemprop=\"datePublished\"]", // Cbsnews.com.
        "html > head meta[name=\"live_date\"]")); // Pcmag.com.
    if (metaNode != null) {
      return DateParser.parseDateTime(metaNode.getAttributeValue("content"));
    }

    // See if we can parse a date out of the URL.
    Long date = DateParser.parseDateFromUrl(documentNode.getUrl(), true /* allowMonth */);
    if (date != null) {
      return date;
    }

    // See if we can derive the date from the article contents.
    // NOTE(jonemerson): Maybe we can do this more heuristically.
    for (Node copyrightNode : documentNode.findAll("body .copyright")) {
      Matcher matcher = Pattern.compile("(0?[1-9]|1[012])\\.[0123]?[0-9]\\.20[0-9]{2}")
          .matcher(copyrightNode.getFlattenedText());
      if (matcher.find()) {
        date = DateParser.parseDateTime(matcher.group());
        if (date != null) {
          return date;
        }
      }
    }

    // If no published time was found in the article body,
    // default to the time that the article was created in the system
    return System.currentTimeMillis();
  }

  /**
   * Removes cruft from the ends of article titles.
   */
  @VisibleForTesting
  static String cleanTitle(String title) {
    for (Pattern pattern : TEXT_TO_REMOVE_FROM_TITLE_ENDS) {
      Matcher matcher = pattern.matcher(title);
      if (matcher.find()) {
        title = title.substring(0, title.length() - matcher.group().length());
      }
    }
    if (title.length() > MAX_TITLE_LENGTH) {
      title = title.substring(0, MAX_TITLE_LENGTH - 1) + "\u2026";
    }
    return title;
  }

  public static String getTitle(DocumentNode documentNode) throws RequiredFieldException {
    // For most sites, we can get it from the meta keywords.
    // For others, the meta keywords are crap, so we skip this step.
    String title = null;
    if (!documentNode.getUrl().contains("//advice.careerbuilder.com/")) {
      Node metaNode = documentNode.findFirst(ImmutableList.of(
          "html > head meta[name=\"fb_title\"]",
          "html > head meta[name=\"hdl\"]",
          "html > head meta[name=\"Headline\"]",
          "html > head meta[name=\"sailthru.title\"]",
          "html > head meta[property=\"og:title\"]",
          "html > head meta[property=\"rnews:headline\"]",
          "html > head meta[itemprop=\"alternativeHeadline\"]"));
      if (metaNode != null) {
        title = metaNode.getAttributeValue("content");
      } else {
        Node titleNode = documentNode.findFirst("title");
        if (titleNode != null) {
          title = StringHelper.unescape(titleNode.getFlattenedText());
        } else {
          throw new RequiredFieldException("Could not find required field: title");
        }
      }
    }

    // Clean and truncate the title, if necessary.
    return cleanTitle(title);
  }

  public static String getType(DocumentNode documentNode) {
    Node metaNode = documentNode.findFirst("html > head meta[property=\"og:type\"]");
    return (metaNode != null) ? metaNode.getAttributeValue("content") : null;
  }

  /**
   * DO NOT CALL THIS DIRECTLY.
   * @see #getParagraphs(DocumentNode)
   */
  @Override
  public Iterable<String> load(final DocumentNode documentNode) throws Exception {
    List<String> paragraphs = Lists.newArrayList();
    for (Node paragraphNode : SiteParser.getParagraphNodes(documentNode)) {
      String text = StringHelper.unescape(paragraphNode.getFlattenedText()).trim();
      if (text.length() > MAX_PARAGRAPH_LENGTH) {
        LOG.warning("Trimming paragraph text on " + documentNode.getUrl());
        text = text.substring(0, MAX_PARAGRAPH_LENGTH - 1) + "\u2026";
      }
      if (text.length() > 0) {
        paragraphs.add(text);
      }
    }
    if (Iterables.isEmpty(paragraphs)) {
      throw new RequiredFieldException("No paragraphs found in " + documentNode.getUrl());
    }
    return paragraphs;
  }
}
