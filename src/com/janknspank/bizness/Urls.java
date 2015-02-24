package com.janknspank.bizness;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.janknspank.common.DateParser;
import com.janknspank.crawler.ArticleCrawler;
import com.janknspank.crawler.ArticleUrlDetector;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.CoreProto.Url;

/**
 * Row in the MySQL database: Url.  Represents a URL we discovered.
 * Basically just tracks that a URL exists and gives it an ID that can be
 * used as a foreign key in other tables.
 */
public class Urls {
  public static final int MAX_CRAWL_PRIORITY = 2000;

  /**
   * Returns the crawl priority for the URL, assuming that we don't know
   * anything about it yet (e.g. it hasn't been discovered before and we don't
   * know about any tweets).
   * 
   * New articles are given precedence over older articles.  Non-articles are
   * given very low priority.
   * 
   * TODO(jonemerson): For news sites that use incrementing article numbers,
   * try to generate a priority based on them.
   * @param url the url to get the crawl priority of
   * @param millis the date the article was published, in milliseconds, if known
   */
  public static int getCrawlPriority(String url, Long millis) {
    if (ArticleUrlDetector.isArticle(url)) {
      millis = (millis == null)
          ? DateParser.parseDateFromUrl(url, true /* allowMonth */) : millis;
      if (millis != null) {
        long millisAgo = Math.max(0, System.currentTimeMillis() - millis);
        return (int) Math.max(100, MAX_CRAWL_PRIORITY - (millisAgo / (1000 * 60 * 60)));
      }
      return 100;
    }
    return url.contains("//twitter.com/") ? 0 : 10;
  }

  private static Url create(String url, String originUrl) {
    return Url.newBuilder()
        .setId(GuidFactory.generate())
        .setUrl(url)
        .setOriginUrl(originUrl)
        .setDiscoveryTime(System.currentTimeMillis())
        .setCrawlPriority(getCrawlPriority(url, null))
        .build();
  }

  /**
   * Makes sure the passed-in URL is stored in our database.  If it exists
   * already, its tweet_count and priority will be updated accordingly.
   * The returned Url is either an updated version of the existing field or
   * a new Url, as necessary.
   */
  public static Url put(String urlString, String originUrl)
      throws BiznessException, DatabaseSchemaException {
    return Iterables.getFirst(put(ImmutableList.of(urlString), originUrl), null);
  }

  /**
   * Makes sure the passed-in URLs are stored in our database.  Existing URLs
   * have their tweet_count and priority updated accordingly, if isTweet is
   * true.  The return Url objects are in the same order as the URL strings,
   * though duplicates will be removed.
   */
  public static Iterable<Url> put(Iterable<String> urlStrings, String originUrl)
      throws BiznessException, DatabaseSchemaException {
    if (Iterables.isEmpty(urlStrings)) {
      return ImmutableList.of();
    }

    // Create a LinkedHashMap for the return values.  (Doing this now preserves
    // the order of our return values, so they match the order of urlStrings.
    LinkedHashMap<String, Url> urls = Maps.newLinkedHashMap();
    for (String urlString : urlStrings) {
      urls.put(urlString, null);
    }

    // Use urls.keySet() instead of urlStrings here as a way to dedupe.
    List<Url> urlsToUpdate = Lists.newArrayList(); // To increment tweet_count.
    for (Url existingUrl :
        Database.with(Url.class).get(new QueryOption.WhereEquals("url", urls.keySet()))) {
      urls.put(existingUrl.getUrl(), existingUrl);
    }

    List<Url> urlsToCreate = Lists.newArrayList();
    Set<String> existingCreates = Sets.newHashSet();
    for (String urlString : urlStrings) {
      // Use existingCreates to prevent the same URL being inserted twice.
      // (Well, the database would actually prevent us, by crashing this whole
      // method!!)
      if (urls.get(urlString) == null && !existingCreates.contains(urlString)) {
        Url newUrl = create(urlString, originUrl);
        urlsToCreate.add(newUrl);
        urls.put(urlString, newUrl);
        existingCreates.add(urlString);
      }
    }

    try {
      Database.insert(urlsToCreate);
      Database.update(urlsToUpdate);
      return urls.values();
    } catch (DatabaseRequestException e) {
      throw new BiznessException("Could not insert new discovered URL", e);
    }
  }

  /**
   * Marks the passed URL as "has started being crawled".
   * @return Url object with an updated last crawl start time, or null, if the
   *     given Url has already been crawled by another thread
   */
  public static Url markCrawlStart(Url url) throws BiznessException, DatabaseSchemaException {
    url = url.toBuilder()
        .setLastCrawlStartTime(System.currentTimeMillis())
        .build();
    try {
      // Only update the URL if last_crawl_start_time hasn't yet been claimed
      // by another crawling thread.
      if (Database.update(url, new QueryOption.WhereNull("last_crawl_start_time"))) {
        return url;
      } else {
        return null;
      }
    } catch (DatabaseRequestException e) {
      throw new BiznessException("Error updating last crawl start time: " + e.getMessage(), e);
    }
  }

  /**
   * Marks the passed URL as "has finished being crawled".
   * @return Url object with an updated last crawl finish time
   */
  public static Url markCrawlFinish(Url url) throws BiznessException, DatabaseSchemaException {
    url = url.toBuilder()
        .setLastCrawlFinishTime(System.currentTimeMillis())
        .setCrawlPriority(0)
        .build();
    try {
      Database.update(url);
    } catch (DatabaseRequestException e) {
      throw new BiznessException("Could not update last crawl finish time", e);
    }
    return url;
  }

  @SuppressWarnings("unused")
  public static Url getNextUrlToCrawl() throws DatabaseSchemaException {
    return Database.with(Url.class).getFirst(
        new QueryOption.WhereNull("last_crawl_start_time"),
        new QueryOption.WhereNotEqualsNumber("crawl_priority", 0),
        new QueryOption.WhereNotLike("url", "https://twitter.com/%"),
        new QueryOption.DescendingSort("crawl_priority"),
        new QueryOption.LimitWithOffset(1,
            ArticleCrawler.THREAD_COUNT == 1 ? 0 : (int) (20 * Math.random())));
  }

  /**
   * Gets a URL by its ID.
   */
  public static Url getById(String id) throws DatabaseSchemaException {
    return Database.with(Url.class).get(id);
  }

  /**
   * Gets a URL by its URL.
   */
  public static Url getByUrl(String url) throws DatabaseSchemaException {
    return Database.with(Url.class).getFirst(new QueryOption.WhereEquals("url", url));
  }

  /** Helper method for creating the discovered-url table. */
  public static void main(String args[]) throws Exception {
    Database.with(Url.class).createTable();
  }
}
