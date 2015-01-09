package com.janknspank;

import java.util.Collection;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.janknspank.common.UrlCleaner;
import com.janknspank.common.UrlWhitelist;
import com.janknspank.data.ArticleKeywords;
import com.janknspank.data.DataInternalException;
import com.janknspank.data.Database;
import com.janknspank.data.Links;
import com.janknspank.data.Urls;
import com.janknspank.data.ValidationException;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.fetch.FetchException;
import com.janknspank.interpreter.Interpreter;
import com.janknspank.interpreter.RequiredFieldException;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.Url;
import com.janknspank.proto.Interpreter.InterpretedData;

public class TheMachine {
  public void start() {
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
      } catch (DataInternalException e) {
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

        InterpretedData interpretedData = Interpreter.interpret(url);
        try {
          Database.insert(interpretedData.getArticle());
        } catch (DataInternalException e) {
          // It could be that some other process decided to steal this article
          // and process it first (mainly due to human error).  If so, delete
          // everything and store it again.
          System.out.println("Handling human error: " + url.getUrl());
          Database.deletePrimaryKey(url.getId(), Article.class);
          ArticleKeywords.deleteForUrlIds(ImmutableList.of(url.getId()));
          Links.deleteFromOriginUrlId(ImmutableList.of(url.getId()));

          // Try again!
          Database.insert(interpretedData.getArticle());
        }

        Database.insert(interpretedData.getKeywordList());

        // Make sure to filter and clean the URLs - only store the ones we want to crawl!
        Collection<Url> destinationUrls = Urls.put(
            Iterables.transform(
                Iterables.filter(interpretedData.getUrlList(), UrlWhitelist.PREDICATE),
                UrlCleaner.TRANSFORM_FUNCTION),
            false /* isTweet */);
        Links.put(url, destinationUrls);
        Urls.markCrawlFinish(url);

      } catch (ValidationException|DataInternalException e) {
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
