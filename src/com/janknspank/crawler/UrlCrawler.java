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
 * Iterates through the home pages of the various sites we support, and RSS
 * feeds of sites that offer those, to push as many new article URLs to the
 * database as possible.
 */
public class UrlCrawler {
  private static final Logger LOG = new Logger(UrlCrawler.class);
  private final Fetcher fetcher = new Fetcher();
  private static final Iterable<String> WEBSITES = Iterables.concat(
      Iterables.transform(SiteManifests.getList(),
          new Function<SiteManifest, Iterable<String>>() {
            @Override
            public Iterable<String> apply(SiteManifest contentSite) {
              return contentSite.getStartUrlList();
            }
          }));
  private static final Iterable<String> RSS_URLS = Iterables.concat(
      Iterables.transform(SiteManifests.getList(),
          new Function<SiteManifest, Iterable<String>>() {
            @Override
            public Iterable<String> apply(SiteManifest contentSite) {
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

  /**
   * Inserts all the passed URLs into the database, preventing duplicates.
   */
  private static void putIfNotExists(Iterable<Url> urls)
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
  }

  public void crawl(String rssUrl) throws DatabaseSchemaException, DatabaseRequestException {
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
    putIfNotExists(urlsToInsert);
  }

  public static void main(String args[]) throws Exception {
    UrlCrawler crawler = new UrlCrawler();
    for (final String website : WEBSITES) {
      LOG.info("WEBSITE: " + website);

      // Find all the URLs on the page, filter them to only have the article
      // URLs, and then clean those before trying to put them into the database.
      Iterable<String> urlStrings = Iterables.transform(
          Iterables.filter(
              Iterables.filter(UrlFinder.findUrls(website), ArticleUrlDetector.PREDICATE),
              UrlWhitelist.PREDICATE),
          UrlCleaner.TRANSFORM_FUNCTION);
      putIfNotExists(Iterables.transform(urlStrings, new Function<String, Url>() {
        @Override
        public Url apply(String urlString) {
          return Url.newBuilder()
              .setUrl(urlString)
              .setOriginUrl(website)
              .setId(GuidFactory.generate())
              .setDiscoveryTime(System.currentTimeMillis())
              // MAX_CRAWL_PRIORITY because we found this URL on the site's HOME PAGE
              // so in all probability it's pretty new, regardless of whether we can
              // parse a date from its URL like getCrawlPriority() tries to do.
              .setCrawlPriority(Urls.MAX_CRAWL_PRIORITY)
              .build();
        }
      }));
    }
    for (String rssUrl : RSS_URLS) {
      LOG.info("RSS FILE: " + rssUrl);
      crawler.crawl(rssUrl);
    }
  }
}
