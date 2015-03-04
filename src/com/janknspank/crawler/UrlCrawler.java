package com.janknspank.crawler;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import com.google.api.client.util.Lists;
import com.google.common.collect.Iterables;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.Urls;
import com.janknspank.common.Logger;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.dom.parser.Node;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.fetch.FetchException;
import com.janknspank.fetch.FetchResponse;
import com.janknspank.fetch.Fetcher;
import com.janknspank.proto.CoreProto.Url;
import com.janknspank.proto.CrawlerProto.SiteManifest;

/**
 * Iterates through the start URLs and RSS feeds for a particular site, inserts
 * any discovered URLs into the database, then returns a set of Url objects for
 * all the currently-relevant pages on the site.
 * 
 * This class is designed to be used by ArticleCrawler, so that it can get a
 * set of URLs on a particular site to crawl.
 */
class UrlCrawler {
  private static final Logger LOG = new Logger(UrlCrawler.class);
  private final Fetcher fetcher = new Fetcher();

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

  private Iterable<Url> parseRss(String rssUrl)
      throws DatabaseSchemaException, DatabaseRequestException, BiznessException {
    FetchResponse fetchResponse;
    List<String> urlsToInsert = Lists.newArrayList();
    try {
      fetchResponse = fetcher.fetch(rssUrl);
      if (fetchResponse.getStatusCode() == HttpServletResponse.SC_OK) {
        DocumentNode documentNode = fetchResponse.getDocumentNode();
        for (Node itemNode : Iterables.concat(
            documentNode.findAll("item"),
            documentNode.findAll("entry"))) {
          String articleUrl = getArticleUrl(itemNode);
          if (ArticleUrlDetector.isArticle(articleUrl)) {
            urlsToInsert.add(articleUrl);
          }
        }
      }
    } catch (FetchException | ParserException e) {
      e.printStackTrace();
    }
    return Urls.put(urlsToInsert, rssUrl);
  }

  /**
   * Returns a set of all the articles on a site's home page and other start
   * URLs, regardless of whether we've indexed them before or not.
   * @throws BiznessException 
   */
  public Iterable<Url> findArticleUrls(SiteManifest manifest)
      throws DatabaseSchemaException, DatabaseRequestException, BiznessException {
    LOG.info("WEBSITE: " + manifest.getRootDomain());

    // Find all the URLs on the page, filter them to only have the article
    // URLs, and then clean those before trying to put them into the database.
    List<Url> siteUrls = Lists.newArrayList();
    for (final String startUrl : manifest.getStartUrlList()) {
      Iterable<String> urlStrings;
      try {
        urlStrings = Iterables.transform(
            Iterables.filter(
                Iterables.filter(UrlFinder.findUrls(startUrl), ArticleUrlDetector.PREDICATE),
                UrlWhitelist.PREDICATE),
            UrlCleaner.TRANSFORM_FUNCTION);
      } catch (FetchException | ParserException | RequiredFieldException e) {
        System.out.println("ERROR: Could not parse " + startUrl);
        e.printStackTrace();
        continue;
      }
      Iterables.addAll(siteUrls, Urls.put(urlStrings, startUrl));
    }
    for (String rssUrl : manifest.getRssUrlList()) {
      LOG.info("RSS FILE: " + rssUrl);
      Iterables.addAll(siteUrls, parseRss(rssUrl));
    }
    return siteUrls;
  }
}
