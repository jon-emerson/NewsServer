package com.janknspank;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.janknspank.data.Articles;
import com.janknspank.data.ValidationException;
import com.janknspank.dom.InterpretedData;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.Url;
import com.janknspank.proto.Validator;

public class ArticleHandler extends DefaultHandler {

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
    Long dateFromUrl = DateHelper.getDateFromUrl(startUrl.getUrl(), true /* allowMonth */);
    if (dateFromUrl != null) {
      System.out.println("Found date in URL: " + dateFromUrl);
      articleBuilder.setPublishedTime(dateFromUrl);
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
        articleBuilder.setPublishedTime(DateHelper.parseDateTime(attrs.getValue("content")));
      }
      if ("utime".equalsIgnoreCase(name)) {
        articleBuilder.setModifiedTime(DateHelper.parseDateTime(attrs.getValue("content")));
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
        articleBuilder.setPublishedTime(DateHelper.parseDateTime(attrs.getValue("content")));
      }
      if ("article:modified_time".equalsIgnoreCase(property)) {
        articleBuilder.setModifiedTime(DateHelper.parseDateTime(attrs.getValue("content")));
      }
      if ("article:tag".equalsIgnoreCase(property)) {
        lastKeywords.add(attrs.getValue("content"));
      }

      String itemprop = attrs.getValue("itemprop");
      if ("dateCreated".equalsIgnoreCase(itemprop) ||
          "datePublished".equalsIgnoreCase(itemprop)) {
        articleBuilder.setPublishedTime(DateHelper.parseDateTime(attrs.getValue("content")));
      }
      if ("dateModified".equalsIgnoreCase(itemprop)) {
        articleBuilder.setModifiedTime(DateHelper.parseDateTime(attrs.getValue("content")));
      }
      if ("alternativeHeadline".equalsIgnoreCase(itemprop)) {
        articleBuilder.setTitle(attrs.getValue("content"));
      }
      if ("thumbnailUrl".equalsIgnoreCase(itemprop)) {
        articleBuilder.setImageUrl(attrs.getValue("content"));
      }
    }
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