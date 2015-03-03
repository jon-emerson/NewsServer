package com.janknspank.crawler;

import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import com.google.api.client.util.Lists;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.janknspank.bizness.GuidFactory;
import com.janknspank.bizness.Urls;
import com.janknspank.common.DateParser;
import com.janknspank.common.Logger;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.dom.parser.DocumentBuilder;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.dom.parser.Node;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.fetch.FetchException;
import com.janknspank.fetch.FetchResponse;
import com.janknspank.fetch.Fetcher;
import com.janknspank.proto.CoreProto.Url;
import com.janknspank.proto.SiteProto.SiteManifest;

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

  /**
   * Inserts all the passed URLs into the database, preventing duplicates.
   * @return The resulting set of URLs, which is either pre-existing URLs, or
   *     the newly created URLs.  No ordering guarantee is given.
   */
  private static Iterable<Url> putIfNotExists(Iterable<Url> urls)
      throws DatabaseSchemaException, DatabaseRequestException {
    Iterable<Url> existingUrlObjects =
        Database.with(Url.class).get(new QueryOption.WhereEquals("url", Iterables.transform(urls,
            new Function<Url, String>() {
              @Override
              public String apply(Url url) {
                return url.getUrl();
              }
            })));
    Set<String> existingUrls = Sets.newHashSet();
    Iterables.addAll(existingUrls, Iterables.transform(existingUrlObjects,
        new Function<Url, String>() {
          @Override
          public String apply(Url url) {
            return url.getUrl();
          }
        }));
    List<Url> urlsToInsert = Lists.newArrayList();
    for (Url url : urls) {
      if (!existingUrls.contains(url.getUrl())) {
        System.out.println("Inserting " + url.getUrl());
        urlsToInsert.add(url);
        existingUrls.add(url.getUrl()); // To prevent inserting dupes.
      }
      if (urlsToInsert.size() > 250) {
        Database.insert(urlsToInsert);
        urlsToInsert.clear();
      }
    }
    Database.insert(urlsToInsert);
    return Iterables.concat(existingUrlObjects, urlsToInsert);
  }

  private Iterable<Url> parseRss(String rssUrl)
      throws DatabaseSchemaException, DatabaseRequestException {
    FetchResponse fetchResponse;
    List<Url> urlsToInsert = Lists.newArrayList();
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
            urlsToInsert.add(Url.newBuilder()
                .setUrl(articleUrl)
                .setOriginUrl(rssUrl)
                .setId(GuidFactory.generate())
                .setDiscoveryTime(System.currentTimeMillis())
                .setCrawlPriority(Urls.getCrawlPriority(articleUrl, millis))
                .build());
          }
        }
      }
    } catch (FetchException | ParserException e) {
      e.printStackTrace();
    }
    return putIfNotExists(urlsToInsert);
  }

  public Iterable<Url> getUrls(SiteManifest manifest)
      throws DatabaseSchemaException, DatabaseRequestException {
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
      Iterables.addAll(siteUrls, putIfNotExists(Iterables.transform(urlStrings,
          new Function<String, Url>() {
            @Override
            public Url apply(String urlString) {
              return Url.newBuilder()
                  .setUrl(urlString)
                  .setOriginUrl(startUrl)
                  .setId(GuidFactory.generate())
                  .setDiscoveryTime(System.currentTimeMillis())
                  // MAX_CRAWL_PRIORITY because we found this URL on the site's HOME PAGE
                  // so in all probability it's pretty new, regardless of whether we can
                  // parse a date from its URL like getCrawlPriority() tries to do.
                  .setCrawlPriority(Urls.MAX_CRAWL_PRIORITY)
                  .build();
            }
          })));
    }
    for (String rssUrl : manifest.getRssUrlList()) {
      LOG.info("RSS FILE: " + rssUrl);
      Iterables.addAll(siteUrls, parseRss(rssUrl));
    }
    return siteUrls;
  }
}
