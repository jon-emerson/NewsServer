package com.janknspank;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.janknspank.bizness.ArticleKeywords;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.Links;
import com.janknspank.bizness.Urls;
import com.janknspank.common.ArticleUrlDetector;
import com.janknspank.common.UrlCleaner;
import com.janknspank.common.UrlWhitelist;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.fetch.FetchException;
import com.janknspank.interpreter.Interpreter;
import com.janknspank.interpreter.RequiredFieldException;
import com.janknspank.interpreter.UrlFinder;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.Url;
import com.janknspank.proto.Interpreter.InterpretedData;

public class TheMachine {
  public void start() {
    // Uncomment this to start the crawl at a specific page.
    try {
      Urls.put("http://recode.net/", false);
    } catch (BiznessException | DatabaseSchemaException e) {
      e.printStackTrace();
    }

    while (true) {
      final Url url;
      try {
        url = Urls.markCrawlStart(Urls.getNextUrlToCrawl());
//      final Url url = Url.newBuilder()
//          .setId("pYZDE7M36zxQNxbFTUVFCQ")
//          .setUrl("http://techcrunch.com/2015/01/03/the-sharing-economy-and-the-"
//              + "future-of-finance/")
//          .setTweetCount(0)
//          .setDiscoveryTime(System.currentTimeMillis())
//          .build();
        if (url == null) {
          // Some other thread has likely claimed this URL - Go get another.
          continue;
        }
      } catch (BiznessException | DatabaseSchemaException e) {
        throw new RuntimeException("Could not read URL to crawl");
      }

      // Save this article and its keywords.
      try {
        if (!UrlWhitelist.isOkay(url.getUrl())) {
          System.err.println("Removing now-blacklisted page: " + url.getUrl());
          Links.deleteIds(ImmutableList.of(url.getId()));
          Database.delete(url);
          continue;
        }

        System.err.println("Crawling: " + url.getUrl());

        List<String> urls;
        if (ArticleUrlDetector.isArticle(url.getUrl())) {
          InterpretedData interpretedData = Interpreter.interpret(url);
          try {
            Database.insert(interpretedData.getArticle());
          } catch (DatabaseRequestException | DatabaseSchemaException e) {
            // It could be that some other process decided to steal this article
            // and process it first (mainly due to human error).  If so, delete
            // everything and store it again.
            System.out.println("Handling human error: " + url.getUrl());
            Database.with(Article.class).delete(url.getId());
            ArticleKeywords.deleteForUrlIds(ImmutableList.of(url.getId()));
            Links.deleteFromOriginUrlId(ImmutableList.of(url.getId()));

            // Try again!
            Database.insert(interpretedData.getArticle());
          }

          Database.insert(interpretedData.getKeywordList());
          urls = interpretedData.getUrlList();
        } else {
          urls = UrlFinder.findUrls(url);
        }

        // Make sure to filter and clean the URLs - only store the ones we want to crawl!
        Iterable<Url> destinationUrls = Urls.put(
            Iterables.transform(
                Iterables.filter(urls, UrlWhitelist.PREDICATE),
                UrlCleaner.TRANSFORM_FUNCTION),
            false /* isTweet */);
        Links.put(url, destinationUrls);
        Urls.markCrawlFinish(url);

      } catch (DatabaseSchemaException | DatabaseRequestException | BiznessException e) {
        // Internal error (bug in our code).
        e.printStackTrace();
      } catch (FetchException|ParserException|RequiredFieldException e) {
        // Bad article.
        e.printStackTrace();
      }
    }
  }

  public static void main(String args[]) throws Exception {
    new TheMachine().start();
  }
}
