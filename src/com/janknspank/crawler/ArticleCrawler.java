package com.janknspank.crawler;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.GuidFactory;
import com.janknspank.bizness.Links;
import com.janknspank.bizness.Urls;
import com.janknspank.common.Host;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.fetch.FetchException;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.InterpretedData;
import com.janknspank.proto.CoreProto.Url;
import com.janknspank.proto.CrawlerProto.CrawlHistory;
import com.janknspank.proto.CrawlerProto.SiteManifest;

/**
 * The is the top-most method of the Crawl task.  It creates a bunch of threads
 * that go and grab articles off of sites, then uses ArticleCreator to interpret
 * those article documents, then stores the results to the database.
 */
public class ArticleCrawler implements Callable<Void> {
  public static final int THREAD_COUNT = 50;

  private final SiteManifest manifest;
  private final static CrawlHistory.Builder CRAWL_HISTORY_BUILDER = CrawlHistory.newBuilder();
  private final static ConcurrentHashMap<String, CrawlHistory.Site.Builder> CRAWL_HISTORY_SITES =
      new ConcurrentHashMap<>();

  private ArticleCrawler(SiteManifest manifest) {
    this.manifest = manifest;
  }

  @Override
  public Void call() throws Exception {
    long startTime = System.currentTimeMillis();
    CrawlHistory.Site.Builder crawlHistorySiteBuilder = CrawlHistory.Site.newBuilder()
        .setRootDomain(manifest.getRootDomain())
        .setStartTime(startTime)
        .setArticlesCrawled(0);
    CRAWL_HISTORY_SITES.put(manifest.getRootDomain(), crawlHistorySiteBuilder);

    // For this site, figure out what URLs are mentioned on its home page, other
    // start URLs, and RSS feeds.  Then figure out whether we already know the
    // URLs and whether we've created articles for them already.
    Iterable<String> articleUrls;
    Map<String, Url> existingArticleUrls = Maps.newHashMap();
    Map<String, Article> existingArticles = Maps.newHashMap();
    try {
      articleUrls = new UrlCrawler().findArticleUrls(manifest);
      for (Url url : Database.with(Url.class).get(
          new QueryOption.WhereEquals("url", articleUrls))) {
        existingArticleUrls.put(url.getUrl(), url);
      }
      for (Article article : Database.with(Article.class).get(
          new QueryOption.WhereEquals("url", articleUrls))) {
        existingArticles.put(article.getUrl(), article);
      }
    } catch (DatabaseSchemaException e) {
      System.out.println("ERROR parsing site: " + manifest.getRootDomain());
      e.printStackTrace();
      return null;
    }

    for (String articleUrl : articleUrls) {
      if (existingArticles.containsKey(articleUrl)) {
        if (!existingArticleUrls.containsKey(articleUrl)) {
          // Clean-up: If the URL was deleted, then delete the article too so it
          // can be recrawled below.
          Database.delete(existingArticles.get(articleUrl));
        } else {
          // Already crawled, everything looks good.  Let's go to the next URL.
          continue;
        }
      }

      // Get a URL object for the article.  If one exists, use it.  If one
      // doesn't, insert one.  If one exists and we've tried to crawl it
      // previously, make sure we're not trying again too soon.
      Url url;
      if (existingArticleUrls.containsKey(articleUrl)) {
        url = existingArticleUrls.get(articleUrl);
        if (url.hasLastCrawlStartTime()
            && url.getLastCrawlStartTime() > (System.currentTimeMillis() - TimeUnit.HOURS.toMillis(4))) {
          // Since the last crawl time has already been marked, we know we've
          // we've tried to crawl this article before.  So unless we tried at
          // least 4 hours ago, don't try again.  (But do try again after 4
          // hours, because the Internet is unreliable and 4 hours is a
          // reasonable amount of time to give folks to fix things...)
          continue;
        }
      } else {
        url = Urls.put(articleUrl, manifest.getStartUrl(0));
      }

      // Mark the crawl start time.
      try {
        url = url.toBuilder()
            .setLastCrawlStartTime(System.currentTimeMillis())
            .build();
        Database.update(url);
      } catch (DatabaseRequestException | DatabaseSchemaException e) {
        e.printStackTrace();
        continue;
      }

      // Save this article and its keywords.
      try {
        crawl(url, false /* retain */);
        crawlHistorySiteBuilder.setArticlesCrawled(
            crawlHistorySiteBuilder.getArticlesCrawled() + 1);

      } catch (DatabaseSchemaException | DatabaseRequestException | BiznessException e) {
        // Internal error (bug in our code).
        e.printStackTrace();
      } catch (FetchException | RequiredFieldException e) {
        // Bad article.
        e.printStackTrace();
      }
    }
    crawlHistorySiteBuilder.setEndTime(System.currentTimeMillis());
    crawlHistorySiteBuilder.setMillis(
        crawlHistorySiteBuilder.getEndTime() - crawlHistorySiteBuilder.getStartTime());
    System.out.println("Finished updating " + manifest.getRootDomain() + " in "
        + (System.currentTimeMillis() - startTime) + "ms");
    return null;
  }

  /**
   * Crawls a URL, putting the article (if found) in the database.
   * @param url the URL to attempt to create an Article object from
   * @param markCrawlStart whether the passed URL should be marked as "crawl
   *     started" in the database, therefore preventing other threads from
   *     attempting to crawl it in the future
   * @param retain Whether to retain this article during prunings.  This should
   *     be set to true only for articles that are important to our training
   *     processes, so that we don't have to re-download them whenever we want
   *     to retrain our vectors or neural network.
   */
  public static Article crawl(Url url, boolean retain)
      throws FetchException, RequiredFieldException, DatabaseSchemaException,
          DatabaseRequestException, BiznessException {
    @SuppressWarnings("unused")
    Set<String> urls;

    Article article = null;
    if (ArticleUrlDetector.isArticle(url.getUrl())) {
      InterpretedData interpretedData = Interpreter.interpret(url);
      article = interpretedData.getArticle();
      if (retain) {
        article = article.toBuilder()
            .setRetain(retain)
            .build();
      }
      try {
        Database.insert(article);
      } catch (DatabaseRequestException | DatabaseSchemaException e) {
        // It could be that some other process decided to steal this article
        // and process it first (mainly due to human error).  If so, delete
        // everything and store it again.
        System.out.println("Handling human error: " + url.getUrl());
        e.printStackTrace();
        Database.with(Article.class).delete(url.getId());
        Links.deleteFromOriginUrlId(ImmutableList.of(url.getId()));

        // Try again!
        Database.insert(article);
      } catch (Throwable e) {
        e.printStackTrace();
        throw e;
      }
      urls = ImmutableSet.copyOf(interpretedData.getUrlList());
    } else {
      urls = UrlFinder.findUrls(url.getUrl());
    }

    // TODO(jonemerson): Someday use URLs we find in articles to augment our
    // corpus.  But for now, to simplify the processing we do down to a finite
    // amount, we ignore URLs we find while crawling.
    // // Make sure to filter and clean the URLs - only store the ones we want to crawl!
    // Iterable<Url> destinationUrls = Urls.put(
    //     Iterables.transform(
    //         Iterables.filter(urls, UrlWhitelist.PREDICATE),
    //         UrlCleaner.TRANSFORM_FUNCTION),
    //     url.getUrl());
    // Links.put(url, destinationUrls);

    Urls.markCrawlFinish(url);
    return article;
  }

  /**
   * Returns a map of URL -> Article for each given article.
   */
  private static Map<String, Article> createArticleMap(Iterable<Article> articles) {
    HashMap<String, Article> articleMap = Maps.newHashMap();
    for (Article article : articles) {
      articleMap.put(article.getUrl(), article);
    }
    return articleMap;
  }

  /**
   * Gets a Map of all the existing Articles for the requested {@code
   * urlStrings}.  The Map maps each article's URL to its Article object.
   */
  private static Map<String, Article> getExistingArticles(Iterable<String> urlStrings)
      throws DatabaseSchemaException {
    return createArticleMap(
        Database.with(Article.class).get(new QueryOption.WhereEquals("url", urlStrings)));
  }

  /**
   * Inserts URL objects for each of the passed URL strings, then crawls said
   * URLs, putting the resulting Urls and Articles into the database.  The
   * retrieved Articles are returned, mapped to their original URLs.
   */
  private static Map<String, Article> getNewArticles(
      final Iterable<String> urlStrings, final boolean retain)
      throws DatabaseRequestException, DatabaseSchemaException, FetchException,
          RequiredFieldException, BiznessException {
    Iterable<Url> urls = Urls.put(urlStrings, "");
    final ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
    final Map<String, Future<Article>> articleFutures = Maps.newHashMap();
    for (final Url url : urls) {
      Callable<Article> articleCallable = new Callable<Article>() {
        @Override
        public Article call() throws Exception {
          Article article = crawl(url, retain);
          if (article == null) {
            throw new BiznessException("URL is not an Article: " + url.getUrl());
          }
          return article;
        }
      };
      articleFutures.put(url.getUrl(), executor.submit(articleCallable));
    }
    executor.shutdown();
    final Map<String, Article> articleMap = Maps.newHashMap();
    for (Map.Entry<String, Future<Article>> entry : articleFutures.entrySet()) {
      try {
        articleMap.put(entry.getKey(), entry.getValue().get());
      } catch (InterruptedException | ExecutionException e) {
        throw new BiznessException("Error fetching Article: " + e.getMessage(), e);
      }
    }
    return articleMap;
  }

  /**
   * Gets articles for each of the requested URL strings, whether they've been
   * previously crawled or not.  (Ones that haven't been crawled will be
   * crawled.)
   */
  public static Map<String, Article> getArticles(Iterable<String> urlStrings, boolean retain)
      throws BiznessException {
    Map<String, Article> articles;
    try {
      articles = getExistingArticles(urlStrings);
      articles = ImmutableMap.<String, Article>builder()
          .putAll(articles)
          .putAll(getNewArticles(Sets.<String>symmetricDifference(
              new HashSet<String>(Lists.newArrayList(urlStrings)), articles.keySet()), retain))
          .build();
    } catch (DatabaseSchemaException | DatabaseRequestException | FetchException
        | RequiredFieldException e) {
      throw new BiznessException("Could not get articles: " + e.getMessage(), e);
    }

    if (retain) {
      // Mark any non-retain articles as retain now, so that we never have to
      // crawl them again.  (Previously, newly crawled articles would make it
      // into the vectors, only to be pruned later, and then we'd run into
      // errors (timeouts, 404s, etc) when trying to crawl them after the prune.)
      Iterable<Article> nonRetainArticles = Iterables.filter(articles.values(),
          new Predicate<Article>() {
            @Override
            public boolean apply(Article article) {
              return !article.getRetain();
            }
          });
      for (Article nonRetainArticle : nonRetainArticles) {
        try {
          Database.set(nonRetainArticle, "retain", true);
        } catch (DatabaseRequestException | DatabaseSchemaException e) {}
      }
    }

    return articles;
  }

  private static void updateCrawlHistoryInDatabase()
      throws DatabaseRequestException, DatabaseSchemaException {
    if (CRAWL_HISTORY_BUILDER.hasEndTime()) {
      CRAWL_HISTORY_BUILDER.setMillis(
          CRAWL_HISTORY_BUILDER.getEndTime() - CRAWL_HISTORY_BUILDER.getStartTime());
    } else {
      CRAWL_HISTORY_BUILDER.setMillis(
          System.currentTimeMillis() - CRAWL_HISTORY_BUILDER.getStartTime());
    }

    CRAWL_HISTORY_BUILDER.clearSite();
    for (CrawlHistory.Site.Builder siteBuilder : CRAWL_HISTORY_SITES.values()) {
      CRAWL_HISTORY_BUILDER.addSite(siteBuilder.build());
    }
    Database.update(CRAWL_HISTORY_BUILDER.build());
  }

  /**
   * Periodically updates the database with statistics about how the crawler is
   * doing.
   */
  private static class CommitCrawlHistoryThread extends Thread {
    @Override
    public void run() {
      try {
        CRAWL_HISTORY_BUILDER
            .setCrawlId(GuidFactory.generate())
            .setHost(Host.get())
            .setStartTime(System.currentTimeMillis());
        Database.insert(CRAWL_HISTORY_BUILDER.build());

        while (true) {
          try {
            Thread.sleep(TimeUnit.MINUTES.toMillis(1));
          } catch (InterruptedException e) {
            return; // Totally expected!
          }
          updateCrawlHistoryInDatabase();
        }
      } catch (Throwable e) {
        e.printStackTrace();
      }
    }
  }

  public static void main(String args[]) throws Exception {
    long startTime = System.currentTimeMillis();

    // If there's any arguments, use them as a filtering method on the manifests
    // we process.  The Boolean marks whether we found the specified root domain
    // in the manifests.
    Map<String, Boolean> rootDomainMap = Maps.newHashMap();
    for (String arg : args) {
      String rootDomain = arg.trim();
      if (rootDomainMap.containsKey(rootDomain)) {
        throw new Error("Root domain specified more than once: " + rootDomain);
      }
      if (rootDomain.length() > 0) {
        rootDomainMap.put(rootDomain, false);
      }
    }
    if (!rootDomainMap.isEmpty()) {
      System.out.println("Restricting crawl to: " + Joiner.on(", ").join(rootDomainMap.keySet()));
    }

    // Record crawl history on a regular basis.
    CommitCrawlHistoryThread commitCrawlHistoryThread = new CommitCrawlHistoryThread();
    commitCrawlHistoryThread.start();

    // Randomly create crawlers, which will be execution poll throttled to
    // THREAD_COUNT threads, for each website in our corpus.
    List<SiteManifest> allManifests = Lists.newArrayList(SiteManifests.getList());
    Collections.shuffle(allManifests, new Random(System.currentTimeMillis()));

    // Create Callables (which will become threads once they're invoked) for
    // every site that we're instructed to crawl.
    List<Callable<Void>> crawlers = Lists.newArrayList();
    for (SiteManifest manifest : allManifests) {
      String rootDomain = manifest.getRootDomain();
      if (rootDomainMap.isEmpty()) {
        crawlers.add(new ArticleCrawler(manifest));
      } else if (rootDomainMap.containsKey(rootDomain)) {
        crawlers.add(new ArticleCrawler(manifest));
        rootDomainMap.put(manifest.getRootDomain(), true);
      }
    }
    for (Map.Entry<String, Boolean> rootDomainMapEntry : rootDomainMap.entrySet()) {
      if (!rootDomainMapEntry.getValue()) {
        throw new Error("Could not find manifest for domain " + rootDomainMapEntry.getKey());
      }
    }

    // Start the threads!
    ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
    executor.invokeAll(crawlers);
    executor.shutdown();
    try {
      // Kill the process after 18 minutes, then let the next crawler take
      // over.  (Note: Heroku's scheduling isn't that reliable, so we can't
      // keep this at 20 minutes - Tasks start to overlap.)
      executor.awaitTermination(TimeUnit.MINUTES.toMillis(18), TimeUnit.MILLISECONDS);
      commitCrawlHistoryThread.interrupt();
    } catch (InterruptedException e) {}
    System.out.println("Finished crawl in " + (System.currentTimeMillis() - startTime) + "ms");

    // Record that we finished.
    CRAWL_HISTORY_BUILDER
        .setWasInterrupted(false)
        .setEndTime(System.currentTimeMillis());
    updateCrawlHistoryInDatabase();
    System.exit(0);
  }
}
