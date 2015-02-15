package com.janknspank.crawler;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.janknspank.bizness.GuidFactory;
import com.janknspank.bizness.Urls;
import com.janknspank.common.DateParser;
import com.janknspank.common.Logger;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.dom.parser.DocumentBuilder;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.dom.parser.Node;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.fetch.FetchException;
import com.janknspank.fetch.FetchResponse;
import com.janknspank.fetch.Fetcher;
import com.janknspank.proto.CoreProto.Url;
import com.janknspank.proto.CrawlProto.ContentSite;

/**
 * Iterates through the home pages of the various sites we support, and RSS
 * feeds of sites that offer those, to push as many new article URLs to the
 * database as possible.
 */
public class UrlCrawler {
  private static final Logger LOG = new Logger(UrlCrawler.class);
  private final Fetcher fetcher = new Fetcher();
  private static final Iterable<String> WEBSITES = Iterables.concat(
      Iterables.transform(UrlWhitelist.CRAWL_INSTRUCTIONS.getContentSiteList(),
          new Function<ContentSite, Iterable<String>>() {
            @Override
            public Iterable<String> apply(ContentSite contentSite) {
              return contentSite.getStartUrlList();
            }
          }));
  private static final Iterable<String> RSS_URLS = Iterables.concat(
      Iterables.transform(UrlWhitelist.CRAWL_INSTRUCTIONS.getContentSiteList(),
          new Function<ContentSite, Iterable<String>>() {
            @Override
            public Iterable<String> apply(ContentSite contentSite) {
              return contentSite.getRssUrlList();
            }
          }));

  private String getArticleUrl(Node itemNode) {
    List<Node> linkNodes = itemNode.findAll("atom:link");
    linkNodes.addAll(itemNode.findAll("link"));
    String articleUrl = null;
    if (linkNodes.size() > 0) {
      String href = linkNodes.get(0).getAttributeValue("href");
      if (href != null) {
        articleUrl = href;
      } else {
        // Some sites (e.g. mercurynews.com) put the URL as character data inside
        // the <link>...</link> element.
        String maybeUrl = linkNodes.get(0).getFlattenedText();
        if (ArticleUrlDetector.isArticle(maybeUrl)) {
          articleUrl = maybeUrl;
        } else {
          // Then, some sites put the URL in the GUID, while using 'link' for
          // a trackable click-through URL that doesn't tell us anything and
          // is not canonicalized.  See if we can use the GUID URL.
          List<Node> guidNodes = itemNode.findAll("guid");
          if (guidNodes.size() > 0) {
            maybeUrl = guidNodes.get(0).getFlattenedText();
            if (ArticleUrlDetector.isArticle(maybeUrl)) {
              articleUrl = maybeUrl;
            }
          }
        }
      }
    }
    if (UrlWhitelist.isOkay(articleUrl)) {
      return UrlCleaner.clean(articleUrl);
    }
    return null;
  }

  private Long getArticleDate(Node itemNode) {
    Long millis = DateParser.parseDateFromUrl(getArticleUrl(itemNode), true /* allowMonth */);
    List<Node> dateNodes = itemNode.findAll("pubDate");
    dateNodes.addAll(itemNode.findAll("published"));
    if (dateNodes.size() > 0) {
      Long rssDate = DateParser.parseDateTime(dateNodes.get(0).getFlattenedText());
      millis = (rssDate == null) ? millis : rssDate;
    }
    return millis;
  }

  private void saveArticle(String url, String originUrl, Long date) {
    Url existing;
    try {
      existing = Urls.getByUrl(url);
      if (existing == null) {
        Database.insert(Url.newBuilder()
            .setUrl(url)
            .setOriginUrl(originUrl)
            .setId(GuidFactory.generate())
            .setTweetCount(0)
            .setDiscoveryTime(System.currentTimeMillis())
            .setCrawlPriority(Urls.getCrawlPriority(url, date))
            .build());
      }
    } catch (DatabaseSchemaException | DatabaseRequestException e) {
      // Oh well, it's just RSS.  Print it out at least, so we can debug it.
      e.printStackTrace();
    }
  }

  public void crawl(String rssUrl) {
    FetchResponse fetchResponse;
    try {
      fetchResponse = fetcher.fetch(rssUrl);
      if (fetchResponse.getStatusCode() == HttpServletResponse.SC_OK) {
        DocumentNode documentNode = DocumentBuilder.build(rssUrl, fetchResponse.getReader());
        for (Node itemNode : Iterables.concat(
            documentNode.findAll("item"),
            documentNode.findAll("entry"))) {
          String articleUrl = getArticleUrl(itemNode);
          if (ArticleUrlDetector.isArticle(articleUrl)) {
            Long millis = getArticleDate(itemNode);
            saveArticle(articleUrl, rssUrl, millis);
          }
        }
      }
    } catch (FetchException | ParserException e) {
      e.printStackTrace();
    }
  }

  public static void main(String args[]) throws Exception {
    UrlCrawler crawler = new UrlCrawler();
    for (String website : WEBSITES) {
      LOG.info("WEBSITE: " + website);
      Urls.put(
          Iterables.transform(
              Iterables.filter(
                  Iterables.filter(UrlFinder.findUrls(website), ArticleUrlDetector.PREDICATE),
                  UrlWhitelist.PREDICATE),
              UrlCleaner.TRANSFORM_FUNCTION),
          website, false /* isTweet */);
    }
    for (String rssUrl : RSS_URLS) {
      LOG.info("RSS FILE: " + rssUrl);
      crawler.crawl(rssUrl);
    }
  }
}
