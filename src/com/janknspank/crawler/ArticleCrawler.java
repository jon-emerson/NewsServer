package com.janknspank.crawler;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.Links;
import com.janknspank.bizness.Urls;
import com.janknspank.common.Logger;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.fetch.FetchException;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.InterpretedData;
import com.janknspank.proto.CoreProto.Url;
import com.janknspank.proto.SiteProto.SiteManifest;

/**
 * The is the top-most method of the Crawl task.  It creates a bunch of threads
 * that go and grab articles off of sites, then uses ArticleCreator to interpret
 * those article documents, then stores the results to the database.
 */
public class ArticleCrawler implements Callable<Void> {
  private static final Logger LOG = new Logger(ArticleCrawler.class);
  public static final int THREAD_COUNT = 20;

  private final SiteManifest manifest;

  private ArticleCrawler(SiteManifest manifest) {
    this.manifest = manifest;
  }

  @Override
  public Void call() throws Exception {
    long startTime = System.currentTimeMillis();
    Iterable<Url> urls;
    try {
      urls = new UrlCrawler().getUrls(manifest);
    } catch (DatabaseSchemaException | DatabaseRequestException e) {
      System.out.println("ERROR parsing site: " + manifest.getRootDomain());
      e.printStackTrace();
      return null;
    }

    for (Url url : urls) {
      // In the new design, we get a list of all the URLs on a site's homepage
      // and RSS pages, and in many cases, we've crawled them before.  If so,
      // skip the crawling, but do refresh the article's social score if it
      // hasn't recently been updated.
      if (url.hasLastCrawlStartTime()) {
        // TODO(jonemerson): Update social score.
        continue;
      }

      try {
        url = Urls.markCrawlStart(url);
        if (url == null) {
          // Some other thread has likely claimed this URL - Move along.
          continue;
        }
      } catch (BiznessException | DatabaseSchemaException e) {
        throw new RuntimeException("Could not read URL to crawl");
      }

      // Save this article and its keywords.
      try {
        crawl(url, false /* markCrawlStart */);

      } catch (DatabaseSchemaException | DatabaseRequestException | BiznessException e) {
        // Internal error (bug in our code).
        e.printStackTrace();
      } catch (FetchException|ParserException|RequiredFieldException e) {
        // Bad article.
        e.printStackTrace();
      }
    }
    System.out.println("Finished updating " + manifest.getRootDomain() + " in "
        + (System.currentTimeMillis() - startTime) + "ms");
    return null;
  }

  /**
   * Crawls a URL, putting the article (if found) in the database, and storing
   * any outbound links.
   */
  public static Article crawl(Url url, boolean markCrawlStart) throws FetchException,
       ParserException, RequiredFieldException, DatabaseSchemaException, DatabaseRequestException,
       BiznessException {
    System.err.println("Crawling: " + url.getUrl());

    if (markCrawlStart) {
      Urls.markCrawlStart(url);
    }

    @SuppressWarnings("unused")
    List<String> urls;

    Article article = null;
    if (ArticleUrlDetector.isArticle(url.getUrl())) {
      InterpretedData interpretedData = Interpreter.interpret(url);
      article = interpretedData.getArticle();
      try {
        Database.insert(article);
      } catch (DatabaseRequestException | DatabaseSchemaException e) {
        // It could be that some other process decided to steal this article
        // and process it first (mainly due to human error).  If so, delete
        // everything and store it again.
        LOG.severe("Handling human error: " + url.getUrl());
        Database.with(Article.class).delete(url.getId());
        Links.deleteFromOriginUrlId(ImmutableList.of(url.getId()));

        // Try again!
        Database.insert(article);
      }
      urls = interpretedData.getUrlList();
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
  private static Map<String, Article> getNewArticles(Iterable<String> urlStrings)
      throws DatabaseRequestException, DatabaseSchemaException, FetchException, ParserException,
          RequiredFieldException, BiznessException {
    Iterable<Url> urls = Urls.put(urlStrings, "http://jonemerson.net/benchmark");
    List<Article> articles = Lists.newArrayList();
    for (Url url : urls) {
      Article article = crawl(url, true /* markCrawlStart */);
      if (article == null) {
        throw new IllegalStateException("URL is not an Article: " + url.getUrl());
      }
      articles.add(article);
    }
    return createArticleMap(articles);
  }

  /**
   * Gets articles for each of the requested URL strings, whether they've been
   * previously crawled or not.  (Ones that haven't been crawled will be
   * crawled.)
   */
  public static Map<String, Article> getArticles(Iterable<String> urlStrings)
      throws BiznessException {
    Map<String, Article> articles;
    try {
      articles = getExistingArticles(urlStrings);
      articles = ImmutableMap.<String, Article>builder()
          .putAll(articles)
          .putAll(getNewArticles(Sets.<String>symmetricDifference(
              new HashSet<String>(Lists.newArrayList(urlStrings)), articles.keySet())))
          .build();
    } catch (DatabaseSchemaException | DatabaseRequestException | FetchException
        | ParserException | RequiredFieldException e) {
      throw new BiznessException("Could not get articles: " + e.getMessage(), e);
    }
    return articles;
  }

  private static class PoisonPillCallable implements Callable<Void> {
    @Override
    public Void call() throws Exception {
      // Kill the process after 9 minutes.  Usually it takes 3 minutes.  But
      // sometimes it takes longer, and we have crawlers running every 10
      // minutes, so if this one's running long, let the next one run with it.
      Thread.sleep(TimeUnit.MINUTES.toMillis(9));
      System.out.println("KILLING PROCESS - TIMEOUT REACHED - See " + this.getClass().getName());
      System.exit(-1);
      return null;
    }
  }

  public static void main(String args[]) throws Exception {
    long startTime = System.currentTimeMillis();

    // Randomly create crawlers, which will be execution poll throttled to
    // THREAD_COUNT threads, for each website in our corpus.
    List<SiteManifest> allManifests = Lists.newArrayList(SiteManifests.getList());
    Collections.shuffle(allManifests, new Random(System.currentTimeMillis()));

    // Schedule all the threads.
    ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT + 1);
    List<Callable<Void>> crawlers = Lists.newArrayList();
    crawlers.add(new PoisonPillCallable());
    for (SiteManifest manifest : allManifests) {
      crawlers.add(new ArticleCrawler(manifest));
    }
    executor.invokeAll(crawlers);
    executor.shutdown();
    System.out.println("Finished crawl in " + (System.currentTimeMillis() - startTime) + "ms");
  }
}
