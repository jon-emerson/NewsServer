package com.janknspank;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.janknspank.data.Articles;
import com.janknspank.data.ValidationException;
import com.janknspank.dom.InterpretedData;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.Url;
import com.janknspank.proto.Validator;

public class ArticleHandler extends DefaultHandler {
  private static final Pattern[] DATE_IN_URL_PATTERNS = {
      Pattern.compile("\\/[0-9]{4}\\/[0-9]{2}\\/[0-9]{2}\\/"),
      Pattern.compile("\\/[0-9]{4}\\-[0-9]{2}\\-[0-9]{2}\\/"),
      Pattern.compile("\\/20[0-9]{6}\\/")
  };
  private static final DateFormat[] KNOWN_DATE_FORMATS = {
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX"), // ISO 8601, BusinessWeek, CNBC.
      new SimpleDateFormat("MMMM dd, yyyy, hh:mm a"), // CBS News.
      new SimpleDateFormat("MMMM dd, yyyy"), // Chicago Tribune.
      new SimpleDateFormat("yyyy-MM-dd"), // New York Times and LA Times.
      new SimpleDateFormat("yyyyMMddHHmmss"), // New York Times 'pdate'.
      new SimpleDateFormat("yyyyMMdd"), // Washington Post.
      new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"), // BBC.
      new SimpleDateFormat("EEE, dd MMM yyyy HH:mm z"), // Boston.com.
      new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z"), // Boston.com.
      new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss"), // Boston.com
      new SimpleDateFormat("/yyyy/MM/dd/"), // In URL.
      new SimpleDateFormat("/yyyy-MM-dd/"), // In URL.
      new SimpleDateFormat("/yyyyMMdd/") // In URL.
  };

  private String lastCharacters;
  private InterpretedData lastInterpretedData;
  private final Set<String> lastKeywords = Sets.newHashSet();
  private final ArticleCallback callback;

  @VisibleForTesting
  final Article.Builder articleBuilder;

  public interface ArticleCallback {
    public void foundUrl(String url);
    public void foundArticle(Article article, InterpretedData interpretedData, Set<String> keywords);
  }

  public ArticleHandler(ArticleCallback callback, Url startUrl) {
    this.callback = callback;

    this.articleBuilder = Article.newBuilder();
    articleBuilder.setId(startUrl.getId());
    articleBuilder.setUrl(startUrl.getUrl());

    // See if we can parse a date out of the URL.
    for (Pattern dateInUrlPattern : DATE_IN_URL_PATTERNS) {
      Matcher dateInUrlMatcher = dateInUrlPattern.matcher(startUrl.getUrl());
      if (dateInUrlMatcher.find()) {
        System.out.println("Found date in URL: " + parseDateTime(dateInUrlMatcher.group()));
        articleBuilder.setPublishedTime(parseDateTime(dateInUrlMatcher.group()));
        break;
      }
    }
  }

  /**
   * Resolves a relative URL to its base based on the URL of this article.
   */
  private String resolveUrl(String relativeUrl) throws MalformedURLException {
    return new URL(new URL(articleBuilder.getUrl()), relativeUrl).toString();
  }

  public void setInterpretedData(InterpretedData interpretedData) {
    this.lastInterpretedData = interpretedData;

    // Save the article body.
    String articleBody = interpretedData.getArticleBody();
    if (articleBody.length() > Articles.MAX_ARTICLE_LENGTH) {
      articleBuilder.setArticleBody(articleBody.substring(0, Articles.MAX_ARTICLE_LENGTH));
    } else {
      articleBuilder.setArticleBody(articleBody);
    }
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    // Simple sanity check before we allocate a huge string.
    if (length < 1000) {
      lastCharacters = String.copyValueOf(ch, start, length);
    } else {
      lastCharacters = null;
    }
  }

  @Override
  public void startDocument() {
    lastKeywords.clear();
  }

  @Override
  public void endDocument() {
    try {
      callback.foundArticle(
          (Article) Validator.assertValid(articleBuilder.build()),
          lastInterpretedData,
          lastKeywords);
    } catch (ValidationException e) {
      // This is OK - Some documents just don't have enough data.
      System.err.println("Bad crawl data for URL: " + articleBuilder.getUrl());
      e.printStackTrace();
    }
  }

  @Override
  public void endElement(String namespaceURI,
      String localName,
      String qName) throws SAXException {
    if (!articleBuilder.hasTitle() && "title".equalsIgnoreCase(qName)) {
      articleBuilder.setTitle(lastCharacters);
    }
  }

  @Override
  public void startElement(String namespaceURI,
      String localName,
      String qName, 
      Attributes attrs)
      throws SAXException {
    if ("a".equalsIgnoreCase(qName)) {
      String href = attrs.getValue("href");
      if (href != null &&
          !"nofollow".equalsIgnoreCase(attrs.getValue("rel")) &&
          (href.startsWith("http://") || href.startsWith("https://"))) {
        try {
          callback.foundUrl(resolveUrl(href));
        } catch (MalformedURLException e) {
          System.out.println("Ignoring malformed URL: " + href + " on page " +
              articleBuilder.getUrl());
        }
      }
    }
    if ("meta".equalsIgnoreCase(qName)) {
      String name = attrs.getValue("name");
      if ("author".equalsIgnoreCase(name)) {
        articleBuilder.setAuthor(attrs.getValue("content"));
      }
      if ("copyright".equalsIgnoreCase(name)) {
        articleBuilder.setCopyright(attrs.getValue("content"));
      }
      if ("description".equalsIgnoreCase(name)) {
        articleBuilder.setDescription(
            StringUtils.substring(attrs.getValue("content"), 0, Articles.MAX_DESCRIPTION_LENGTH));
      }
      if ("fb_title".equalsIgnoreCase(name) ||
          "hdl".equalsIgnoreCase(name) ||
          "Headline".equalsIgnoreCase(name) ||
          "sailthru.title".equalsIgnoreCase(name)) {
        articleBuilder.setTitle(attrs.getValue("content"));
      }
      if ("thumbnail".equalsIgnoreCase(name) ||
          "THUMBNAIL_URL".equalsIgnoreCase(name) ||
          "sailthru.image.full".equalsIgnoreCase(name)) {
        articleBuilder.setImageUrl(attrs.getValue("content"));
      }
      if ("date".equalsIgnoreCase(name) ||
          "OriginalPublicationDate".equalsIgnoreCase(name) ||
          "ptime".equalsIgnoreCase(name) ||
          "publish-date".equalsIgnoreCase(name) ||
          "pub_date".equalsIgnoreCase(name) ||
          "sailthru.date".equalsIgnoreCase(name)) {
        articleBuilder.setPublishedTime(parseDateTime(attrs.getValue("content")));
      }
      if ("utime".equalsIgnoreCase(name)) {
        articleBuilder.setModifiedTime(parseDateTime(attrs.getValue("content")));
      }
      if ("keywords".equalsIgnoreCase(name) ||
          "news_keywords".equalsIgnoreCase(name) ||
          "sailthru.tags".equalsIgnoreCase(name)) {
        handleKeywords(attrs.getValue("content"));
      }

      String property = attrs.getValue("property");
      if ("og:title".equalsIgnoreCase(property) ||
          "rnews:headline".equalsIgnoreCase(property)) {
        articleBuilder.setTitle(attrs.getValue("content"));
      }
      if ("rnews:description".equalsIgnoreCase(property)) {
        articleBuilder.setDescription(
            StringUtils.substring(attrs.getValue("content"), 0, Articles.MAX_DESCRIPTION_LENGTH));
      }
      if ("og:type".equalsIgnoreCase(property)) {
        articleBuilder.setType(attrs.getValue("content"));
      }
      if ("og:image".equalsIgnoreCase(property) ||
          "rnews:thumbnailUrl".equalsIgnoreCase(property)) {
        articleBuilder.setImageUrl(attrs.getValue("content"));
      }
      if ("og:description".equalsIgnoreCase(property) ) {
        articleBuilder.setDescription(
            StringUtils.substring(attrs.getValue("content"), 0, Articles.MAX_DESCRIPTION_LENGTH));
      }
      if ("article:published_time".equalsIgnoreCase(property) ||
          "rnews:datePublished".equalsIgnoreCase(property)) {
        articleBuilder.setPublishedTime(parseDateTime(attrs.getValue("content")));
      }
      if ("article:modified_time".equalsIgnoreCase(property)) {
        articleBuilder.setModifiedTime(parseDateTime(attrs.getValue("content")));
      }
      if ("article:tag".equalsIgnoreCase(property)) {
        lastKeywords.add(attrs.getValue("content"));
      }

      String itemprop = attrs.getValue("itemprop");
      if ("dateCreated".equalsIgnoreCase(itemprop) ||
          "datePublished".equalsIgnoreCase(itemprop)) {
        articleBuilder.setPublishedTime(parseDateTime(attrs.getValue("content")));
      }
      if ("dateModified".equalsIgnoreCase(itemprop)) {
        articleBuilder.setModifiedTime(parseDateTime(attrs.getValue("content")));
      }
      if ("alternativeHeadline".equalsIgnoreCase(itemprop)) {
        articleBuilder.setTitle(attrs.getValue("content"));
      }
      if ("thumbnailUrl".equalsIgnoreCase(itemprop)) {
        articleBuilder.setImageUrl(attrs.getValue("content"));
      }
    }
  }

  // TODO(jonemerson): It seems like this is returning long's that have been
  // adjusted for PDT.  E.g. they're bigger than they should be by 7-8 hours.
  private long parseDateTime(String dateStr) {
    if (Strings.isNullOrEmpty(dateStr)) {
      return 0;
    }
    for (DateFormat format : KNOWN_DATE_FORMATS) {
      try {
        return format.parse(dateStr).getTime();
      } catch (ParseException e2) {
        // This is OK - we just don't match.  Try the next one.
      }
    }
    System.err.println("COULD NOT PARSE DATE: " + dateStr);
    return 0;
  }
  
  private void handleKeywords(String rawKeywords) {
    String[] keywords;
    // Some sites use semicolons, others use commas.  Split based on whichever is more prevalent.
    if (StringUtils.countMatches(rawKeywords, ",") > StringUtils.countMatches(rawKeywords, ";")) {
      keywords = rawKeywords.split(",");
    } else {
      keywords = rawKeywords.split(";");
    }
    for (String keyword : keywords) {
      lastKeywords.add(keyword.trim());
    }
  }
}