package com.janknspank;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.protobuf.Message;
import com.janknspank.data.Articles;
import com.janknspank.data.Database;
import com.janknspank.data.ValidationException;
import com.janknspank.dom.InterpretedData;
import com.janknspank.dom.parser.Node;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.Url;
import com.janknspank.proto.Validator;

public class ArticleHandler extends DefaultHandler {
  private static final Set<String> IMAGE_URL_BLACKLIST = ImmutableSet.of(
      "http://www.chron.com/img/pages/article/opengraph_default.jpg",
      "http://media.cleveland.com/design/alpha/img/logo_cleve.gif");
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
    articleBuilder.setUrlId(startUrl.getId());
    articleBuilder.setUrl(startUrl.getUrl());

    // See if we can parse a date out of the URL.
    Long dateFromUrl = DateHelper.getDateFromUrl(startUrl.getUrl(), true /* allowMonth */);
    if (dateFromUrl != null) {
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
    if (interpretedData.getArticleNodes().size() == 0) {
      Preconditions.checkArgument(interpretedData.getArticleNodes().size() > 0,
          "Article nodes must be present");
    }
    this.lastInterpretedData = interpretedData;

    // Save the article body.
    String articleBody = Joiner.on("\n").join(Iterables.transform(
        interpretedData.getArticleNodes(),
        new Function<Node, String>() {
          @Override
          public String apply(Node paragraphNode) {
            return paragraphNode.getFlattenedText();
          }
        }));
    articleBuilder.setDescription(
        StringUtils.substring(interpretedData.getArticleNodes().get(0).getFlattenedText(), 0,
            Articles.MAX_DESCRIPTION_LENGTH));
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

  /**
   * Unescapes the passed string, including &apos;, which was added in XHTML 1.0
   * but for some reason isn't handled by Apache's StringEscapeUtils.  Also,
   * &nbsp; is converted to a standard space, since we don't want to worry about
   * that.
   */
  private static String unescape(String escaped) {
    return StringEscapeUtils
        .unescapeHtml4(escaped
            .replaceAll("&nbsp;", " "))
        .replaceAll("&apos;", "'")
        .replaceAll("\u00A0", " ");
  }

  @Override
  public void endDocument() {
    try {
      // Since many sites double-escape their HTML entities (why anyone would
      // do this is beyond me), do another escape pass on everything before we
      // start telling people about this article.
      if (articleBuilder.hasDescription()) {
        articleBuilder.setDescription(unescape(articleBuilder.getDescription()));
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
      if ("description".equalsIgnoreCase(name) ||
          "lp".equalsIgnoreCase(name)) {
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
        handleImageUrl(attrs.getValue("content"));
      }
      if ("cXenseParse:recs:publishtime".equalsIgnoreCase(name) ||
          "date".equalsIgnoreCase(name) ||
          "date.release".equalsIgnoreCase(name) || // chron.com
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
        handleImageUrl(attrs.getValue("content"));
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
        handleImageUrl(attrs.getValue("content"));
      }
    }
  }

  private void handleImageUrl(String imageUrl) {
    if (!IMAGE_URL_BLACKLIST.contains(imageUrl)) {
      // Collect the longest URL we find, since longer URLs are more likely to
      // be specific to the current article (actually I'm totally guessing).
      if (!articleBuilder.hasImageUrl() ||
          (imageUrl.length() > articleBuilder.getImageUrl().length())) {
        articleBuilder.setImageUrl(imageUrl);
      }
    }
  }

  private void handleKeywords(String rawKeywords) {
    rawKeywords = unescape(rawKeywords);
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

  /**
   * Cleans any HTML entities that were stored in article metadata before we
   * started cleaning up at parse time.
   */
  public static void main(String args[]) throws Exception {
    // Figure out what articles we've crawled already.
    List<Message> articlesToUpdate = Lists.newArrayList();
    PreparedStatement stmt = Database.getConnection().prepareStatement(
        "SELECT * FROM " + Database.getTableName(Article.class));
    ResultSet result = stmt.executeQuery();
    while (!result.isAfterLast()) {
      Article article = Database.createFromResultSet(result, Article.class);
      if (article != null) {
        Article.Builder articleBuilder = article.toBuilder();
        if (articleBuilder.hasDescription()) {
          articleBuilder.setDescription(unescape(articleBuilder.getDescription()));
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
        if (!StringUtils.equals(articleBuilder.getDescription(), article.getDescription()) ||
            !StringUtils.equals(articleBuilder.getAuthor(), article.getAuthor()) ||
            !StringUtils.equals(articleBuilder.getCopyright(), article.getCopyright()) ||
            !StringUtils.equals(articleBuilder.getTitle(), article.getTitle()) ||
            !StringUtils.equals(articleBuilder.getImageUrl(), article.getImageUrl())) {
          System.out.println("Updating " + article.getUrl() + ". Before=" +
              article.toString() + " After=" + articleBuilder.build().toString());
          articlesToUpdate.add(articleBuilder.build());
        }
      }
      if (articlesToUpdate.size() == 100 || article == null) {
        System.out.println(Database.update(articlesToUpdate) + " rows updated");
        articlesToUpdate.clear();
      }
    }
  }
}
