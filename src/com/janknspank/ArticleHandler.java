package com.janknspank;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.janknspank.data.Article;
import com.janknspank.data.DiscoveredUrl;
import com.janknspank.data.ValidationException;

public class ArticleHandler extends DefaultHandler {
  public static final String ISO_8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZZ";
  private static final DateTimeFormatter ISO_DATE_TIME_FORMAT =
      DateTimeFormat.forPattern(ISO_8601_DATE_FORMAT).withOffsetParsed();
  private static final DateFormat[] KNOWN_DATE_FORMATS = {
      new SimpleDateFormat("MMMM dd, yyyy, hh:mm a"), // CBS News.
      new SimpleDateFormat("MMMM dd, yyyy"), // Chicago Tribune.
      new SimpleDateFormat("yyyy-MM-dd"), // New York Times.
      new SimpleDateFormat("yyyyMMdd") // Washington Post.
  };

  private final URL baseUrl;
  private final Article.Builder articleBuilder = new Article.Builder();
  private String lastCharacters;
  private final ArticleCallback callback;

  public interface ArticleCallback {
    public void foundUrl(String url);
    public void foundArticle(Article data);
  }

  public ArticleHandler(ArticleCallback callback, DiscoveredUrl startUrl) {
    this.callback = callback;

    try {
      this.baseUrl = new URL(startUrl.getUrl());
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
    articleBuilder.setId(startUrl.getId());
  }

  public void setArticleBody(String articleBody) {
    articleBuilder.setArticleBody(articleBody);
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
  public void endDocument() {
    try {
      callback.foundArticle(articleBuilder.build());
    } catch (ValidationException e) {
      // This is OK - Some documents just don't have enough data.
      System.err.println("Bad crawl data for URL: " + baseUrl.toString());
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
          callback.foundUrl(new URL(baseUrl, href).toString());
        } catch (MalformedURLException e) {
          e.printStackTrace();
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
        articleBuilder.setDescription(attrs.getValue("content"));
      }
      if ("fb_title".equalsIgnoreCase(name) ||
          "hdl".equalsIgnoreCase(name) ||
          "Headline".equalsIgnoreCase(name)) {
        articleBuilder.setTitle(attrs.getValue("content"));
      }
      if ("thumbnail".equalsIgnoreCase(name) ||
          "THUMBNAIL_URL".equalsIgnoreCase(name)) {
        articleBuilder.setImageUrl(attrs.getValue("content"));
      }

      String property = attrs.getValue("property");
      if ("og:title".equalsIgnoreCase(property)) {
        articleBuilder.setTitle(attrs.getValue("content"));
      }
      if ("og:type".equalsIgnoreCase(property)) {
        articleBuilder.setType(attrs.getValue("content"));
      }
      if ("og:image".equalsIgnoreCase(property)) {
        articleBuilder.setImageUrl(attrs.getValue("content"));
      }
      if ("og:description".equalsIgnoreCase(property) ) {
        articleBuilder.setDescription(attrs.getValue("content"));
      }

      String itemprop = attrs.getValue("itemprop");
      if ("datePublished".equalsIgnoreCase(itemprop)) {
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

  private Date parseDateTime(String dateStr) {
    if (dateStr == null) {
      return null;
    }
    try {
      // This is the most common date format.
      return ISO_DATE_TIME_FORMAT.parseDateTime(dateStr).toDate();
    } catch (IllegalArgumentException e) {
      for (DateFormat format : KNOWN_DATE_FORMATS) {
        try {
          return format.parse(dateStr);
        } catch (ParseException e2) {
          // This is OK - we just don't match.  Try the next one.
        }
      }
    }
    System.err.println("COULD NOT PARSE DATE: " + dateStr);
    return null;
  }
}